package org.hustsse.spider.handler.crawl.fetcher.nio;

import org.hustsse.spider.exception.BossException;
import org.hustsse.spider.framework.Pipeline;
import org.hustsse.spider.model.CrawlURL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

import static org.hustsse.spider.handler.crawl.fetcher.nio.NioConstants._CONNECT_ATTEMPT_MILLIS;
import static org.hustsse.spider.handler.crawl.fetcher.nio.NioConstants._CONNECT_DEADLINE_NANOS;
import static org.hustsse.spider.handler.crawl.fetcher.nio.NioConstants._CONNECT_SUCCESS_MILLIS;
import static org.hustsse.spider.model.CrawlURL.FETCH_FAILED;

/**
 * Boss，监听未能一次性连接成功的CHannel的OP_CONNECT，在成功连接后移交给Reactor， 同时负责超时控制。
 *
 * @author novo
 */
class MainReactor implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(MainReactor.class);
    private Executor bossExecutor;
    private int index;
    private Queue<Runnable> registerQueue = new LinkedBlockingQueue<Runnable>();
    private Selector selector;
    private volatile Boolean started;
    private String threadName;
    private long lastConnectTimeoutCheckTimeNanos;
    private NioFetcher nioFetcher;

    public MainReactor(NioFetcher nioFetcher, Executor bossExecutor, int i) {
        this.nioFetcher = nioFetcher;
        this.bossExecutor = bossExecutor;
        this.index = i;
        threadName = "New I/O boss线程 #" + index;
    }

    /**
     * fail the url fetch, cancel the key registion and close the channel,
     * and resume the pipeline.
     *
     * @param k
     */
    private void failAndResumePipeline(SelectionKey k) {
        CrawlURL url = (CrawlURL) k.attachment();
        url.setFetchStatus(FETCH_FAILED);
        k.cancel();
        try {
            k.channel().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        url.getPipeline().resume(Pipeline.EMPTY_MSG);
    }

    @Override
    public void run() {
        Thread.currentThread().setName(threadName);
        while (started) {
            try {
                int selectedKeyCount = selector.select(500);
                processRegisterTaskQueue();
                if (selectedKeyCount > 0) {
                    processSelectedKeys(selector.selectedKeys());
                }

                // 超时处理
                long currentTimeNanos = System.nanoTime();
                if (currentTimeNanos - lastConnectTimeoutCheckTimeNanos >= 500 * 1000000L) {
                    lastConnectTimeoutCheckTimeNanos = currentTimeNanos;
                    processConnectTimeout(selector.keys(), currentTimeNanos);
                }

                // TODO 关闭

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            if(selector != null)
                selector.close();
        } catch (IOException e) {
            // ignore
        }
    }

    /**
     * 处理超时连接
     *
     * @param keys
     * @param currentTimeNanos
     */
    private void processConnectTimeout(Set<SelectionKey> keys, long currentTimeNanos) {
        for (Iterator<SelectionKey> i = keys.iterator(); i.hasNext(); ) {
            SelectionKey k = i.next();
            CrawlURL u = (CrawlURL) k.attachment();
            Long connectDeadlineNanos = (Long) u.getAttr(_CONNECT_DEADLINE_NANOS);
            if (connectDeadlineNanos > 0 && currentTimeNanos > connectDeadlineNanos) {
                int duration = getConAttemptDuration(u);
                logger.debug("连接服务器超时，距尝试连接时刻(s)：{},url：{},重试次数：{}", new Object[]{duration, u, u.getRetryTimes()});
                // cancel the key and close the channel
                failAndResumePipeline(k);
            }
        }
    }

    private int getConAttemptDuration(CrawlURL u) {
        long now = System.currentTimeMillis();
        long duration = (now - (Long) (u.getAttr(_CONNECT_ATTEMPT_MILLIS))) / 1000;
        return (int) duration;
    }

    /**
     * Boss处理Select出来的key，注册到Boss的Channel只会注册OP_CONNECT， 在该方法内部调用
     * {@link java.nio.channels.SocketChannel#finishConnect()}完成连接的剩余
     * 步骤，如果成功连接，则将该Channel从Boss移入Reactor；否则取消注册，关闭 Channel，（并做些日志记录？）
     *
     * @param selectedKeys
     */
    private void processSelectedKeys(Set<SelectionKey> selectedKeys) {
        for (Iterator<SelectionKey> i = selectedKeys.iterator(); i.hasNext(); ) {
            SelectionKey key = i.next();
            i.remove();

            SocketChannel channel = (SocketChannel) key.channel();
            CrawlURL url = (CrawlURL) key.attachment();
            if (key.isConnectable()) {
                boolean isConnected = false;

                // try to finish connect,
                // 如果超时，抛出异常：java.net.ConnectException: Connection timed out
                // WIN7下默认超时时间经测试在20s左右
                // 在连接数很多时超时现象严重
                try {
                    isConnected = channel.finishConnect();
                } catch (IOException e2) {
                    // connect timed out
                    logger.debug("连接服务器超时，距尝试连接时刻(s)：{},url：{},重试次数：{}",
                            new Object[]{getConAttemptDuration(url), url, url.getRetryTimes(), e2});
                    failAndResumePipeline(key);
                    return;
                }

                // connect successed
                if (isConnected) {
                    url.setAttr(_CONNECT_SUCCESS_MILLIS, System.currentTimeMillis());
                    key.cancel();
                    try {
                        nioFetcher.sendHttpRequest(channel, url);
                        nioFetcher.nextReactor().register(channel, url);
                    } catch (IOException e) {
                        // send http request failed
                        logger.debug("发送http请求失败，url：{},重试次数：{}", new Object[]{url, url.getRetryTimes(), e});
                        failAndResumePipeline(key);
                    }
                    return;
                }

                // connect failed，较少见
                logger.debug("连接服务器失败，url：{},重试次数：{}", new Object[]{url, url.getRetryTimes()});
                failAndResumePipeline(key);
            }
        }
    }

    private void processRegisterTaskQueue() {
        while (true) {
            Runnable task = registerQueue.poll();
            if (task == null)
                break;
            task.run();
        }
    }

    /**
     * 注册一个Channel到Boss上，监听OP_CONNECT状态。如果Boss未启动，则 提交到BossExecutor中执行。
     *
     * @param channel
     * @param uri
     */
    void register(SocketChannel channel, CrawlURL uri) {
        if (started == null || !started) {
            // Open a selector if this boss didn't start yet.
            try {
                this.selector = Selector.open();
            } catch (Throwable t) {
                throw new BossException("Failed to create a selector.", t);
            }
            bossExecutor.execute(this);
            started = true;
        }
        assert started;
        RegisterTask task = new RegisterTask(channel, this, uri);
        registerQueue.offer(task);
    }

    public void stop() {
        this.started = false;
    }

    /**
     * Register Task for Boss
     *
     * @author Administrator
     */
    private static class RegisterTask implements Runnable {
        SocketChannel channel;
        MainReactor boss;
        CrawlURL uri;

        public RegisterTask(SocketChannel channel, MainReactor boss, CrawlURL uri) {
            this.channel = channel;
            this.boss = boss;
            this.uri = uri;
        }

        @Override
        public void run() {
            try {
                channel.register(boss.selector, SelectionKey.OP_CONNECT, uri);
            } catch (ClosedChannelException e) {
                e.printStackTrace();
            }
        }
    }
}
