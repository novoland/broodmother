package org.hustsse.spider.handler.candidate.preparer.policy.priority;

import org.hustsse.spider.handler.candidate.preparer.UrlPriorityPolicy;
import org.hustsse.spider.model.CrawlURL;

/**
 * 默认的URL优先级策略，种子优先级最高（HIGHEST），其余的均为NORMAL。
 *
 * @author Anderson
 *
 */
public class SeedFirstUrlPriorityPolicy implements UrlPriorityPolicy {

	@Override
	public int getPriorityFor(CrawlURL url) {
		if (url.isSeed()) {
			return CrawlURL.HIGHEST;
		}
		return getPriorityForNonSeed(url);
	}

    public int getPriorityForNonSeed(CrawlURL url){
        return CrawlURL.NORMAL;
    }

}
