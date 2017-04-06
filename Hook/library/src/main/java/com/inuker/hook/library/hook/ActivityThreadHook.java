package com.inuker.hook.library.hook;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;

import com.inuker.hook.library.compat.ActivityThreadCompat;
import com.inuker.hook.library.compat.HandlerCompat;

import org.apache.commons.lang3.reflect.FieldUtils;

/**
 * Created by workstation on 17/4/7.
 */

public class ActivityThreadHook {

    private static Handler mH;

    public static void hook() {
        mH = ActivityThreadCompat.getmH();

        HandlerHook.hook(mH, new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case 100:
                        performLaunchActivity(msg.obj);
                        break;
                }
                return false;
            }
        });
    }

    private static void performLaunchActivity(Object r) {
        try {
            Intent intent = (Intent) FieldUtils.getField(r.getClass(), "intent", true).get(r);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static void restore() {
        HandlerHook.restore(mH);
    }

}
