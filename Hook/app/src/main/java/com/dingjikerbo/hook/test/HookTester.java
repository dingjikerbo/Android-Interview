package com.dingjikerbo.hook.test;

import android.content.Context;

import com.dingjikerbo.hook.MyApplication;
import com.inuker.hook.library.utils.LogUtils;

/**
 * Created by workstation on 17/4/4.
 */

public abstract class HookTester implements IHookTester {

    Context context;

    private static final HookTester[] TESTER = new HookTester[]{
            new HandlerHookTester(MyApplication.getInstance()),
            new BinderHookTester(MyApplication.getInstance()),
    };

    public static HookTester get(int index) {
        HookTester tester = TESTER[index];
        LogUtils.v(String.format("HookTester.get() = %s", tester));
        return tester;
    }

    HookTester(Context context) {
        this.context = context;
    }

}
