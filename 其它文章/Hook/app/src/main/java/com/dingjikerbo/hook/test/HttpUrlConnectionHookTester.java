package com.dingjikerbo.hook.test;

import android.content.Context;

import com.dingjikerbo.hook.utils.HttpUtils;
import com.inuker.hook.library.utils.LogUtils;

/**
 * Created by workstation on 17/4/10.
 */

public class HttpUrlConnectionHookTester extends HookTester {

    private static final String URL = "http://baidu.com";

    HttpUrlConnectionHookTester(Context context) {
        super(context);
    }

    @Override
    public void hook() {

    }

    @Override
    public void call() {
        new Thread() {
            @Override
            public void run() {
                String result = null;
                try {
                    result = HttpUtils.postByHttpUrlConnection(URL);
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
                LogUtils.v(String.format("result: \n%s", result));
            }
        }.start();
    }

    @Override
    public void restore() {

    }
}
