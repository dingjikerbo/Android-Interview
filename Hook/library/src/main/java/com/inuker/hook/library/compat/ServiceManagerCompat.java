package com.inuker.hook.library.compat;

import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.commons.lang.reflect.ReflectionUtils;

import java.lang.reflect.Field;

/**
 * Created by liwentian on 2017/3/23.
 */

public class ServiceManagerCompat {

    private static Class<?> serviceManagerClazz;

    private static Field sServiceManagerField;

    public static Class<?> getServiceManagerClazz() {
        if (serviceManagerClazz == null) {
            serviceManagerClazz = ReflectionUtils.getClass("android.os.ServiceManager");
        }
        return serviceManagerClazz;
    }

    public static Field getsServiceManagerField() {
        if (sServiceManagerField == null) {
            sServiceManagerField = FieldUtils.getField(getServiceManagerClazz(), "sServiceManager", true);
        }
        return sServiceManagerField;
    }

}
