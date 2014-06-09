package org.hustsse.spider.handler.crawl.fetcher;

import org.apache.commons.lang3.StringUtils;
import org.hustsse.spider.model.CrawlURL;
import org.hustsse.spider.util.httpcodec.*;

/**
 * Created with IntelliJ IDEA.
 * User: anderson
 * Date: 14-6-7
 * Time: 下午8:12
 * To change this template use File | Settings | File Templates.
 */
public class DefaultHttpRequestBuildPolicy implements HttpRequestBuildPolicy {

    private static DefaultHttpRequestBuildPolicy SINGLETON = new DefaultHttpRequestBuildPolicy();

    private String userAgent =  "Mozilla/5.0 (Windows NT 6.1; rv:16.0) Gecko/20100101 Firefox/16.0";
    private String accept = "text/*";

    @Override
    public HttpRequest buildHttpRequest(CrawlURL url) {
        String  host = url.getURL().getEscapedHost(),
                path = url.getURL().getEscapedPath(),
                query = url.getURL().getEscapedQuery();

        if (StringUtils.isEmpty(path))
            path = "/";
        if (query != null) {
            path += '?';
            path += query;
        }

        // 降级到1.0协议，避免对Chunked解码。毕竟我们在拿到所有数据之后才能进行下一步处理
        HttpRequest httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.GET, path);
        httpRequest.setHeader(HttpHeaders.Names.HOST, host);
        httpRequest.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
        httpRequest.setHeader(HttpHeaders.Names.USER_AGENT, userAgent);
        httpRequest.setHeader(HttpHeaders.Names.ACCEPT, accept);

        prepareRequest(url,httpRequest);

        return httpRequest;
    }

    protected void prepareRequest(CrawlURL url, HttpRequest req){
        // to be overrided by child classes
    }

    private DefaultHttpRequestBuildPolicy(){}

    public static DefaultHttpRequestBuildPolicy getInstance(){
        return DefaultHttpRequestBuildPolicy.SINGLETON;
    }

    // getter setter for http header values

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getAccept() {
        return accept;
    }

    public void setAccept(String accept) {
        this.accept = accept;
    }

}
