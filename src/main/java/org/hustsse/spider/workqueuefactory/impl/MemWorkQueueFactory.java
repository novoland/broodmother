package org.hustsse.spider.workqueuefactory.impl;

import org.hustsse.spider.core.WorkQueue;
import org.hustsse.spider.core.WorkQueueFactory;
import org.hustsse.spider.workqueue.impl.MemWorkQueue;
import org.hustsse.spider.workqueuefactory.AbstractWorkQueueFactory;

/**
 * MemWorkQueue的工厂，负责创建{@link MemWorkQueue}。
 * @author Anderson
 *
 */
public class MemWorkQueueFactory extends AbstractWorkQueueFactory {
	/** 产品的元素上限，创建的MemWorkQueue最大长度都是一样的 */
	private int maxLengthPerWorkQueue;

	public int getMaxLengthPerWorkQueue() {
		return maxLengthPerWorkQueue;
	}

	public void setMaxLengthPerWorkQueue(int maxLengthPerWorkQueue) {
		this.maxLengthPerWorkQueue = maxLengthPerWorkQueue;
	}

	@Override
	public WorkQueue createWorkQueueInner(String workQueueKey) {
		return new MemWorkQueue(workQueueKey, maxLengthPerWorkQueue);
	}

}
