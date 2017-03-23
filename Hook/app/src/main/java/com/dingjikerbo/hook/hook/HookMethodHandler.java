package com.dingjikerbo.hook.hook;

import java.lang.reflect.Method;

/**
 * Created by liwentian on 2017/3/20.
 */

public interface HookMethodHandler {

    boolean onPreInvoke(Object object, Method method, Object[] args);

    Object onPostInvoke(Object object, Method method, Object[] args, Object result);
}
