package com.inuker.hook.library.hook;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.inuker.hook.library.compat.ActivityThreadCompat;
import com.inuker.hook.library.compat.HandlerCompat;
import com.inuker.hook.library.utils.LogUtils;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by liwentian on 2017/3/30.
 */

public class ReceiverHook {

    public static void hook() {
        final Object mH = ActivityThreadCompat.getmH();

        HandlerCompat.setCallback(mH, new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                LogUtils.e(String.format("handleMessage msg = %d", msg.what));

                switch (msg.what) {
                    case ActivityThreadCompat.RECEIVER:
                        Intent intent = ActivityThreadCompat.getReceiverDataIntent(msg.obj);
                        LogUtils.e(String.format("component = %s, action = %s",
                                intent.getComponent(), intent.getAction()));
                        break;
                }

                return false;
            }
        });
    }
}
