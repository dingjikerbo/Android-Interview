package com.dingjikerbo.hook.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.dingjikerbo.hook.R;

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
    }
}
