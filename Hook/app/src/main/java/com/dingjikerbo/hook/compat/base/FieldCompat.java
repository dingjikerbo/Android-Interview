package com.dingjikerbo.hook.compat.base;

import org.apache.commons.lang.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * Created by dingjikerbo on 17/3/18.
 */

public class FieldCompat implements Compat<Field> {

    private Class<?> clazz;

    private String fieldName;

    private static HashMap<Class<?>, HashMap<String, Field>> cache = new HashMap<>();

    FieldCompat(Class<?> clazz, String fieldName) {
        if (clazz == null || fieldName == null) {
            throw new NullPointerException();
        }

        this.clazz = clazz;
        this.fieldName = fieldName;
    }

    @Override
    public Field compat() {
        HashMap<String, Field> map = cache.get(clazz);
        if (map == null) {
            map = new HashMap<>();
            cache.put(clazz, map);
        }
        Field field = map.get(fieldName);
        if (field == null) {
            field = FieldUtils.getField(clazz, fieldName, true);
            if (field != null) {
                map.put(fieldName, field);
            }
        }
        return field;
    }

    public static Field compat(Class<?> clazz, String fieldName) {
        return new FieldCompat(clazz, fieldName).compat();
    }
}
