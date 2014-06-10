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

	/** 内部容器的初始长度 */
	private static final int DEFAULT_INITIAL_CAPACITY = 1000;

	/** 内部容器 */
	Queue<CrawlURL> urls  = new PriorityBlockingQueue<CrawlURL>(DEFAULT_INITIAL_CAPACITY,new Comparator<CrawlURL>() {
        @Override
        public int compare(CrawlURL o1, CrawlURL o2) {
            // 优先级相等时按入frontier顺序排序
            if (o1.getPriority() == o2.getPriority())
                return o1.getOrdinal() > o2.getOrdinal()?  -1 : 1;

            return o1.getPriority() > o2.getPriority()? -1 : 1;
        }
    });
	/** 元素数目 */
	protected AtomicLong count = new AtomicLong(0);

	public MemWorkQueue(String key, int maxLength) {
		super(key,(long)maxLength);
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
			synchronized (urls) {
				count.decrementAndGet();
				urls.notifyAll();
			}
		}
		return u;
	}

	@Override
	public void setMaxLength(long maxLength) {
		if(Integer.MAX_VALUE < maxLength)
			this.maxLength = Integer.MAX_VALUE;
		else
			this.maxLength = (int) maxLength;
	}

	@Override
	public long count() {
		return count.intValue();
	}

}
