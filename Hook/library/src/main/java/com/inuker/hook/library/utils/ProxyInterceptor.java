package com.inuker.hook.library.utils;

import java.lang.reflect.Method;

/**
 * Created by liwentian on 2017/3/24.
 */

public interface ProxyInterceptor {
    boolean onIntercept(Object object, Method method, Object[] args);
}
