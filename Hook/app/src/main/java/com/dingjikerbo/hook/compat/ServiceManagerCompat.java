package com.dingjikerbo.hook.compat;

import android.os.IBinder;

import com.dingjikerbo.hook.compat.base.ClassCompat;

import org.apache.commons.lang.reflect.MethodUtils;

/**
 * Created by dingjikerbo on 17/3/19.
 */

public class ServiceManagerCompat {

    public static Class<?> getServiceManagerClazz() {
        return ClassCompat.compat("android.os.ServiceManager");
    }

    public static IBinder getServiceBinder(String serviceName) {
        return (IBinder) MethodUtils.invokeMethod(getServiceManagerClazz(), "getService", serviceName);
    }
}
