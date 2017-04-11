package com.dingjikerbo.hook.test;

import android.content.Context;

import java.lang.reflect.Constructor;

/**
 * Created by workstation on 17/4/4.
 */

public abstract class HookTester implements IHookTester {

    Context context;

    private static final Class[] TESTER = new Class[]{
            NetworkHookTester.class,
            HttpUrlConnectionHookTester.class,
            HandlerHookTester.class,
            BinderHookTester.class,
            PowerManagerHookTester.class,
            ActivityHookTester.class,
    };

    public static HookTester get(Context context, int index) {
        Class<?> clazz = TESTER[index];

        try {
            Constructor constructor = clazz.getDeclaredConstructor(Context.class);
            constructor.setAccessible(true);
            return (HookTester) constructor.newInstance(context);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    HookTester(Context context) {
        this.context = context;
    }

}
