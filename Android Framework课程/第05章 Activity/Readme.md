# 5.1 说说应用的冷启动流程


# 5.2 应用的UI线程是怎么启动的？
谈谈你对应用UI线程的理解？
https://developer.android.com/studio/write/annotations.html#thread-annotations
什么是应用的UI线程，是UI绘制所在的线程么，还是应用的主线程。那么UI线程有什么特点，
应用的UI线程是怎么启动的？UI线程是应用进程的主线程么？是，但是可以不用是。
只有UI线程能更新UI么？是的，surfaceView是不能响应事件的。
UI线程是个什么东西？runOnUIThread?所有的生命周期回调是在UI线程，因为AMS调过来都切换了

UI线程一定只能是主线程么？
UI线程和主线程是一回事么？主线程是不能退出的。
对比主线程和普通线程，一个只能在线程内访问looper，一个是进程内都能访问looper
ActivityThread是什么，是UI线程么，不，这个不是继承线程的。
说说prepareMainLooper和prepareLooper的区别？
可以提一下主线程是干嘛的，为什么UI绘制要在主线程，看看应用里哪里用到了这个main looper
可参考深入理解Android内核设计思想5.3UI主线程-ActivitytHREAD
UI绘制是怎么放在UI线程的，那个异常是怎么抛出来的

# 5.3 主线程Looper.loop了，为什么没有ANR呢？
你要搞清楚怎么样才会ANR？

# 5.3 说说Activity的显示原理
从setContentView到界面显示出来中间经历了哪些过程？
Activity 是如何生成一个 view 的，view的加载机制
这个问题特别大，因为涉及到的角色很多，把每个都讲清楚不太现实
所以我们有个侧重

说说Activity, window和view的区别

[Android Activity 生成 View 过程](https://www.jianshu.com/p/e7c9916940b6)

# 5.4 说说Activity界面的刷新机制
《深入理解Android卷一》
vsync， chreographer
[Android Choreographer 源码分析](https://www.jianshu.com/p/996bca12eb1d)
[AndroidUI系列—浅谈图像渲染机制](https://www.jianshu.com/p/1998182670fb)


# 5.5 说说输入事件是怎么分发的
怎么从framework层分发到应用层的？
InputEventReceiver


