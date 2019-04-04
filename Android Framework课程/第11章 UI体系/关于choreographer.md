先看下这个类的注释，

协调动画，输入及绘制的时机。
接收来自于display子系统的时间脉冲，调度下一帧的渲染。
应用通常不直接跟choreographer交互，而是在动画框架或者view体系里间接地用到。
只有少部分情况用到choreographer,
如果应用渲染在一个单独的线程，比如用OpenGL，可以用postFrameCallback

再看下这个类的构造函数，这个是线程内的单例，因为Choreographer要用到looper，所以这里所在的线程要准备好looper。

```
public static Choreographer getInstance() {
    return sThreadInstance.get();
}

private static final ThreadLocal<Choreographer> sThreadInstance =
        new ThreadLocal<Choreographer>() {
    @Override
    protected Choreographer initialValue() {
        Looper looper = Looper.myLooper();
        if (looper == null) {
            throw new IllegalStateException("The current thread must have a looper!");
        }
        return new Choreographer(looper);
    }
};

private Choreographer(Looper looper) {
    mLooper = looper;
    mHandler = new FrameHandler(looper);
    mDisplayEventReceiver = USE_VSYNC ? new FrameDisplayEventReceiver(looper) : null;
    mLastFrameTimeNanos = Long.MIN_VALUE;

    mFrameIntervalNanos = (long)(1000000000 / getRefreshRate());

    mCallbackQueues = new CallbackQueue[CALLBACK_LAST + 1];
    for (int i = 0; i <= CALLBACK_LAST; i++) {
        mCallbackQueues[i] = new CallbackQueue();
    }
}
```

咱们假设用的就是vsync信号，所以mDisplayEventReceiver是FrameDisplayEventReceiver对象。这里有个CallbackQueue数组，为什么，因为callback有好几种类型。优先级最高的是input，其实是动画，再次是traversal，最后是commit。

```
/**
 * Callback type: Input callback.  Runs first.
 * @hide
 */
public static final int CALLBACK_INPUT = 0;

/**
 * Callback type: Animation callback.  Runs before traversals.
 * @hide
 */
public static final int CALLBACK_ANIMATION = 1;

/**
 * Callback type: Traversal callback.  Handles layout and draw.  Runs
 * after all other asynchronous messages have been handled.
 * @hide
 */
public static final int CALLBACK_TRAVERSAL = 2;

/**
 * Callback type: Commit callback.  Handles post-draw operations for the frame.
 * Runs after traversal completes.  The {@link #getFrameTime() frame time} reported
 * during this callback may be updated to reflect delays that occurred while
 * traversals were in progress in case heavy layout operations caused some frames
 * to be skipped.  The frame time reported during this callback provides a better
 * estimate of the start time of the frame in which animations (and other updates
 * to the view hierarchy state) actually took effect.
 * @hide
 */
public static final int CALLBACK_COMMIT = 3;
```

咱们接下来看postFrameCallback的实现，这个callback会在下一帧渲染的时候调到，注意啊，只会调一次。这里先给callback加到对应的CallbackQueue里，然后看看时间，如果dueTime<=now，说明要马上触发了，否则就延后一下再触发，注意消息是异步的。

```
public void postFrameCallback(FrameCallback callback) {
    postFrameCallbackDelayed(callback, 0);
}

void postFrameCallbackDelayed(FrameCallback callback, long delayMillis) {
    postCallbackDelayedInternal(CALLBACK_ANIMATION,
                callback, FRAME_CALLBACK_TOKEN, delayMillis);
}

private void postCallbackDelayedInternal(int callbackType,
        Object action, Object token, long delayMillis) {
    synchronized (mLock) {
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
}
```

咱们先看怎么给callback加到CallbackQueue里的，这个addCallbackLocked实现，这个obtainCallbackLocked就是从callbackRecord的对象池里取一个出来，callbackRecord里有个next，可以构成单链表。这个record取出来之后，根据时间先后顺序插入到callbackRecord列表里，列表头是mHead。

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

接下来咱们看scheduleFrameLocked，这里用一个bool变量防御了一下，在doFrame的时候设为false的。咱们只看USE_VSYNC的分支，这里直接看scheduleVsyncLocked，

