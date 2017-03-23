package com.dingjikerbo.hook.hook;

import android.content.Context;
import android.os.PowerManager;

import com.dingjikerbo.hook.compat.PowerManagerCompat;

import java.lang.reflect.Field;

/**
 * Created by dingjikerbo on 17/3/18.
 */

public class PowerManagerHook extends Hook {

    PowerManagerHook(Context context) {
        super(context);
    }

    @Override
    public boolean hook() {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        Field field = PowerManagerCompat.getPowerManagerBinderField();
        return false;
    }

    @Override
    HookHandler createHookHandler() {
        return null;
    }
}
