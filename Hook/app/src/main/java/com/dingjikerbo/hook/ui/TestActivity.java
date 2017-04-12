package com.dingjikerbo.hook.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.dingjikerbo.hook.R;
import com.inuker.hook.library.hook.MethodHook;
import com.inuker.hook.library.utils.LogUtils;

import java.lang.reflect.Method;

/**
 * Created by liwentian on 2017/3/30.
 */

public class TestActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        TextView tvTitle = (TextView) findViewById(R.id.title);
        tvTitle.setText(getClass().getSimpleName());

        String name = getIntent().getStringExtra("name");

        TextView tvName = (TextView) findViewById(R.id.name);
        tvName.setText("hello " + name);

        Method[] methods = TestActivity.class.getDeclaredMethods();
        for (Method method : methods) {
            LogUtils.v(String.format(">>> %s", method));
        }

        try {
            Method helloWorld = TestActivity.class.getDeclaredMethod("helloWorld");
            Method helloFrank = TestActivity.class.getDeclaredMethod("helloFrank");
            MethodHook.hook(helloFrank, helloWorld);
        } catch (Throwable e) {
            e.printStackTrace();
        }


        Method[] methods2 = TestActivity.class.getDeclaredMethods();
        for (Method method : methods2) {
            LogUtils.v(String.format(">>> %s", method.getName()));
        }

        tvName.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                helloFrank();
            }
        });
    }

    private void helloFrank(String name) {
        Toast.makeText(this, "hello frank", Toast.LENGTH_SHORT).show();
    }

    private void helloWorld() {
        Toast.makeText(this, "hello world", Toast.LENGTH_SHORT).show();
    }

}
