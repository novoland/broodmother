package org.hustsse.spider.handler.crawl.fetcher;

import org.hustsse.spider.handler.AbstractBeanNameAwareHandler;
import org.hustsse.spider.model.CrawlURL;
import org.hustsse.spider.util.httpcodec.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created with IntelliJ IDEA.
 * User: anderson
 * Date: 14-6-7
 * Time: 下午7:52
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractFetcher extends AbstractBeanNameAwareHandler {

    @Autowired
    private HttpRequestBuildPolicy requestBuilder;

    protected HttpRequest buildHttpRequest(CrawlURL url) {
        return requestBuilder.buildHttpRequest(url);
    }

    public HttpRequestBuildPolicy getRequestBuilder() {
        return requestBuilder;
    }

    public void setRequestBuilder(HttpRequestBuildPolicy requestBuilder) {
        this.requestBuilder = requestBuilder;
    }
}
