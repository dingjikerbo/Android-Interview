// IMyAidlInterface.aidl
package com.dingjikerbo.hook;

// Declare any non-default types here with import statements

import com.dingjikerbo.hook.ICallback;

interface IHookCaller {

    void register(ICallback callback);
}
