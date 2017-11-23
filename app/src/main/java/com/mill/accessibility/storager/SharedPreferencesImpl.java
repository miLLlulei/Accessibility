/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//package android.app;
package com.mill.accessibility.storager;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import com.mill.accessibility.thread.PriorityThreadFactory;
import com.mill.accessibility.thread.ThreadPoolExecutorHandleCrash;
import com.mill.accessibility.utils.AndroidVersionCodes;
import com.mill.accessibility.utils.LogUtils;
import com.mill.accessibility.utils.ReflectUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * 解决：Android4.4以下KXmlParser解析xml的ArrayIndexOutOfBoundsException崩溃；
 *
 * 1、ArrayIndexOutOfBoundsException in android's KXmlParser：http://stackoverflow.com/questions/11967062/arrayindexoutofboundsexception-in-androids-kxmlparser
 *    So the proper exception you should be getting is: Unterminated element content spec, meaning unexpected EOF.
 * 2、bug-fix for Android 4.4 mentioning this: Change 61530：https://android-review.googlesource.com/#/c/61530
 * 3、本类基于Android7.1.1_r6源码修改：http://androidxref.com/7.1.1_r6/xref/frameworks/base/core/java/android/app/SharedPreferencesImpl.java
 */
public class SharedPreferencesImpl implements SharedPreferences {
    private static final String TAG = "SharedPreferencesImpl";
    private static final boolean DEBUG = LogUtils.isDebug();

    // Lock ordering rules:
    //  - acquire SharedPreferencesImpl.this before EditorImpl.this
    //  - acquire mWritingToDiskLock before EditorImpl.this

    private final File mFile;
    private final File mBackupFile;
    private final int mMode;

    private Map<String, Object> mMap;     // guarded by 'this'
    private int mDiskWritesInFlight = 0;  // guarded by 'this'
    private boolean mLoaded = false;      // guarded by 'this'
    private long mStatTimestamp;          // guarded by 'this'
    private long mStatSize;               // guarded by 'this'

    private final Object mWritingToDiskLock = new Object();
    private static final Object mContent = new Object();
    private final WeakHashMap<OnSharedPreferenceChangeListener, Object> mListeners =
            new WeakHashMap<OnSharedPreferenceChangeListener, Object>();

    /**
     * Map from package name, to preference name, to cached preferences.
     */
    private static Map<String, HashMap<String, SharedPreferencesImpl>> sSharedPrefs;
    private static java.util.concurrent.ThreadPoolExecutor mExecutor;

    public static SharedPreferences getDefaultSharedPreferencesByPreferenceManager(Context context) {
        if (Build.VERSION.SDK_INT > AndroidVersionCodes.N_MR1) {
            return android.preference.PreferenceManager.getDefaultSharedPreferences(context);
        } else {
            String name = (String) ReflectUtils.invokeStaticMethod("android.preference.PreferenceManager", "getDefaultSharedPreferencesName", new Class[]{Context.class}, context);
            int mode = (int) ReflectUtils.invokeStaticMethod("android.preference.PreferenceManager", "getDefaultSharedPreferencesMode", null);
            return getSharedPreferences(context, name, mode);
        }
    }

    public static SharedPreferences getSharedPreferences(Context context, String name, int mode) {
        // 虽然官方在4.4的时候解决了ArrayIndexOutOfBoundsException的问，为了解决其他异常先用下这个类的实现；
        // java.lang.ClassCastException: java.lang.Boolean cannot be cast to java.util.HashMap at com.android.internal.util.XmlUtils.readMapXml(XmlUtils.java:494)
        if (Build.VERSION.SDK_INT > AndroidVersionCodes.N_MR1) { // 原本应该“>= 19”，为了解决崩溃看了所有版本SharedPreferences实现的源码，并做了反射调用时的适配；
            return context.getSharedPreferences(name, mode);
        } else {
            SharedPreferencesImpl sp;
            synchronized (SharedPreferencesImpl.class) {
                if (sSharedPrefs == null) {
                    sSharedPrefs = new HashMap<String, HashMap<String, SharedPreferencesImpl>>();
                }
                if (mExecutor == null) {
                    mExecutor = new ThreadPoolExecutorHandleCrash(2, 2, 10 * 1000L, java.util.concurrent.TimeUnit.MILLISECONDS,
                            new java.util.concurrent.LinkedBlockingQueue<Runnable>(),
                            new PriorityThreadFactory(TAG, android.os.Process.THREAD_PRIORITY_DEFAULT));
                    mExecutor.allowCoreThreadTimeOut(true);
                }

                final String packageName = context.getPackageName();
                HashMap<String, SharedPreferencesImpl> packagePrefs = sSharedPrefs.get(packageName);
                if (packagePrefs == null) {
                    packagePrefs = new HashMap<String, SharedPreferencesImpl>();
                    sSharedPrefs.put(packageName, packagePrefs);
                }

                // At least one application in the world actually passes in a null
                // name.  This happened to work because when we generated the file name
                // we would stringify it to "null.xml".  Nice.
                if (context.getApplicationInfo().targetSdkVersion <
                        Build.VERSION_CODES.KITKAT) {
                    if (name == null) {
                        name = "null";
                    }
                }

                sp = packagePrefs.get(name);
                if (sp == null) {
//                File prefsFile = getSharedPrefsFile(name);
                    File prefsFile = (File) ReflectUtils.invokeMethod(context, "getSharedPrefsFile", new Class[]{String.class}, name);
                    sp = new SharedPreferencesImpl(prefsFile, mode);
                    packagePrefs.put(name, sp);
                    return sp;
                }
            }
            if ((mode & Context.MODE_MULTI_PROCESS) != 0 ||
                    context.getApplicationInfo().targetSdkVersion < Build.VERSION_CODES.HONEYCOMB) {
                // If somebody else (some other process) changed the prefs
                // file behind our back, we reload it.  This has been the
                // historical (if undocumented) behavior.
                sp.startReloadIfChangedUnexpectedly();
            }
            return sp;
        }
    }

