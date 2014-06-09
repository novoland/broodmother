package org.hustsse.spider.handler.crawl.fetcher.nio;

import static org.hustsse.spider.handler.crawl.fetcher.nio.NioConstants.DEFAULT_CONNECT_TIMEOUT_MILLIS;
import static org.hustsse.spider.handler.crawl.fetcher.nio.NioConstants.WRITE_SPIN_COUNT;
import static org.hustsse.spider.handler.crawl.fetcher.nio.NioConstants._CONNECT_ATTEMPT_MILLIS;
import static org.hustsse.spider.handler.crawl.fetcher.nio.NioConstants._CONNECT_DEADLINE_NANOS;
import static org.hustsse.spider.handler.crawl.fetcher.nio.NioConstants._CONNECT_SUCCESS_MILLIS;
import static org.hustsse.spider.handler.crawl.fetcher.nio.NioConstants._RAW_RESPONSE;
import static org.hustsse.spider.handler.crawl.fetcher.nio.NioConstants._REQUEST_ALREADY_SEND_SIZE;
import static org.hustsse.spider.handler.crawl.fetcher.nio.NioConstants._REQUEST_BUFFER;
import static org.hustsse.spider.handler.crawl.fetcher.nio.NioConstants._REQUEST_SEND_FINISHED;
import static org.hustsse.spider.handler.crawl.fetcher.nio.NioConstants._REQUEST_SEND_TIMES;
import static org.hustsse.spider.handler.crawl.fetcher.nio.NioConstants._REQUEST_SIZE;
import static org.hustsse.spider.model.CrawlURL.FETCH_FAILED;
import static org.hustsse.spider.model.CrawlURL.FETCH_ING;
import static org.hustsse.spider.model.CrawlURL.FETCH_SUCCESSED;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.hustsse.spider.framework.HandlerContext;
import org.hustsse.spider.handler.crawl.fetcher.AbstractFetcher;
import org.hustsse.spider.model.CrawlURL;
import org.hustsse.spider.util.HttpMessageUtil;
import org.hustsse.spider.util.httpcodec.HttpRequest;
import org.hustsse.spider.util.httpcodec.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

/**
 * 下载器，用于下载网页，基于JDK的NIO实现，没有用Netty等框架，遵循了Reactor模式，这一点是模仿Netty的。
 * <p/>
 * <h1>Reactor模式</h1>
 * 两种角色：Boss和Reactor，Boss负责监听连接的Connect状态，当连接上时将其转移注册到Reactor，后者负责连接的读写。
 * <p/>
 * <h1>Pipeline pause&resume机制的使用</h1>
 * 当连接注册到Boss时，NioFetcher会将Pipeline挂起(pause)，NioFetcher将成为断点；
 * 当Reactor接收到数据后resume
 * Pipeline，并将数据当做Message传递至Pipeline，Pipeline从上次断点处继续运行，重新进入NioFetcher
 * ；若CrawlURL并未爬取完 ，NioFetcher保存当前数据片并继续挂起pipeline，等待下次被Reactor
 * resume；若CrawlURL数据读取完毕，则对所有数据进行合并、解析，并在最后proceed
 * pipeline，CrawlURL流入下一个handler被处理。
 * <p/>
 * <p/>
 * TODO：提供OIO/Netty/httpclient的版本。
 *
 * @author Anderson
 */
public class NioFetcher extends AbstractFetcher implements ApplicationListener {
    private static final Logger logger = LoggerFactory.getLogger(NioFetcher.class);
    /**
     * 默认Reactor数量，=处理器核数*2
     */
    static final int DEFAULT_REACTOR_NUMS = Runtime.getRuntime().availableProcessors() * 2;
    /**
     * 默认Boss数量
     */
    static final int DEFAULT_BOSS_NUMS = 1;
    private SubReactor[] subReactors;
    private MainReactor[] mainReactors;
    private int curReactorIndex;
    private int curBossIndex;

    /**
     * Boss线程池
     */
    @SuppressWarnings("unused")
    private Executor bossExecutor;
    /**
     * Reactor线程池
     */
    @SuppressWarnings("unused")
    private Executor reactorExecutor;

    public NioFetcher(Executor bossExecutor, Executor reactorExecutor) {
        this(bossExecutor, reactorExecutor, DEFAULT_BOSS_NUMS, DEFAULT_REACTOR_NUMS);
    }

    public NioFetcher(Executor bossExecutor, Executor reactorExecutor, int reactorCount) {
        this(bossExecutor, reactorExecutor, DEFAULT_BOSS_NUMS, reactorCount);
    }

