
这是surfaceFlinger启动的入口函数，里面会调用init，
```
int main(int, char**) {
    // When SF is launched in its own process, limit the number of
    // binder threads to 4.
    ProcessState::self()->setThreadPoolMaxThreadCount(4);

    // start the thread pool
    sp<ProcessState> ps(ProcessState::self());
    ps->startThreadPool();

    // instantiate surfaceflinger
    sp<SurfaceFlinger> flinger = new SurfaceFlinger();

    setpriority(PRIO_PROCESS, 0, PRIORITY_URGENT_DISPLAY);

    set_sched_policy(0, SP_FOREGROUND);

    // initialize before clients can connect
    flinger->init();

    // publish surface flinger
    sp<IServiceManager> sm(defaultServiceManager());
    sm->addService(String16(SurfaceFlinger::getServiceName()), flinger, false);

    // run in this thread
    flinger->run();

    return 0;
}
```

看下Init函数，这里会创建一个EventThread线程，传了一个DisplaySyncSource，这个继承VSyncSource，这怎么创建了两个EventThread，对应两个VSyncSource。
```
void SurfaceFlinger::init() {
    ......

    // start the EventThread
    sp<VSyncSource> vsyncSrc = new DispSyncSource(&mPrimaryDispSync,
            vsyncPhaseOffsetNs, true, "app");
    mEventThread = new EventThread(vsyncSrc);
    sp<VSyncSource> sfVsyncSrc = new DispSyncSource(&mPrimaryDispSync,
            sfVsyncPhaseOffsetNs, true, "sf");
    mSFEventThread = new EventThread(sfVsyncSrc);
    mEventQueue.setEventThread(mSFEventThread);

    // Initialize the H/W composer object.  There may or may not be an
    // actual hardware composer underneath.
    mHwc = new HWComposer(this,
            *static_cast<HWComposer::EventHandler *>(this));

    ......

    mEventControlThread = new EventControlThread(this);
    mEventControlThread->run("EventControl", PRIORITY_URGENT_DISPLAY);

    // set a fake vsync period if there is no HWComposer
    if (mHwc->initCheck() != NO_ERROR) {
        mPrimaryDispSync.setPeriod(16666667);
    }

    ......
}
```

咱们看这个HWComposer，不管这个vsync信号是硬件生成的还是软件生成的，咱们不care，咱么就当他是软件生成的好吧。

```
HWComposer::HWComposer(const sp<SurfaceFlinger>& flinger, 
    EventHandler& handler) {

    ......

    if (needVSyncThread) {
        // we don't have VSYNC support, we need to fake it
        mVSyncThread = new VSyncThread(*this);
    }
}

```

构造函数没做什么，看threadLoop，这里逻辑很简单，首先，mNextFakeVsync表示下次触发vsync的时间戳，计算一下离当前还有多久，如果已经过了，那就修正一下。然后线程就sleep了，到点了唤醒，
然后执行mEventHandler的onVSyncReceived。这个EventHandler其实就是SurfaceFlinger自己了，他实现了这个接口。
```
HWComposer::VSyncThread::VSyncThread(HWComposer& hwc)
    : mHwc(hwc), mEnabled(false),
      mNextFakeVSync(0),
      mRefreshPeriod(hwc.getRefreshPeriod(HWC_DISPLAY_PRIMARY)) {
}

void HWComposer::VSyncThread::onFirstRef() {
    run("VSyncThread", PRIORITY_URGENT_DISPLAY + PRIORITY_MORE_FAVORABLE);
}

bool HWComposer::VSyncThread::threadLoop() {
    const nsecs_t period = mRefreshPeriod;
    const nsecs_t now = systemTime(CLOCK_MONOTONIC);
    nsecs_t next_vsync = mNextFakeVSync;
    nsecs_t sleep = next_vsync - now;
    if (sleep < 0) {
        // we missed, find where the next vsync should be
        sleep = (period - ((now - next_vsync) % period));
        next_vsync = now + sleep;
    }
    mNextFakeVSync = next_vsync + period;

    struct timespec spec;
    spec.tv_sec  = next_vsync / 1000000000;
    spec.tv_nsec = next_vsync % 1000000000;

    int err;
    do {
        err = clock_nanosleep(CLOCK_MONOTONIC, TIMER_ABSTIME, &spec, NULL);
    } while (err<0 && errno == EINTR);

    if (err == 0) {
        mHwc.mEventHandler.onVSyncReceived(0, next_vsync);
    }

    return true;
}

```

这通过mPrimaryDispSync.addResyncSample来分发vsync信号，这是个DispSync类型的变量，他的构造函数里会创建一个DispSyncThread，
```
void SurfaceFlinger::onVSyncReceived(int type, nsecs_t timestamp) {
    bool needsHwVsync = false;

    { // Scope for the lock
        Mutex::Autolock _l(mHWVsyncLock);
        if (type == 0 && mPrimaryHWVsyncEnabled) {
            needsHwVsync = mPrimaryDispSync.addResyncSample(timestamp);
        }
    }
}
```

