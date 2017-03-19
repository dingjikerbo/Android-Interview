package com.dingjikerbo.hook;

import android.os.Build;

/**
 * Created by dingjikerbo on 17/3/18.
 */

public class Version {

    /**
     * Build.VERSION_CODES.GINGERBREAD
     */
    public static final int V2_3 = 9;

    /**
     * Build.VERSION_CODES.HONEYCOMB
     */
    public static final int V3_0 = 11;

    /**
     * Build.VERSION_CODES.ICE_CREAM_SANDWICH
     */
    public static final int V4_0 = 14;

    /**
     * Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
     */
    public static final int V4_0_3 = 15;

    /**
     * Build.VERSION_CODES.JELLY_BEAN
     */
    public static final int V4_1 = 16;

    /**
     * Build.VERSION_CODES.JELLY_BEAN_MR1
     */
    public static final int V4_2 = 17;

    /**
     * Build.VERSION_CODES.JELLY_BEAN_MR2
     */
    public static final int V4_3 = 18;

    /**
     * Build.VERSION_CODES.KITKAT
     */
    public static final int v4_4 = 19;

    /**
     * Build.VERSION_CODES.KITKAT_WATCH
     */
    public static final int V4_4W = 20;

    /**
     * Build.VERSION_CODES.LOLLIPOP
     */
    public static final int V5_0 = 21;

    /**
     * Build.VERSION_CODES.LOLLIPOP_MR1
     */
    public static final int V5_1 = 22;

    /**
     * Build.VERSION_CODES.M
     */
    public static final int V6_0 = 23;

    /**
     * Build.VERSION_CODES.N
     */
    public static final int V7_0 = 24;

    /**
     * Build.VERSION_CODES.N_MR1
     */
    public static final int V7_1_1 = 25;


    private static final int VERSION = Build.VERSION.SDK_INT;

    public static int getVersionCode() {
        return VERSION;
    }

    public static String getVersionName() {
        return String.valueOf(VERSION);
    }

}
