动态注册BroadcastReceiver，实现在ContextImpl中，如下：

```
@Override
public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
    return registerReceiver(receiver, filter, null, null);
}

@Override
public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter,
        String broadcastPermission, Handler scheduler) {
    return registerReceiverInternal(receiver, getUserId(),
            filter, broadcastPermission, scheduler, getOuterContext());
}

private Intent registerReceiverInternal(BroadcastReceiver receiver, int userId,
            IntentFilter filter, String broadcastPermission,
            Handler scheduler, Context context) {
    IIntentReceiver rd = null;
    if (receiver != null) {
        if (mPackageInfo != null && context != null) {
            if (scheduler == null) {
                scheduler = mMainThread.getHandler();
            }
            rd = mPackageInfo.getReceiverDispatcher(
                receiver, context, scheduler,
                mMainThread.getInstrumentation(), true);
        } else {
            if (scheduler == null) {
                scheduler = mMainThread.getHandler();
            }
            rd = new LoadedApk.ReceiverDispatcher(
                    receiver, context, scheduler, null, true).getIIntentReceiver();
        }
    }
    try {
        return ActivityManagerNative.getDefault().registerReceiver(
                mMainThread.getApplicationThread(), mBasePackageName,
                rd, filter, broadcastPermission, userId);
    } catch (RemoteException e) {
        return null;
    }
}
```

可见这里先获取IIntentReceiver，然后注册到AMS中。mPackageInfo就是LoadedApk，这里通常不为空，所以直接调到getReceiverDispatcher。这个IIntentReceiver定义如下：

```
oneway interface IIntentReceiver {
    void performReceive(in Intent intent, int resultCode, String data,
            in Bundle extras, boolean ordered, boolean sticky, int sendingUser);
}
```

既然定义为AIDL，看来是为了跨进程调用的。

这里我们要研究的是LoadedApk.ReceiverDispatcher，LoadedApk中缓存了所有的Receiver，如下：

```
private final ArrayMap<Context, ArrayMap<BroadcastReceiver, ReceiverDispatcher>> mReceivers
        = new ArrayMap<Context, ArrayMap<BroadcastReceiver, LoadedApk.ReceiverDispatcher>>();
        
public IIntentReceiver getReceiverDispatcher(BroadcastReceiver r, Context context, Handler handler, Instrumentation instrumentation, boolean registered) {
    synchronized (mReceivers) {
        LoadedApk.ReceiverDispatcher rd = null;
        ArrayMap<BroadcastReceiver, LoadedApk.ReceiverDispatcher> map = null;
        if (registered) {
            map = mReceivers.get(context);
            if (map != null) {
                rd = map.get(r);
            }
        }
        if (rd == null) {
            rd = new ReceiverDispatcher(r, context, handler,
                    instrumentation, registered);
            if (registered) {
                if (map == null) {
                    map = new ArrayMap<BroadcastReceiver, LoadedApk.ReceiverDispatcher>();
                    mReceivers.put(context, map);
                }
                map.put(r, rd);
            }
        } else {
            rd.validate(context, handler);
        }
        rd.mForgotten = false;
        return rd.getIIntentReceiver();
    }
}
```

这里每个LoadedApk中，每个Context都对应一个ReceiverDispatcher列表，这里如果缓存中有就返回，没有就创建一个缓存起来。

我们重点关注ReceiverDispatcher这个类，

