package com.dingjikerbo.hook.hook;

import android.content.Context;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by dingjikerbo on 17/3/19.
 */

public abstract class Hook {

    protected Context context;

    private boolean enable;

    private HookHandler hookHandler;

    Hook(Context context) {
        this.context = context;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    abstract public boolean hook();

    abstract HookHandler createHookHandler();

    abstract class HookHandler implements InvocationHandler {

        Object object;

        HookHandler(Object object) {
            this.object = object;
        }

        abstract boolean onPreInvoke(Object object, Method method, Object[] args);

        abstract Object onPostInvoke(Object object, Method method, Object[] args, Object result);

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (!isEnable()) {
                return method.invoke(object, args);
            }

            boolean intercepted = onPreInvoke(object, method, args);
            Object invokeResult = null;
            if (!intercepted) {
                invokeResult = method.invoke(object, args);
            }
            return onPostInvoke(object, method, args, invokeResult);
        }
    }
}
