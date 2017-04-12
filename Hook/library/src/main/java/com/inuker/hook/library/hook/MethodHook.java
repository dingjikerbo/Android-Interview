package com.inuker.hook.library.hook;

import com.inuker.hook.library.compat.Memory;
import com.inuker.hook.library.compat.MethodCompat;

import java.lang.reflect.Method;

/**
 * Created by workstation on 17/4/12.
 */

public class MethodHook {

    public static void hook(Method origin, Method replace) {
        Memory.memcpy(MethodCompat.getArtMethodAddress(origin),
                MethodCompat.getArtMethodAddress(replace),
                MethodCompat.getArtMethodSize());
    }
}
