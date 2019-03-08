


ActivityManagerService.bindService
-> ActivityServices.bindServiceLocked
-> ActivityServices.requestServiceBindingLocked
-> IApplicationThread.scheduleBindService
-> ActivityThread.scheduleBindService
-> ActivityThread.handleBindService
-> ActivityManagerNative.publishService:给service的onbind的句柄发布到AMS
-> ActivityServices.publishServiceLocked，这里面会将这个binder分发出去

```
// ActivityManagerService
public int bindService() {
        synchronized(this) {
            return mServices.bindServiceLocked(caller, token, service,
                    resolvedType, connection, flags, callingPackage, userId);
        }
    }
```

这个mServices是ActiveServices类，

```
// ActivityServices
// 如果进程已经存在，则真正启动服务，如果AMS中binder已经OK了，则直接回调给应用
// 如果AMS中binder还没OK，则命令服务publish自己的Binder到AMS，publish的时候，遍历所有的connectionRecord，调用回调
int bindServiceLocked() {
        ServiceLookupResult res = retrieveServiceLocked();
        ServiceRecord s = res.record;

        ......
        if ((flags&Context.BIND_AUTO_CREATE) != 0) {
            bringUpServiceLocked(s, service.getFlags(), callerFg, false);
        }

        if (s.app != null && b.intent.received) {
            // Service is already running, so we can immediately
            // publish the connection.
            c.conn.connected(s.name, b.intent.binder);
        } else if (!b.intent.requested) {
            requestServiceBindingLocked(s, b.intent, callerFg, false);
        }
    }
```

如果flags带BIND_AUTO_CREATE的，就先bringUpServiceLocked启动一下，

```
// 拉起一个服务，如果该服务进程存在，则执行真正的拉起，即回调生命周期，查询该服务的binder句柄
否则启动服务进程，将这个服务的ServiceRecord加到pending列表，等进程启动好了，再执行真正的拉起。
private final String bringUpServiceLocked() {
    app = mAm.getProcessRecordLocked(procName, r.appInfo.uid, false);

    if (app != null && app.thread != null) {
        realStartServiceLocked(r, app, execInFg);
        return null;
    }

    // Not running -- get it started, and enqueue this service record
    // to be executed when the app comes up.
    if (app == null) {
        mAm.startProcessLocked(procName, r.appInfo, true, intentFlags,
                "service", r.name, false, isolated, false);
    }

    if (!mPendingServices.contains(r)) {
        mPendingServices.add(r);
    }

    return null;
}
```

再看下realStartServiceLocked函数，

```
private final void realStartServiceLocked() {
    app.thread.scheduleCreateService(r, r.serviceInfo,
            mAm.compatibilityInfoForPackageLocked(r.serviceInfo.applicationInfo),
            app.repProcState);

    requestServiceBindingsLocked(r, execInFg);
}

private final boolean requestServiceBindingLocked() {
    if (r.app == null || r.app.thread == null) {
        // If service is not currently running, can't yet bind.
        return false;
    }
    if ((!i.requested || rebind) && i.apps.size() > 0) {
        r.app.thread.scheduleBindService(r, i.intent.getIntent());
    }
    return true;
}
```

serviceRecord里有一个IntentBindRecord的表，还有一个ConnectionRecord的表，


[Service的一些迷思](https://juejin.im/post/5be53084e51d45709d2efb6f)