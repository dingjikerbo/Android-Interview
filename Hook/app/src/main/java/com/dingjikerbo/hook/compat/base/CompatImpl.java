package com.dingjikerbo.hook.compat.base;

import com.dingjikerbo.hook.Version;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by dingjikerbo on 17/3/18.
 */

public class CompatImpl<T> implements Compat<T> {

    private Map<String, Compat<T>> mCompatCache = new HashMap<>();

    @Override
    public T compat() throws Exception {
        String version = Version.getVersionName();
        Compat<T> compat = mCompatCache.get(version);
        return compat != null ? compat.compat() : null;
    }

    protected CompatImpl<T> registerCompator(Compat<T> compat, int from) {
        return registerCompator(compat, from, from);
    }

    protected CompatImpl<T> registerCompator(Compat<T> compat, int from, int to) {
        if (from > to) {
            throw new IllegalArgumentException();
        }
        if (compat == null) {
            throw new NullPointerException();
        }
        for (int i = from; i <= to; i++) {
            mCompatCache.put(String.valueOf(from), compat);
        }
        return this;
    }
}
