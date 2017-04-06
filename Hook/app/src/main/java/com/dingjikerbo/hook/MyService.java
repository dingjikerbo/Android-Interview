package com.dingjikerbo.hook;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.inuker.hook.library.utils.LogUtils;

/**
 * Created by workstation on 17/4/6.
 */

public class MyService extends Service {

    private final ICaller mCaller = new ICaller.Stub() {
        @Override
        public String sayHi(String name) throws RemoteException {
            return String.format("hello %s", name);
        }

    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mCaller.asBinder();
    }
}
