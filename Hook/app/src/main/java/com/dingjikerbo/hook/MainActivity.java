package com.dingjikerbo.hook;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.view.View;

import com.inuker.hook.library.IResponse;
import com.inuker.hook.library.compat.ServiceManagerCompat;
import com.inuker.hook.library.hook.BinderHook;
import com.inuker.hook.library.hook.ServiceManagerHook;
import com.inuker.hook.library.utils.LogUtils;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                onclick();
            }
        });
    }

    private void onclick() {
        ServiceManagerHook.hook(new BinderHook.BinderHookHandler() {
            @Override
            public Object invoke(Object original, Method method, Object[] args) {
                LogUtils.v(String.format("%s.%s()", original, method));
                IBinder binder =  new Response.Stub() {

                    @Override
                    public void onResponse(int code) throws RemoteException {
                        LogUtils.v(String.format("code is %d", code));
                    }
                };
                LogUtils.v(String.format("binder here is %s", binder));
                return binder;
            }
        });

        IBinder binder = null;
        try {
            Class<?> clazz = ServiceManagerCompat.getServiceManagerClazz();
            HashMap<String, IBinder> cache = (HashMap) FieldUtils.getField(clazz, "sCache", true).get(null);
            String name = "bluetooth_manager";
            LogUtils.v(String.format("contains %b", cache.containsKey(name)));
            binder = (IBinder) MethodUtils.invokeStaticMethod(clazz, "getService", name);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        LogUtils.v(String.format("binder is %s", binder));
        try {
            IResponse.Stub.asInterface(binder).onResponse(4);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

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
