package com.example;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Main {

    static final String UNSAFE_CLASS = "sun.misc.Unsafe";
    static Object THE_UNSAFE = Reflection.get(null, UNSAFE_CLASS, "THE_ONE", null);

    public static long getObjectAddress(Object o) {
        Object[] objects = {o};
        Integer baseOffset = (Integer) Reflection.call(null, UNSAFE_CLASS,
                "arrayBaseOffset", THE_UNSAFE, new Class[]{Class.class}, new Object[]{Object[].class});

        System.out.println(baseOffset);

        long value = ((Number) Reflection.call(null, UNSAFE_CLASS, "getInt", THE_UNSAFE,
                new Class[]{Object.class, long.class}, new Object[]{objects, baseOffset.longValue()})).longValue();
        System.out.println(value);

        return value;
    }

    private static class A {
        int age;
    }

    public static void main(String[] args) {
        A a = new A();
        System.out.println(String.format("0X%x", getObjectAddress(a)));
    }

    private static class Reflection {
        public static Object call(Class<?> clazz, String className, String methodName, Object receiver,
                                  Class[] types, Object[] params) {
            try {
                if (clazz == null) clazz = Class.forName(className);
                Method method = clazz.getDeclaredMethod(methodName, types);
                method.setAccessible(true);
                return method.invoke(receiver, params);
            } catch (Throwable throwable) {
            }
            return null;
        }

        public static Object get(Class<?> clazz, String className, String fieldName, Object receiver) {
            try {
                if (clazz == null) clazz = Class.forName(className);
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(receiver);
            } catch (Throwable e) {
            }
            return null;
        }
    }
}