```
private void scheduleFrameLocked(long now) {
    if (!mFrameScheduled) {
        mFrameScheduled = true;
        if (USE_VSYNC) {
            // If running on the Looper thread, then schedule the vsync immediately,
            // otherwise post a message to schedule the vsync from the UI thread
            // as soon as possible.
            if (isRunningOnLooperThreadLocked()) {
                scheduleVsyncLocked();
            } else {
                Message msg = mHandler.obtainMessage(MSG_DO_SCHEDULE_VSYNC);
                msg.setAsynchronous(true);
                mHandler.sendMessageAtFrontOfQueue(msg);
            }
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


这个mDisplayEventReceiver我们前面说过，是FrameDisplayEventReceiver，这的scheduleVsync是调的native函数，
```
private void scheduleVsyncLocked() {
    mDisplayEventReceiver.scheduleVsync();
}

public void scheduleVsync() {
    nativeScheduleVsync(mReceiverPtr);
}
```

咱们先看DisplayEventReceiver的构造函数，

```
public DisplayEventReceiver(Looper looper) {
    mMessageQueue = looper.getQueue();
    mReceiverPtr = nativeInit(new WeakReference<DisplayEventReceiver>(this), mMessageQueue);
}
```

继续看nativeInit，这创建了一个NativeDisplayEventReceiver对象，初始化，然后返回。

```
static jlong nativeInit(JNIEnv* env, jclass clazz, jobject receiverWeak,
        jobject messageQueueObj) {
    sp<MessageQueue> messageQueue = android_os_MessageQueue_getMessageQueue(env, messageQueueObj);

    sp<NativeDisplayEventReceiver> receiver = new NativeDisplayEventReceiver(env, receiverWeak, messageQueue);
    status_t status = receiver->initialize();

    return reinterpret_cast<jlong>(receiver.get());
}
```

咱们看NativeDisplayEventReceiver的构造函数，好像没干嘛。

```
NativeDisplayEventReceiver::NativeDisplayEventReceiver(JNIEnv* env,
        jobject receiverWeak, const sp<MessageQueue>& messageQueue) :
        mReceiverWeakGlobal(env->NewGlobalRef(receiverWeak)),
        mMessageQueue(messageQueue), mWaitingForVsync(false) {
    ALOGV("receiver %p ~ Initializing display event receiver.", this);
}
```

继续看他的initialize函数，这里

```
status_t NativeDisplayEventReceiver::initialize() {
    status_t result = mReceiver.initCheck();

    int rc = mMessageQueue->getLooper()->addFd(mReceiver.getFd(), 0, Looper::EVENT_INPUT, this, NULL);
    return OK;
}
```

这个NativeDisplayEventReceiver的类定义，继承LooperCallback，

```
class NativeDisplayEventReceiver : public LooperCallback {
public:
    NativeDisplayEventReceiver(JNIEnv* env,
            jobject receiverWeak, const sp<MessageQueue>& messageQueue);

    status_t initialize();
    void dispose();
    status_t scheduleVsync();

protected:
    virtual ~NativeDisplayEventReceiver();

private:
    jobject mReceiverWeakGlobal;
    sp<MessageQueue> mMessageQueue;
    DisplayEventReceiver mReceiver;
    bool mWaitingForVsync;

    virtual int handleEvent(int receiveFd, int events, void* data);
    bool processPendingEvents(nsecs_t* outTimestamp, int32_t* id, uint32_t* outCount);
    void dispatchVsync(nsecs_t timestamp, int32_t id, uint32_t count);
    void dispatchHotplug(nsecs_t timestamp, int32_t id, bool connected);
};
```

里面有个DisplayEventReceiver对象，叫mReceiver，这个对象会跟着NativeDisplayEventReceiver的构造一起构造，咱们看他的构造函数，
```
DisplayEventReceiver::DisplayEventReceiver() {
    sp<ISurfaceComposer> sf(ComposerService::getComposerService());
    if (sf != NULL) {
        mEventConnection = sf->createDisplayEventConnection();
        if (mEventConnection != NULL) {
            mDataChannel = mEventConnection->getDataChannel();
        }
    }
}
```

这个ComposerService的getComposerService是啥？

```
sp<ISurfaceComposer> ComposerService::getComposerService() {
    ComposerService& instance = ComposerService::getInstance();
    Mutex::Autolock _l(instance.mLock);
    if (instance.mComposerService == NULL) {
        ComposerService::getInstance().connectLocked();
        assert(instance.mComposerService != NULL);
        ALOGD("ComposerService reconnected");
    }
    return instance.mComposerService;
}
```

看看这个ComposerService的类定义，从注释上看，这个类持有一个connection，连到SurfaceFlinger的。另外这个类继承Singleton，是个单例。

```
class ComposerService : public Singleton<ComposerService>
{
    sp<ISurfaceComposer> mComposerService;
    sp<IBinder::DeathRecipient> mDeathObserver;
    Mutex mLock;

