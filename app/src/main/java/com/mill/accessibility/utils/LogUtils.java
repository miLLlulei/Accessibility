package com.mill.accessibility.utils;

import android.util.Log;

public class LogUtils {

    /**
     * 判断是否可以进行日志输出，debug包或者开了写日志开关都可以进行日志输出
     *
     * @return
     */
    public static boolean isDebug() {
        return true;
    }

    public static int d(String tag, String msgString) {
        return Log.d(tag, msgString);
    }

    public static int d(String tag, String msgString, Throwable tr) {
        return Log.d(tag, msgString, tr);
    }

    public static int e(String tag, String msgString) {
        return Log.e(tag, msgString);

    }

    public static int e(String tag, String msgString, Throwable tr) {
        return Log.e(tag, msgString, tr);
    }
}
