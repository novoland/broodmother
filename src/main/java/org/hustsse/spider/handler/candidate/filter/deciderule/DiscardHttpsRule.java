package org.hustsse.spider.handler.candidate.filter.deciderule;

import org.hustsse.spider.handler.candidate.filter.DecideResult;
import org.hustsse.spider.handler.candidate.filter.DecideRule;
import org.hustsse.spider.model.CrawlURL;

/**
 * 很遗憾，我们暂时还不支持https协议。
 *
 * @author Anderson
 *
 */
public class DiscardHttpsRule implements DecideRule {

	@Override
	public DecideResult test(CrawlURL url) {
		if (url.getURL().getProtocol().toLowerCase().equals("https"))
			return DecideResult.REJECT;
		return DecideResult.PASS;
	}

}
