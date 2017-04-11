package com.inuker.hook.library.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by workstation on 17/4/11.
 */

public class Unsafe {

    private static final String UNSAFE_CLASS = "sun.misc.Unsafe";

    public static Class<?> getUnsafeClazz() {
        try {
            return Class.forName(UNSAFE_CLASS);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object getUnsafeInstance() {
        try {
            Field field = getUnsafeClazz().getDeclaredField("THE_ONE");
            field.setAccessible(true);
            return field.get(null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Method getMethod(String methodName, Class<?>... parameterTypes) {
        Method method = null;
        try {
            method = getUnsafeClazz().getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return method;
    }

    public static Method arrayBaseOffset() {
        return getMethod("arrayBaseOffset");
    }

    public static Method addressSize() {
        Method method = null;
        try {
            method = getUnsafeClazz().getDeclaredMethod("arrayBaseOffset", Class.class);
            method.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return method;
    }

    public static long getObjectAddress(Object object) {
        Method method = arrayBaseOffset();
        Object[] objects = {object};
        return 0;
    }
}
