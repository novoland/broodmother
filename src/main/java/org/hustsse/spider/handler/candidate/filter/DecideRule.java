package org.hustsse.spider.handler.candidate.filter;

import org.hustsse.spider.model.CrawlURL;

/**
 * test a CrawlURL
 *
 * @author Anderson
 *
 */
public interface DecideRule {
	DecideResult test(CrawlURL url);
}
