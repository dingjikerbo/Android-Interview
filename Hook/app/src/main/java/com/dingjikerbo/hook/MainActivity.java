package com.dingjikerbo.hook;

import android.app.Activity;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;

import com.dingjikerbo.hook.compat.PowerManagerCompat;
import com.dingjikerbo.hook.utils.LogUtils;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                onclick();
            }
        });
    }

    private void onclick() {
        try {
        } catch (Exception e) {
            LogUtils.e(e);
        }
    }
}
