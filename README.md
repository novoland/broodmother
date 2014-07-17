broodmother
===========
```
                  _.._
                .'    '.
               /   __   \
            ,  |   ><   |  ,
           . \  \      /  / .
            \_'--`(  )'--'_/
              .--'/()\'--.
             /  /` '' `\  \
               |        |
                \      /
```

A web crawler.

# Build
在根目录下:
```
mvn clean install 
```

使用方式：解压BroodMother.zip后可以看到如下目录结构：

* `Bin`
Bin目录下只有startup.bat文件，双击即可启动爬虫。
* `Conf`
Conf下存放任务的全局配置文件，如ehcache.xml用于配置dns缓存,redis.properties用于配置RedisWorkQueue。
* `Jobs`
存放所有任务，每一个子目录会在启动时被当做一个爬取任务。每个任务的子目录下都必须有job.xml配置文件，它用于初始化Spring容器。各任务的子目录下又可以有：
    1. `Lib`: 这是任务对BroodMother的扩展，任务的Spring容器启动时将优先从该目录下加载类；
    2. `Conf`: 这是特定任务所需的配置文件，任务启动后将优先从该目录下查找配置文件，如果查找失败则进入BroodMother根目录下的Conf查找。
* `Lib`
存放BroodMother所需的第三方jar包依赖。

Jobs目录中带有一个名为“douban”的示例任务，该任务以若干url为种子进行爬取，将下载的网页输出至job根目录下的page目录。

# 设计
见 [WIKI](https://github.com/novoland/broodmother/wiki/broodmother%E6%95%B4%E4%BD%93%E8%AE%BE%E8%AE%A1)