```
static final class ReceiverDispatcher {

    final static class InnerReceiver extends IIntentReceiver.Stub {
        final WeakReference<LoadedApk.ReceiverDispatcher> mDispatcher;
        final LoadedApk.ReceiverDispatcher mStrongRef;

        InnerReceiver(LoadedApk.ReceiverDispatcher rd, boolean strong) {
            mDispatcher = new WeakReference<LoadedApk.ReceiverDispatcher>(rd);
            mStrongRef = strong ? rd : null;
        }
        
        public void performReceive(Intent intent, int resultCode, String data,
                Bundle extras, boolean ordered, boolean sticky, int sendingUser) {
            LoadedApk.ReceiverDispatcher rd = mDispatcher.get();
           
            rd.performReceive(intent, resultCode, data, extras,
                    ordered, sticky, sendingUser);
        }
    }

    final IIntentReceiver.Stub mIIntentReceiver;
    final BroadcastReceiver mReceiver;
    final Context mContext;
    final Handler mActivityThread;
    final Instrumentation mInstrumentation;
    final boolean mRegistered;
    final IntentReceiverLeaked mLocation;
    RuntimeException mUnregisterLocation;
    boolean mForgotten;

    final class Args extends BroadcastReceiver.PendingResult implements Runnable {
        private Intent mCurIntent;
        private final boolean mOrdered;

        public Args(Intent intent, int resultCode, String resultData, Bundle resultExtras,
                boolean ordered, boolean sticky, int sendingUser) {
            super(resultCode, resultData, resultExtras,
                    mRegistered ? TYPE_REGISTERED : TYPE_UNREGISTERED, ordered,
                    sticky, mIIntentReceiver.asBinder(), sendingUser, intent.getFlags());
            mCurIntent = intent;
            mOrdered = ordered;
        }
        
        public void run() {
            final BroadcastReceiver receiver = mReceiver;
            final boolean ordered = mOrdered;
            
            final IActivityManager mgr = ActivityManagerNative.getDefault();
            final Intent intent = mCurIntent;
            mCurIntent = null;
            
            ClassLoader cl =  mReceiver.getClass().getClassLoader();
            intent.setExtrasClassLoader(cl);
            setExtrasClassLoader(cl);
            receiver.setPendingResult(this);
            receiver.onReceive(mContext, intent);
            
            if (receiver.getPendingResult() != null) {
                finish();
            }
        }
    }

    ReceiverDispatcher(BroadcastReceiver receiver, Context context,
            Handler activityThread, Instrumentation instrumentation,
            boolean registered) {
        mIIntentReceiver = new InnerReceiver(this, !registered);
        mReceiver = receiver;
        mContext = context;
        mActivityThread = activityThread;
        mInstrumentation = instrumentation;
        mRegistered = registered;
        mLocation = new IntentReceiverLeaked(null);
        mLocation.fillInStackTrace();
    }

    BroadcastReceiver getIntentReceiver() {
        return mReceiver;
    }

    IIntentReceiver getIIntentReceiver() {
        return mIIntentReceiver;
    }

    public void performReceive(Intent intent, int resultCode, String data,
            Bundle extras, boolean ordered, boolean sticky, int sendingUser) {
        Args args = new Args(intent, resultCode, data, extras, ordered,
                sticky, sendingUser);
        mActivityThread.post(args);
    }

}
```


到这里整个流程大概清楚了，Receiver是不能跨进程传递的，但是这里又要注册到AMS中参与事件分发，所以就委托给了IIntentReceiver，他持有ReceiverDispatcher的应用。当AMS端触发了
IIntentReceiver的performReceive，就会调到了本地的LoadedApk中的Receiver。我们可以看到ReceiverDispatcher的performReceive其实是封装了一个Runnable丢到ActivityThread
的主线程执行了。难怪我拦截了ActivityThread的handleMessage但是没有拦截到动态注册的广播的onReceive，只拦截到了静态广播。可见动态广播是通过post Runnable到主线程，静态广播
是通过发送Message到主线程。接下来，我们进一步印证这一点。
 
 
我们全局搜索RECEIVER，发现在ApplicationThreadNative中，这是个Stub类，其onTransact中有关于广播的处理，消息为SCHEDULE_RECEIVER_TRANSACTION，调到了scheduleReceiver，
这个ApplicationThreadNative是个抽象的Stub类，其具体实现在ActivityThread的内部类ApplicationThread中，其scheduleReceiver如下：

```
public final void scheduleReceiver(Intent intent, ActivityInfo info,
                CompatibilityInfo compatInfo, int resultCode, String data, Bundle extras,
                boolean sync, int sendingUser, int processState) {
    updateProcessState(processState, false);
    ReceiverData r = new ReceiverData(intent, resultCode, data, extras,
            sync, false, mAppThread.asBinder(), sendingUser);
    r.info = info;
    r.compatInfo = compatInfo;
    sendMessage(H.RECEIVER, r);
}
```

可见这里果然向主线程发送了RECEIVER消息。

那现在问题就在于为何静态广播会走ApplicationThreadNative，而动态广播会走ReceiverDispatcher。我们搜索SCHEDULE_RECEIVER_TRANSACTION，看该消息是哪里触发的，
是ApplicationThreadProxy中调用scheduleReceiver触发的，而这又是由BroadcastQueue中的processCurBroadcastLocked触发的。

我们研究BroadcastQueue，发现其中有个BroadcastHandler，如下：

```
final BroadcastHandler mHandler;

private final class BroadcastHandler extends Handler {
    public BroadcastHandler(Looper looper) {
        super(looper, null, true);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case BROADCAST_INTENT_MSG: {
                processNextBroadcast(true);
            } break;
            case BROADCAST_TIMEOUT_MSG: {
                synchronized (mService) {
                    broadcastTimeoutLocked(true);
                }
            } break;
        }
    }
};
```

静态广播由于是通过Message发到主线程的，所以可以被拦截。而动态广播是通过Runnable发到主线程的，暂时没找到拦截的办法。不过我们并不关心动态广播，因为
自定义的广播我们没必要去拦截。我们需要拦截的主要是系统的唤醒广播。