    private Map<String, ?> readMapXml(InputStream in) {
        Map map = null;
        Exception exception = null;
        int exceptionCount = 0;
        for (int i = 0; i < 2; i++) {
            try {
                map = (HashMap<String, ?>) ReflectUtils.invokeStaticMethod(
                        "com.android.internal.util.XmlUtils",
                        "readMapXml",
                        new Class[]{InputStream.class},
                        in);
                break;
            } catch (Exception e) {
                // 从外网收集上来的错误的SharedPreferences文件：<project>doc/Bad SharedPreferences.xml
                Throwable cause = e.getCause() != null && e.getCause().getCause() != null ? e.getCause().getCause() : new Throwable();
                if (cause instanceof ArrayIndexOutOfBoundsException) {
//                    java.lang.ArrayIndexOutOfBoundsException:src.length = 8192 srcPos = 1 dst.length = 8192
//                    dstPos = 0 length = -1
//                    at java.lang.System.arraycopy(Native Method)
//                    at org.kxml2.io.KXmlParser.fillBuffer(KXmlParser.java:1489)
//                    at org.kxml2.io.KXmlParser.skip(KXmlParser.java:1574)
//                    at org.kxml2.io.KXmlParser.parseStartTag(KXmlParser.java:1049)
//                    at org.kxml2.io.KXmlParser.next(KXmlParser.java:369)
//                    at org.kxml2.io.KXmlParser.next(KXmlParser.java:310)
//                    at com.android.internal.util.XmlUtils.readThisMapXml(XmlUtils.java:578)
//                    at com.android.internal.util.XmlUtils.readThisValueXml(XmlUtils.java:821)
//                    at com.android.internal.util.XmlUtils.readValueXml(XmlUtils.java:755)
//                    at com.android.internal.util.XmlUtils.readMapXml(XmlUtils.java:494)
//                    at android.app.SharedPreferencesImpl.loadFromDiskLocked(SharedPreferencesImpl.java:120)
//                    at android.app.SharedPreferencesImpl.access$000(SharedPreferencesImpl.java:52)
//                    at android.app.SharedPreferencesImpl$1.run(SharedPreferencesImpl.java:91)
                    exception = e;
                } else if (cause instanceof NumberFormatException) {
//                    java.lang.NumberFormatException: Invalid int: "1437831141868"
//                    at java.lang.Integer.invalidInt(Integer.java:138)
//                    at java.lang.Integer.parse(Integer.java:378)
//                    at java.lang.Integer.parseInt(Integer.java:366)
//                    at java.lang.Integer.parseInt(Integer.java:332)
//                    at com.android.internal.util.XmlUtils.readThisValueXml(XmlUtils.java:804)
//                    at com.android.internal.util.XmlUtils.readThisMapXml(XmlUtils.java:563)
//                    at com.android.internal.util.XmlUtils.readThisValueXml(XmlUtils.java:821)
//                    at com.android.internal.util.XmlUtils.readValueXml(XmlUtils.java:755)
//                    at com.android.internal.util.XmlUtils.readMapXml(XmlUtils.java:494)
//                    at android.app.SharedPreferencesImpl.loadFromDiskLocked(SharedPreferencesImpl.java:120)
//                    at android.app.SharedPreferencesImpl.access$000(SharedPreferencesImpl.java:52)
//                    at android.app.SharedPreferencesImpl$1.run(SharedPreferencesImpl.java:91)
//
//                    java.lang.NumberFormatException: Invalid long: "null"
//                    at java.lang.Long.invalidLong(Long.java:125)
//                    at java.lang.Long.parseLong(Long.java:342)
//                    at java.lang.Long.parseLong(Long.java:319)
//                    at java.lang.Long.valueOf(Long.java:477)
//                    at com.android.internal.util.XmlUtils.readThisValueXml(XmlUtils.java:806)
//                    at com.android.internal.util.XmlUtils.readThisMapXml(XmlUtils.java:563)
//                    at com.android.internal.util.XmlUtils.readThisValueXml(XmlUtils.java:821)
//                    at com.android.internal.util.XmlUtils.readValueXml(XmlUtils.java:755)
//                    at com.android.internal.util.XmlUtils.readMapXml(XmlUtils.java:494)
//                    at android.app.SharedPreferencesImpl.loadFromDiskLocked(SharedPreferencesImpl.java:120)
//                    at android.app.SharedPreferencesImpl.access$000(SharedPreferencesImpl.java:52)
//                    at android.app.SharedPreferencesImpl$1.run(SharedPreferencesImpl.java:91)
//
//                    使用反射调用后崩溃堆栈有3层；
//                    java.lang.RuntimeException: invokeStaticMethod exception, className = com.android.internal.util.XmlUtils, methodName = readMapXml
//                    at com.qihoo.utils.hideapi.ReflectUtils.invokeStaticMethod(ReflectUtils.java:162)
//                    at com.qihoo.storager.SharedPreferencesImpl.readMapXml(SharedPreferencesImpl.java:138)
//                    at com.qihoo.storager.SharedPreferencesImpl.loadFromDiskLocked(SharedPreferencesImpl.java:328)
//                    at com.qihoo.storager.SharedPreferencesImpl.access$100(SharedPreferencesImpl.java:57)
//                    at com.qihoo.storager.SharedPreferencesImpl$1.run(SharedPreferencesImpl.java:297)
//                    Caused by: java.lang.reflect.InvocationTargetException
//                    at java.lang.reflect.Method.invokeNative(Native Method)
//                    at java.lang.reflect.Method.invoke(Method.java:511)
//                    at com.qihoo.utils.hideapi.ReflectUtils.invokeStaticMethod(ReflectUtils.java:160)
//                    at com.qihoo.storager.SharedPreferencesImpl.readMapXml(SharedPreferencesImpl.java:138) 
//                    at com.qihoo.storager.SharedPreferencesImpl.loadFromDiskLocked(SharedPreferencesImpl.java:328) 
//                    at com.qihoo.storager.SharedPreferencesImpl.access$100(SharedPreferencesImpl.java:57) 
//                    at com.qihoo.storager.SharedPreferencesImpl$1.run(SharedPreferencesImpl.java:297) 
//                    Caused by: java.lang.NumberFormatException: Invalid long: "null"
//                    at java.lang.Long.invalidLong(Long.java:125)
//                    at java.lang.Long.parse(Long.java:362)
//                    at java.lang.Long.parseLong(Long.java:353)
//                    at java.lang.Long.parseLong(Long.java:319)
//                    at java.lang.Long.valueOf(Long.java:477)
//                    at com.android.internal.util.XmlUtils.readThisValueXml(XmlUtils.java:806)
//                    at com.android.internal.util.XmlUtils.readThisMapXml(XmlUtils.java:563)
//                    at com.android.internal.util.XmlUtils.readThisValueXml(XmlUtils.java:821)
//                    at com.android.internal.util.XmlUtils.readValueXml(XmlUtils.java:755)
//                    at com.android.internal.util.XmlUtils.readMapXml(XmlUtils.java:494)
//                    at java.lang.reflect.Method.invokeNative(Native Method) 
//                    at java.lang.reflect.Method.invoke(Method.java:511) 
//                    at com.qihoo.utils.hideapi.ReflectUtils.invokeStaticMethod(ReflectUtils.java:160) 
//                    at com.qihoo.storager.SharedPreferencesImpl.readMapXml(SharedPreferencesImpl.java:138) 
//                    at com.qihoo.storager.SharedPreferencesImpl.loadFromDiskLocked(SharedPreferencesImpl.java:328) 
//                    at com.qihoo.storager.SharedPreferencesImpl.access$100(SharedPreferencesImpl.java:57) 
//                    at com.qihoo.storager.SharedPreferencesImpl$1.run(SharedPreferencesImpl.java:297) 
                    exception = e;
                    break;
                }  else if (cause instanceof ClassCastException) {
//                    java.lang.ClassCastException: java.lang.Boolean cannot be cast to java.util.HashMap
//                    at com.android.internal.util.XmlUtils.readMapXml(XmlUtils.java:494)
//                    at android.app.SharedPreferencesImpl.loadFromDiskLocked(SharedPreferencesImpl.java:120)
//                    at android.app.SharedPreferencesImpl.access$000(SharedPreferencesImpl.java:52)
//                    at android.app.SharedPreferencesImpl$1.run(SharedPreferencesImpl.java:91)
//
//                    java.lang.ClassCastException: java.lang.Long cannot be cast to java.util.HashMap
//                    at com.android.internal.util.XmlUtils.readMapXml(XmlUtils.java:494)
//                    at android.app.SharedPreferencesImpl.loadFromDiskLocked(SharedPreferencesImpl.java:120)
//                    at android.app.SharedPreferencesImpl.access$000(SharedPreferencesImpl.java:52)
//                    at android.app.SharedPreferencesImpl$1.run(SharedPreferencesImpl.java:91)
                    exception = e;
                    break;
                } else {
                    throw e;
                }
            }
        }
        if (exception != null) {
        }
        return map;
    }

