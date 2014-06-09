package org.hustsse.spider.workqueue.factory;

import org.hustsse.spider.framework.WorkQueue;
import org.hustsse.spider.framework.WorkQueueFactory;
import org.hustsse.spider.workqueue.MemWorkQueue;

/**
 * MemWorkQueue的工厂，负责创建{@link MemWorkQueue}。
 * @author Anderson
 *
 */
public class MemWorkQueueFactory implements WorkQueueFactory {
	/** 产品的元素上限，创建的MemWorkQueue最大长度都是一样的 */
	private int maxLengthPerWorkQueue;

	public int getMaxLengthPerWorkQueue() {
		return maxLengthPerWorkQueue;
	}

	public void setMaxLengthPerWorkQueue(int maxLengthPerWorkQueue) {
		this.maxLengthPerWorkQueue = maxLengthPerWorkQueue;
	}

	@Override
	public WorkQueue createWorkQueueFor(String workQueueKey) {
		return new MemWorkQueue(workQueueKey, maxLengthPerWorkQueue);
	}

}
