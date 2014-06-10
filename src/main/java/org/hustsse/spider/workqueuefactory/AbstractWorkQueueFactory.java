package org.hustsse.spider.workqueuefactory;

import org.hustsse.spider.core.WorkQueue;
import org.hustsse.spider.core.WorkQueueFactory;

/**
 * MemWorkQueue的工厂，负责创建{@link MemWorkQueue}。
 * @author Anderson
 *
 */
public abstract class AbstractWorkQueueFactory implements WorkQueueFactory {
	private PolitenessPolicy politenessPolicy;

	@Override
	public WorkQueue createWorkQueueFor(String workQueueKey) {
		WorkQueue wq = this.createWorkQueueInner(workQueueKey);
		wq.setPolitenessInterval(this.politenessPolicy.getPoliteness(wq));
		return wq;
	}
	
	protected abstract WorkQueue createWorkQueueInner(String workQueueKey);

	public PolitenessPolicy getPolitenessPolicy() {
		return politenessPolicy;
	}

	public void setPolitenessPolicy(PolitenessPolicy politenessPolicy) {
		this.politenessPolicy = politenessPolicy;
	}
}
