package com.dingjikerbo.hook.test;

import android.content.Context;

import com.dingjikerbo.hook.utils.HttpUtils;
import com.inuker.hook.library.utils.LogUtils;

import org.apache.commons.lang3.reflect.FieldUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.RealInterceptorChain;

/**
 * Created by workstation on 17/4/9.
 */

public class NetworkHookTester extends HookTester {
    private static final String URL = "http://baidu.com";

    private OkHttpClient client;
    private HookInterceptor hookInterceptor;

    private volatile boolean hooked;

    NetworkHookTester(Context context) {
        super(context);

        hookInterceptor = new HookInterceptor();
        client = new OkHttpClient.Builder().addNetworkInterceptor(hookInterceptor).build();
    }

    private class HookInterceptor implements Interceptor {

        @Override
        public Response intercept(Chain chain) throws IOException {
            RealInterceptorChain realChain = (RealInterceptorChain) chain;

            Request request = chain.request();
            if (hooked) {
                try {
                    FieldUtils.getField(RealInterceptorChain.class, "calls", true).set(realChain, 1);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                return new Response.Builder()
                        .request(request)
                        .protocol(chain.connection().protocol())
                        .body(ResponseBody.create(null, "hello world!"))
                        .code(404).build();
            } else {
                return chain.proceed(request);
            }
        }
    }

    @Override
    public void hook() {
        hooked = true;
    }

    @Override
    public void call() {
        HttpUtils.postByOkHttp(client, URL, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                LogUtils.e(String.format("onFailure"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String text = response.body().string();
                LogUtils.v(String.format("onResponse:\n%s", text));
            }
        });
    }



    @Override
    public void restore() {
        hooked = false;
    }
}
