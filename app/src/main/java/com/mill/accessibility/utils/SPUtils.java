package com.mill.accessibility.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;

import com.mill.accessibility.storager.SharedPreferencesImpl;

import java.util.Map;
import java.util.Set;

public class SPUtils {
    private static final String TAG = "SPUtils";
    private static final String MAIN_PROC_NAME = "com.qihoo.appstore";

    /**
     * 保存在手机里面的文件名o
     */
    public static final String FILE_NAME = "share_data";

    /**
     * 暴露一个由外部传入editor 的方法
     */
    public static void applyEditor(Editor editor) {
        editor.apply();
    }

    /**
     * 非主进程调用请使用带文件名参数的方法
     * 保存数据的方法，我们需要拿到保存数据的具体类型，然后根据类型调用不同的保存方法
     */
    public static void put(String key, Object object) {
        put(ContextUtils.getApplicationContext(), key, object);
    }

    /**
     * 非主进程调用请使用带文件名参数的方法
     */
    public static void put(Context context, String key, Object object) {
        put(FILE_NAME, context, key, object);
    }

    public static void put(String fileName, Context context, String key, Object object) {
        SharedPreferences sp = SharedPreferencesImpl.getSharedPreferences(context, getFileName(fileName),
                Context.MODE_PRIVATE);
        Editor editor = sp.edit();

        if (object instanceof String) {
            editor.putString(key, (String) object);
        } else if (object instanceof Integer) {
            editor.putInt(key, (Integer) object);
        } else if (object instanceof Boolean) {
            editor.putBoolean(key, (Boolean) object);
        } else if (object instanceof Float) {
            editor.putFloat(key, (Float) object);
        } else if (object instanceof Long) {
            editor.putLong(key, (Long) object);
        } else if (object instanceof Set) {
            editor.putStringSet(key, (Set) object);
        } else if(object == null) {
            editor.remove(key);
        }
        editor.apply();
    }

    /**
     * 得到保存数据的方法，我们根据默认值得到保存的数据的具体类型，然后调用相对于的方法获取值
     */

    public static Object get(String key, Object defaultObject) {
        return get(ContextUtils.getApplicationContext(), key, defaultObject);
    }

    public static Object get(Context context, String key, Object defaultObject) {
        return get(FILE_NAME, context, key, defaultObject);
    }

    public static Object get(String fileName, Context context, String key, Object defaultObject) {
        SharedPreferences sp = SharedPreferencesImpl.getSharedPreferences(context, getFileName(fileName),
                Context.MODE_PRIVATE);

        if (defaultObject instanceof String) {
            return sp.getString(key, (String) defaultObject);
        } else if (defaultObject instanceof Integer) {
            return sp.getInt(key, (Integer) defaultObject);
        } else if (defaultObject instanceof Boolean) {
            return sp.getBoolean(key, (Boolean) defaultObject);
        } else if (defaultObject instanceof Float) {
            return sp.getFloat(key, (Float) defaultObject);
        } else if (defaultObject instanceof Long) {
            return sp.getLong(key, (Long) defaultObject);
        } else if (null == defaultObject) {
            return sp.getString(key, null);
        }

        return null;
    }

    public static String getString(String fileName, Context context, String key, String defValue) {
        return (String) get(fileName, context, key, defValue);
    }

    public static void setString(String fileName, Context context, String key, String value) {
        put(fileName, context, key, value);
    }

    public static int getInt(String fileName, Context context, String key, int defValue) {
        return (int) get(fileName, context, key, defValue);
    }

    public static void setInt(String fileName, Context context, String key, int value) {
        put(fileName, context, key, value);
    }


    public static long getLong(String fileName, Context context, String key, long defValue) {
        Object val = get(fileName, context, key, defValue);
        long ret = 0;
        if (val != null) {
            if (val instanceof Long) {
                ret = (long) val;
            } else {
//                CrashHandler.getInstance().tryCatch(new RuntimeException(), "getLong_" + val);
            }
        }
        return ret;
    }

    public static void setLong(String fileName, Context context, String key, long value) {
        put(fileName, context, key, value);
    }

    public static boolean getBoolean(String fileName, Context context, String key, boolean defValue) {
        return (boolean) get(fileName, context, key, defValue);
    }

    public static void setBoolean(String fileName, Context context, String key, boolean value) {
        put(fileName, context, key, value);
    }

    public static float getFloat(String fileName, Context context, String key, float defValue) {
        return (float) get(fileName, context, key, defValue);
    }

    public static void setFloat(String fileName, Context context, String key, float value) {
        put(fileName, context, key, value);
    }

    /**
     * 移除某个key值已经对应的值
     */
    public static void remove(String key) {
        remove(ContextUtils.getApplicationContext(), key);
    }

    public static void remove(Context context, String key) {
        remove(FILE_NAME, context, key);
    }

    public static void remove(String fileName, Context context, String key) {
        SharedPreferences sp = SharedPreferencesImpl.getSharedPreferences(context, getFileName(fileName),
                Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.remove(key);
        editor.apply();
    }

    /**
     * 清除所有数据
     */
    public static void clear(Context context) {
        SharedPreferences sp = SharedPreferencesImpl.getSharedPreferences(context, getFileName(),
                Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.clear();
        editor.apply();
    }

    public static void clear(Context context, String fileName) {
        SharedPreferences sp = SharedPreferencesImpl.getSharedPreferences(context, getFileName(fileName),
                Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.clear();
        editor.apply();
    }

    /**
     * 查询某个key是否已经存在
     */

    public static boolean contains(String key) {
        return contains(ContextUtils.getApplicationContext(), key);
    }

    public static boolean contains(Context context, String key) {
        SharedPreferences sp = SharedPreferencesImpl.getSharedPreferences(context, getFileName(),
                Context.MODE_PRIVATE);
        return sp.contains(key);
    }


    public static boolean contains(Context context, String fileName, String key) {
        SharedPreferences sp = SharedPreferencesImpl.getSharedPreferences(context, getFileName(fileName),
                Context.MODE_PRIVATE);
        return sp.contains(key);
    }

    /**
     * 返回所有的键值对
     */
    public static Map<String, ?> getAll(Context context) {
        return getAll(context, FILE_NAME);
    }

    public static Map<String, ?> getAll(Context context, String fileName) {
        SharedPreferences sp = SharedPreferencesImpl.getSharedPreferences(context, getFileName(fileName),
                Context.MODE_PRIVATE);
        return sp.getAll();
    }

    public static boolean clearKey(Context context, String fileName, String key) {
        SharedPreferences sp = SharedPreferencesImpl.getSharedPreferences(context, getFileName(fileName),
                Context.MODE_PRIVATE);
        sp.edit().remove(key).apply();
        return true;
    }

    public static Editor getEditor(String fileName, Context context) {
        SharedPreferences sp = SharedPreferencesImpl.getSharedPreferences(context, getFileName(fileName),
                Context.MODE_PRIVATE);
        return sp.edit();
    }

    private static String getFileName(String fileName){
        if(TextUtils.isEmpty(fileName) || FILE_NAME.equals(fileName)){
            return getFileName();
        }
        return fileName;
    }

    private static String getFileName(){
//        String procName = ProcessUtils.getCurrentProcessName();
//        if(!TextUtils.isEmpty(procName) && !MAIN_PROC_NAME.equals(procName)){
//            StringBuilder stringBuilder = new StringBuilder(FILE_NAME);
//            stringBuilder.append("_").append(procName);
//            return stringBuilder.toString();
//        }
        return FILE_NAME;
    }
}
