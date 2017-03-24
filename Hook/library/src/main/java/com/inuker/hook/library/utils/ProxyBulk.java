package com.inuker.hook.library.utils;

import java.lang.reflect.Method;

/**
 * Created by liwentian on 2017/3/24.
 */

public class ProxyBulk {

    public Object object;
    public Method method;
    public Object[] args;

    public ProxyBulk(Object object, Method method, Object[] args) {
        this.object = object;
        this.method = method;
        this.args = args;
    }

    public Object safeInvoke() {
        Object result = null;
        try {
//            BluetoothLog.v(String.format("safeInvoke method = %s, object = %s", method, object));
            result = method.invoke(object, args);
        } catch (Throwable e) {
            LogUtils.e(e);
        }
        return result;
    }

    public static Object safeInvoke(Object obj) {
        return ((ProxyBulk) obj).safeInvoke();
    }
}
