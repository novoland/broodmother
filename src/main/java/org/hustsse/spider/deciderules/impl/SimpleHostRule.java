package org.hustsse.spider.deciderules.impl;

import org.hustsse.spider.deciderules.DecideResult;
import org.hustsse.spider.deciderules.DecideRule;
import org.hustsse.spider.model.CrawlURL;

/**
 * 根据url中的host过滤url，简单粗暴
 *
 * @author Anderson
 *
 */
public class SimpleHostRule implements DecideRule {

    private String allowedHost;

	@Override
	public DecideResult test(CrawlURL url) {
		return url.getURL().getHost().equals(allowedHost)? DecideResult.PASS:DecideResult.REJECT;
	}

    public String getAllowedHost() {
        return allowedHost;
    }

    public void setAllowedHost(String allowedHost) {
        this.allowedHost = allowedHost;
    }
}
