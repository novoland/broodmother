package org.hustsse.spider.examples.qsbk;

import org.hustsse.spider.deciderules.DecideResult;
import org.hustsse.spider.deciderules.impl.SimpleHostRule;
import org.hustsse.spider.model.CrawlURL;

/**
 * Created with IntelliJ IDEA.
 * User: anderson
 * Date: 14-6-8
 * Time: 下午8:44
 * To change this template use File | Settings | File Templates.
 */
public class QsbkDecideRule extends SimpleHostRule {

    private String tumblrImgUrlPattern;

    @Override
    public DecideResult test(CrawlURL url) {
        if(super.test(url) == DecideResult.PASS)
            return DecideResult.PASS;
        if(url.getURL().toString().matches(tumblrImgUrlPattern))
            return DecideResult.PASS;
        return DecideResult.REJECT;
    }

    public String getTumblrImgUrlPattern() {
        return tumblrImgUrlPattern;
    }

    public void setTumblrImgUrlPattern(String tumblrImgPathPattern) {
        this.tumblrImgUrlPattern = tumblrImgPathPattern;
    }
}
