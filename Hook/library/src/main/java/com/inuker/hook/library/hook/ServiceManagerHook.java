package com.inuker.hook.library.hook;

import android.os.IBinder;
import android.os.IInterface;

import com.inuker.hook.library.hook.utils.LogUtils;

import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.commons.lang.reflect.MethodUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.Provider;
import java.util.HashMap;

/**
 * Created by liwentian on 2017/3/23.
 */

public class ServiceManagerHook {

    private static HashMap<String, ServiceHook> binderCache = new HashMap<>();

    private static Class<?> iserviceManagerClaz;

    private static Class<?> serviceManagerClaz;

    private static Object sServiceManager;

    private static HashMap<String, IBinder> sCache;

    public static void hook() throws Exception {
        serviceManagerClaz = Class.forName("android.os.ServiceManager");
        iserviceManagerClaz = Class.forName("android.os.IServiceManager");
        Field field = FieldUtils.getField(serviceManagerClaz, "sServiceManager", true);
        sServiceManager = field.get(null);
        field.set(null, getProxyServiceManager());

        Field sCacheField = FieldUtils.getField(serviceManagerClaz, "sCache", true);
        sCache = (HashMap<String, IBinder>) sCacheField.get(null);
    }

    public static void hookService(final String serviceName, final String binderClassName) {
        ServiceHook serviceHook = binderCache.get(serviceName);
        if (serviceHook == null) {
            serviceHook = new ServiceHook(serviceName, binderClassName);
            binderCache.put(serviceName, serviceHook);
        }
    }

    private static Object getProxyServiceManager() {
        return Proxy.newProxyInstance(getClassLoader(),
                new Class<?>[]{iserviceManagerClaz, IInterface.class}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        LogUtils.v(String.format("invoke %s", method));
                        return method.invoke(sServiceManager, args);
                    }
                });
    }

    private static ClassLoader getClassLoader() {
        return ServiceManagerHook.class.getClassLoader();
    }
}