    ComposerService();
    void connectLocked();
    void composerServiceDied();
    friend class Singleton<ComposerService>;
public:

    // Get a connection to the Composer Service.  This will block until
    // a connection is established.
    static sp<ISurfaceComposer> getComposerService();
};
```

看下这个类的构造函数，原来构造函数里就会去connect了啊，

```
ComposerService::ComposerService() : Singleton<ComposerService>() {
    Mutex::Autolock _l(mLock);
    connectLocked();
}
```

看下这个类的connectLocked函数，应该是这里连接surfaceFlinger的，这个getService是定义在IServiceManager.h里，其实就是从ServiceManager获取名称为name的服务的binder句柄，设置这个mComposerService，这是ISurfaceComposer类型的。

```
void ComposerService::connectLocked() {
    const String16 name("SurfaceFlinger");
    while (getService(name, &mComposerService) != NO_ERROR) {
        usleep(250000);
    }
}
```

回到DisplayEventReceiver的构造函数，拿到了SurfaceFlinger服务的Binder句柄，也就是ISurfaceComposer之后，通过createDisplayEventConnection调用，获得了另一个IDisplayEventConnection的Binder句柄，再调这个句柄的getDataChannel函数，拿到mDataChannel，这是个啥，是个BitTube对象，这个对象干嘛的咱们先不管。


总结一下，choreographer里会启动一个DisplayEventReceiver，这个对象是跟SurfaceFlinger有关系的。

咱们回到choreographer的看nativeScheduleVsync，

```
static void nativeScheduleVsync(JNIEnv* env, jclass clazz, jlong receiverPtr) {
    sp<NativeDisplayEventReceiver> receiver =
            reinterpret_cast<NativeDisplayEventReceiver*>(receiverPtr);
    status_t status = receiver->scheduleVsync();
}
```

这儿处理一下pendingEvents，然后requestNextVsync。
```
status_t NativeDisplayEventReceiver::scheduleVsync() {
    if (!mWaitingForVsync) {
        // Drain all pending events.
        nsecs_t vsyncTimestamp;
        int32_t vsyncDisplayId;
        uint32_t vsyncCount;
        processPendingEvents(&vsyncTimestamp, &vsyncDisplayId, &vsyncCount);

        status_t status = mReceiver.requestNextVsync();

        mWaitingForVsync = true;
    }
    return OK;
}
```

咱们看DisplayEventReceiver里面的requestNextVsync，

```
status_t DisplayEventReceiver::requestNextVsync() {
    if (mEventConnection != NULL) {
        mEventConnection->requestNextVsync();
        return NO_ERROR;
    }
    return NO_INIT;
}
```

这个mEventConnection是在SurfaceFlinger中实现的，这个mEventThread是啥呢，是SurfaceFlinger的init函数里创建的，咱们先不管connection的事了，就当是一个双方调用的桥梁好了。
```
sp<IDisplayEventConnection> SurfaceFlinger::createDisplayEventConnection() {
    return mEventThread->createEventConnection();
}

