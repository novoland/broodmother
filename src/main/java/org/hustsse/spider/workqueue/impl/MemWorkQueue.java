package org.hustsse.spider.workqueue.impl;

import java.util.Comparator;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.hustsse.spider.exception.EnqueueFailedException;
import org.hustsse.spider.exception.OverFlowException;
import org.hustsse.spider.model.CrawlURL;
import org.hustsse.spider.workqueue.AbstractWorkQueue;

/**
 * workqueue基于内存的实现，based on j.u.c中的PriorityBlockingQueue
 *
 * @author Anderson
 *
 */
public class MemWorkQueue extends AbstractWorkQueue {

	/** 内部容器 */
	Queue<CrawlURL> urls;
	/** 元素数目 */
	protected AtomicLong count = new AtomicLong(0);

	public MemWorkQueue(String key, int maxLength) {
		super(key,maxLength);

        urls = new PriorityBlockingQueue<CrawlURL>(
                maxLength > 0?maxLength:Integer.MAX_VALUE,
                new Comparator<CrawlURL>() {
            @Override
            public int compare(CrawlURL o1, CrawlURL o2) {
                // 优先级相等时按入frontier顺序排序
                if (o1.getPriority() == o2.getPriority())
                    return o1.getOrdinal() > o2.getOrdinal()?  -1 : 1;

                return o1.getPriority() > o2.getPriority()? -1 : 1;
            }
        });
	}

	@Override
	public void enqueue(CrawlURL u){
		// unbound queue
		if(maxLength <= 0){
			urls.add(u);
			count.incrementAndGet();
			return;
		}
		
		// bounded queue
		synchronized (urls) {
			while (count.intValue() >= maxLength && maxLength > 0) {
				try {
					urls.wait();
				} catch (InterruptedException e) {
				}
			}
			urls.add(u);
			count.incrementAndGet();
		}
	}

	@Override
	public CrawlURL dequeue() {
		CrawlURL u = urls.poll();
		if (u != null){
            // unbound queue
            if(maxLength <= 0){
                count.decrementAndGet();
                return u;
            }
            // bounded queue
			synchronized (urls) {
				count.decrementAndGet();
				urls.notifyAll();
			}
		}
		return u;
	}

	@Override
	public int count() {
		return count.intValue();
	}

}
