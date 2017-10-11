package com.inuker.hook.library.hook;

import com.inuker.hook.library.compat.AMSCompat;

/**
 * Created by workstation on 17/4/7.
 */

public class AMSHook {

    private static BinderHook mHook;

    public static void hook(BinderHook.BinderHookInvoker invoker) {
        Object object = AMSCompat.getIActivityManager();
        mHook = new BinderHook(object, invoker);
        AMSCompat.setIActivityManager(mHook.getProxyInterface());
    }

    public static void restore() {
        AMSCompat.setIActivityManager(mHook.getOriginalInterface());
    }
}
