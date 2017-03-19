package com.dingjikerbo.hook.compat.base;

import java.util.HashMap;

/**
 * Created by dingjikerbo on 17/3/18.
 */

public class ClassCompat implements Compat<Class<?>> {

    private String className;

    private static HashMap<String, Class<?>> cache = new HashMap<>();

    public ClassCompat(String className) {
        if (className == null) {
            throw new NullPointerException();
        }

        this.className = className;
    }

    @Override
    public Class<?> compat() {
        Class<?> clazz = cache.get(className);
        if (clazz == null) {
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            if (clazz != null) {
                cache.put(className, clazz);
            }
        }
        return clazz;
    }

    public static Class<?> compat(String className) {
        return new ClassCompat(className).compat();
    }
}
