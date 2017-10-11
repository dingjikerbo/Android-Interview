package com.dingjikerbo.hook.test;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.Toast;

import com.dingjikerbo.hook.ICaller;
import com.dingjikerbo.hook.MyService;
import com.dingjikerbo.hook.utils.ToastUtils;
import com.inuker.hook.library.hook.BinderHook;

import java.lang.reflect.Method;

/**
 * Created by workstation on 17/4/6.
 */

public class BinderHookTester extends HookTester {

    private BinderHook<ICaller> mBinderHook;

    private ICaller mCaller;

    BinderHookTester(Context context) {
        super(context);

        Intent intent = new Intent();
        intent.setClass(context, MyService.class);
        context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mCaller = ICaller.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mCaller = null;
        }
    };

    private final BinderHook.BinderHookInvoker mInvoker = new BinderHook.BinderHookInvoker() {
        @Override
        public Object onInvoke(Object original, Method method, Object[] args) throws Throwable {
            return "hello world!";
        }
    };

    @Override
    public void hook() {
        mBinderHook = new BinderHook(mCaller, mInvoker);
        mCaller = mBinderHook.getProxyInterface();
    }

    @Override
    public void call() {
        try {
            ToastUtils.show(mCaller.sayHi("frank"));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void restore() {
        mCaller = mBinderHook.getOriginalInterface();
    }
}
