package com.dingjikerbo.hook;

import android.app.Application;
import android.content.Context;

/**
 * Created by workstation on 17/4/7.
 */

public class MyApplication extends Application {

    private static Context mContext;

    public static Context getContext() {
        return mContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }
}
