package org.hustsse.spider.examples.qsbk;

import org.hustsse.spider.model.CrawlJob;

/**
 * Created with IntelliJ IDEA.
 * User: anderson
 * Date: 14-6-8
 * Time: 下午1:28
 * To change this template use File | Settings | File Templates.
 */
public class Main {

    public static void main(String[] args){
        CrawlJob job = new CrawlJob("qiushibaike.info","qsbk.xml");
        job.launch();
    }
}
