package com.dingjikerbo.hook.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.dingjikerbo.hook.R;
import com.inuker.hook.library.compat.Unsafe;
import com.inuker.hook.library.utils.LogUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

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

        Man man = new Man();
        man.age = 10;
        man.name = "frank";

        LogUtils.v(String.format("man = %s, 0x%X", man, Unsafe.getObjectAddress(man)));

        try {
            Field field = Man.class.getField("age");
            long offset = (long) Unsafe.objectFieldOffset().invoke(Unsafe.getUnsafeInstance(), field);
            LogUtils.v(String.format("offset = %d", offset));

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        Memory.

    }

    private static class Man {
        public String name;

        public int age;
    }
}
