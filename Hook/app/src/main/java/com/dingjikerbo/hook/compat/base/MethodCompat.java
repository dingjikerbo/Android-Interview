package com.dingjikerbo.hook.compat.base;

import org.apache.commons.lang.reflect.MethodUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;

/**
 * Created by dingjikerbo on 17/3/19.
 */

public class MethodCompat implements Compat<Method> {

    private Class<?> clazz;

    private String methodName;

    private Class<?>[] parameterTypes;

    private String key;

    private static HashMap<Class<?>, HashMap<String, Method>> cache = new HashMap<>();

    MethodCompat(Class<?> clazz, String methodName, Class<?>[] parameterTypes) {
        if (clazz == null || methodName == null) {
            throw new NullPointerException();
        }

        this.clazz = clazz;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.key = getKey(methodName, parameterTypes);
    }

    private static String getKey(String methodName, Class<?>... parameterTypes) {
        StringBuilder sb = new StringBuilder();
        sb.append(methodName).append("#");
        if (parameterTypes != null && parameterTypes.length > 0) {
            for (Class<?> parameterType : parameterTypes) {
                sb.append(parameterType.toString()).append("#");
            }
        } else {
            sb.append(Void.class.toString());
        }
        return sb.toString();
    }

    @Override
    public Method compat() {
        HashMap<String, Method> map = cache.get(clazz);
        if (map == null) {
            map = new HashMap<>();
            cache.put(clazz, map);
        }
        Method method = map.get(methodName);
        if (method == null) {
            method = MethodUtils.getMethod(clazz, methodName, parameterTypes, true);
            if (method != null) {
                map.put(key, method);
            }
        }
        return method;
    }

    public static Method compat(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        return new MethodCompat(clazz, methodName, parameterTypes).compat();
    }
}
