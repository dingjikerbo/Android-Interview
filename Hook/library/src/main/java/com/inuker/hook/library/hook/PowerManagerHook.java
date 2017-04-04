package com.inuker.hook.library.hook;

import android.content.Context;
import android.os.PowerManager;

import com.inuker.hook.library.utils.LogUtils;

import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by liwentian on 2017/3/25.
 */

public class PowerManagerHook {

    private static Context mContext;

    public static void hook(Context context) throws Throwable {
        mContext = context;

        PowerManager manager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        Field field = FieldUtils.getField(PowerManager.class, "mService", true);

        // IPowerManager
        Object mService = field.get(manager);

        BinderHook hook = new BinderHook(mService, new BinderHook.BinderHookInvoker() {
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
                LogUtils.w(String.format("onInvoke %s", sb));
                return method.invoke(original, args);
            }
        });

        field.set(manager, hook.proxyInterface);
    }
}