addResyncSample是干嘛的呢？这个updateModelLocked最后调到了mThread的updateModel函数，这个会发出signal信号去唤醒线程，线程可能阻塞在threadLoop里呢。

```
bool DispSync::addResyncSample(nsecs_t timestamp) {
    ......

    updateModelLocked();

    ......
}

void updateModel(nsecs_t period, nsecs_t phase) {
    Mutex::Autolock lock(mMutex);
    mPeriod = period;
    mPhase = phase;
    mCond.signal();
}
```

咱们看他的threadLoop，gatherCallbackInvocationsLocked用于获取需要本次vsync信号的回调列表，

```
virtual bool threadLoop() {
    status_t err;
    nsecs_t now = systemTime(SYSTEM_TIME_MONOTONIC);
    nsecs_t nextEventTime = 0;

    while (true) {
        Vector<CallbackInvocation> callbackInvocations;

        nsecs_t targetTime = 0;

        if (mPeriod == 0) {
            err = mCond.wait(mMutex);
            continue;
        }

        nextEventTime = computeNextEventTimeLocked(now);
        targetTime = nextEventTime;

        bool isWakeup = false;

        if (now < targetTime) {
            err = mCond.waitRelative(mMutex, targetTime - now);

            if (err == TIMED_OUT) {
                isWakeup = true;
            }
        }

        callbackInvocations = gatherCallbackInvocationsLocked(now);

        if (callbackInvocations.size() > 0) {
            fireCallbackInvocations(callbackInvocations);
        }
    }

    return false;
}

// 分发出去了，这个onDispSyncEvent是谁的，是DispSyncSource的，他实现了Callback接口，
void fireCallbackInvocations(const Vector<CallbackInvocation>& callbacks) {
    for (size_t i = 0; i < callbacks.size(); i++) {
        callbacks[i].mCallback->onDispSyncEvent(callbacks[i].mEventTime);
    }
}
```


这个callback是哪里设置的呢？是在EventThread里，
```
virtual void onDispSyncEvent(nsecs_t when) {
    if (callback != NULL) {
        callback->onVSyncEvent(when);
    }
}
```

原来callback就是EventThread自己，这个onVsyncEvent是干嘛的呢？发广播，
```
void EventThread::enableVSyncLocked() {
    if (!mUseSoftwareVSync) {
        // never enable h/w VSYNC when screen is off
        if (!mVsyncEnabled) {
            mVsyncEnabled = true;
            mVSyncSource->setCallback(static_cast<VSyncSource::Callback*>(this));
            mVSyncSource->setVSyncEnabled(true);
        }
    }
    mDebugVsyncEnabled = true;
    sendVsyncHintOnLocked();
}

void EventThread::onVSyncEvent(nsecs_t timestamp) {
    Mutex::Autolock _l(mLock);
    mVSyncEvent[0].header.type = DisplayEventReceiver::DISPLAY_EVENT_VSYNC;
    mVSyncEvent[0].header.id = 0;
    mVSyncEvent[0].header.timestamp = timestamp;
    mVSyncEvent[0].vsync.count++;
    mCondition.broadcast();
}
```

eventThread自己的loop里肯定在wait，收到广播后就唤醒了，

这只有两件事，首先waitForEvent，然后给event通过Connection这个Binder句柄的postEvent分发出去。
```
bool EventThread::threadLoop() {
    DisplayEventReceiver::Event event;
    Vector< sp<EventThread::Connection> > signalConnections;
    signalConnections = waitForEvent(&event);

    // dispatch events to listeners...
    const size_t count = signalConnections.size();
    for (size_t i=0 ; i<count ; i++) {
        const sp<Connection>& conn(signalConnections[i]);
        // now see if we still need to report this event
        status_t err = conn->postEvent(event);
    }
    return true;
}
```

看看waitForEvent，

```
Vector< sp<EventThread::Connection> > EventThread::waitForEvent(
        DisplayEventReceiver::Event* event)
{
    Mutex::Autolock _l(mLock);
    Vector< sp<EventThread::Connection> > signalConnections;

    do {
        // find out connections waiting for events
        size_t count = mDisplayEventConnections.size();

        // note: !timestamp implies signalConnections.isEmpty(), because we
        // don't populate signalConnections if there's no vsync pending
        if (!timestamp && !eventPending) {
            // wait for something to happen
            if (waitForVSync) {
    
                if (mCondition.waitRelative(mLock, timeout) == TIMED_OUT) {
                    ......
                }
            } else {
                // Nobody is interested in vsync, so we just want to sleep.
                // h/w vsync should be disabled, so this will wait until we
                // get a new connection, or an existing connection becomes
                // interested in receiving vsync again.
                mCondition.wait(mLock);
            }
        }
    } while (signalConnections.isEmpty());

  ......
}

```