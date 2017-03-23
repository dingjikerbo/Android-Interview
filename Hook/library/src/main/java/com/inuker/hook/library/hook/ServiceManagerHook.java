package com.inuker.hook.library.hook;

import android.app.Service;
import android.os.IBinder;
import android.os.IInterface;

import org.apache.commons.lang.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;

import com.inuker.hook.library.compat.ServiceManagerCompat;

/**
 * Created by liwentian on 2017/3/23.
 */

public class ServiceManagerHook {

    private static Object sServiceManager;

    public static void hook() throws Exception {
        Field field = ServiceManagerCompat.getsServiceManagerField();
        Object sServiceManager = field.get(null);
        
    }

    private static ClassLoader getClassLoader() {
        return ServiceManagerHook.class.getClassLoader();
    }
}
