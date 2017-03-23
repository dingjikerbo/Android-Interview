package com.inuker.hook.library.hook;

import java.lang.reflect.Field;

/**
 * Created by liwentian on 2017/3/23.
 */

public class PowerManagerHook {

    public static void hook() throws Exception {
        Class<?> clazz = Class.forName("android.os.PowerManager");

    }


}
