ActivityThread和AMS交互是通过token来的，这是个Binder。

我们要了解以下几个问题：
 - token是哪里生成的
 - token是什么时候赋给Activity的

我们先从ActivityThread开始，看看token是在哪里生成的：

首先以startActivity为入口，辗转调到了Instrumentation：
```
public ActivityResult execStartActivity(......) {
    IApplicationThread whoThread = (IApplicationThread) contextThread;
    
    ...... 
    
    int result = ActivityManagerNative.getDefault()
        .startActivity(......);
}
```

然后调到ActivityManagerService中：

```
@Override
public final int startActivity(IApplicationThread caller, ......) {
    return startActivityAsUser(......);
}

@Override
public final int startActivityAsUser() {
    return mStackSupervisor.startActivityMayWait(......);
}
```

再来看ActivityStackSupervisor的startActivityMayWait:

```
int res = startActivityLocked(......);
```

在startActivityLocked中初始化的ActivityRecord，如下：

```
ActivityRecord r = new ActivityRecord(mService, callerApp, ......);

```

再来看ActivityRecord类的构造函数：

```
ActivityRecord(......) {
        service = _service;
        appToken = new Token(this);
}

```

这个appToken是ActivityRecord中的token，看注释是说WindowManager的token。

来看看Token的定义，这是个Binder实体类，可以跨进程传输，里面只是保存了ActivityRecord的弱引用。

```
static class Token extends IApplicationToken.Stub {
        final WeakReference<ActivityRecord> weakActivity;

        Token(ActivityRecord activity) {
            weakActivity = new WeakReference<ActivityRecord>(activity);
        }
}
```


Activity中的mToken是在ActivityThread.attach中设置的，对应的是ActivityClientRecord中的token，这是在ActivityThread的scheduleLaunchActivity中设置的。
ActivityRecord是AMS中的，而ActivityClientRecord是ActivityThread中的。

```
 @Override
public final void scheduleLaunchActivity(......) {

    ActivityClientRecord r = new ActivityClientRecord();

    r.token = token;
    r.ident = ident;
    r.intent = intent;
    r.referrer = referrer;
    r.voiceInteractor = voiceInteractor;
    r.activityInfo = info;
    r.compatInfo = compatInfo;
    r.state = state;
    r.persistentState = persistentState;

    r.pendingResults = pendingResults;
    r.pendingIntents = pendingNewIntents;

    r.startsNotResumed = notResumed;
    r.isForward = isForward;

    r.profilerInfo = profilerInfo;

    r.overrideConfig = overrideConfig;
    updatePendingConfiguration(curConfig);

    sendMessage(H.LAUNCH_ACTIVITY, r);
}
```

scheduleLaunchActivity是在ActivityStackSupervisor的realStartActivityLocked中调的，此时还在AMS中：

```
app.thread.scheduleLaunchActivity(......);

```

这里app.thread是ApplicationThread，是ActivityThread的内部类，并且在ActivityThread中new了一个，并传入AMS中。
传的token是ActivityRecord的appToken，在ActivityThread的performLaunchActivity中attach到Activity的token。


再来看Activity的finish，

```
private void finish(boolean finishTask) {
    ActivityManagerNative.getDefault()
            .finishActivity(mToken, resultCode, resultData, finishTask);
}
```

再看ActivityManagerService，

```
@Override
public final boolean finishActivity(IBinder token, int resultCode, Intent resultData,
        boolean finishTask) {
    // Refuse possible leaked file descriptors
    
    synchronized(this) {
        ActivityRecord r = ActivityRecord.isInStackLocked(token);
    }
    
    ......
}
```

这里token是AMS中创建的，传给了Activity，再由Activity传回AMS后，token就变成了实体对象，而不是空壳代理。所以我们看ActivityRecord，

```
static ActivityRecord isInStackLocked(IBinder token) {
    final ActivityRecord r = ActivityRecord.forTokenLocked(token);
    return (r != null) ? r.task.stack.isInStackLocked(r) : null;
}
    
static ActivityRecord forTokenLocked(IBinder token) {
    try {
        return Token.tokenToActivityRecordLocked((Token)token);
    } catch (ClassCastException e) {
        Slog.w(TAG, "Bad activity token: " + token, e);
        return null;
    }
}

```

这里IBinder可以直接转成Token。如果这个Binder是代理就不能转token，这里是实体，所以是可以直接转的。

再看AMS回调到ActivityThread中后的操作，

```
private ActivityClientRecord performDestroyActivity(IBinder token, ......) {
        ActivityClientRecord r = mActivities.get(token);
        Class<? extends Activity> activityClass = null;
        activityClass = r.activity.getClass();
        ......
}
```

可见这里根据token到本地缓存里找出对应的ActivityClientRecord，然后执行对应的生命周期回调。

为什么要这么做呢？因为AMS要频繁和本地Activity交互，如果每次都传输一堆Activity的数据结构就太费事了，不如创建的时候AMS端和ActivityThread都保存好对应
的数据结构，同时约定一个句柄，之后所有通信都用该句柄，在缓存中取出对应的Activity数据结构，非常轻量。

可以看到scheduleLaunchActivity的注释:

```

// we use token to identify this activity without having to send the
// activity itself back to the activity manager. (matters more with ipc)
```