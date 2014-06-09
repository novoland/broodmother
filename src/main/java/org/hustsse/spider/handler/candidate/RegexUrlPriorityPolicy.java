package org.hustsse.spider.handler.candidate;

import org.hustsse.spider.model.CrawlURL;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: anderson
 * Date: 14-6-8
 * Time: 下午2:21
 * To change this template use File | Settings | File Templates.
 */
public class RegexUrlPriorityPolicy extends SeedFirstUrlPriorityPolicy {

    private List<Pattern> lowestPatterns;
    private List<Pattern> highestPatterns;
    private List<Pattern> lowPatterns;
    private List<Pattern> highPatterns;

    @Override
    public int getPriorityForNonSeed(CrawlURL url) {

        if( !CollectionUtils.isEmpty(highestPatterns) && matchAny(url,highestPatterns))
            return CrawlURL.HIGHEST;
        if( !CollectionUtils.isEmpty(highPatterns) && matchAny(url,highPatterns))
            return CrawlURL.HIGH;
        if( !CollectionUtils.isEmpty(lowPatterns) && matchAny(url,lowPatterns))
            return CrawlURL.LOW;
        if( !CollectionUtils.isEmpty(lowestPatterns) && matchAny(url,lowestPatterns))
            return CrawlURL.LOWEST;

        return CrawlURL.NORMAL;
    }

    private boolean matchAny(CrawlURL url, List<Pattern> patterns) {
        for (Pattern p : patterns){
            if(p.matcher(url.toString()).find())
                return true;
        }
        return false;
    }

    public List<Pattern> getLowestPatterns() {
        return lowestPatterns;
    }

    public void setLowestPatterns(List<Pattern> lowestPatterns) {
        this.lowestPatterns = lowestPatterns;
    }

    public List<Pattern> getHighestPatterns() {
        return highestPatterns;
    }

    public void setHighestPatterns(List<Pattern> highestPatterns) {
        this.highestPatterns = highestPatterns;
    }

    public List<Pattern> getLowPatterns() {
        return lowPatterns;
    }

    public void setLowPatterns(List<Pattern> lowPatterns) {
        this.lowPatterns = lowPatterns;
    }

    public List<Pattern> getHighPatterns() {
        return highPatterns;
    }

    public void setHighPatterns(List<Pattern> highPatterns) {
        this.highPatterns = highPatterns;
    }
}
