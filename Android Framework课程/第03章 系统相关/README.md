# 第三章 系统启动

### 3.1 谈谈你对zygote的理解？
初级：是否知道zygote的作用？
中级：是否了解zygote是怎么启动的？    
高级：是否了解zygote创建进程的实现机制
可以提一提Resource

### 3.2 说一说Android系统的启动？
初级：知道Android有哪些主要系统进程，了解其作用
中级：是否了解大概的Android系统启动流程
高级：是否了解一些核心进程的启动原理和工作原理
说一下systemServer启动，一些关键服务的启动
AMS启动，
关于launcher的启动？

### 3.3 你知道怎么添加一个系统服务吗？
SystemServer里的服务
说说serviceManager的启动流程
如果是单独进程的系统服务，在init.rc里和servicemanager一起启动，怎么保证添加的时候sm已经准备好了
单独进程的服务还是systemServer里的进程，单独进程的话还要在init.rc里配置
这两个可不一样，但也有共同点，单独进程的话要自己启用ipc，而且因为不是zygote fork出来的，没有虚拟机，系统类库等等，这种纯粹是native的系统服务就不用虚拟机，系统类库，主题资源了，单独cpp实现就好了。
服务可以是双向的
应用层怎么用这个系统服务呢？缓存






