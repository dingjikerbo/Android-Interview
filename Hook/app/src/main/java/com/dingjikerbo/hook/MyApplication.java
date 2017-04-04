package com.dingjikerbo.hook;

import android.app.Application;
import android.content.Context;

/**
 * Created by workstation on 17/4/4.
 */

public class MyApplication extends Application {

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }

    public static Context getInstance() {
        return mContext;
    }
}
