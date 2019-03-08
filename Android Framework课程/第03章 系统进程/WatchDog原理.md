# WatchDog原理

在SystemServer的startOtherServices时，会初始化WatchDog，

```
final Watchdog watchdog = Watchdog.getInstance();
watchdog.init(context, mActivityManagerService);
```

这个WatchDog是一个单例线程，在系统启动完毕后，WatchDog会正式启动，执行在一个单独的线程。

```
Watchdog.getInstance().start();
```

WatchDog继承自Thread，以下是私有构造函数，WatchDog中有个HandlerChecker的ArrayList，这个HandlerChecker是个Runnable，

```
private Watchdog() {
    super("watchdog");

    mMonitorChecker = new HandlerChecker(FgThread.getHandler(),
            "foreground thread", DEFAULT_TIMEOUT);
    mHandlerCheckers.add(mMonitorChecker);

    mHandlerCheckers.add(new HandlerChecker(new HandlerLooper.getMainLooper()),
            "main thread", DEFAULT_TIMEOUT));
    mHandlerCheckers.add(new HandlerChecker(UiThread.getHandler(),
            "ui thread", DEFAULT_TIMEOUT));
    mHandlerCheckers.add(new HandlerChecker(IoThread.getHandler(),
            "i/o thread", DEFAULT_TIMEOUT));
    mHandlerCheckers.add(new HandlerChecker(DisplayThread.getHandler(),
            "display thread", DEFAULT_TIMEOUT));

    addMonitor(new BinderThreadMonitor());
}
```

我们来看WatchDog启动之后做什么，由于WatchDog是个单例线程，所以start会执行到这里的run，

首先一个死循环，然后遍历HandlerCheckers，调用scheduleCheckLocked，这个是异步的，是在每个HandlerChecker对应线程中做的。所以发起检查的调用后，就是等CHECK_INTERVAL这么久再检查是不是每个HandlerChecker都Complete了。检查就是在每个HandlerChecker对应的Handler的前面post一个Runnable，如果这个Runnable执行到了，就表示这个线程没有block。有一种特殊情况，就是MessageQueue本身是polling状态，也就是堵在epoll_wait上，那肯定是没有发生死锁。

默认是每30秒检查一轮，最大容忍超时60s，从检查开始算起，如果超了30s了就算wait_half。也就是看门狗会检查两次，进行最终的确认。

```
@Override
public void run() {
    boolean waitedHalf = false;
    while (true) {
        final ArrayList<HandlerChecker> blockedCheckers;
        final String subject;
        final boolean allowRestart;
        synchronized (this) {
            long timeout = CHECK_INTERVAL;

            for (int i=0; i<mHandlerCheckers.size(); i++) {
                HandlerChecker hc = mHandlerCheckers.get(i);
                hc.scheduleCheckLocked();
            }

            long start = SystemClock.uptimeMillis();
            while (timeout > 0) {
                try {
                    wait(timeout);
                } catch (InterruptedException e) {
                    Log.wtf(TAG, e);
                }
                timeout = CHECK_INTERVAL - (SystemClock.uptimeMillis() - start);
            }

            final int waitState = evaluateCheckerCompletionLocked();
            if (waitState == COMPLETED) {
                waitedHalf = false;
                continue;
            } else if (waitState == WAITING) {
                continue;
            } else if (waitState == WAITED_HALF) {
                if (!waitedHalf) {
                    waitedHalf = true;
                }
                continue;
            }
            blockedCheckers = getBlockedCheckersLocked();
            subject = describeCheckersLocked(blockedCheckers);
        }

        SystemClock.sleep(2000);
        ......
        Process.killProcess(Process.myPid());
        System.exit(10);
    
        waitedHalf = false;
    }
}
```

HandlerCheck里可以添加monitors，HandlerCheck是检查线程用的，往线程postAtFront一个runnable，如果能执行，就表示没阻塞。而monitor是检查服务的，首先要通过addMonitor添加到WatchDog里，然后WatchDog统一调monitor，获取锁。

block in Handler和block in monitor是有区别的，一个是阻塞在handler的消息处理函数了，一个是服务死锁了。