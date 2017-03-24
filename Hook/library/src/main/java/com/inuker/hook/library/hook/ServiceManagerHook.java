package com.inuker.hook.library.hook;

import com.inuker.hook.library.compat.ServiceManagerCompat;

import java.lang.reflect.Method;

/**
 * Created by liwentian on 2017/3/23.
 */

public class ServiceManagerHook {

    private static BinderHook mHookedBinder;

    public static void hook(BinderHook.BinderHookHandler handler) {
        Object sServiceManager = ServiceManagerCompat.getsServiceManager();
        mHookedBinder = new BinderHook(sServiceManager, new BinderHook.BinderHookInvoker() {
            @Override
            public Object onInvoke(Object original, Method method, Object[] args) throws Throwable {
                return null;
            }
        });

        try {
            ServiceManagerCompat.getsServiceManagerField().set(null, mHookedBinder.proxyInterface);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
