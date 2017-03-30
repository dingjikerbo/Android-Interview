package com.inuker.hook.library.compat;

import android.os.Handler;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;

/**
 * Created by liwentian on 2017/3/30.
 */

public class HandlerCompat {

    public static Class<?> getHandlerClazz() {
        try {
            return ClassUtils.getClass("android.os.Handler");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Field getCallbackField() {
        return FieldUtils.getField(getHandlerClazz(), "mCallback", true);
    }

    public static void setCallback(Object handler, Handler.Callback callback) {
        try {
            getCallbackField().set(handler, callback);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
