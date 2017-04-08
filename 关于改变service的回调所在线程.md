在LoadedApk中有一个

```
private static class InnerConnection extends IServiceConnection.Stub {
    final WeakReference<LoadedApk.ServiceDispatcher> mDispatcher;

    InnerConnection(LoadedApk.ServiceDispatcher sd) {
        mDispatcher = new WeakReference<LoadedApk.ServiceDispatcher>(sd);
    }

    public void connected(ComponentName name, IBinder service) throws RemoteException {
        LoadedApk.ServiceDispatcher sd = mDispatcher.get();
        if (sd != null) {
            sd.connected(name, service);
        }
    }
}
```

这个IServiceConnection定义如下：

```
oneway interface IServiceConnection {
    void connected(in ComponentName name, IBinder service);
}
```

所以这个InnerConnection是注册到AMS中的，然后被AMS调到APP进程，执行onServiceConnected回调。每个ServiceDispatcher中都会创建
一个InnerConnection，那么这个ServiceDispatcher又是在哪里创建的呢？还是在LoadedApk中，

```
public final IServiceConnection getServiceDispatcher(ServiceConnection c,
        Context context, Handler handler, int flags) {
    synchronized (mServices) {
        LoadedApk.ServiceDispatcher sd = null;
        ArrayMap<ServiceConnection, LoadedApk.ServiceDispatcher> map = mServices.get(context);
        if (map != null) {
            sd = map.get(c);
        }
        if (sd == null) {
            sd = new ServiceDispatcher(c, context, handler, flags);
            if (map == null) {
                map = new ArrayMap<ServiceConnection, LoadedApk.ServiceDispatcher>();
                mServices.put(context, map);
            }
            map.put(c, sd);
        } else {
            sd.validate(context, handler);
        }
        return sd.getIServiceConnection();
    }
}
```

而这个getServiceDispatcher又是被谁调用的呢？这次是在ContextImpl中，我们平时调用bindService最终实现就是在这里。

```
private boolean bindServiceCommon(Intent service, ServiceConnection conn, int flags,
        UserHandle user) {
    IServiceConnection sd = mPackageInfo.getServiceDispatcher(conn, getOuterContext(), mMainThread.getHandler(), flags);
    IBinder token = getActivityToken();
    ActivityManagerNative.getDefault().bindService(
        mMainThread.getApplicationThread(), getActivityToken(), service,
        service.resolveTypeIfNeeded(getContentResolver()),
        sd, flags, getOpPackageName(), user.getIdentifier());
}
```

好了，现在我们再看什么时候会触发InnerConnection的connected回调，结果是在ActiveServices中，这个是AMS进程中的。

所以，如果要改变ServiceConnection的回调所在线程，就得再ServiceDispatcher中做文章，这是其中的connected函数：

```
public void connected(ComponentName name, IBinder service) {
    if (mActivityThread != null) {
        mActivityThread.post(new RunConnection(name, service, 0));
    } else {
        doConnected(name, service);
    }
}

```

可见这里会post一个RunConnection到UI线程，这是个Runnable。我们知道Runnable形式的Message是不好Hook的。我们只能打ServiceDispatcher的主意。

不过看了一下，没找到好的Hook点，看来得寻求更高级的Hook技术了，可以参考Dexposed。
