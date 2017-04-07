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

    public static final int MSG_LAUNCH_ACTIVITY = 100;

    private static Handler mH;

    public static void hookMH(Handler.Callback callback) {
        mH = ActivityThreadCompat.getmH();
        HandlerHook.hook(mH, callback);
    }

    public static void restoreMH() {
        HandlerHook.restore(mH);
    }

}
