package com.dingjikerbo.hook.utils;

import android.graphics.Color;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dingjikerbo.hook.MyApplication;

/**
 * Created by workstation on 17/4/7.
 */

public class ToastUtils {

    public static void show(String msg) {
        Toast toast = Toast.makeText(MyApplication.getContext(), msg, Toast.LENGTH_SHORT);
        LinearLayout layout = (LinearLayout) toast.getView();
        layout.setBackgroundColor(Color.parseColor("#FFFFFF"));
        TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
        v.setTextColor(Color.BLACK);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 35);
        toast.show();
    }
}
