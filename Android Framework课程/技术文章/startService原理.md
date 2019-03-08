

ActivityManagerService.startService
-> ActiveServices.startServiceLocked


```
ComponentName startServiceLocked(IApplicationThread caller, Intent service) {
    return startServiceInnerLocked(smap, service, r, callerFg, addToStarting);
}

ComponentName startServiceInnerLocked(ServiceMap smap, Intent service) {
    bringUpServiceLocked(r, service.getFlags(), callerFg, false);
    return r.name;
}

// 如果进程没启动，则先启动进程，然后给serviceRecord加到pendingServices里，等进程启动之后，attachApplication的时候再来处理这些pendingServices，执行realStartServiceLocked
// 如果进程已经启动了，就真正的startService
private final String bringUpServiceLocked(ServiceRecord r) {
    if (r.app != null && r.app.thread != null) {
        sendServiceArgsLocked(r, execInFg, false);
        return null;
    }
    app = mAm.getProcessRecordLocked(procName, r.appInfo.uid, false);
    if (app != null && app.thread != null) {
        realStartServiceLocked(r, app, execInFg);
        return null;
    }

    if (app == null) {
        mAm.startProcessLocked(procName, r.appInfo, true, intentFlags);
    }

    if (!mPendingServices.contains(r)) {
        mPendingServices.add(r);
    }

    return null;
}

// 这里启动service，执行service的onCreate回调
// 注意这里
private final void realStartServiceLocked(ServiceRecord r) {
    app.thread.scheduleCreateService(r, r.serviceInfo,
            mAm.compatibilityInfoForPackageLocked(r.serviceInfo.applicationInfo),
            app.repProcState);

    requestServiceBindingsLocked(r, execInFg);
}

// 这里只是向应用发起一个请求，等待应用主动给binder publish到AMS
private final boolean requestServiceBindingLocked(ServiceRecord r) {
    r.app.thread.scheduleBindService(r, i.intent.getIntent(), rebind,
            r.app.repProcState);
    return true;
}
```