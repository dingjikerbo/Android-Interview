先看这个Chreographer是干嘛的，先看怎么初始化的,
```
private static final ThreadLocal<Choreographer> sThreadInstance =
        new ThreadLocal<Choreographer>() {
    @Override
    protected Choreographer initialValue() {
        Looper looper = Looper.myLooper();

        Choreographer choreographer = new Choreographer(looper, VSYNC_SOURCE_APP);

        return choreographer;
    }
};

public static Choreographer getInstance() {
    return sThreadInstance.get();
}
```

可以看到，这个choreographer是线程内的单例，看看构造函数，假设咱们没用到vsync，那mDisplayEventReceiver就是null了。这还创建了mCallbackQueues数组，每个元素都是一个CallbackQueue，数组的索引对应的是Callback的type，有四种type，从0到3，依次为CALLBACK_INPUT，CALLBACK_ANIMATION，CALLBACK_TRAVERSAL，CALLBACK_COMMIT。越前面的优先级越高。

```
private Choreographer(Looper looper, int vsyncSource) {
    mLooper = looper;
    mHandler = new FrameHandler(looper);
    mDisplayEventReceiver = USE_VSYNC
            ? new FrameDisplayEventReceiver(looper, vsyncSource)
            : null;
    mLastFrameTimeNanos = Long.MIN_VALUE;

    mFrameIntervalNanos = (long)(1000000000 / getRefreshRate());

    mCallbackQueues = new CallbackQueue[CALLBACK_LAST + 1];
    for (int i = 0; i <= CALLBACK_LAST; i++) {
        mCallbackQueues[i] = new CallbackQueue();
    }
}
```

咱们看postCallback的实现，

```
public void postCallback(int callbackType, Runnable action, Object token) {
    postCallbackDelayed(callbackType, action, token, 0);
}
    
public void postCallbackDelayed(int callbackType, 
        Runnable action, Object token, long delayMillis) {
    postCallbackDelayedInternal(callbackType, action, token, delayMillis);
}

private void postCallbackDelayedInternal(int callbackType,
            Object action, Object token, long delayMillis) {

    final long now = SystemClock.uptimeMillis();
    final long dueTime = now + delayMillis;
    mCallbackQueues[callbackType].addCallbackLocked(dueTime, action, token);

    if (dueTime <= now) {
        scheduleFrameLocked(now);
    } else {
        Message msg = mHandler.obtainMessage(MSG_DO_SCHEDULE_CALLBACK, action);
        msg.arg1 = callbackType;
        msg.setAsynchronous(true);
        mHandler.sendMessageAtTime(msg, dueTime);
    }
}
```

这首先有mCallbackQueues，给这个runnable加到对应callback type的queue里面去。dueTime表示任务开始的时间，如果小于now表示任务早就该开始了，就会调scheduleFrameLocked，否则的话延时一下再处理任务。

我们先看下addCallbackLocked实现，这个是CallbackQueue里的函数，这个obtainCallbackLocked就是获取一个CallbackRecord，这个record只是一个封装runnable属性的类，给time, runnable和token封装到一起，便于管理。另外还有一个record对象池子，是个单链表，这就是从单链表里取了个空闲的对象。CallbackQueue里有个record的单链表，头结点是mHead。

看来新到的record要加到CallbackQueue的record链表里，这个链表其实是排了序的，离runnable触发时间越近的排在最前面。所以这里插入链表的时候要遍历一下，找个合适的插入点。

```
public void addCallbackLocked(long dueTime, Object action, Object token) {
    CallbackRecord callback = obtainCallbackLocked(dueTime, action, token);
    CallbackRecord entry = mHead;
    if (entry == null) {
        mHead = callback;
        return;
    }
    if (dueTime < entry.dueTime) {
        callback.next = entry;
        mHead = callback;
        return;
    }
    while (entry.next != null) {
        if (dueTime < entry.next.dueTime) {
            callback.next = entry.next;
            break;
        }
        entry = entry.next;
    }
    entry.next = callback;
}
```

咱们再看下scheduleFrameLocked，先假设没有vsync，这里不是来一个runnable就马上执行的，还是得严格按照一帧一帧的渲染周期来的。所以丢到handler里，延时处理。这里防御了一下，如果mFrameScheduled是true，可能是因为上一个任务太耗时了，这里就直接返回，什么也不做。

```
private void scheduleFrameLocked(long now) {
    if (!mFrameScheduled) {
        mFrameScheduled = true;
        if (USE_VSYNC) {
            ......
        } else {
            final long nextFrameTime = Math.max(
                    mLastFrameTimeNanos / TimeUtils.NANOS_PER_MS + sFrameDelay, now);

            Message msg = mHandler.obtainMessage(MSG_DO_FRAME);
            msg.setAsynchronous(true);
            mHandler.sendMessageAtTime(msg, nextFrameTime);
        }
    }
}
```

处理函数在doFrame里，这个FrameHandler的looper就是Cherographer构造时所在的线程的looper里，注意doFrame传入了一个时间，而下面又要对比一下时间，看看真正处理任务的时候是不是超时了，这两个不一样可能是因为获取不到mLock一直在等待。这里是个疑点。

```
private final class FrameHandler extends Handler {
    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_DO_FRAME:
                doFrame(System.nanoTime(), 0);
                break;
                ......
        }
    }
}

void doFrame(long frameTimeNanos, int frame) {
    final long startNanos;
    synchronized (mLock) {
        if (!mFrameScheduled) {
            return; // no work to do
        }

        long intendedFrameTimeNanos = frameTimeNanos;
        startNanos = System.nanoTime();
        final long jitterNanos = startNanos - frameTimeNanos;
        if (jitterNanos >= mFrameIntervalNanos) {
            final long skippedFrames = jitterNanos / mFrameIntervalNanos;
            if (skippedFrames >= SKIPPED_FRAME_WARNING_LIMIT) {
                Log.i(TAG, "Skipped " + skippedFrames + " frames!  "
                        + "The application may be doing too much work on its main thread.");
            }
            final long lastFrameOffset = jitterNanos % mFrameIntervalNanos;
            
            frameTimeNanos = startNanos - lastFrameOffset;
        }

        if (frameTimeNanos < mLastFrameTimeNanos) {
            scheduleVsyncLocked();
            return;
        }

        if (mFPSDivisor > 1) {
            long timeSinceVsync = frameTimeNanos - mLastFrameTimeNanos;
            if (timeSinceVsync < (mFrameIntervalNanos * mFPSDivisor) && timeSinceVsync > 0) {
                scheduleVsyncLocked();
                return;
            }
        }

        mFrameInfo.setVsync(intendedFrameTimeNanos, frameTimeNanos);
        mFrameScheduled = false;
        mLastFrameTimeNanos = frameTimeNanos;
    }


    AnimationUtils.lockAnimationClock(frameTimeNanos / TimeUtils.NANOS_PER_MS);

    mFrameInfo.markInputHandlingStart();
    doCallbacks(Choreographer.CALLBACK_INPUT, frameTimeNanos);

    mFrameInfo.markAnimationsStart();
    doCallbacks(Choreographer.CALLBACK_ANIMATION, frameTimeNanos);

    mFrameInfo.markPerformTraversalsStart();
    doCallbacks(Choreographer.CALLBACK_TRAVERSAL, frameTimeNanos);

    doCallbacks(Choreographer.CALLBACK_COMMIT, frameTimeNanos);

    AnimationUtils.unlockAnimationClock();
}
```