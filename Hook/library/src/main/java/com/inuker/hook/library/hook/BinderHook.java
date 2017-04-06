package com.inuker.hook.library.hook;

import android.os.IBinder;

import com.inuker.hook.library.utils.BinderUtils;
import com.inuker.hook.library.utils.LogUtils;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by liwentian on 2017/3/24.
 */

public class BinderHook {

    public Class<?> binderIntfClazz;

    public Object originalInterface;

    public IBinder originalBinder;

    public Object proxyInterface;

    public IBinder proxyBinder;

    public BinderHook(Object object, BinderHookInvoker invoker) {
        this.originalInterface = object;
        LogUtils.v(String.format("originalInterface: %s", originalInterface));

        this.binderIntfClazz = BinderUtils.getBinderInterface(object);
        LogUtils.v(String.format("binderIntfClazz: %s", binderIntfClazz));

        this.originalBinder = getOriginalBinder();
        LogUtils.v(String.format("originalBinder: %s", originalBinder));

        this.proxyInterface = getProxyInterface(invoker);
        LogUtils.v(String.format("proxyInterface = %s", proxyInterface));

        this.proxyBinder = getProxyBinder();
        LogUtils.v(String.format("proxyBinder = %s", proxyBinder));
    }

    private IBinder getOriginalBinder() {
        Method method = MethodUtils.getAccessibleMethod(binderIntfClazz, "asBinder");
        try {
            return (IBinder) method.invoke(originalInterface);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    private IBinder getProxyBinder() {
        return (IBinder) Proxy.newProxyInstance(IBinder.class.getClassLoader(),
                new Class<?>[]{IBinder.class}, new BinderHookHandler(originalBinder, "Binder") {
                    @Override
                    public Object onInvoke(Object original, Method method, Object[] args) throws Throwable {
                        if (method.getName().equals("queryLocalInterface")) {
                            return proxyInterface;
                        }
                        return method.invoke(original, args);
                    }
                });
    }

    private Object getProxyInterface(final BinderHookInvoker invoker) {
        return Proxy.newProxyInstance(originalInterface.getClass().getClassLoader(),
                ClassUtils.getAllInterfaces(originalInterface.getClass()).toArray(new Class<?>[0]),
                new BinderHookHandler(originalInterface, "Interface") {
                    @Override
                    public Object onInvoke(Object original, Method method, Object[] args) throws Throwable {
                        return invoker.onInvoke(original, method, args);
                    }
                });
    }

    public interface BinderHookInvoker {
        Object onInvoke(Object original, Method method, Object[] args) throws Throwable;
    }

    abstract static class BinderHookHandler implements InvocationHandler, BinderHookInvoker {

        Object originalObject;

        String tag;

        public BinderHookHandler(Object originalObject, String tag) {
            this.tag = tag;
            this.originalObject = originalObject;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (Object.class == method.getDeclaringClass()) {
                String name = method.getName();
                if ("equals".equals(name)) {
                    return proxy == args[0];
                } else if ("hashCode".equals(name)) {
                    return System.identityHashCode(proxy);
                } else if ("toString".equals(name)) {
                    return proxy.getClass().getName() + "@" +
                            Integer.toHexString(System.identityHashCode(proxy));
                } else {
                    throw new IllegalStateException(String.valueOf(method));
                }
            }

            LogUtils.w(String.format("Intercepted %s >>> %s", tag, method));
            return onInvoke(originalObject, method, args);
        }
    }

    public <T> T getProxyInterface() {
        return (T) proxyInterface;
    }

    public <T> T getOriginalInterface() {
        return (T) originalInterface;
    }
}
