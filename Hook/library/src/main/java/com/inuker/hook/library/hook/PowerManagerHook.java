package com.inuker.hook.library.hook;

import android.content.Context;
import android.os.PowerManager;

import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;

/**
 * Created by liwentian on 2017/3/25.
 */

public class PowerManagerHook {

    private static Context mContext;

    private static PowerManager mPowerManager;
    private static Field mField;

    private static BinderHook mHook;

    public static void hook(Context context, BinderHook.BinderHookInvoker invoker) {
        mContext = context;

        if (mPowerManager != null) {
            return;
        }

        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mField = FieldUtils.getField(PowerManager.class, "mService", true);

        try {
            mHook = new BinderHook(mField.get(mPowerManager), invoker);
            mField.set(mPowerManager, mHook.proxyInterface);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static void restore() {
        try {
            mField.set(mPowerManager, mHook.originalInterface);
            mPowerManager = null;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
