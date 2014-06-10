package org.hustsse.spider.workqueuefactory.policy.politeness;

import org.hustsse.spider.core.WorkQueue;
import org.hustsse.spider.workqueuefactory.PolitenessPolicy;

public class ConstantPolitenessPolicy implements PolitenessPolicy {
	
	/**
	 * ms
	 **/
	private long defaultPoliteness;

	@Override
	public long getPoliteness(WorkQueue q) {
		return defaultPoliteness;
	}

	public long getDefaultPoliteness() {
		return defaultPoliteness;
	}

	public void setDefaultPoliteness(long defaultPoliteness) {
		this.defaultPoliteness = defaultPoliteness;
	}

}
