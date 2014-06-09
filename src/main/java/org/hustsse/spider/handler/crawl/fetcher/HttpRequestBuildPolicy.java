package org.hustsse.spider.handler.crawl.fetcher;

import org.hustsse.spider.model.CrawlURL;
import org.hustsse.spider.util.httpcodec.HttpRequest;

/**
 * Created with IntelliJ IDEA.
 * User: anderson
 * Date: 14-6-7
 * Time: 下午8:11
 * To change this template use File | Settings | File Templates.
 */
public interface HttpRequestBuildPolicy {

    public HttpRequest buildHttpRequest(CrawlURL url);

}
