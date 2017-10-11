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
        return getMethod("arrayBaseOffset", Class.class);
    }

    public static Method arrayIndexScale() {
        return getMethod("arrayIndexScale", Class.class);
    }

    public static Method objectFieldOffset() {
        return getMethod("objectFieldOffset", Field.class);
    }

    public static Method getInt() {
        return getMethod("getInt", Object.class, long.class);
    }

    public static Method getLong() {
        return getMethod("getLong", Object.class, long.class);
    }

    public static long getObjectAddress(Object object) {
        try {
            Object[] objects = { object };
            Object unsafe = getUnsafeInstance();
            int baseOffset = (int) arrayBaseOffset().invoke(unsafe, Object[].class);
            int arrayIndexScale = (int) arrayIndexScale().invoke(unsafe, Object[].class);
            Method getAddrMethod = null;
            switch (arrayIndexScale) {
                case 4:
                    getAddrMethod = getInt();
                    break;
                case 8:
                    getAddrMethod = getLong();
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
            return ((Number) getAddrMethod.invoke(unsafe, objects, baseOffset)).longValue();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return 0;
    }
}
