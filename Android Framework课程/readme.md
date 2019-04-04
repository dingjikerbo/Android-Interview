
# 总进度 30/45

# 第01章 课程介绍
| # | 课程题目 | 状态 |
|---|-----|------|
|1|课程概述|Pending|

# 第02章 架构相关
| # | 课程题目 | 状态 |备注|
|---|-----|------|-----|
|1|请说说Android的系统架构|Done|
|2|Android Framework里用到了哪些设计模式，请举例说明。哪个是让你印象最深刻的，为什么？|Pending|参考书籍|
|3|以你做过的一个项目为例，讲讲其整体架构及原理，从应用层到framework层|Pending|相机预览，插件，ui架构，蓝牙|
虚拟机原理，如何自己设计一个虚拟机(内存管理，类加载，双亲委派)

# 第03章 系统相关
| # | 课程题目 | 状态 |
|---|-----|------|
|1|谈谈你对zygote的理解？|Done|
|2|说一说Android系统的启动？|Done|
|3|你知道怎么添加一个系统服务吗？|Done|

# 第04章 应用进程
| # | 课程题目 | 状态 | 
|---|-----|------|
|1|你知道应用进程是怎么启动的吗？|Done|
|2|谈谈你对Application的理解？|Done|
|3|谈谈对Context的理解|Done|
|4|谈谈应用的安装流程|Pending|

待定
|5|你知道android是怎么管理这些应用进程的嘛？|Pending|
ANR产生的原因是什么？如何定位和修正

# 第05章 Activity
| # | 课程题目 | 状态 | 
|---|-----|------|
|1|说说应用的冷启动流程|Done|
|2|应用的UI线程是怎么启动的？|Done|
|3|说说输入事件是怎么分发的|Pending|

# 第06章 Service
| # | 课程题目 | 状态 | 
|---|-----|------|
|1|说说service的启动原理|Done|
|2|说说service的绑定原理|Done|
|3|系统服务和应用的Service有什么区别吗？|Done|

# 第07章 广播
| # | 课程题目 | 状态 | 
|---|-----|------|
|1|说说动态广播的注册和收发原理|Done|
|2|说说静态广播的注册和收发原理|Done|
请描述一下广播BroadcastReceiver的理解
BroadcastReceiver，LocalBroadcastReceiver 区别

# 第08章 provider
| # | 课程题目 | 状态 | 
|---|-----|------|
|1|Provider的启动流程？|Done|

待定
|2|provider的工作原理？|Pending|
|3|provider如何跨进程传输数据的？|Pending|
请介绍下ContentProvider 是如何实现数据共享的？
谈谈你对ContentProvider的理解
说说ContentProvider、ContentResolver、ContentObserver 之间的关系

# 第09章 进程通信
| # | 课程题目 | 状态 | 
|---|-----|------|
|1|Android Framework层用到了哪些跨进程通信方式，请举例说明？|Done|
|2|应用程序是怎么支持binder机制的？|Done|
|3|说说你对binder的理解|Done||
|4|binder是怎么跨进程传递的？|Done||
|5|一次完整的IPC通信流程是怎样的？|Done||
|6|说说binder的oneway|Done|
|7|intent传递数据大小有限制么，怎么解决？|Pending|ashmem|

说说parlcel的原理
说说bitmap的原理

# 第10章 线程通信
| # | 课程题目 | 状态 | 
|---|-----|------|
|1|说说线程的消息队列是怎么创建的？|Done|
|2|说说Android里线程间消息传递机制？|Done|
|3|handler的消息延时是怎么实现的？|Done|
|4|说说idleHandler原理|Done|
|5|threadlocal原理|Done|
|6|主线程Looper.loop了，为什么没有ANR呢？|Pending|

待定：
|1|怎么设计一个同步的消息处理？|Pending|


# 第11章 UI体系
| # | 课程题目 | 状态 | 备注|
|---|-----|------|------|
|1|说说Android的屏幕刷新机制|Done|
|2|谈谈你对surface的理解？|Done|
|3|说说SurfaceView的原理|Done|
|4|说说Activity的显示原理|Done||


待定
view动画实现原理
Bitmap对象的理解
activity创建和dialog创建的异同
AlertDialog,popupWindow,Activity区别

# 第12章 综合问题
| # | 课程题目 | 状态 | 备注|
|---|-----|------| ------|
|1|你去了解framework是为了解决一个什么样的问题，怎么解决的？|Pending|插件，拦截系统调用，清除系统resource|
|2|Framework中有什么你觉得设计的很巧妙的地方，请举例说明|Pending|可以说zygote，binder|

# 待定
|3|Framework中有什么你觉得设计的不合理的地方，请举例说明|Pending|
