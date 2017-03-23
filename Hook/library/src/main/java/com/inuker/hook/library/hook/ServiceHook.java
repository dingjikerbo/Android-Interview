package com.inuker.hook.library.hook;

import android.os.IBinder;

import com.inuker.hook.library.hook.utils.LogUtils;

import org.apache.commons.lang.reflect.MethodUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by liwentian on 2017/3/23.
 */

public class ServiceHook {

    public String serviceName;

    public String binderClassName;

    private IBinder originalBinder;

    private Object originalIntf;

    private IBinder proxyBinder;

    private Object proxyIntf;

    public boolean cacheDirty;

    private Class<?> binderStubClazz;

    public ServiceHook(String serviceName, String binderClassName) {
        this.serviceName = serviceName;
        this.binderClassName = binderClassName;

        try {
            hook();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void hook() throws ClassNotFoundException {
        Class<?> serviceManagerClaz = Class.forName("android.os.ServiceManager");
        originalBinder = (IBinder) MethodUtils.invokeMethod(serviceManagerClaz, "getService", serviceName);
        binderStubClazz = Class.forName(binderClassName + "$Stub");
        originalIntf = MethodUtils.invokeMethod(binderStubClazz, "asInterface", originalBinder);
        proxyIntf = getProxyIntf();
        proxyBinder = getProxyBinder();
    }

    private Object getProxyIntf() {
        return Proxy.newProxyInstance(ServiceHook.class.getClassLoader(),
                originalIntf.getClass().getInterfaces(), new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        LogUtils.v(String.format("invoke: %s", method.getName()));
                        return method.invoke(originalIntf, args);
                    }
                });
    }

    private IBinder getProxyBinder() {
        return (IBinder) Proxy.newProxyInstance(ServiceHook.class.getClassLoader(),
                new Class<?>[]{IBinder.class}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if (method.getName().equals("queryLocalInterface")) {
                            return proxyIntf;
                        }
                        return method.invoke(originalBinder, args);
                    }
                });
    }
}
