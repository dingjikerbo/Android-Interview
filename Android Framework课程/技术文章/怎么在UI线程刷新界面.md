

参考这两篇文章
https://blog.csdn.net/shangsxb/article/details/78898318
https://www.2cto.com/kf/201608/541586.html
https://ivanljt.github.io/blog/2017/08/16/Android%20中子线程真的不能更新UI吗/
https://juejin.im/entry/584f7078ac502e006937a392

ViewRootImpl负责和WMS通信，ViewRootImpl在应用端，真正负责显示其实是在系统端，所以系统端不care应用端摆放控件是在哪个线程，你们自己玩好就行，最后一起丢过来。

但是从系统设计者的角度，如果大家都各玩各的线程，就乱了。所以不如定一个规范，大家都老老实实在主线程刷新UI。


我们要考虑两件事情，
一个是能否在子线程刷新UI
一个是能否在子线程收到事件


https://developer.android.com/studio/write/annotations.html#thread-annotations