package com.inuker.hook.library.utils;

import android.os.IBinder;
import android.os.IInterface;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Created by liwentian on 2017/3/25.
 */

public class BinderUtils {

    public static Object binderToInterface(IBinder originalBinder, Class<?> binderIntfClazz) {
        try {
            String stubClazzName = binderIntfClazz.getName() + "$Stub";
            Class<?> stubClazz = ClassUtils.getClass(stubClazzName);
            return MethodUtils.invokeMethod(stubClazz, "asInterface", originalBinder);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static IBinder asBinder(Object object) {
        try {
            return (IBinder) MethodUtils.invokeMethod(object, "asBinder");
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Class<?> getBinderInterface(Object object) {
        List<Class<?>> clazzes = ClassUtils.getAllInterfaces(object.getClass());
        for (Class<?> clazz : clazzes) {
            if (clazz.equals(IBinder.class)) {
                continue;
            }
            if (clazz.equals(IInterface.class)) {
                continue;
            }
            return clazz;
        }
        return null;
    }
}