    private void writeMapXml(Map val, OutputStream out) {
        Exception exception = null;
        int exceptionCount = 0;
        for (int i = 0; i < 2; i++) {
            try {
                ReflectUtils.invokeStaticMethod(
                        "com.android.internal.util.XmlUtils",
                        "writeMapXml",
                        new Class[]{Map.class, OutputStream.class},
                        val,
                        out);
                break;
            } catch (Exception e) {
                Throwable cause = e.getCause() != null && e.getCause().getCause() != null ? e.getCause().getCause() : new Throwable();
                if (cause instanceof ArrayIndexOutOfBoundsException) {
//                    java.lang.ArrayIndexOutOfBoundsException:src.length = 75 srcPos = 0 dst.length = 0
//                    dstPos = 0 length = 57
//                    at java.lang.System.arraycopy(Native Method)
//                    at java.lang.String.getChars(String.java:889)
//                    at com.android.internal.util.FastXmlSerializer.append(FastXmlSerializer.java:89)
//                    at com.android.internal.util.FastXmlSerializer.append(FastXmlSerializer.java:113)
//                    at com.android.internal.util.FastXmlSerializer.startDocument(FastXmlSerializer.java:
//                    326)
//                    at com.android.internal.util.XmlUtils.writeMapXml(XmlUtils.java:183)
//                    at android.app.SharedPreferencesImpl.writeToFile(SharedPreferencesImpl.java:598)
//                    at android.app.SharedPreferencesImpl.access$800(SharedPreferencesImpl.java:52)
//                    at android.app.SharedPreferencesImpl$2.run(SharedPreferencesImpl.java:513)
//                    at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1080)
//                    at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:573)
//                    at java.lang.Thread.run(Thread.java:838)
                    exception = e;
                } else {
                    throw e;
                }
            }
        }
        if (exception != null) {

        }
    }

    private class FileStatus {
        boolean isSucceed;
        long mtime;
        long size;
    }

