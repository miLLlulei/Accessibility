package com.mill.accessibility.utils;

import android.text.TextUtils;

// 各种基本类型数据转化。
public class ConvertUtils {

    public static String long2String(Long val) {
        return Long.toString(val);
    }

    public static long string2Long(String val) {
        if (!TextUtils.isEmpty(val)) {
            try {
                return Long.parseLong(val);
            } catch (NumberFormatException e) {// 字符串中有非数字字符或者数字过大。
                e.printStackTrace();
            }
        }
        return 0;
    }

    public static String int2String(int val) {
        return Integer.toString(val);
    }

    public static int string2Int(String val) {
        return string2Int(val, 10);
    }

    public static int string2Int(String val, int radix) {
        if (!TextUtils.isEmpty(val)) {
            try {
                return Integer.parseInt(val, radix);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                if (LogUtils.isDebug()) {
                    throw e;
                }
            }
        }
        return 0;
    }

}
