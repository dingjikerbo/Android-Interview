package com.dingjikerbo.hook;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.inuker.hook.library.utils.LogUtils;

/**
 * Created by liwentian on 2017/3/30.
 */

public class MyReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        LogUtils.e(String.format("onReceive: %s", intent.getAction()));
    }
}
