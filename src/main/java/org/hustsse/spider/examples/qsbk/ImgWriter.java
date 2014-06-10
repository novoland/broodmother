package org.hustsse.spider.examples.qsbk;

import org.hustsse.spider.core.HandlerContext;
import org.hustsse.spider.handler.crawl.writer.SimpleFileWriter;
import org.hustsse.spider.model.CrawlURL;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created with IntelliJ IDEA.
 * User: anderson
 * Date: 14-6-8
 * Time: 下午2:32
 * To change this template use File | Settings | File Templates.
 */
public class ImgWriter extends SimpleFileWriter {

    private AtomicLong count = new AtomicLong(0);

    private long maxImgNum;

    private File baseImgDir;

    // 只有URL符合该模式；
    private String tumblrImgUrlPattern;
    // 且从符合下列模式的页面导航而来的照片才会被下载
    private List<String> viaUrlPatterns;

    @Override
    public void process(HandlerContext ctx, CrawlURL url) {

        // 非图片不写磁盘
        if (!shouldDownload(url)){
            ctx.proceed();
            return;
        }

        // 抓取任务完毕
        if(count.incrementAndGet() == maxImgNum){
            ctx.getController().stop();
        }

        // 写图片到磁盘
        super.process(ctx, url);
    }

    private boolean shouldDownload(CrawlURL url) {
        // 不是tumblr的图片
        if(!url.toString().matches(tumblrImgUrlPattern)){
            return false;
        }

        for(String pattern: viaUrlPatterns){
            if(url.getVia().toString().matches(pattern))
                return true;
        }
        return false;
    }

    @Override
    protected String genFilePath(HandlerContext ctx, CrawlURL url) {
        String suffix = getFileSuffix(url);
        return new File(baseImgDir,count.toString() + "." + suffix).getAbsolutePath();
    }

    private String getFileSuffix(CrawlURL url) {
        String[] a = url.getURL().getPath().toString().split("\\.");
        return a[a.length - 1];
    }

    public File getBaseImgDir() {
        return baseImgDir;
    }

    public void setBaseImgDir(File baseImgDir) {
        this.baseImgDir = baseImgDir;
    }

    public long getMaxImgNum() {
        return maxImgNum;
    }

    public void setMaxImgNum(long maxImgNum) {
        this.maxImgNum = maxImgNum;
    }

    public String getTumblrImgUrlPattern() {
        return tumblrImgUrlPattern;
    }

    public void setTumblrImgUrlPattern(String imgPattern) {
        this.tumblrImgUrlPattern = imgPattern;
    }

    public List<String> getViaUrlPatterns() {
        return viaUrlPatterns;
    }

    public void setViaUrlPatterns(List<String> viaUrlPatterns) {
        this.viaUrlPatterns = viaUrlPatterns;
    }
}
