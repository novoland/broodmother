package org.hustsse.spider.handler.candidate;

import org.hustsse.spider.framework.HandlerContext;
import org.hustsse.spider.handler.AbstractBeanNameAwareHandler;
import org.hustsse.spider.model.CrawlURL;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 使用FrontierPreparer在CrawlURL被调度进Frontier之前做必要的处理。典型的两个应用场景：
 *
 * <pre>
 * a) prepare the candidate before schedule it into the frontier;
 * b) prepare the seed
 * </pre>
 *
 * @author Anderson
 *
 */
public class FrontierPreparer extends AbstractBeanNameAwareHandler {

	@Autowired(required = false)
	private QueueAssignmentPolicy queueAssignmentPolicy;

	@Autowired(required = false)
    private UrlPriorityPolicy urlPriorityPolicy;

	@Autowired(required = false)
    private CanonicalPolicy canonicalPolicy;

	/**
	 * use default policy，can be changed by Spring Injection
	 */
	public FrontierPreparer() {
		urlPriorityPolicy = new SeedFirstUrlPriorityPolicy();
		queueAssignmentPolicy = new DefaultQueueAssignmentPolicy();
		canonicalPolicy = new DefaultCanonicalPolicy();
	}

	@Override
	public void process(HandlerContext ctx, CrawlURL url) {
		prepare(url);
		ctx.proceed();
	}

	/**
	 * prepare the url before schedule into frontier:
	 *
	 * <pre>
	 * 1. calculate the workqueue key;
	 * 2. set priority for it;
	 * 3. calculate the canonical format string,which will be used by the UriUniqFilter.
	 * </pre>
	 *
	 * @param url
	 */
	public void prepare(CrawlURL url) {
		url.setWorkQueueKey(getWorkQueueKeyFor(url));

		url.setPriority(getPriorityFor(url));

		url.setCanonicalStr(getCanonicalStrFor(url));
	}

	private String getCanonicalStrFor(CrawlURL url) {
		return canonicalPolicy.getCanonicalStrFor(url);
	}

	private int getPriorityFor(CrawlURL url) {
		return urlPriorityPolicy.getPriorityFor(url);
	}

	private String getWorkQueueKeyFor(CrawlURL url) {
		return queueAssignmentPolicy.getWorkQueueKeyFor(url);
	}

    public QueueAssignmentPolicy getQueueAssignmentPolicy() {
        return queueAssignmentPolicy;
    }

    public void setQueueAssignmentPolicy(QueueAssignmentPolicy queueAssignmentPolicy) {
        this.queueAssignmentPolicy = queueAssignmentPolicy;
    }

    public UrlPriorityPolicy getUrlPriorityPolicy() {
        return urlPriorityPolicy;
    }

    public void setUrlPriorityPolicy(UrlPriorityPolicy urlPriorityPolicy) {
        this.urlPriorityPolicy = urlPriorityPolicy;
    }

    public CanonicalPolicy getCanonicalPolicy() {
        return canonicalPolicy;
    }

    public void setCanonicalPolicy(CanonicalPolicy canonicalPolicy) {
        this.canonicalPolicy = canonicalPolicy;
    }
}
