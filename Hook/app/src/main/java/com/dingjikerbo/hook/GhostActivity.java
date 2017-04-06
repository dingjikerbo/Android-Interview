package com.dingjikerbo.hook;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Created by liwentian on 2017/3/30.
 */

public class GhostActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ghost);

        String name = getIntent().getStringExtra("name");

        TextView tvName = (TextView) findViewById(R.id.name);
        tvName.setText("hello " + name);
    }
}
