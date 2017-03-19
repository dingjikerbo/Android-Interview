package com.dingjikerbo.hook.hook;

import android.content.Context;

/**
 * Created by dingjikerbo on 17/3/19.
 */

public class ServiceManagerHook extends Hook {

    ServiceManagerHook(Context context) {
        super(context);
    }

    @Override
    public boolean hook() {

        return false;
    }

    @Override
    HookHandler createHookHandler() {
        return null;
    }
}
