package com.dingjikerbo.hook.utils;

import android.app.Notification;

import com.inuker.hook.library.utils.LogUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by workstation on 17/4/10.
 */

public class HttpUtils {

    public static void postByOkHttp(OkHttpClient client, String url, Callback callback) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(callback);
    }

    public static String postByHttpUrlConnection(String url) throws Throwable {
        URL link = new URL(url);

        HttpURLConnection connection = (HttpURLConnection) link.openConnection();
        connection.connect();

        connection.getInputStream();

        int code = connection.getResponseCode();

        StringBuilder buffer = new StringBuilder();

        if (code == 200) {
            InputStream inputStream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String liner;
            while ((liner = reader.readLine()) != null) {
                buffer.append(liner);
            }
        }

        return buffer.toString();
    }
}
