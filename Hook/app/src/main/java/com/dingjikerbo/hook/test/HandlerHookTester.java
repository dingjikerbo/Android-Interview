package com.dingjikerbo.hook.test;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.inuker.hook.library.hook.HandlerHook;

/**
 * Created by workstation on 17/4/4.
 */

public class HandlerHookTester extends HookTester {

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(context, "hello " + msg.obj, Toast.LENGTH_SHORT).show();
        }
    };

    HandlerHookTester(Context context) {
        super(context);
    }

    @Override
    public void hook() {
        HandlerHook.hook(mHandler);
    }

    @Override
    public void call() {
        mHandler.obtainMessage(0, "frank").sendToTarget();
    }

    @Override
    public void restore() {
        HandlerHook.recover(mHandler);
    }
}
