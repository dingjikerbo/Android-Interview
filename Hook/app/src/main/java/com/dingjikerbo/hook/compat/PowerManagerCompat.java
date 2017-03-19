package com.dingjikerbo.hook.compat;


import com.dingjikerbo.hook.compat.base.ClassCompat;
import com.dingjikerbo.hook.compat.base.FieldCompat;

import java.lang.reflect.Field;

/**
 * Created by dingjikerbo on 17/3/18.
 */

public class PowerManagerCompat {

    public static Class<?> getPowerManagerClaz() {
        return ClassCompat.compat("android.os.PowerManager");
    }

    public static Field getPowerManagerBinderField() {
        return FieldCompat.compat(getPowerManagerClaz(), "mService");
    }
}
