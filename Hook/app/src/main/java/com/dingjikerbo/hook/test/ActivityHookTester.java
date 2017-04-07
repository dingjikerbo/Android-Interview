package com.dingjikerbo.hook.test;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;

import com.dingjikerbo.hook.ui.TestActivity1;
import com.dingjikerbo.hook.ui.TestActivity2;
import com.dingjikerbo.hook.ui.TestActivity3;
import com.inuker.hook.library.compat.ActivityThreadCompat;
import com.inuker.hook.library.hook.AMSHook;
import com.inuker.hook.library.hook.ActivityThreadHook;
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

                    intent.setClass(context, TestActivity2.class);
                    intent.putExtra("name", "world");
                }

                return method.invoke(original, args);
            }
        });

        ActivityThreadHook.hookMH(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case ActivityThreadHook.MSG_LAUNCH_ACTIVITY:
                        Intent intent = ActivityThreadCompat.getActivityClientRecordIntent(msg.obj);
                        intent.setClass(context, TestActivity3.class);
                        break;
                }
                return false;
            }
        });
    }

    @Override
    public void call() {
        Intent intent = new Intent(context, TestActivity1.class);
        intent.putExtra("name", "frank");
        context.startActivity(intent);
    }

    @Override
    public void restore() {
        AMSHook.restore();
        ActivityThreadHook.restoreMH();
    }
}