    // 快捷方式，使用的线程池都是 newCached 的
    public NioFetcher(int reactorCount) {
        this(Executors.newCachedThreadPool(), Executors.newCachedThreadPool(), DEFAULT_BOSS_NUMS, reactorCount);
    }

    public NioFetcher() {
        this(Executors.newCachedThreadPool(), Executors.newCachedThreadPool(), DEFAULT_BOSS_NUMS, DEFAULT_REACTOR_NUMS);
    }

    // bossCount暂时不开放，默认为1
    private NioFetcher(Executor bossExecutor, Executor reactorExecutor, int bossCount, int reactorCount) {
        this.bossExecutor = bossExecutor;
        this.reactorExecutor = reactorExecutor;

        subReactors = new SubReactor[reactorCount];
        mainReactors = new MainReactor[bossCount];
        for (int i = 0; i < reactorCount; i++) {
            subReactors[i] = new SubReactor(reactorExecutor, i);
        }
        for (int i = 0; i < bossCount; i++) {
            mainReactors[i] = new MainReactor(this, bossExecutor, i);
        }
    }

    @Override
    public void process(HandlerContext ctx, CrawlURL url) {
        Object msg = url.getPipeline().getMessage();

        if (msg == null)
            // 初始进入NioFetcher，并未注册到Boss或Reactor。
            connectAndFetch(ctx, url);
        else
            // resumed
            pipelineResumed(ctx, url, (ByteBuffer) msg);
    }

    private void pipelineResumed(HandlerContext ctx, CrawlURL url, ByteBuffer msg) {
        int fetchStatus = url.getFetchStatus();
        switch (fetchStatus) {
            // 抓取失败，重试并finish pipeline
            case FETCH_FAILED:
                url.setNeedRetry(true);
                ctx.finish();
                return;

            // 抓取未完成，保存数据片并pause pipeline
            case FETCH_ING:
                appendToSegList((ByteBuffer) msg, url);
                url.getPipeline().clearMessage();
                ctx.pause();
                return;

            // 抓取成功，合并数据片并解析成http响应，proceed pipeline
            case FETCH_SUCCESSED:
                ByteBuffer segment = (ByteBuffer) msg;
                List<ByteBuffer> responseSegments = appendToSegList(segment, url);
                ByteBuffer merged = merge(responseSegments);
                merged.flip(); // 设置为写出模式
                /*
                 * raw http
                 * response(ByteBuffer形式，未解析)和解析之后的HttpResponse（但是Content依然是ByteBuffer形式
                 * ）， are backed by the SAME buffer,be careful to modify them
                 */
                url.setAttr(_RAW_RESPONSE, merged); // 后续processor会用到原始http响应么？不需要的话可以删除之
                HttpResponse response = HttpMessageUtil.decodeHttpResponse(merged.duplicate());
                url.setResponse(response);
                // TODO 对content生成消息摘要，用于对内容判重

                ctx.proceed();
                return;

            default:
                return;
        }
    }

    private void connectAndFetch(HandlerContext ctx, CrawlURL url) {

        SocketChannel channel = null;
        try {
            channel = SocketChannel.open();
            channel.configureBlocking(false);
        } catch (IOException e) {
            logger.debug("发送http请求失败，url：{},重试次数：{}", new Object[]{url, url.getRetryTimes(), e});
            fail(ctx, url);
            return;
        }


        // 构造服务器地址，DNS已经在上一级的DnsResolver中被解析
        SocketAddress reomteAddress = new InetSocketAddress(url.getDns().getIp(), url.getURL().getPort());

        // 发起connect请求，若立即connect成功，成功则注册到Reactor
        boolean immediatelyConnect;
        try {
            immediatelyConnect = channel.connect(reomteAddress);
        } catch (IOException e) {
            logger.debug("连接失败，url：{},重试次数：{}", new Object[]{url, url.getRetryTimes(), e});
            fail(ctx, url);
            return;
        }

        if (immediatelyConnect) {
            url.setAttr(_CONNECT_SUCCESS_MILLIS, System.currentTimeMillis());
            // 连接成功后立刻发送http请求。考虑到Http请求一般不会太大（GET），目前的处理方式是一旦连接上立刻发送并假设一定可以发送成功
            try {
                sendHttpRequest(channel, url);
            } catch (IOException e) {
                logger.debug("发送http请求失败，url：{},重试次数：{}", new Object[]{url, url.getRetryTimes(), e});
                fail(ctx, url);
                return;
            }
            ctx.pause();
            nextReactor().register(channel, url);
        } else {
            // 立即connect失败则注册到Boss，监听其OP_CONNECT状态
            url.setAttr(_CONNECT_ATTEMPT_MILLIS, System.currentTimeMillis());
            long curNano = System.nanoTime();
            // 设置超时时刻为当前时间+NioConstants.DEFAULT_CONNECT_TIMEOUT_MILLIS
            url.setAttr(_CONNECT_DEADLINE_NANOS, curNano + DEFAULT_CONNECT_TIMEOUT_MILLIS * 1000 * 1000L);
            // pause before register，boss/reactor线程可能在pause之前resume
            ctx.pause();
            nextBoss().register(channel, url);
        }
    }

