

# WindowManager的addView和addContentView有什么区别


# Dialog和PopupWindow的区别在哪里?为什么Dialog传入application的Context会报错?


# ViewRootImpl是什么,一个Activity有多少个ViewRootImpl对象?


# 该怎样理解Window的作用?


# Activity 是如何生成一个 view 的，机制是什么

# SurfaceView和view的区别？


# 谈谈你对应用UI线程的理解？
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


# 好的资源
https://silencedut.github.io/2016/08/10/Android视图框架Activity,Window,View,ViewRootImpl理解/
https://juejin.im/entry/584f7078ac502e006937a392
[Android 面试问题: Framework 工作方式及原理，Activity 是如何生成一个 view 的，机制是什么?](https://www.zhihu.com/question/20025633)