sp<EventThread::Connection> EventThread::createEventConnection() const {
    return new Connection(const_cast<EventThread*>(this));
}
```

注意啊，NativeDisplayEventReceiver的initialize函数还有一个我们没讲，就是addFd，给mReceiver的fd添加到looper的关注里面，关注可读事件，这个fd是mReceiver里的mDataChannel的fd，那是个BitTube，我们可以认为是一块缓存，fd就是这个缓存的描述符。有了这个fd，如果别人往里面写东西，那么就会触发LooperCallback的handleEvent，
```
status_t NativeDisplayEventReceiver::initialize() {
    status_t result = mReceiver.initCheck();

    int rc = mMessageQueue->getLooper()->addFd(mReceiver.getFd(), 0, Looper::EVENT_INPUT, this, NULL);
    return OK;
}
```

看这个handleEvent，这个events必须是咱们关注的可读事件，不然就返回。这里返回1表示还要继续监听这个fd的可读事件。dispatchVsync是干嘛的？
```
int NativeDisplayEventReceiver::handleEvent(int receiveFd, int events, void* data) {
    // Drain all pending events, keep the last vsync.
    nsecs_t vsyncTimestamp;
    int32_t vsyncDisplayId;
    uint32_t vsyncCount;
    if (processPendingEvents(&vsyncTimestamp, &vsyncDisplayId, &vsyncCount)) {
        mWaitingForVsync = false;
        dispatchVsync(vsyncTimestamp, vsyncDisplayId, vsyncCount);
    }
    return 1; // keep the callback
}
```

看来是调到java层了，是DisplayEventReceiver.java的dispatchVsync函数，
```
void NativeDisplayEventReceiver::dispatchVsync(nsecs_t timestamp, int32_t id, uint32_t count) {
    JNIEnv* env = AndroidRuntime::getJNIEnv();

    ScopedLocalRef<jobject> receiverObj(env, jniGetReferent(env, mReceiverWeakGlobal));
    if (receiverObj.get()) {
        env->CallVoidMethod(receiverObj.get(),
                gDisplayEventReceiverClassInfo.dispatchVsync, timestamp, id, count);
        ALOGV("receiver %p ~ Returned from vsync handler.", this);
    }

    mMessageQueue->raiseAndClearException(env, "dispatchVsync");
}
```

带了三个参数，时间戳，displayId，还有frame。这个onVsync被FrameDisplayEventReceiver覆写了，
```
@SuppressWarnings("unused")
private void dispatchVsync(long timestampNanos, int builtInDisplayId, int frame) {
    onVsync(timestampNanos, builtInDisplayId, frame);
}
```

```
@Override
public void onVsync(long timestampNanos, int builtInDisplayId, int frame) {
    long now = System.nanoTime();

    if (timestampNanos > now) {
        timestampNanos = now;
    }

    if (!mHavePendingVsync) {
        mHavePendingVsync = true;
    }

    mTimestampNanos = timestampNanos;
    mFrame = frame;
    Message msg = Message.obtain(mHandler, this);
    msg.setAsynchronous(true);
    mHandler.sendMessageAtTime(msg, timestampNanos / TimeUtils.NANOS_PER_MS);
}
```

这个message是怎么处理的呢？这个其实是发了个runnable，

```
@Override
public void run() {
    mHavePendingVsync = false;
    doFrame(mTimestampNanos, mFrame);
}
```

```
void doFrame(long frameTimeNanos, int frame) {
    final long startNanos;
    synchronized (mLock) {
        if (!mFrameScheduled) {
            return; // no work to do
        }

        long intendedFrameTimeNanos = frameTimeNanos;
        startNanos = System.nanoTime();
        final long jitterNanos = startNanos - frameTimeNanos;

        // 如果当前执行的时间太晚了，说明有些帧执行得太耗时了，导致当前延后
        if (jitterNanos >= mFrameIntervalNanos) {
            final long skippedFrames = jitterNanos / mFrameIntervalNanos;
            if (skippedFrames >= SKIPPED_FRAME_WARNING_LIMIT) {
                Log.i(TAG, "Skipped " + skippedFrames + " frames!  "
                        + "The application may be doing too much work on its main thread.");
            }
            final long lastFrameOffset = jitterNanos % mFrameIntervalNanos;
            frameTimeNanos = startNanoos - lastFrameOffset;
        }

        if (frameTimeNanos < mLastFrameTimeNanos) {
            scheduleVsyncLocked();
            return;
        }

        mFrameInfo.setVsync(intendedFrameTimeNanos, frameTimeNanos);
        mFrameScheduled = false;
        mLastFrameTimeNanos = frameTimeNanos;
    }

    doCallbacks(Choreographer.CALLBACK_INPUT, frameTimeNanos);
    doCallbacks(Choreographer.CALLBACK_ANIMATION, frameTimeNanos);
    doCallbacks(Choreographer.CALLBACK_TRAVERSAL, frameTimeNanos);
    doCallbacks(Choreographer.CALLBACK_COMMIT, frameTimeNanos);
}
```


看下doCallback，这应该才是真正的开始处理callback，这个extractDueCallbacksLocked传入了一个now，返回当前到时了，可以执行的callback，然后依次执行。
```
void doCallbacks(int callbackType, long frameTimeNanos) {
    CallbackRecord callbacks;

    callbacks = mCallbackQueues[callbackType].extractDueCallbacksLocked(
                    now / TimeUtils.NANOS_PER_MS);

    for (CallbackRecord c = callbacks; c != null; c = c.next) {
        c.run(frameTimeNanos);
    }

    ......
}
```

最后再说说, scheduleVsync会向SurfaceFlinger发起requestNextVsync调用，

```
void EventThread::requestNextVsync(
        const sp<EventThread::Connection>& connection) {
    Mutex::Autolock _l(mLock);
    if (connection->count < 0) {
        connection->count = 0;
        mCondition.broadcast();
    }
}
```