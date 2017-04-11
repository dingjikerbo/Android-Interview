package com.inuker.hook.library.compat;

import java.lang.reflect.Method;

/**
 * Created by workstation on 17/4/11.
 */

public class Memory {

    private static Class<?> getClazz() {
        try {
            return Class.forName("libcore.io.Memory");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Method getMethod(String methodName, Class<?>... parameterTypes) {
        Method method = null;
        try {
            method = getClazz().getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return method;
    }

    private static Method peekByte() {
        return getMethod("peekByte", long.class);
    }

    private static Method pokeByte() {
        return getMethod("pokeByte", long.class, byte.class);
    }

    public static byte peekByte(long address) {
        try {
            return (byte) peekByte().invoke(null, address);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static void pokeByte(long address, byte value) {
        try {
            pokeByte().invoke(null, address, value);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
