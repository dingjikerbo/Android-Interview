package com.inuker.hook.library.compat;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

/**
 * Created by workstation on 17/4/7.
 */

public class AMSCompat {

    public static Class<?> getAMNClass() {
        try {
            return ClassUtils.getClass("android.app.ActivityManagerNative");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object gDefault() {
        try {
            return FieldUtils.getField(getAMNClass(), "gDefault", true).get(null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object getIActivityManager() {
        return SingletonCompat.getInstance(gDefault());
    }

    public static void setIActivityManager(Object object) {
        SingletonCompat.setInstance(gDefault(), object);
    }
}
