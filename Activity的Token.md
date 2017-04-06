

以下是Instrumentation中：
```
public ActivityResult execStartActivity(
        Context who, IBinder contextThread, IBinder token, Activity target,
        Intent intent, int requestCode, Bundle options) {
    IApplicationThread whoThread = (IApplicationThread) contextThread;
    
    ...... 
    
    int result = ActivityManagerNative.getDefault()
        .startActivity(whoThread, who.getBasePackageName(), intent,
                intent.resolveTypeIfNeeded(who.getContentResolver()),
                token, target != null ? target.mEmbeddedID : null,
                requestCode, 0, null, null, options);
}
```

然后调到ActivityManagerService中：

```
@Override
public final int startActivity(IApplicationThread caller, String callingPackage,
        Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode,
        int startFlags, ProfilerInfo profilerInfo, Bundle options) {
    return startActivityAsUser(caller, callingPackage, intent, resolvedType, resultTo,
        resultWho, requestCode, startFlags, profilerInfo, options,
        UserHandle.getCallingUserId());
}

@Override
public final int startActivityAsUser(IApplicationThread caller, String callingPackage,
        Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode,
        int startFlags, ProfilerInfo profilerInfo, Bundle options, int userId) {
    enforceNotIsolatedCaller("startActivity");
    userId = handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId,
            false, ALLOW_FULL_ONLY, "startActivity", null);
    // TODO: Switch to user app stacks here.
    return mStackSupervisor.startActivityMayWait(caller, -1, callingPackage, intent,
            resolvedType, null, null, resultTo, resultWho, requestCode, startFlags,
            profilerInfo, null, null, options, false, userId, null, null);
}
```

再来看ActivityStackSupervisor的startActivityMayWait:

```
int res = startActivityLocked(caller, intent, resolvedType, aInfo,
    voiceSession, voiceInteractor, resultTo, resultWho,
    requestCode, callingPid, callingUid, callingPackage,
    realCallingPid, realCallingUid, startFlags, options, ignoreTargetSecurity,
    componentSpecified, null, container, inTask);
```

在startActivityLocked中初始化的ActivityRecord，如下：

```
ActivityRecord r = new ActivityRecord(mService, callerApp, callingUid, callingPackage,
                intent, resolvedType, aInfo, mService.mConfiguration, resultRecord, resultWho,
                requestCode, componentSpecified, voiceSession != null, this, container, options);
                
ActivityRecord(ActivityManagerService _service, ProcessRecord _caller,
            int _launchedFromUid, String _launchedFromPackage, Intent _intent, String _resolvedType,
            ActivityInfo aInfo, Configuration _configuration,
            ActivityRecord _resultTo, String _resultWho, int _reqCode,
            boolean _componentSpecified, ActivityStackSupervisor supervisor) {
        service = _service;
        appToken = new Token(this);
}

static class Token extends IApplicationToken.Stub {
        final WeakReference<ActivityRecord> weakActivity;

        Token(ActivityRecord activity) {
            weakActivity = new WeakReference<ActivityRecord>(activity);
        }
}

```



这个appToken是ActivityRecord中的token，看注释是说WindowManager的token。而ActivityClientRecord中也有一个token，
Activity中的mToken是在ActivityThread.attach中设置的，对应的是ActivityClientRecord中的token。这是在哪里设置的呢？
在ActivityThread的scheduleLaunchActivity中，如下：

```
 @Override
public final void scheduleLaunchActivity(Intent intent, IBinder token, int ident,
        ActivityInfo info, Configuration curConfig, Configuration overrideConfig,
        CompatibilityInfo compatInfo, String referrer, IVoiceInteractor voiceInteractor,
        int procState, Bundle state, PersistableBundle persistentState,
        List<ResultInfo> pendingResults, List<ReferrerIntent> pendingNewIntents,
        boolean notResumed, boolean isForward, ProfilerInfo profilerInfo) {

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

这是在ActivityStackSupervisor的realStartActivityLocked中调的：

```
app.thread.scheduleLaunchActivity(new Intent(r.intent), r.appToken,
                    System.identityHashCode(r), r.info, new Configuration(mService.mConfiguration),
                    new Configuration(stack.mOverrideConfig), r.compat, r.launchedFromPackage,
                    task.voiceInteractor, app.repProcState, r.icicle, r.persistentState, results,
                    newIntents, !andResume, mService.isNextTransitionForward(), profilerInfo);

```

这里app.thread是ApplicationThread，是ActivityThread的内部类。传的token是ActivityRecord的appToken。


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