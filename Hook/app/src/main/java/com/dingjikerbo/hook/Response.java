package com.dingjikerbo.hook;

import android.os.RemoteException;

import com.inuker.hook.library.IResponse;
import com.inuker.hook.library.utils.LogUtils;

/**
 * Created by liwentian on 2017/3/24.
 */

public class Response extends IResponse.Stub {
    @Override
    public void onResponse(int code) throws RemoteException {
        LogUtils.v(String.format("IResponse.Stub.onResponse code = %d", code));
    }
}