    private void fail(HandlerContext ctx, CrawlURL url) {
        url.setFetchStatus(FETCH_FAILED);
        ctx.finish();
    }

    /**
     * 将读到的Http响应片段添加到URI对应的segment list末尾，segment list作为handler attr保存在
     * {@link CrawlURL#attrs}中，key为{@link NioConstants#_RAW_RESPONSE}。
     *
     * @param segment      http响应数据片
     * @param uriProcessed
     * @return
     */
    @SuppressWarnings("unchecked")
    private List<ByteBuffer> appendToSegList(ByteBuffer segment, CrawlURL uriProcessed) {
        Object val = uriProcessed.getAttr(_RAW_RESPONSE);
        List<ByteBuffer> responseSegments;
        if (val == null) {
            responseSegments = new LinkedList<ByteBuffer>();
        } else {
            responseSegments = (List<ByteBuffer>) val;
        }
        responseSegments.add(segment);
        uriProcessed.setAttr(_RAW_RESPONSE, responseSegments);
        return responseSegments;
    }

    /**
     * merge http响应数据片。
     * <p/>
     * TODO:Netty的CompositeChannelBuffer性能更好。
     *
     * @param buffers
     * @return
     */
    private ByteBuffer merge(List<ByteBuffer> buffers) {
        int size = 0;
        for (ByteBuffer buffer : buffers) {
            size += buffer.position();
        }
        ByteBuffer merged = ByteBuffer.allocate(size);
        for (ByteBuffer buffer : buffers) {
            buffer.flip();
            merged.put(buffer);
        }
        return merged;
    }

    /**
     * 组装并发送http请求，网络情况差时不一定能一次性发送完毕。
     *
     * @param channel
     * @param url
     * @throws IOException
     */
    public void sendHttpRequest(SocketChannel channel, CrawlURL url) throws IOException {
        HttpRequest httpRequest = super.buildHttpRequest(url);

        ByteBuffer reqBuffer = HttpMessageUtil.encodeHttpRequest(httpRequest);
        // 一些统计信息
        url.setAttr(_REQUEST_SIZE, reqBuffer.capacity());
        url.setAttr(_REQUEST_ALREADY_SEND_SIZE, 0);
        url.setAttr(_REQUEST_SEND_TIMES, 0);

        // 发送http request
        // 为了防止在网络高负载情况下无法写入Socket内核缓冲区（几乎不会发生，毕竟http请求一般很小），自旋若干次。
        int writtenBytes = 0;
        for (int i = WRITE_SPIN_COUNT; i > 0; i--) {
            writtenBytes = channel.write(reqBuffer);
            if (writtenBytes != 0) {
                break;
            }
        }
        // 99%的情况都会一次性发送完毕，不会注册到reactor上，但是以防万一还是做点处理。
        boolean reqSendFinished = !reqBuffer.hasRemaining();
        url.setAttr(_REQUEST_SEND_FINISHED, reqSendFinished);
        // save the request buffer for next sending
        if (!reqSendFinished) {
            url.setAttr(_REQUEST_BUFFER, reqBuffer);
        }
    }

    /**
     * 获取下一个使用的Boss，round-robbin方式使用所有boss
     *
     * @return
     */
    public MainReactor nextBoss() {
        return mainReactors[curBossIndex++ % mainReactors.length];
    }

    /**
     * 获取下一个使用的Reactor，round-robbin方式使用所有reactor
     *
     * @return
     */
    public SubReactor nextReactor() {
        return subReactors[curReactorIndex++ % subReactors.length];
    }

    public void stop(){
        for(MainReactor r : mainReactors)
            r.stop();
        for(SubReactor r : subReactors)
            r.stop();
    }

    @Override
    /**
     * job退出时将关闭spring容器，NioFetcher监听容器关闭事件并关闭自己。
     */
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if(applicationEvent instanceof ContextClosedEvent)
            this.stop();
    }
}
