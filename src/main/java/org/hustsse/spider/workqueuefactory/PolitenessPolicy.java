package org.hustsse.spider.workqueuefactory;

import org.hustsse.spider.core.WorkQueue;

/**
 * 为{@link WorkQueue}设置 politeness 时间间隔
 * @author anderson
 *
 */
public interface PolitenessPolicy {

	public long getPoliteness(WorkQueue q);
	
}
