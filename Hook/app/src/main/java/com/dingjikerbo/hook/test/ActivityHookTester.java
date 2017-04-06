package com.dingjikerbo.hook.test;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.dingjikerbo.hook.GhostActivity;
import com.dingjikerbo.hook.TestActivity;
import com.inuker.hook.library.hook.AMSHook;
import com.inuker.hook.library.hook.BinderHook;
import com.inuker.hook.library.utils.LogUtils;
import com.inuker.hook.library.utils.ProxyBulk;

import java.lang.reflect.Method;

/**
 * Created by workstation on 17/4/7.
 */

public class ActivityHookTester extends HookTester {

    ActivityHookTester(Context context) {
        super(context);
    }

    @Override
    public void hook() {
        AMSHook.hook(new BinderHook.BinderHookInvoker() {
            @Override
            public Object onInvoke(Object original, Method method, Object[] args) throws Throwable {
                LogUtils.w(new ProxyBulk(method, args).toString());

                if (method.getName().equals("startActivity")) {
                    Intent intent = (Intent) args[2];

                    ComponentName component = new ComponentName(context, GhostActivity.class);
                    intent.setComponent(component);

                    intent.putExtra("name", "world");
                }

                return method.invoke(original, args);
            }
        });
    }

    @Override
    public void call() {
        Intent intent = new Intent(context, TestActivity.class);
        intent.putExtra("name", "frank");
        context.startActivity(intent);
    }

    @Override
    public void restore() {
        AMSHook.restore();
    }
}
