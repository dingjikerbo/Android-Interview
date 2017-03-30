package com.dingjikerbo.hook;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ReceiverCallNotAllowedException;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;

import com.inuker.hook.library.compat.ServiceManagerCompat;
import com.inuker.hook.library.hook.BinderHook;
import com.inuker.hook.library.hook.PowerManagerHook;
import com.inuker.hook.library.hook.ReceiverHook;
import com.inuker.hook.library.hook.ServiceManagerHook;
import com.inuker.hook.library.utils.LogUtils;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

public class MainActivity extends Activity {

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                LogUtils.e(String.format("onReceive: %s", intent.getAction()));
            }
        };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        IntentFilter filter = new IntentFilter();
        filter.addAction("hello");
        registerReceiver(mReceiver, filter);

        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                hook();
            }
        });

        findViewById(R.id.btn2).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                call();
            }
        });

//        try {
//            Class<?> clazz = Class.forName("android.os.IPowerManager$Stub$Proxy");
//            Method method = MethodUtils.getMatchingMethod(clazz, "asBinder");
//
//            Object obj1 = field.get(manager);
//            Object obj2 = field.get(manager2);
//
//            LogUtils.v(String.format("%s -> %s", method.invoke(obj1), method.invoke(obj2)));
//            LogUtils.v(String.format("manager = %s, mService = %s", manager, obj1));
//            LogUtils.v(String.format("manager2 = %s, mService2 = %s", manager2, obj2));
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        }
    }

    private void call() {
        Intent intent = new Intent(this, TestActivity.class);
        startActivity(intent);

        Intent intent2 = new Intent("hello");
        sendBroadcast(intent2);
    }

    private void hook() {
//        ServiceManagerHook.hook();

        try {
            ReceiverHook.hook();
        } catch (Throwable e) {
            e.printStackTrace();
        }

//        LogUtils.v(String.format("WakeLock %s", lock));
//
//        ServiceManagerHook.hook(new BinderHook.BinderHookInvoker() {
//            @Override
//            public Object onInvoke(Object original, Method method, Object[] args) throws Throwable {
//                LogUtils.v(String.format("onInvoke %s -> %s()", original, method));
//
//                if (method.getName().equals("getService")) {
//                    return new Response.Stub() {
//
//                        @Override
//                        public void onResponse(int code) throws RemoteException {
//                            LogUtils.v(String.format("code is %d", code));
//                        }
//                    };
//                }
//
//                return method.invoke(original, args);
//            }
//        });
//
//        IBinder binder = null;
//        try {
//            Class<?> clazz = ServiceManagerCompat.getServiceManagerClazz();
//            HashMap<String, IBinder> cache = (HashMap) FieldUtils.getField(clazz, "sCache", true).get(null);
//            String name = "bluetooth_manager";
//            LogUtils.v(String.format("contains %b", cache.containsKey(name)));
//            binder = (IBinder) MethodUtils.invokeStaticMethod(clazz, "getService", name);
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        }
//        LogUtils.v(String.format("binder is %s", binder));
//        try {
//            IResponse.Stub.asInterface(binder).onResponse(4);
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }

//        Response response = new Response();
//
//        BinderHook hookedBinder = new BinderHook(response, new BinderHook.BinderHookHandler() {
//            @Override
//            public Object invoke(Object original, Method method, Object[] args) {
//                LogUtils.v(String.format("invoke here: %s, %s", method.getName(), method.getDeclaringClass()));
//                return null;
//            }
//        });
//
//        IBinder binder = response.asBinder();
//        IResponse response2 = IResponse.Stub.asInterface(binder);
//        LogUtils.v(String.format("%s -> (%s)", response2, hookedBinder.proxyInterface));
//
//        try {
//            IResponse proxy = (IResponse) hookedBinder.proxyInterface;
//            proxy.onResponse(3);
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }
    }
}
