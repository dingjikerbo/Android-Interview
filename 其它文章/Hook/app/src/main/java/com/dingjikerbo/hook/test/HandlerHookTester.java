package com.dingjikerbo.hook.test;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.dingjikerbo.hook.utils.ToastUtils;
import com.inuker.hook.library.hook.HandlerHook;

/**
 * Created by workstation on 17/4/4.
 */

public class HandlerHookTester extends HookTester {

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            ToastUtils.show("hello " + msg.obj);
        }
    };

    HandlerHookTester(Context context) {
        super(context);
    }

    @Override
    public void hook() {
        HandlerHook.hook(mHandler, new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                msg.obj = "world";
                return false;
            }
        });
    }

    @Override
    public void call() {
        mHandler.obtainMessage(0, "frank").sendToTarget();
    }

    @Override
    public void restore() {
        HandlerHook.restore(mHandler);
    }
}
