package com.dingjikerbo.hook;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.view.View;

import com.inuker.hook.library.compat.ServiceManagerCompat;
import com.inuker.hook.library.hook.BinderHook;
import com.inuker.hook.library.hook.PowerManagerHook;
import com.inuker.hook.library.hook.ServiceManagerHook;
import com.inuker.hook.library.utils.LogUtils;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

public class MainActivity extends Activity {

    private void testPowerManager() throws Exception {
        Class<?> clazz = Class.forName("android.os.IPowerManager");
        Method method = MethodUtils.getAccessibleMethod(clazz, "asBinder");


        PowerManager manager1 = (PowerManager) getSystemService(POWER_SERVICE);
        Object object1 = FieldUtils.getField(PowerManager.class, "mService", true).get(manager1);
        IBinder binder1 = (IBinder) method.invoke(object1);
        LogUtils.v(String.format("manager1 = %s, object1 = %s, binder1 = %s", manager1, object1, binder1));

        PowerManager manager2 = (PowerManager) getApplicationContext().getSystemService(POWER_SERVICE);
        Object object2 = FieldUtils.getField(PowerManager.class, "mService", true).get(manager2);
        IBinder binder2 = (IBinder) method.invoke(object2);
        LogUtils.v(String.format("manager2 = %s, object2 = %s, binder2 = %s", manager2, object2, binder2));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        PowerManager manager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock lock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "hello");
        lock.acquire();
    }

    private void hook() {
//        ServiceManagerHook.hook();

        try {
            PowerManagerHook.hook(this);
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
