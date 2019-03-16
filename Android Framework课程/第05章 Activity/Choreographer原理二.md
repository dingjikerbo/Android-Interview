假设是采用vsync，看postCallback的实现，

下面是带vsync的，如果是跑在Choreographer的线程里，就直接scheduleVsyncLocked，否则丢一个message到Choreographer的工作线程里。

```
private void scheduleFrameLocked(long now) {
    if (!mFrameScheduled) {
        mFrameScheduled = true;

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
    }
}
```

再来看scheduleVsyncLocked，

```
private void scheduleVsyncLocked() {
    mDisplayEventReceiver.scheduleVsync();
}
```

再看scheduleVsync函数，

```
public void scheduleVsync() {
    nativeScheduleVsync(mReceiverPtr);
}
```


```
static void nativeScheduleVsync(JNIEnv* env, jclass clazz, jlong receiverPtr) {
    sp<NativeDisplayEventReceiver> receiver =
            reinterpret_cast<NativeDisplayEventReceiver*>(receiverPtr);
    status_t status = receiver->scheduleVsync();
}
```

```
status_t NativeDisplayEventReceiver::scheduleVsync() {
    if (!mWaitingForVsync) {
        // Drain all pending events.
        nsecs_t vsyncTimestamp;
        int32_t vsyncDisplayId;
        uint32_t vsyncCount;
        processPendingEvents(&vsyncTimestamp, &vsyncDisplayId, &vsyncCount);

        status_t status = mReceiver.requestNextVsync();
        if (status) {
            return status;
        }

        mWaitingForVsync = true;
    }
    return OK;
}
```