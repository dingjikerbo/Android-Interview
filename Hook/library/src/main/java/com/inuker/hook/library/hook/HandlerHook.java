package com.inuker.hook.library.hook;

import android.os.Handler;

import com.inuker.hook.library.compat.HandlerCompat;

import java.util.HashMap;

/**
 * Created by workstation on 17/4/4.
 */

public class HandlerHook {

    private static HashMap<Handler, Handler.Callback> mCache = new HashMap<>();

    public static void hook(Handler handler, Handler.Callback callback) {
        Handler.Callback old = HandlerCompat.setCallback(handler, callback);
        mCache.put(handler, old);
    }

    public static void restore(Handler handler) {
        Handler.Callback old = mCache.get(handler);
        HandlerCompat.setCallback(handler, old);
        mCache.remove(handler);
    }
}
