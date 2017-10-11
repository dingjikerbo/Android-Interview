package com.inuker.hook.library.compat;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;

/**
 * Created by workstation on 17/4/7.
 */

public class SingletonCompat {

    public static Class<?> getClazz() {
        try {
            return ClassUtils.getClass("android.util.Singleton");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Field getField() {
        return FieldUtils.getField(getClazz(), "mInstance", true);
    }

    public static Object getInstance(Object object) {
        try {
            return getField().get(object);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void setInstance(Object object, Object inst) {
        try {
            getField().set(object, inst);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
