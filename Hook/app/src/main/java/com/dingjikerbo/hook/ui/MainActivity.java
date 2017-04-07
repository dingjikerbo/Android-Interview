package com.dingjikerbo.hook.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;

import com.dingjikerbo.hook.R;
import com.dingjikerbo.hook.test.HookTester;
import com.dingjikerbo.hook.test.IHookTester;


public class MainActivity extends Activity {

    private Button mBtnHook, mBtnRestore;

    private IHookTester mHookTester;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtnHook = (Button) findViewById(R.id.hook);
        mBtnRestore = (Button) findViewById(R.id.restore);

        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mHookTester = HookTester.get(MainActivity.this, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mHookTester = null;
            }
        });

        spinner.setSelection(0, true);
    }

    public void hook(View view) {
        mBtnHook.setEnabled(false);
        mBtnRestore.setEnabled(true);

        mHookTester.hook();
    }

    public void call(View view) {
        mHookTester.call();
    }

    public void restore(View view) {
        mBtnHook.setEnabled(true);
        mBtnRestore.setEnabled(false);

        mHookTester.restore();
    }
}