    private FileStatus getFileStatus(String path) {
        FileStatus fileStatus = new FileStatus();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) { // android.os.FileUtils.FileStatus stat = new android.os.FileUtils.FileStatus(); android.os.FileUtils.getFileStatus(mFile.getPath(), stat)
            Object stat = ReflectUtils.getObjectNewInstance("android.os.FileUtils$FileStatus", null);
            fileStatus.isSucceed = (Boolean) ReflectUtils.invokeStaticMethod("android.os.FileUtils", "getFileStatus", new Class[]{String.class, stat.getClass()}, path, stat);
            if (fileStatus.isSucceed) {
                fileStatus.mtime = (long) ReflectUtils.getFieldValue(stat, "mtime");
                fileStatus.size = (long) ReflectUtils.getFieldValue(stat, "size");
            }
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) { // libcore.io.StructStat stat = libcore.io.Libcore.os.stat(mFile.getPath());
            try {
                Object os = ReflectUtils.getStaticObjectField("libcore.io.Libcore", "os");
                Object stat = ReflectUtils.invokeMethod(os, "stat", new Class[]{String.class}, mFile.getPath());
                fileStatus.isSucceed = true;
                fileStatus.mtime = (long) ReflectUtils.getFieldValue(stat, "st_mtime");
                fileStatus.size = (long) ReflectUtils.getFieldValue(stat, "st_size");
            } catch (Exception e) {
                Throwable cause = e.getCause() != null && e.getCause().getCause() != null ? e.getCause().getCause() : new Throwable();
                if (!cause.getClass().getName().equals("libcore.io.ErrnoException")) {
                    throw e;
                }
            }
        } else { // android.system.StructStat stat = android.system.Os.stat(mFile.getPath());
            try {
                Object stat = ReflectUtils.invokeStaticMethod("android.system.Os", "stat", new Class[]{String.class}, mFile.getPath());
                fileStatus.isSucceed = true;
                fileStatus.mtime = (long) ReflectUtils.getFieldValue(stat, "st_mtime");
                fileStatus.size = (long) ReflectUtils.getFieldValue(stat, "st_size");
            } catch (Exception e) {
                Throwable cause = e.getCause() != null && e.getCause().getCause() != null ? e.getCause().getCause() : new Throwable();
                if (!cause.getClass().getName().equals("android.system.ErrnoException")) {
                    throw e;
                }
            }
        }
        return fileStatus;
    }

    SharedPreferencesImpl(File file, int mode) {
        mFile = file;
        mBackupFile = makeBackupFile(file);
        mMode = mode;
        mLoaded = false;
        mMap = null;
        startLoadFromDisk();
    }

    private void startLoadFromDisk() {
        synchronized (this) {
            mLoaded = false;
        }
        new Thread(TAG + "-startLoadFromDisk") {
            public void run() {
                loadFromDisk();
            }
        }.start();
    }

    private void loadFromDisk() {
        synchronized (SharedPreferencesImpl.this) {
            if (mLoaded) {
                return;
            }
            if (mBackupFile.exists()) {
                mFile.delete();
                mBackupFile.renameTo(mFile);
            }
        }

        // Debugging
        if (mFile.exists() && !mFile.canRead()) {
            Log.w(TAG, "Attempt to read preferences file " + mFile + " without permission");
        }

        Map map = null;
//        StructStat stat = null;
//        try {
//            stat = Os.stat(mFile.getPath());
        FileStatus stat = getFileStatus(mFile.getPath());
        if (stat.isSucceed && mFile.canRead()) {
            BufferedInputStream str = null;
            try {
                str = new BufferedInputStream(
                        new FileInputStream(mFile), 16 * 1024);
//                    map = XmlUtils.readMapXml(str);
                map = readMapXml(str);
            }/* catch (XmlPullParserException e) {
                    Log.w(TAG, "getSharedPreferences", e);
                }*/ catch (FileNotFoundException e) {
                Log.w(TAG, "getSharedPreferences", e);
            }/* catch (IOException e) {
                    Log.w(TAG, "getSharedPreferences", e);
                }*/ catch (Exception e) {
                Throwable cause = e.getCause() != null && e.getCause().getCause() != null ? e.getCause().getCause() : new Throwable();
                if (cause.getClass().getName().equals("org.xmlpull.v1.XmlPullParserException")) { // readMapXml
                    Log.w(TAG, "getSharedPreferences", e);
                } else if (cause instanceof IOException) { // readMapXml
                    Log.w(TAG, "getSharedPreferences", e);
                } else {
                    throw e;
                }
            } finally {
                if (str != null) {
                    try {
                        str.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
        /** 已经在{@link #getFileStatus}中处理了*/
//        } catch (ErrnoException e) {
//            /* ignore */
//        }
        synchronized (SharedPreferencesImpl.this) {
            mLoaded = true;
            if (map != null) {
                mMap = map;
//            mStatTimestamp = stat.st_mtime;
                mStatTimestamp = stat.mtime;
//            mStatSize = stat.st_size;
                mStatSize = stat.size;
            } else {
                mMap = new HashMap<>();
            }
            notifyAll();
        }
    }

    static File makeBackupFile(File prefsFile) {
        return new File(prefsFile.getPath() + ".bak");
    }

    void startReloadIfChangedUnexpectedly() {
        synchronized (this) {
            // TODO: wait for any pending writes to disk?
            if (!hasFileChangedUnexpectedly()) {
                return;
            }
            startLoadFromDisk();
        }
    }

    // Has the file changed out from under us?  i.e. writes that
    // we didn't instigate.
    private boolean hasFileChangedUnexpectedly() {
        synchronized (this) {
            if (mDiskWritesInFlight > 0) {
                // If we know we caused it, it's not unexpected.
                if (DEBUG) Log.d(TAG, "disk write in flight, not unexpected.");
                return false;
            }
        }

//        final StructStat stat;
//        try {
            /*
             * Metadata operations don't usually count as a block guard
             * violation, but we explicitly want this one.
             */
//            BlockGuard.getThreadPolicy().onReadFromDisk();
        Object blockGuardPolicy = ReflectUtils.invokeStaticMethod("dalvik.system.BlockGuard", "getThreadPolicy", null);
        ReflectUtils.invokeMethod(blockGuardPolicy, "onReadFromDisk", null);
//            stat = Os.stat(mFile.getPath());
//        } catch (ErrnoException e) {
//            return true;
//        }
        FileStatus stat = getFileStatus(mFile.getPath());
        if (!stat.isSucceed) {
            return true;
        }

        synchronized (this) {
//            return mStatTimestamp != stat.st_mtime || mStatSize != stat.st_size;
            return mStatTimestamp != stat.mtime || mStatSize != stat.size;
        }
    }

    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        synchronized(this) {
            mListeners.put(listener, mContent);
        }
    }

    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        synchronized(this) {
            mListeners.remove(listener);
        }
    }

    private void awaitLoadedLocked() {
//        TestUtil.start("awaitLoadedLocked");
        if (!mLoaded) {
            // Raise an explicit StrictMode onReadFromDisk for this
            // thread, since the real read will be in a different
            // thread and otherwise ignored by StrictMode.
//            BlockGuard.getThreadPolicy().onReadFromDisk();
            Object blockGuardPolicy = ReflectUtils.invokeStaticMethod("dalvik.system.BlockGuard", "getThreadPolicy", null);
            ReflectUtils.invokeMethod(blockGuardPolicy, "onReadFromDisk", null);
        }
        while (!mLoaded) {
            try {
                wait();
            } catch (InterruptedException unused) {
            }
        }
//        TestUtil.end("awaitLoadedLocked");
    }

    public Map<String, ?> getAll() {
        synchronized (this) {
            awaitLoadedLocked();
            //noinspection unchecked
            return new HashMap<String, Object>(mMap);
        }
    }

    @Nullable
    public String getString(String key, @Nullable String defValue) {
        synchronized (this) {
            awaitLoadedLocked();
            String v = (String)mMap.get(key);
            return v != null ? v : defValue;
        }
    }

    @Nullable
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        synchronized (this) {
            awaitLoadedLocked();
            Set<String> v = (Set<String>) mMap.get(key);
            return v != null ? v : defValues;
        }
    }

    public int getInt(String key, int defValue) {
        synchronized (this) {
            awaitLoadedLocked();
            Integer v = (Integer)mMap.get(key);
            return v != null ? v : defValue;
        }
    }
    public long getLong(String key, long defValue) {
        synchronized (this) {
            awaitLoadedLocked();
            Long v = (Long)mMap.get(key);
            return v != null ? v : defValue;
        }
    }
    public float getFloat(String key, float defValue) {
        synchronized (this) {
            awaitLoadedLocked();
            Float v = (Float)mMap.get(key);
            return v != null ? v : defValue;
        }
    }
    public boolean getBoolean(String key, boolean defValue) {
        synchronized (this) {
            awaitLoadedLocked();
            Boolean v = (Boolean)mMap.get(key);
            return v != null ? v : defValue;
        }
    }

    public boolean contains(String key) {
        synchronized (this) {
            awaitLoadedLocked();
            return mMap.containsKey(key);
        }
    }

    public Editor edit() {
        // TODO: remove the need to call awaitLoadedLocked() when
        // requesting an editor.  will require some work on the
        // Editor, but then we should be able to do:
        //
        //      SharedPreferencesImpl.getSharedPreferences(context, ..).edit().putString(..).apply()
        //
        // ... all without blocking.
        synchronized (this) {
            awaitLoadedLocked();
        }

        return new EditorImpl();
    }

    // Return value from EditorImpl#commitToMemory()
    private static class MemoryCommitResult {
        public boolean changesMade;  // any keys different?
        public List<String> keysModified;  // may be null
        public Set<OnSharedPreferenceChangeListener> listeners;  // may be null
        public Map<?, ?> mapToWriteToDisk;
        public final CountDownLatch writtenToDiskLatch = new CountDownLatch(1);
        public volatile boolean writeToDiskResult = false;

        public void setDiskWriteResult(boolean result) {
            writeToDiskResult = result;
            writtenToDiskLatch.countDown();
        }
    }

    public final class EditorImpl implements Editor {
        private final Map<String, Object> mModified = new HashMap<>();
        private boolean mClear = false;

        public Editor putString(String key, @Nullable String value) {
            synchronized (this) {
                mModified.put(key, value);
                return this;
            }
        }
        public Editor putStringSet(String key, @Nullable Set<String> values) {
            synchronized (this) {
                mModified.put(key,
                        (values == null) ? null : new HashSet<String>(values));
                return this;
            }
        }
        public Editor putInt(String key, int value) {
            synchronized (this) {
                mModified.put(key, value);
                return this;
            }
        }
        public Editor putLong(String key, long value) {
            synchronized (this) {
                mModified.put(key, value);
                return this;
            }
        }
        public Editor putFloat(String key, float value) {
            synchronized (this) {
                mModified.put(key, value);
                return this;
            }
        }
        public Editor putBoolean(String key, boolean value) {
            synchronized (this) {
                mModified.put(key, value);
                return this;
            }
        }

        public Editor remove(String key) {
            synchronized (this) {
                mModified.put(key, this);
                return this;
            }
        }

        public Editor clear() {
            synchronized (this) {
                mClear = true;
                return this;
            }
        }

        public void apply() {
            final MemoryCommitResult mcr = commitToMemory();
            final Runnable awaitCommit = new Runnable() {
                public void run() {
                    try {
                        mcr.writtenToDiskLatch.await();
                    } catch (InterruptedException ignored) {
                    }
                }
            };

//            QueuedWork.add(awaitCommit);
//            不再执行QueuedWork.add(awaitCommit)，避免系统QueuedWork.waitToFinish导致界面卡顿和Anr的发生，
//            同时SharedPreferences的存储在startService、Activity.onStop后可能不能立即生效；
//            ReflectUtils.invokeStaticMethod("android.app.QueuedWork", "add", new Class[]{Runnable.class}, awaitCommit);

            Runnable postWriteRunnable = new Runnable() {
                public void run() {
                    awaitCommit.run();
//                    QueuedWork.remove(awaitCommit);
//                    ReflectUtils.invokeStaticMethod("android.app.QueuedWork", "remove", new Class[]{Runnable.class}, awaitCommit);
                }
            };

            SharedPreferencesImpl.this.enqueueDiskWrite(mcr, postWriteRunnable);

            // Okay to notify the listeners before it's hit disk
            // because the listeners should always get the same
            // SharedPreferences instance back, which has the
            // changes reflected in memory.
            notifyListeners(mcr);

            if (DEBUG) {
                LogUtils.d(TAG, "apply.mFile = " + mFile + ", mFile.length() = " + mFile.length() + ", mMap.size() = " + mMap.size());
            }
        }

        // Returns true if any changes were made
        private MemoryCommitResult commitToMemory() {
            MemoryCommitResult mcr = new MemoryCommitResult();
            synchronized (SharedPreferencesImpl.this) {
                // We optimistically don't make a deep copy until
                // a memory commit comes in when we're already
                // writing to disk.
                if (mDiskWritesInFlight > 0) {
                    // We can't modify our mMap as a currently
                    // in-flight write owns it.  Clone it before
                    // modifying it.
                    // noinspection unchecked
                    mMap = new HashMap<String, Object>(mMap);
                }
                mcr.mapToWriteToDisk = mMap;
                mDiskWritesInFlight++;

                boolean hasListeners = mListeners.size() > 0;
                if (hasListeners) {
                    mcr.keysModified = new ArrayList<String>();
                    mcr.listeners =
                            new HashSet<OnSharedPreferenceChangeListener>(mListeners.keySet());
                }

                synchronized (this) {
                    if (mClear) {
                        if (!mMap.isEmpty()) {
                            mcr.changesMade = true;
                            mMap.clear();
                        }
                        mClear = false;
                    }

                    for (Map.Entry<String, Object> e : mModified.entrySet()) {
                        String k = e.getKey();
                        Object v = e.getValue();
                        // "this" is the magic value for a removal mutation. In addition,
                        // setting a value to "null" for a given key is specified to be
                        // equivalent to calling remove on that key.
                        if (v == this || v == null) {
                            if (!mMap.containsKey(k)) {
                                continue;
                            }
                            mMap.remove(k);
                        } else {
                            if (mMap.containsKey(k)) {
                                Object existingValue = mMap.get(k);
                                if (existingValue != null && existingValue.equals(v)) {
                                    continue;
                                }
                            }
                            mMap.put(k, v);
                        }

                        mcr.changesMade = true;
                        if (hasListeners) {
                            mcr.keysModified.add(k);
                        }
                    }

                    mModified.clear();
                }
            }
            return mcr;
        }

        public boolean commit() {
            MemoryCommitResult mcr = commitToMemory();
            SharedPreferencesImpl.this.enqueueDiskWrite(
                    mcr, null /* sync write on this thread okay */);
            try {
                mcr.writtenToDiskLatch.await();
            } catch (InterruptedException e) {
                return false;
            }
            notifyListeners(mcr);

            if (DEBUG) {
                LogUtils.d(TAG, "commit.mFile = " + mFile + ", mFile.length() = " + mFile.length() + ", mMap.size() = " + mMap.size());
            }
            return mcr.writeToDiskResult;
        }

        private void notifyListeners(final MemoryCommitResult mcr) {
            if (mcr.listeners == null || mcr.keysModified == null ||
                    mcr.keysModified.size() == 0) {
                return;
            }
            if (Looper.myLooper() == Looper.getMainLooper()) {
                for (int i = mcr.keysModified.size() - 1; i >= 0; i--) {
                    final String key = mcr.keysModified.get(i);
                    for (OnSharedPreferenceChangeListener listener : mcr.listeners) {
                        if (listener != null) {
                            listener.onSharedPreferenceChanged(SharedPreferencesImpl.this, key);
                        }
                    }
                }
            } else {
                // Run this function on the main thread.
//                ActivityThread.sMainThreadHandler.post(new Runnable() {
//                    public void run() {
//                        notifyListeners(mcr);
//                    }
//                });
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    public void run() {
                        notifyListeners(mcr);
                    }
                });
            }
        }
    }

    /**
     * Enqueue an already-committed-to-memory result to be written
     * to disk.
     *
     * They will be written to disk one-at-a-time in the order
     * that they're enqueued.
     *
     * @param postWriteRunnable if non-null, we're being called
     *   from apply() and this is the runnable to run after
     *   the write proceeds.  if null (from a regular commit()),
     *   then we're allowed to do this disk write on the main
     *   thread (which in addition to reducing allocations and
     *   creating a background thread, this has the advantage that
     *   we catch them in userdebug StrictMode reports to convert
     *   them where possible to apply() ...)
     */
    private void enqueueDiskWrite(final MemoryCommitResult mcr,
                                  final Runnable postWriteRunnable) {
        final Runnable writeToDiskRunnable = new Runnable() {
            public void run() {
                synchronized (mWritingToDiskLock) {
                    writeToFile(mcr);
                }
                synchronized (SharedPreferencesImpl.this) {
                    mDiskWritesInFlight--;
                }
                if (postWriteRunnable != null) {
                    postWriteRunnable.run();
                }
            }
        };

        final boolean isFromSyncCommit = (postWriteRunnable == null);

        // Typical #commit() path with fewer allocations, doing a write on
        // the current thread.
        if (isFromSyncCommit) {
            boolean wasEmpty;
            synchronized (SharedPreferencesImpl.this) {
                wasEmpty = mDiskWritesInFlight == 1;
            }
            if (wasEmpty) {
                writeToDiskRunnable.run();
                return;
            }
        }

//        QueuedWork.singleThreadExecutor().execute(writeToDiskRunnable);
//        ((java.util.concurrent.ExecutorService) ReflectUtils.invokeStaticMethod("android.app.QueuedWork", "singleThreadExecutor", null)).execute(writeToDiskRunnable);

//      解决：由于系统对SharedPreferences的存储采用的是进程内全局单线程存储，
//           当系统IO操作占用系统资源过多、SharedPreferences文件过大、SharedPreferences文件操作过于频繁时，
//           系统对SharedPreferences的保护措施（android.app.QueuedWork.waitToFinish）会导致anr的问题；
//      因此：使用多线程的线程池代替android.app.QueuedWork.singleThreadExecutor单线程池，加快存储速度；
//      系统源码中，android.app.QueuedWork.waitToFinish被执行的地方有：
//           android.app.ActivityThread.handleServiceArgs(ServiceArgsData)
//           android.app.ActivityThread.handleStopService(IBinder)
//           android.app.ActivityThread.handleStopActivity(IBinder, boolean, int, int)
//           android.app.ActivityThread.handleSleeping(IBinder, boolean)
//      anr traces 日志：
//        "main" prio=5 tid=1 Waiting
//          | group="main" sCount=1 dsCount=0 obj=0x742d5690 self=0xb4e36500
//          | sysTid=6387 nice=-6 cgrp=default sched=0/0 handle=0xb6ff7c00
//          | state=S schedstat=( 0 0 0 ) utm=8248 stm=1550 core=3 HZ=100
//          | stack=0xbe421000-0xbe423000 stackSize=8MB
//          | held mutexes=
//          at java.lang.Object.wait!(Native method)
//         - waiting on <0x03c83afa> (a java.lang.Object)
//         at java.lang.Thread.parkFor$(Thread.java:1220)
//         - locked <0x03c83afa> (a java.lang.Object)
//         at sun.misc.Unsafe.park(Unsafe.java:299)
//         at java.util.concurrent.locks.LockSupport.park(LockSupport.java:158)
//         at java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt(AbstractQueuedSynchronizer.java:810)
//         at java.util.concurrent.locks.AbstractQueuedSynchronizer.doAcquireSharedInterruptibly(AbstractQueuedSynchronizer.java:971)
//         at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireSharedInterruptibly(AbstractQueuedSynchronizer.java:1278)
//         at java.util.concurrent.CountDownLatch.await(CountDownLatch.java:203)
//         at com.qihoo.storager.SharedPreferencesImpl$EditorImpl$1.run(AppStore:694)
//         at android.app.QueuedWork.waitToFinish(QueuedWork.java:88)
//         at android.app.ActivityThread.handleServiceArgs(ActivityThread.java:3041)
//         at android.app.ActivityThread.-wrap17(ActivityThread.java:-1)
//         at android.app.ActivityThread$H.handleMessage(ActivityThread.java:1455)
//         at android.os.Handler.dispatchMessage(Handler.java:102)
//         at android.os.Looper.loop(Looper.java:148)
//         at android.app.ActivityThread.main(ActivityThread.java:5497)
//         at java.lang.reflect.Method.invoke!(Native method)
//         at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:799)
//         at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:689)
        mExecutor.execute(writeToDiskRunnable);
    }

    private static FileOutputStream createFileOutputStream(File file) {
        FileOutputStream str = null;
        try {
            str = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            File parent = file.getParentFile();
            if (!parent.mkdir()) {
                Log.e(TAG, "Couldn't create directory for SharedPreferences file " + file);
                return null;
            }
//            FileUtils.setPermissions(
//                    parent.getPath(),
//                    FileUtils.S_IRWXU | FileUtils.S_IRWXG | FileUtils.S_IXOTH,
//                    -1, -1);
            ReflectUtils.invokeStaticMethod(
                    "android.os.FileUtils",
                    "setPermissions",
                    new Class[]{String.class, int.class, int.class, int.class},
                    parent.getPath(),
                    (ReflectUtils.getStaticIntField("android.os.FileUtils", "S_IRWXU")
                            | ReflectUtils.getStaticIntField("android.os.FileUtils", "S_IRWXG")
                            | ReflectUtils.getStaticIntField("android.os.FileUtils", "S_IXOTH")),
                    -1,
                    -1);
            try {
                str = new FileOutputStream(file);
            } catch (FileNotFoundException e2) {
                Log.e(TAG, "Couldn't create SharedPreferences file " + file, e2);
            }
        }
        return str;
    }

    // Note: must hold mWritingToDiskLock
    private void writeToFile(MemoryCommitResult mcr) {
        // Rename the current file so it may be used as a backup during the next read
        if (mFile.exists()) {
            if (!mcr.changesMade) {
                // If the file already exists, but no changes were
                // made to the underlying map, it's wasteful to
                // re-write the file.  Return as if we wrote it
                // out.
                mcr.setDiskWriteResult(true);
                return;
            }
            if (!mBackupFile.exists()) {
                if (!mFile.renameTo(mBackupFile)) {
                    Log.e(TAG, "Couldn't rename file " + mFile
                            + " to backup file " + mBackupFile);
                    mcr.setDiskWriteResult(false);
                    return;
                }
            } else {
                mFile.delete();
            }
        }

        // Attempt to write the file, delete the backup and return true as atomically as
        // possible.  If any exception occurs, delete the new file; next time we will restore
        // from the backup.
        FileOutputStream str = null;
        try {
            str = createFileOutputStream(mFile);
            if (str == null) {
                mcr.setDiskWriteResult(false);
                return;
            }
//            XmlUtils.writeMapXml(mcr.mapToWriteToDisk, str);
            writeMapXml(mcr.mapToWriteToDisk, str);
//            FileUtils.sync(str);
            ReflectUtils.invokeStaticMethod("android.os.FileUtils", "sync", new Class[]{FileOutputStream.class}, str);
//            str.close();
//            ContextImpl.setFilePermissionsFromMode(mFile.getPath(), mMode, 0);
            try {
                ReflectUtils.invokeStaticMethod(
                        "android.app.ContextImpl",
                        "setFilePermissionsFromMode",
                        new Class[]{String.class, int.class, int.class},
                        mFile.getPath(), mMode, 0);
            } catch (RuntimeException e) {
//                samsung SCH-I959 5.0.1(21)、samsung SM-G9008V 5.0(21)、OPPO R805 2.3.6(10)、Huawei HUAWEI Y321-C00 4.1.2(16)
//                java.lang.RuntimeException: invokeStaticMethod exception, className = android.app.ContextImpl, methodName = setFilePermissionsFromMode
//                at com.qihoo.utils.hideapi.ReflectUtils.invokeStaticMethod(AppStore:161)
//                at com.qihoo.storager.SharedPreferencesImpl.writeToFile(AppStore:951)
//                at com.qihoo.storager.SharedPreferencesImpl.access$100(AppStore:71)
//                access$200
//                        access$502
//                access$1100
//                at com.qihoo.storager.SharedPreferencesImpl$2.run(AppStore:848)
//                at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1112)
//                at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:587)
//                at java.lang.Thread.run(Thread.java:818)
//                Caused by: java.lang.RuntimeException: getDeclaredMethod exception, object = class android.app.ContextImpl, methodName = setFilePermissionsFromMode
//                at com.qihoo.utils.hideapi.ReflectUtils.getDeclaredMethod(AppStore:58)
//                at com.qihoo.utils.hideapi.ReflectUtils.invokeStaticMethod(AppStore:157)
//                ... 6 more
                String msg = null;
                try {
                    Class clazz = Class.forName("android.app.ContextImpl");
                    java.lang.reflect.Method[] ms = clazz.getDeclaredMethods();
                    msg = java.util.Arrays.toString(ms);
                } catch (Throwable ignore) {
                }
            }
//            try {
//                final StructStat stat = Os.stat(mFile.getPath());
//                synchronized (this) {
//                    mStatTimestamp = stat.st_mtime;
//                    mStatSize = stat.st_size;
//                }
//            } catch (ErrnoException e) {
//                // Do nothing
//            }
            FileStatus stat = getFileStatus(mFile.getPath());
            if (stat.isSucceed) {
                synchronized (this) {
                    mStatTimestamp = stat.mtime;
                    mStatSize = stat.size;
                }
            }
            // Writing was successful, delete the backup file if there is one.
            mBackupFile.delete();
            mcr.setDiskWriteResult(true);
            return;
        }/* catch (XmlPullParserException e) {
            Log.w(TAG, "writeToFile: Got exception:", e);
        } catch (IOException e) {
            Log.w(TAG, "writeToFile: Got exception:", e);
        } */ catch (Exception e) {
            Throwable cause = e.getCause() != null && e.getCause().getCause() != null ? e.getCause().getCause() : new Throwable();
            if (cause.getClass().getName().equals("org.xmlpull.v1.XmlPullParserException")) { // writeMapXml
                Log.w(TAG, "writeToFile: Got exception:", e);
            } else if (cause.getClass().getName().equals("android.system.ErrnoException")) { // writeMapXml
//                java.lang.RuntimeException: invokeMethod exception, receiver = libcore.io.BlockGuardOs@349683c6, methodName = stat
//                at com.qihoo.utils.hideapi.ReflectUtils.invokeMethod(AppStore:75)
//                at com.qihoo.storager.SharedPreferencesImpl.getFileStatus(AppStore:346)
//                at com.qihoo.storager.SharedPreferencesImpl.loadFromDisk(AppStore:412)
//                at com.qihoo.storager.SharedPreferencesImpl.access$100(AppStore:71)
//                access$200
//                        access$502
//                access$1100
//                at com.qihoo.storager.SharedPreferencesImpl$1.run(AppStore:387)
//                Caused by: java.lang.reflect.InvocationTargetException
//                at java.lang.reflect.Method.invoke(Native Method)
//                at java.lang.reflect.Method.invoke(Method.java:372)
//                at com.qihoo.utils.hideapi.ReflectUtils.invokeMethod(AppStore:73)
//                ... 4 more
//                Caused by: android.system.ErrnoException: stat failed: ENOENT (No such file or directory)
//                at libcore.io.Posix.stat(Native Method)
//                at libcore.io.BlockGuardOs.stat(BlockGuardOs.java:289)
//                ... 7 more
                Log.w(TAG, "writeToFile: Got exception:", e);
            } else if (cause instanceof IOException) { // writeMapXml
//                java.lang.RuntimeException: invokeStaticMethod exception, className = com.android.internal.util.XmlUtils, methodName = writeMapXml
//                at com.qihoo.utils.hideapi.ReflectUtils.invokeStaticMethod(AppStore:162)
//                at com.qihoo.storager.SharedPreferencesImpl.writeMapXml(AppStore:263)
//                at com.qihoo.storager.SharedPreferencesImpl.writeToFile(AppStore:916)
//                at com.qihoo.storager.SharedPreferencesImpl.access$100(AppStore:56)
//                access$200
//                        access$502
//                access$900
//                at com.qihoo.storager.SharedPreferencesImpl$2.run(AppStore:819)
//                at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1080)
//                at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:573)
//                at java.lang.Thread.run(Thread.java:856)
//                Caused by: java.lang.reflect.InvocationTargetException
//                at java.lang.reflect.Method.invokeNative(Native Method)
//                at java.lang.reflect.Method.invoke(Method.java:511)
//                at com.qihoo.utils.hideapi.ReflectUtils.invokeStaticMethod(AppStore:160)
//                ... 7 more
//                Caused by: java.io.IOException: write failed: ENOSPC (No space left on device)
//                at libcore.io.IoBridge.write(IoBridge.java:462)
//                at java.io.FileOutputStream.write(FileOutputStream.java:187)
//                at com.android.internal.util.FastXmlSerializer.flushBytes(FastXmlSerializer.java:212)
//                at com.android.internal.util.FastXmlSerializer.flush(FastXmlSerializer.java:233)
//                at com.android.internal.util.FastXmlSerializer.endDocument(FastXmlSerializer.java:183)
//                at com.android.internal.util.XmlUtils.writeMapXml(XmlUtils.java:186)
//                ... 10 more
//                Caused by: libcore.io.ErrnoException: write failed: ENOSPC (No space left on device)
//                at libcore.io.Posix.writeBytes(Native Method)
//                at libcore.io.Posix.write(Posix.java:187)
//                at java.lang.reflect.Method.invokeNative(Native Method)
//                at java.lang.reflect.Method.invoke(Method.java:511)
//                at com.morgoo.droidplugin.hook.proxy.ProxyHook.invoke(AppStore:62)
//                at $Proxy10.write(Native Method)
//                at libcore.io.BlockGuardOs.write(BlockGuardOs.java:197)
//                at libcore.io.IoBridge.write(IoBridge.java:457)
                Log.w(TAG, "writeToFile: Got exception:", e);
            } else {
                throw e;
            }
        } finally {
            if (str != null) {
                try {
                    str.close();
                } catch (IOException e) {
                    Log.w(TAG, "writeToFile: close exception:", e);
                }
            }
        }
        // Clean up an unsuccessfully written file
        if (mFile.exists()) {
            if (!mFile.delete()) {
                Log.e(TAG, "Couldn't clean up partially-written file " + mFile);
            }
        }
        mcr.setDiskWriteResult(false);
    }
}
