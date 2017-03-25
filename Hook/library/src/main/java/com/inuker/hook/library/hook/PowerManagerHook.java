package com.inuker.hook.library.hook;

import android.content.Context;
import android.os.IBinder;
import android.os.PowerManager;

import com.inuker.hook.library.compat.ServiceManagerCompat;
import com.inuker.hook.library.utils.LogUtils;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by liwentian on 2017/3/25.
 */

public class PowerManagerHook {

    private static Context mContext;

    public static void hook(Context context) throws IllegalAccessException {
        mContext = context.getApplicationContext();

        PowerManager manager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        Field field = FieldUtils.getField(PowerManager.class, "mService", true);
        BinderHook hook = new BinderHook(field.get(manager), new BinderHook.BinderHookInvoker() {
            @Override
            public Object onInvoke(Object original, Method method, Object[] args) throws Throwable {
                StringBuilder sb = new StringBuilder(String.format("%s(", method.getName()));
                for (int i = 0; i < args.length; i++) {
                    sb.append(args[i]);
                    if (i != args.length - 1) {
                        sb.append(", ");
                    } else {
                        sb.append(")");
                    }
                }
                LogUtils.v(String.format("onInvoke %s", sb));
                return method.invoke(original, args);
            }
        });


        LogUtils.v(String.format("ss: %s", field.get(manager)));

        LogUtils.v(String.format("uu: %s", hook.proxyInterface));

        field.setAccessible(true);
        field.set(manager, hook.proxyInterface);

        LogUtils.v(String.format("tt: %s", field.get(manager)));
    }
}
