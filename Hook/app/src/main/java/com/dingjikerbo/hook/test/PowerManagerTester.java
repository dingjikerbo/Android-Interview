package com.dingjikerbo.hook.test;

import android.content.Context;
import android.os.PowerManager;
import android.widget.Toast;

import com.inuker.hook.library.hook.BinderHook;
import com.inuker.hook.library.hook.PowerManagerHook;

import java.lang.reflect.Method;

/**
 * Created by workstation on 17/4/6.
 */

public class PowerManagerTester extends HookTester {

    PowerManagerTester(Context context) {
        super(context);
    }

    @Override
    public void hook() {
        PowerManagerHook.hook(context, new BinderHook.BinderHookInvoker() {
            @Override
            public Object onInvoke(Object original, Method method, Object[] args) throws Throwable {
                Toast.makeText(context, method.getName(), Toast.LENGTH_SHORT).show();
                return null;
            }
        });
    }

    @Override
    public void call() {
        PowerManager manager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock lock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "test");
        lock.acquire(1000);
    }

    @Override
    public void restore() {
        PowerManagerHook.restore();
    }
}
