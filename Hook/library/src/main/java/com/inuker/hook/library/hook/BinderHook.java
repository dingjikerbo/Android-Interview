package com.inuker.hook.library.hook;

import android.os.IBinder;

import com.inuker.hook.library.utils.LogUtils;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by liwentian on 2017/3/24.
 */

public class BinderHook {

    public Class<?>[] binderIntfClazz;

    public IBinder originalBinder;

    public IBinder proxyBinder;

    public Object originalInterface;

    public Object proxyInterface;

    /**
     * IServiceManager sServiceManager;
     */
    public BinderHook(Object object, BinderHookInvoker invoker) {
        binderIntfClazz = (Class<?>[]) ClassUtils.getAllInterfaces(object.getClass()).toArray(new Class<?>[0]);
        originalInterface = object;

        try {
            originalBinder = (IBinder) MethodUtils.invokeMethod(object, "asBinder");
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        proxyInterface = getProxyInterface(invoker);
        proxyBinder = getProxyBinder();
    }

//    BinderHook(IBinder originalBinder, Class<?> binderIntfClazz) {
//        this.originalBinder = originalBinder;
//        this.binderIntfClazz = binderIntfClazz;
//
//        Class<?> stubClazz = ReflectionUtils.getClass(binderIntfClazz.getName() + "$Stub");
//        originalInterface = MethodUtils.invokeMethod(stubClazz, "asInterface", originalBinder);
//    }

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
                binderIntfClazz, new BinderHookHandler(originalInterface, "InterfaceHook") {
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

    public abstract static class BinderHookHandler implements InvocationHandler, BinderHookInvoker {

        Object originalObject;

        String tag;

        public BinderHookHandler(Object originalObject, String tag) {
            this.tag = tag;
            this.originalObject = originalObject;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            LogUtils.v(String.format("Tag %s: invoke: %s", tag, method));

            if (method.getDeclaringClass().equals(Object.class)) {
                return method.invoke(originalObject, args);
            }

            return onInvoke(originalObject, method, args);
        }
    }
}
