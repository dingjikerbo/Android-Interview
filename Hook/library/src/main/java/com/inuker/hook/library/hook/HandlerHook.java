package com.inuker.hook.library.hook;

import android.os.Handler;
import android.os.Message;

import com.inuker.hook.library.compat.HandlerCompat;

import java.util.HashMap;

/**
 * Created by workstation on 17/4/4.
 */

public class HandlerHook {

    private static HashMap<Handler, Handler.Callback> mCache = new HashMap<>();

    public static void hook(Handler handler) {
        boolean hooked = mCache.containsKey(handler);

        Handler.Callback old = HandlerCompat.setCallback(handler, new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                return true;
            }
        });

        if (!hooked) {
            mCache.put(handler, old);
        }
    }

    public static void recover(Handler handler) {
        if (!mCache.containsKey(handler)) {
            return;
        }

        Handler.Callback old = mCache.get(handler);
        HandlerCompat.setCallback(handler, old);

        mCache.remove(handler);
    }
}
