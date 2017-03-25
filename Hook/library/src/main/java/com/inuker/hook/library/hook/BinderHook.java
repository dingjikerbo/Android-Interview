package com.inuker.hook.library.hook;

import android.os.IBinder;

import com.inuker.hook.library.utils.BinderUtils;
import com.inuker.hook.library.utils.LogUtils;

import org.apache.commons.lang3.ClassUtils;

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

    /**
     * IServiceManager sServiceManager;
     */
    public BinderHook(Object object, BinderHookInvoker invoker) {
        this.originalInterface = object;
        this.originalBinder = BinderUtils.asBinder(object);
        this.binderIntfClazz = BinderUtils.getBinderInterface(object);
        this.proxyInterface = getProxyInterface(invoker);
        LogUtils.v(String.format("proxyInterface = %s", proxyInterface));
        this.proxyBinder = getProxyBinder();
    }

    public IBinder getProxyBinder() {
        if (proxyBinder == null) {
            proxyBinder = (IBinder) Proxy.newProxyInstance(IBinder.class.getClassLoader(),
                    new Class<?>[]{IBinder.class}, new BinderHookHandler(originalBinder, "BinderHook") {
                        @Override
                        public Object onInvoke(Object original, Method method, Object[] args) throws Throwable {
                            if (method.getName().equals("queryLocalInterface")) {
                                return proxyInterface;
                            }
                            return method.invoke(original, args);
                        }
                    });
        }
        return proxyBinder;
    }

    Object getProxyInterface(final BinderHookInvoker invoker) {
        if (proxyInterface != null) {
            throw new IllegalStateException();
        }

        ClassLoader loader = originalInterface.getClass().getClassLoader();
        proxyInterface = Proxy.newProxyInstance(loader,
                ClassUtils.getAllInterfaces(originalInterface.getClass()).toArray(new Class<?>[0]),
                new BinderHookHandler(originalInterface, "InterfaceHook") {
                    @Override
                    public Object onInvoke(Object original, Method method, Object[] args) throws Throwable {
                        return invoker.onInvoke(original, method, args);
                    }
                });
        return proxyInterface;
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
            if (method.getDeclaringClass().equals(Object.class)) {
                return method.invoke(originalObject, args);
            }

            LogUtils.v(String.format("Invoke %s: %s", tag, method));
            return onInvoke(originalObject, method, args);
        }
    }
}
