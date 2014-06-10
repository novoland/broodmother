package org.hustsse.spider.core;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * WorkQueue的Wrapper，含有Delay信息，配合{@link DelayQueue}使用实现WorkQueue的Politeness
 * Interval。
 *
 * @author Anderson
 *
 */
public class DelayedWorkQueue implements Delayed {

	WorkQueue wq;
	/** 唤醒时刻,nanoSecond */
	long wakeTime;

	/**
	 *
	 * @param wq
	 *            work queue
	 * @param delay
	 *            多久之后被唤醒，单位ms
	 */
	public DelayedWorkQueue(WorkQueue wq, long delay) {
		this.wq = wq;
		this.wakeTime = System.currentTimeMillis() + delay;
	}

	@Override
	public int compareTo(Delayed o) {
		if (o == this)
			return 0;
		DelayedWorkQueue other = (DelayedWorkQueue) o;
		long d = wakeTime - other.getWakeTime();
		return (d == 0) ? 0 : ((d < 0) ? -1 : 1);
	}

	@Override
	public long getDelay(TimeUnit unit) {
		return unit.convert(wakeTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
	}

	public WorkQueue getWorkQueue() {
		return wq;
	}

	public long getWakeTime() {
		return wakeTime;
	}

}
