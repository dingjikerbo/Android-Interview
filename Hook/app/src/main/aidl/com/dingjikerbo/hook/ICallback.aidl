// ICallback.aidl
package com.dingjikerbo.hook;

// Declare any non-default types here with import statements

import android.os.Bundle;

interface ICallback {

    void onCall(in Bundle args);
}
