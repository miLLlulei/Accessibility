package com.mill.accessibility.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.mill.accessibility.utils.process.AndroidAppProcess;
import com.mill.accessibility.utils.process.ProcFile;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 安卓api兼容工具类
 */
public class AndroidUtilsCompat {
    private static final String TAG = "AndroidUtilsCompat";

    private static class ForegroundApp {
        public static String lastForegroundAppName = null;
        public static int lastForegroundAppPid = 0;
        public static int lastForegroundAppScore = -2;
    }

    public static boolean supportNewsSdk() {
        return Build.VERSION.SDK_INT >= AndroidVersionCodes.ICE_CREAM_SANDWICH_MR1;
    }

	@TargetApi(AndroidVersionCodes.JELLY_BEAN)
    public static void removeGlobalOnLayoutListener(ViewTreeObserver mViewTreeObserver, ViewTreeObserver.OnGlobalLayoutListener listener) {
        if (Build.VERSION.SDK_INT < AndroidVersionCodes.JELLY_BEAN) {
            mViewTreeObserver.removeGlobalOnLayoutListener(listener);
        } else {
            mViewTreeObserver.removeOnGlobalLayoutListener(listener);
        }
    }

	@TargetApi(AndroidVersionCodes.LOLLIPOP_MR1)
    public static Drawable getDrawable(Resources resources, int id) {
        if (Build.VERSION.SDK_INT < AndroidVersionCodes.LOLLIPOP_MR1) {
            return resources.getDrawable(id);
        } else {
            return resources.getDrawable(id, null);
        }
    }

	@TargetApi(AndroidVersionCodes.JELLY_BEAN)
    public static void setBackgroundDrawable(View view, Drawable d) {
        if (Build.VERSION.SDK_INT < AndroidVersionCodes.JELLY_BEAN) {
            view.setBackgroundDrawable(d);
        } else {
            view.setBackground(d);
        }
    }

    /**
     * 解决在onAnimationEnd里面setLayerType崩溃的问题；
     * http://stackoverflow.com/questions/19614526/android-crash-system-lib-libhwui-so
     *
     * 05-16 16:32:27.076 F/DEBUG   (  398): *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** ***
     * 05-16 16:32:27.076 F/DEBUG   (  398): Build fingerprint: 'QiKU/QK8692/QK8692:6.0.1/MMB29M/16.05.13:user/test-keys'
     * 05-16 16:32:27.076 F/DEBUG   (  398): Revision: '0'
     * 05-16 16:32:27.076 F/DEBUG   (  398): ABI: 'arm'
     * 05-16 16:32:27.077 F/DEBUG   (  398): pid: 21194, tid: 21260, name: RenderThread  >>> com.qihoo.appstore <<<
     * 05-16 16:32:27.077 F/DEBUG   (  398): signal 11 (SIGSEGV), code 1 (SEGV_MAPERR), fault addr 0x4c4b4a59
     * 05-16 16:32:27.078 F/google-breakpad(21837): S D27BF180 F40E14E0010000002C0100D131303230FC0C70D1FFFFFFFFAC0B00D1FFFFFFFFFC0C18E0010000002C0198D0FFFFFFFF6C0618E06F7D1DF7F40E14E001000000240318E0FFFFFFFF5C0A70D1010000005C0A70D1477C1DF710F27BD2F8F17BD2E40124F5AC0B00D1F40E14E0E80124F5002000006F7D1DF72C0100D1FFFFFFFFBC07B0D901000000F40E14E001000000F40E14E001000000AC0B00D1FFFFFFFF8C1398D040F294D080F196D000F094D088F196D000040000903520F71700000004000000E00524F504000000E00524F560F094D08D981DF730F920F760F094D00300000060F094D080F196D000F094D088F196D020000000903520F703000000400124F5DFAB1EF7000000000000000003000000C1561EF7030000009CF37BD220000000F8775FE4C4F27BD228F37BD288F37BD2F8775FE470F37BD20000000010BE4DD9000000000000000010F37BD280F57BD2F8B94DD9E0F27BD243C580E3000000000000000000F37BD20000000058D44DD9FFBF3F000000000040F37BD2
     * 05-16 16:32:27.115 F/DEBUG   (  398):     r0 ef06a6c0  r1 dc191640  r2 dfd286a4  r3 4c4b4a49
     * 05-16 16:32:27.116 F/DEBUG   (  398):     r4 dfd28640  r5 d170a84c  r6 dfd286a4  r7 d170a884
     * 05-16 16:32:27.116 F/DEBUG   (  398):     r8 f39cf884  r9 d4509ce0  sl d09aba00  fp d4509ce0
     * 05-16 16:32:27.116 F/DEBUG   (  398):     ip f627a28d  sp dfd28630  lr f6276a8d  pc f627a2ac  cpsr 600f0030
     * 05-16 16:32:27.138 F/DEBUG   (  398):
     * 05-16 16:32:27.138 F/DEBUG   (  398): backtrace:
     * 05-16 16:32:27.138 F/DEBUG   (  398):     #00 pc 0002a2ac  /system/lib/libhwui.so
     * 05-16 16:32:27.138 F/DEBUG   (  398):     #01 pc 00026a8b  /system/lib/libhwui.so
     * 05-16 16:32:27.138 F/DEBUG   (  398):     #02 pc 0004ed83  /system/lib/libhwui.so
     * 05-16 16:32:27.139 F/DEBUG   (  398):     #03 pc 0004ed83  /system/lib/libhwui.so
     * 05-16 16:32:27.139 F/DEBUG   (  398):     #04 pc 0004ed83  /system/lib/libhwui.so
     * 05-16 16:32:27.139 F/DEBUG   (  398):     #05 pc 0004ed83  /system/lib/libhwui.so
     * 05-16 16:32:27.139 F/DEBUG   (  398):     #06 pc 0004ed83  /system/lib/libhwui.so
     * 05-16 16:32:27.139 F/DEBUG   (  398):     #07 pc 0004ed83  /system/lib/libhwui.so
     * 05-16 16:32:27.139 F/DEBUG   (  398):     #08 pc 0004ed83  /system/lib/libhwui.so
     * 05-16 16:32:27.139 F/DEBUG   (  398):     #09 pc 0004ed83  /system/lib/libhwui.so
     * 05-16 16:32:27.139 F/DEBUG   (  398):     #10 pc 0004ed83  /system/lib/libhwui.so
     * 05-16 16:32:27.139 F/DEBUG   (  398):     #11 pc 0004ed83  /system/lib/libhwui.so
     * 05-16 16:32:27.139 F/DEBUG   (  398):     #12 pc 0003bc9d  /system/lib/libhwui.so
     * 05-16 16:32:27.139 F/DEBUG   (  398):     #13 pc 0001a2e3  /system/lib/libhwui.so
     * 05-16 16:32:27.139 F/DEBUG   (  398):     #14 pc 0001be6d  /system/lib/libhwui.so
     * 05-16 16:32:27.139 F/DEBUG   (  398):     #15 pc 0001e907  /system/lib/libhwui.so (_ZN7android10uirenderer12renderthread12RenderThread10threadLoopEv+62)
     * 05-16 16:32:27.139 F/DEBUG   (  398):     #16 pc 00010115  /system/lib/libutils.so (_ZN7android6Thread11_threadLoopEPv+112)
     * 05-16 16:32:27.139 F/DEBUG   (  398):     #17 pc 000631e3  /system/lib/libandroid_runtime.so (_ZN7android14AndroidRuntime15javaThreadShellEPv+70)
     * 05-16 16:32:27.139 F/DEBUG   (  398):     #18 pc 0003fa7b  /system/lib/libc.so (_ZL15__pthread_startPv+30)
     * 05-16 16:32:27.139 F/DEBUG   (  398):     #19 pc 00019fd5  /system/lib/libc.so (__start_thread+6)
     * 05-16 16:32:28.579 F/DEBUG   (  398):
     * 05-16 16:32:28.579 F/DEBUG   (  398): Tombstone written to: /data/tombstones/tombstone_00
     * 05-16 16:32:28.579 I/chatty  (  398): uid=0(root) /system/bin/debuggerd expire 1 line

     * 05-16 16:32:29.240 W/WindowAnimator( 1149): Failed to dispatch window animation state change.
     * 05-16 16:32:29.240 W/WindowAnimator( 1149): android.os.DeadObjectException
     * 05-16 16:32:29.240 W/WindowAnimator( 1149): 	at android.os.BinderProxy.transactNative(Native Method)
     * 05-16 16:32:29.240 W/WindowAnimator( 1149): 	at android.os.BinderProxy.transact(Binder.java:503)
     * 05-16 16:32:29.240 W/WindowAnimator( 1149): 	at android.view.IWindow$Stub$Proxy.onAnimationStopped(IWindow.java:534)
     * 05-16 16:32:29.240 W/WindowAnimator( 1149): 	at com.android.server.wm.WindowAnimator.updateWindowsLocked(WindowAnimator.java:289)
     * 05-16 16:32:29.240 W/WindowAnimator( 1149): 	at com.android.server.wm.WindowAnimator.updateWindowsLocked(WindowAnimator.java:289)
     * 05-16 16:32:29.240 W/WindowAnimator( 1149): 	at com.android.server.wm.WindowAnimator.updateWindowsLocked(WindowAnimator.java:289)
     * 05-16 16:32:29.240 W/WindowAnimator( 1149): 	at com.android.server.wm.WindowAnimator.animateLocked(WindowAnimator.java:687)
     * 05-16 16:32:29.240 W/WindowAnimator( 1149): 	at com.android.server.wm.WindowAnimator.animateLocked(WindowAnimator.java:687)
     * 05-16 16:32:29.240 W/WindowAnimator( 1149): 	at com.android.server.wm.WindowAnimator.animateLocked(WindowAnimator.java:687)
     * 05-16 16:32:29.240 W/WindowAnimator( 1149): 	at com.android.server.wm.WindowAnimator.access$000(WindowAnimator.java:53)
     * 05-16 16:32:29.240 W/WindowAnimator( 1149): 	at com.android.server.wm.WindowAnimator.access$000(WindowAnimator.java:53)
     * 05-16 16:32:29.240 W/WindowAnimator( 1149): 	at com.android.server.wm.WindowAnimator.access$000(WindowAnimator.java:53)
     * 05-16 16:32:29.240 W/WindowAnimator( 1149): 	at com.android.server.wm.WindowAnimator$1.doFrame(WindowAnimator.java:123)
     * 05-16 16:32:29.240 W/WindowAnimator( 1149): 	at com.android.server.wm.WindowAnimator$1.doFrame(WindowAnimator.java:123)
     * 05-16 16:32:29.240 W/WindowAnimator( 1149): 	at com.android.server.wm.WindowAnimator$1.doFrame(WindowAnimator.java:123)
     * 05-16 16:32:29.240 W/WindowAnimator( 1149): 	at android.view.Choreographer$CallbackRecord.run(Choreographer.java:856)
     * 05-16 16:32:29.240 W/WindowAnimator( 1149): 	at android.view.Choreographer.doCallbacks(Choreographer.java:670)
     * 05-16 16:32:29.240 W/WindowAnimator( 1149): 	at android.view.Choreographer.doFrame(Choreographer.java:603)
     * 05-16 16:32:29.240 W/WindowAnimator( 1149): 	at android.view.Choreographer$FrameDisplayEventReceiver.run(Choreographer.java:844)
     * 05-16 16:32:29.240 W/WindowAnimator( 1149): 	at android.os.Handler.handleCallback(Handler.java:739)
     * 05-16 16:32:29.240 W/WindowAnimator( 1149): 	at android.os.Handler.dispatchMessage(Handler.java:95)
     * 05-16 16:32:29.240 W/WindowAnimator( 1149): 	at android.os.Looper.loop(Looper.java:148)
     * 05-16 16:32:29.240 W/WindowAnimator( 1149): 	at android.os.HandlerThread.run(HandlerThread.java:61)
     * 05-16 16:32:29.240 W/WindowAnimator( 1149): 	at com.android.server.ServiceThread.run(ServiceThread.java:46)
     *
     * @param view
     */
    public static void fixedSetLayerTypeWhenOnAnimationEnd(final View view, final int layerType, final Paint paint) {
        if (view != null) {
            view.post(new Runnable() {
                @Override
                public void run() {
                    view.setLayerType(layerType, paint);
                }
            });
        }
    }

    /**
     * 全局设置AsyncTask中的线程池为并发线程池；
     * 因为：如果设置不同的targetSdkVersion则导致AsyncTask的执行顺序是不一样的，如果设置为小于13，则AsyncTask是并行执行的；如果设置为大于等于13，则会顺序执行；
     */
    public static void setAsyncTaskDefaultExecutor(final GetAsyncTaskNamesProxyOnRejectedExecution getAsyncTaskNamesProxy) {
        Class<?> cls = AsyncTask.class;
//        解决：低版本设备AsyncTask在子线程首次实例化（new AsyncTask()）崩溃的问题；
//        java.lang.RuntimeException: Can't create handler inside thread that has not called Looper.prepare()
//        at android.os.Handler.<init>(Handler.java:121)
//        at android.os.AsyncTask$InternalHandler.<init>(AsyncTask.java:421)
//        at android.os.AsyncTask$InternalHandler.<init>(AsyncTask.java:421)
//        at android.os.AsyncTask.<clinit>(AsyncTask.java:152)
        if (Build.VERSION.SDK_INT < AndroidVersionCodes.ICE_CREAM_SANDWICH) { // Android4.0
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    return null;
                }
            };
        } else if (Build.VERSION.SDK_INT >= AndroidVersionCodes.ICE_CREAM_SANDWICH
                && Build.VERSION.SDK_INT <= AndroidVersionCodes.ICE_CREAM_SANDWICH_MR1) { // 4.0~4.0.4
            try {
                java.lang.reflect.Method init = cls.getMethod("init");
                init.invoke(cls);
            } catch (Throwable e) {
                if (LogUtils.isDebug()) {
                    e.printStackTrace();
                }
            }
        }
        if (Build.VERSION.SDK_INT >= AndroidVersionCodes.HONEYCOMB_MR2) { // 3.2
            try {
                Object objectExecutor = cls.getField("THREAD_POOL_EXECUTOR").get(null);
                if (objectExecutor instanceof ThreadPoolExecutor) {
                    ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) objectExecutor;
                    threadPoolExecutor.allowCoreThreadTimeOut(true);
                    RejectedExecutionHandler rejectedExecutionHandler = new RejectedExecutionHandler() {
                        @Override
                        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                            String tasks;
                            if (getAsyncTaskNamesProxy != null) {
                                tasks = getAsyncTaskNamesProxy.getAsyncTaskNames(r, executor);
                            } else {
                                tasks = getDefaultAsyncTaskNamesOnRejectedExecution().getAsyncTaskNames(r, executor);
                            }
                            throw new RejectedExecutionException(
                                    "setAsyncTaskDefaultExecutor.rejectedExecution"
                                            + ".Tasks: " + tasks
                                            + " rejected from " + executor.toString());
                        }
                    };
                    threadPoolExecutor.setRejectedExecutionHandler(rejectedExecutionHandler);
                }
                java.lang.reflect.Method setDefaultExecutor = cls.getMethod("setDefaultExecutor", Executor.class);
                setDefaultExecutor.invoke(cls, objectExecutor);
            } catch (Throwable e) {
                if (LogUtils.isDebug()) {
                    LogUtils.e(TAG, "setAsyncTaskDefaultExecutor", e);
                }
            }
        }
    }

    /**
     * 在AsyncTask发生RejectedExecution时获取AsyncTask中任务类的外部类名称（如：new AsyncTask()匿名内部类所在的外部类），
     * 便于检查哪些任务被拒绝、哪些任务在队列里、哪些任务存在死循环问题；
     *
     * @return
     */
    public static GetAsyncTaskNamesProxyOnRejectedExecution getDefaultAsyncTaskNamesOnRejectedExecution() {
        return new GetAsyncTaskNamesProxyOnRejectedExecution() {
            @Override
            public String getAsyncTaskNames(Runnable r, ThreadPoolExecutor executor) {
                StringBuilder result = new StringBuilder();
                result.append("r.OuterClass = " + getOuterClass(r));
                result.append("\nqueue.runnable.OuterClasses = [");
                BlockingQueue<Runnable> queue = executor.getQueue();
                boolean isFirst = false;
                for (Runnable task : queue) {
                    if (!isFirst) {
                        isFirst = true;
                    } else {
                        result.append("\n");
                    }
                    Object outerClass = getOuterClass(task);
                    result.append(outerClass);
                }
                result.append("]");
                return result.toString();
            }
        };
    }

    /**
     * 获取内部类所在的外部类的引用对象
     *
     * @param obj
     * @return
     */
    public static Object getOuterClass(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            Field fieldThis0 = obj.getClass().getDeclaredField("this$0"); // this$0特指该内部类所在的外部类的引用，不需要手动定义，编译时自动加上；
            fieldThis0.setAccessible(true);
            return fieldThis0.get(obj);
        } catch (Exception e) {
            if (LogUtils.isDebug()) {
                LogUtils.d(TAG, "getOuterClass", e);
            }
            return null;
        }
    }


    public interface GetAsyncTaskNamesProxyOnRejectedExecution{
        String getAsyncTaskNames(Runnable r, ThreadPoolExecutor executor);
    }

    public static String getTopPkgName(Context context) {
        return getTopPkgName(context, System.currentTimeMillis() - 5000);
    }

    private static boolean isCanReadThirdPartyProcDir() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP // Android5.0
                && Build.VERSION.SDK_INT < AndroidVersionCodes.N;
    }

    /**
     * Android5.0以下，{@link ActivityManager#getRunningTasks(int)}，能读取自己和第三方应用
     * Android5.0及以上，{@link ActivityManager#getRunningAppProcesses()}，只能读取自己
     * Android5.0及以上，Android7.0以下：使用读取proc进程id子目录的方式，能读取自己和第三方应用
     * Android7.0及以上，{@link ActivityManager#getRunningAppProcesses()}，只能读取自己
     *
     * @param context
     * @param beginTime
     * @return
     */
    public static String getTopPkgName(Context context, long beginTime) {
        String topPackageName = PackageUsageHelper.getTopPackageName(context, beginTime);
        if (TextUtils.isEmpty(topPackageName)) {
            if (isCanReadThirdPartyProcDir()) {
                topPackageName = getTopPkgNameByPid(context);
                if (TextUtils.isEmpty(topPackageName)) { // 部分手机Android6.0也不让读proc目录
                    topPackageName = getTopPkgNameByActivityManager(context);
                }
            } else {
                topPackageName = getTopPkgNameByActivityManager(context);
            }
        }
        return topPackageName;
    }

    private static String getTopPkgNameByActivityManager(Context context) {
        if (context == null) {
            return null;
        }
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        String strTopPName = null;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) { // Android5.0
            List<ActivityManager.RunningTaskInfo> runningTaskInfos = null;
            try {
                runningTaskInfos = activityManager.getRunningTasks(1);
            } catch (NullPointerException e) {
//                vivo vivo X3t 4.2.1(17)、Huawei HUAWEI G750-T01 4.2.2(17)、OPPO R8207 4.4.4(19)
//                java.lang.NullPointerException
//                at android.app.ActivityManager.getRunningTasks(ActivityManager.java:769)
//                at android.app.ActivityManager.getRunningTasks(ActivityManager.java:805)
//                at com.qihoo.utils.AndroidUtilsCompat.getTopPkgName(AppStore:311)
//                at com.qihoo360.mobilesafe.util.OSUtils.isLauncherTop(AppStore:620)
//                at com.qihoo.core.CoreService$CheckAppUpdateTask.a(AppStore:293)
//                at com.qihoo.core.CoreService$CheckAppUpdateTask.doInBackground(AppStore:284)
//                at android.os.AsyncTask$2.call(AsyncTask.java:288)
//                at java.util.concurrent.FutureTask.run(FutureTask.java:237)
                if (LogUtils.isDebug()) {
                    LogUtils.e(TAG, "getTopPkgNameByActivityManager", e);
                }
            }
            if (runningTaskInfos != null && !runningTaskInfos.isEmpty()) {
                ComponentName componentName = runningTaskInfos.get(0).topActivity;
                if (componentName != null && componentName.getPackageName() != null) {
                    strTopPName = componentName.getPackageName();
                }
            }
        } else {
            final List<ActivityManager.RunningAppProcessInfo> processInfos = getRunningAppProcessesHandleException(activityManager);
            if (processInfos != null && !processInfos.isEmpty()) {
                for (ActivityManager.RunningAppProcessInfo processInfo : processInfos) {
                    if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                            && ReflectUtils.getIntField(processInfo, "flags") == 4 // android.app.ActivityManager.RunningAppProcessInfo.FLAG_HAS_ACTIVITIES
                            && processInfo.pkgList.length == 1) {
                        strTopPName = processInfo.pkgList[0];
                        break;
                    }
                }
            }
        }
        return strTopPName;
    }

    /**
     * Android5.0及以上版本，{@link ActivityManager#getRunningAppProcesses()}只能读取到自己的进程信息；
     *
     * @param activityManager
     * @return
     */
    private static List<ActivityManager.RunningAppProcessInfo> getRunningAppProcessesHandleException(ActivityManager activityManager) {
        List<ActivityManager.RunningAppProcessInfo> processInfos = null;
        if (activityManager != null) {
            try {
                processInfos = activityManager.getRunningAppProcesses();
            } catch (RuntimeException e) {
                if (Build.VERSION.SDK_INT >= AndroidVersionCodes.N) { // Android7.0
//                    lge LG-H860 7.0(24)、motorola Moto G 7.1(25)、Xiaomi Mi-4c 7.0(24)、Sony F8132 7.0(24)
//                    java.lang.RuntimeException: An error occurred while executing doInBackground()
//                    at android.os.AsyncTask$3.done(AsyncTask.java:318)
//                    at java.util.concurrent.FutureTask.finishCompletion(FutureTask.java:354)
//                    at java.util.concurrent.FutureTask.setException(FutureTask.java:223)
//                    at java.util.concurrent.FutureTask.run(FutureTask.java:242)
//                    at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1133)
//                    at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:607)
//                    at java.lang.Thread.run(Thread.java:761)
//                    Caused by: java.lang.RuntimeException: android.os.DeadSystemException
//                    at android.app.ActivityManager.getRunningAppProcesses(ActivityManager.java:3024)
//                    at com.qihoo.utils.AndroidUtilsCompat.getTopPkgNameByActivityManager(AppStore:320)
//                    at com.qihoo.utils.AndroidUtilsCompat.getTopPkgName(AppStore:298)
//                    at com.qihoo.appstore.utils.InjectActivityThreadH$StatRunnable.run(AppStore:75)
//                    at com.qihoo.utils.thread.ThreadUtils$1.doInBackground(AppStore:44)
//                    at com.qihoo.utils.thread.ThreadUtils$1.doInBackground(AppStore:38)
//                    at android.os.AsyncTask$2.call(AsyncTask.java:304)
//                    at java.util.concurrent.FutureTask.run(FutureTask.java:237)
//                    ... 3 more
//                    Caused by: android.os.DeadSystemException
//                    ... 11 more
                } else {
                    throw e;
                }
            }
        }
        return processInfos;
    }

    /**
     * 获取运行中的应用进程信息，代替{@link ActivityManager#getRunningAppProcesses()}方法，使用“读取/proc目录”的方式实现；
     * 解决Android5.0及以上版本，{@link ActivityManager#getRunningAppProcesses()}只能读取到自己的进程信息问题；
     *
     * @param context
     * @return
     */
    public static List<ActivityManager.RunningAppProcessInfo> getRunningAppProcessesCompat(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processInfos;
        if (!isCanReadThirdPartyProcDir()) {
            processInfos = getRunningAppProcessesHandleException(activityManager);
        } else {
            String strTopName = null;
            processInfos = new ArrayList<>();
            File[] files = (new File("/proc")).listFiles();
            Map<String, AndroidAppProcess> allProcess1 = new HashMap<>();
            int lowestOomScore = Integer.MAX_VALUE;
            for(int i = 0; i < files.length; i++) {
                File file = files[i];
                if(file.isDirectory()) {
                    int pid;
                    try {
                        pid = Integer.parseInt(file.getName());
                    } catch (NumberFormatException var12) {
                        continue;
                    }
                    try {
                        AndroidAppProcess e = new AndroidAppProcess(pid);
                        //排除多进程同名情况 只保留pid较小的那个进程
                        AndroidAppProcess tmpe = allProcess1.get(e.getPackageName());
                        if (tmpe != null) {
                            if (tmpe.pid > e.pid) {
                                allProcess1.remove(tmpe.getPackageName());
                            }
                        } else {
                            //这里添加判断条件，可以将要排除的进程放入一个列表内，下一版添加
                            allProcess1.put(e.getPackageName(), e);
                        }
                    } catch (AndroidAppProcess.NotAndroidAppProcessException var10) {
                    } catch (IOException var11) {
                    }
                }
            }
            for (Map.Entry<String, AndroidAppProcess> entry : allProcess1.entrySet()) {
                AndroidAppProcess e = entry.getValue();
                ActivityManager.RunningAppProcessInfo processInfo = new ActivityManager.RunningAppProcessInfo();
                try {
                    if (ApkUtils.isApkInstalled(context,e.getPackageName())) {
                        processInfo.uid = e.uid;
                        processInfo.pid = e.pid;
                        processInfo.pkgList = new String[1];
                        processInfo.processName = e.name;
                        processInfo.pkgList[0] = e.getPackageName();
                        if (e.oom_adj() == -2 || (e.oom_adj() == 0 && e.oom_score() > 4)) {
                            if (e.oom_score() < lowestOomScore) {
                                lowestOomScore = e.oom_score();
                                strTopName = e.getPackageName();
                            }
                        }
                        processInfos.add(processInfo);
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

            for (ActivityManager.RunningAppProcessInfo processInfo : processInfos) {
                String packageName = processInfo.pkgList[0];
                if (packageName != null && strTopName != null && strTopName.equals(packageName)) {
                    ReflectUtils.modifyPushPriority(processInfo,"flags",4); // android.app.ActivityManager.RunningAppProcessInfo.FLAG_HAS_ACTIVITIES
                }
            }
        }
        return processInfos;
    }

    /**
     * Android5.0及以上版本，通过读proc目录下进程id子目录文件来判断前台app，github:https://github.com/wenmingvs/AndroidProcess
     *
     * Android7.0及以上版本，/proc/目录下的进程id子目录，只能读取到自己的进程状态信息，不能读取别的应用的状态信息，等同于{@link ActivityManager#getRunningAppProcesses()}调用；
     *
     * @param ctx
     * @return
     */
    private static String getTopPkgNameByPid(Context ctx) {
        String foregroundName = null;
        if (ForegroundApp.lastForegroundAppName != null) {
            String pidFileName = Integer.toString(ForegroundApp.lastForegroundAppPid);
            File lastAppScoreFile = new File("/proc/" + pidFileName + "/oom_score");
            if (lastAppScoreFile.exists()) {
                try {
                    String scoreStr = ProcFile.readFile("/proc/" + pidFileName + "/oom_score");
                    int nowScore = ConvertUtils.string2Int(scoreStr);
                    String oom_score_adj = ProcFile.readFile("/proc/" + pidFileName + "/oom_score_adj");
                    int score_adj = ConvertUtils.string2Int(oom_score_adj);
                    if (nowScore == ForegroundApp.lastForegroundAppScore && score_adj == 0) {
                        foregroundName = ForegroundApp.lastForegroundAppName;
                    }
                } catch (IOException e) {
                    if (LogUtils.isDebug()) {
                        LogUtils.e(TAG, "getTopPkgNameByPid", e);
                    }
                }
            }
        }

        if (TextUtils.isEmpty(foregroundName)) {
            foregroundName = getForegroundAppbyProcFile(ctx);
        }
        if (LogUtils.isDebug()) {
            LogUtils.d("DownloadingNumLayout", "getTopPkgNameByPid() result: " + ForegroundApp.lastForegroundAppName);
        }
        return foregroundName;
    }

    // 部分手机（Vivo Xplay5A Adndroid5.1.1）判断顶层包名不准确；
    // 将"com.android.providers.downloads"进程排除掉,以后将需要排除的进程添加入set
    private static Set<String> sIgnoreProcessSet;
    private static void initIgnoreProcessSet() {
        if (sIgnoreProcessSet == null) {
            synchronized (AndroidUtilsCompat.class) {
                if (sIgnoreProcessSet == null) {
                    sIgnoreProcessSet = new HashSet<>();
                    sIgnoreProcessSet.add("com.android.providers.downloads");
                }
            }
        }
    }

    private static String getForegroundAppbyProcFile(Context ctx) {
        String foregroundName = null;
        File[] files = (new File("/proc")).listFiles();
        Map<String, AndroidAppProcess> allProcess1 = new HashMap<>();
        int lowestOomScore = Integer.MAX_VALUE;
        initIgnoreProcessSet();
        for(int i = 0; i < files.length; i++) {
            File file = files[i];
            if(file.isDirectory()) {
                int pid;
                try {
                    pid = Integer.parseInt(file.getName());
                } catch (NumberFormatException var12) {
                    continue;
                }
                try {
                    AndroidAppProcess e = new AndroidAppProcess(pid);
                    AndroidAppProcess temp = allProcess1.get(e.name);
                    if (temp != null) {
                        //排除多进程同名情况 只保留pid较小的那个进程
                        if (temp.pid > e.pid) {
                            allProcess1.remove(temp.getPackageName());
                        }
                    } else {
                        //去除包含":"的进程
                        if (!e.name.contains(":") && !sIgnoreProcessSet.contains(e.name)) {
                            allProcess1.put(e.getPackageName(), e);
                        }
                    }
                } catch (AndroidAppProcess.NotAndroidAppProcessException var10) {
                } catch (IOException var11) {
                }
            }
        }
        for (Map.Entry<String, AndroidAppProcess> entry : allProcess1.entrySet()) {
            AndroidAppProcess androidAppProcess = entry.getValue();
            try {
                if (androidAppProcess.foreground && (androidAppProcess.uid < 1000 || androidAppProcess.uid > 9999)  && ApkUtils.isApkInstalled(ctx, androidAppProcess.getPackageName())) {
                    //说明oom_adj不可读 根据oom_score判断
                    if (androidAppProcess.oom_adj() == -2 || (androidAppProcess.oom_adj() == 0 && androidAppProcess.oom_score() != -1000 && androidAppProcess.oom_score() > 4 && androidAppProcess.oom_score_adj() == 0)) {
                        if (androidAppProcess.oom_score() < lowestOomScore) {
                            lowestOomScore = androidAppProcess.oom_score();
                            foregroundName = androidAppProcess.getPackageName();
                            ForegroundApp.lastForegroundAppName = foregroundName;
                            ForegroundApp.lastForegroundAppScore = androidAppProcess.oom_score();
                            ForegroundApp.lastForegroundAppPid = androidAppProcess.pid;
                        }
                    }
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        return foregroundName;
    }

    /**
     * 判断助手自己是否在前台
     *
     * 使用activityManager.getRunningAppProcesses和activityManager.getRunningTasks是可以读取进程状态信息的，
     * 读取助手自己，不用使用低效的读取proc进程id子目录的方式；
     *
     * @param context
     * @return
     */
    public static boolean isAppStoreOnForeground(Context context) {
        boolean isForeground = context != null ? context.getPackageName().equals(getTopPkgNameByActivityManager(context)) : false;
        if (LogUtils.isDebug()) {
            LogUtils.d(TAG, "isAppStoreOnForeground.isForeground = " + isForeground);
        }
        return isForeground;
    }

    /**
     * 指定packageName的应用是否在前台运行
     *
     * @param context
     * @param packageName
     * @return
     */
    public static boolean isAppOnForeground(Context context, String packageName) {
        boolean isForeground = false;
        if (!TextUtils.isEmpty(packageName)) {
            if (packageName.equals(context.getPackageName())) {
                isForeground = isAppStoreOnForeground(context);
            } else {
                isForeground = packageName.equals(getTopPkgName(context));
            }
        }
        if (LogUtils.isDebug()) {
            LogUtils.d(TAG, "isAppOnForeground.isForeground = " + isForeground);
        }
        return isForeground;
    }

    /**
     * Android5.0开始，activityManager.getRunningTasks只能读取自己的，不能读取别的应用的状态信息；
     *
     * @param context
     * @return
     */
    public static String getTopActivity(Context context) {
        try {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> tasks = activityManager.getRunningTasks(1);
            ActivityManager.RunningTaskInfo task = tasks.get(0);
            return task.baseActivity.getClassName();
        } catch (Exception e) {
            if (LogUtils.isDebug()) {
                LogUtils.e(TAG, "getTopActivity", e);
            }
        }
        return null;
    }

    /**
     * 判断某个服务是否正在运行的方法
     *
     * @param context
     * @param serviceClassName
     * @return
     */
    public static boolean isServiceRunning(Context context, Class<? extends Service> serviceClassName) {
        return isServiceRunning(context, serviceClassName.getName());
    }

    /**
     * 判断某个服务是否正在运行的方法
     *
     * @param context
     * @param serviceClassName 包名+类名
     * @return
     */
    public static boolean isServiceRunning(Context context, String serviceClassName) {
        try {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningServiceInfo> infos = activityManager.getRunningServices(100);
            if (infos != null) {
                for (ActivityManager.RunningServiceInfo info : infos) {
                    if (info.service.getClassName().toString().equals(serviceClassName)) {
                        return true;
                    }
                }
            }
        } catch (Throwable e) {
            if (LogUtils.isDebug()) {
                LogUtils.e(TAG, "isServiceRunning", e);
            }
        }
        return false;
    }

    /**
     * 使用反射设置FragmentManagerImpl类中mStateSaved值为false；
     *
     * @param activity android.app.Activity或者android.support.v4.app.FragmentActivity
     */
    private static void fixedActionAfterOnSaveInstanceState(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            Object mFragments = ReflectUtils.getFieldValue(activity, "mFragments");
            ReflectUtils.invokeMethod(mFragments, "noteStateNotSaved", null);
        }
    }

    /**
     * 使用继承基类的方式解决“Can not perform this action after onSaveInstanceState”崩溃；
     * http://blog.csdn.net/edisonchang/article/details/49873669
     * http://stackoverflow.com/questions/7469082/getting-exception-illegalstateexception-can-not-perform-this-action-after-onsa
     *
     * 崩溃原因：activity在某种场景下处于被kill 掉的边缘，系统就调用了onSaveInstanceState 方法，
     * 这个方法里面会调用 FragmentManager saveAllState 方法，将fragment 的状态保存，
     * 在状态保存后用户又主动调了 onBackPressed ，而这个方法的超类super.onBackPressed 方法会判断FragmentManager 是否保存了状态，
     * 如果已经保存就会抛出IllegalStateException 的异常
     */
    public static class FixedActionAfterOnSaveInstanceStateFragmentActivity extends FragmentActivity {
        @Override
        public void onBackPressed() {
            fixedActionAfterOnSaveInstanceState(this);
            try {
                super.onBackPressed();
            } catch (IllegalStateException e) { // 部分手机用反射调用后，还有少量崩溃，这里用trycatch的方式解决
//                java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
//                at android.support.v4.app.FragmentManagerImpl.checkStateLoss(AppStore:1377)
//                at android.support.v4.app.FragmentManagerImpl.popBackStackImmediate(AppStore:504)
//                at android.support.v4.app.FragmentActivity.onBackPressed(AppStore:178)
//                at com.qihoo.appstore.home.MainActivity.access$1201(AppStore:161)
//                at com.qihoo.appstore.home.MainActivity.quit(AppStore:1153)
//                at com.qihoo.appstore.home.MainActivity.checkShowInstallDialogWhenQuit(AppStore:1161)
//                at com.qihoo.appstore.home.MainActivity.onBackPressed(AppStore:1123)
//                at android.app.Activity.onKeyUp(Activity.java:2165)
//                at android.view.KeyEvent.dispatch(KeyEvent.java:2739)
//                at android.app.Activity.dispatchKeyEvent(Activity.java:2400)
//                at com.android.internal.policy.impl.PhoneWindow$DecorView.dispatchKeyEvent(PhoneWindow.java:2071)
//                at android.view.ViewRootImpl.deliverKeyEventPostIme(ViewRootImpl.java:3834)
//                at android.view.ViewRootImpl.deliverKeyEvent(ViewRootImpl.java:3769)
//                at android.view.ViewRootImpl.deliverInputEvent(ViewRootImpl.java:3327)
//                at android.view.ViewRootImpl.doProcessInputEvents(ViewRootImpl.java:4422)
//                at android.view.ViewRootImpl.enqueueInputEvent(ViewRootImpl.java:4401)
//                at android.view.ViewRootImpl$WindowInputEventReceiver.onInputEvent(ViewRootImpl.java:4493)
//                at android.view.InputEventReceiver.dispatchInputEvent(InputEventReceiver.java:171)
//                at android.os.MessageQueue.nativePollOnce(Native Method)
//                at android.os.MessageQueue.next(MessageQueue.java:125)
//                at android.os.Looper.loop(Looper.java:124)
//                at android.app.ActivityThread.main(ActivityThread.java:4867)
//                at java.lang.reflect.Method.invokeNative(Native Method)
//                at java.lang.reflect.Method.invoke(Method.java:511)
//                at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:1007)
//                at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:774)
//                at dalvik.system.NativeStart.main(Native Method)
                finish();
            }
        }
    }

    /**
     * 使用继承基类的方式解决“Can not perform this action after onSaveInstanceState”崩溃；
     *
     * @see FixedActionAfterOnSaveInstanceStateFragmentActivity
     */
    public static class FixedActionAfterOnSaveInstanceStateActivity extends Activity {
        @Override
        public void onBackPressed() {
            fixedActionAfterOnSaveInstanceState(this);
            try {
                super.onBackPressed();
            } catch (IllegalStateException e) {
//                java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
//                at android.app.FragmentManagerImpl.checkStateLoss(FragmentManager.java:1318)
//                at android.app.FragmentManagerImpl.popBackStackImmediate(FragmentManager.java:488)
//                at android.app.Activity.onBackPressed(Activity.java:2267)
//                at android.app.Activity.onKeyUp(Activity.java:2245)
//                at android.view.KeyEvent.dispatch(KeyEvent.java:2633)
//                at android.app.Activity.dispatchKeyEvent(Activity.java:2475)
//                at com.android.internal.policy.impl.PhoneWindow$DecorView.dispatchKeyEvent(PhoneWindow.java:1952)
//                at android.view.ViewRootImpl.deliverKeyEventPostIme(ViewRootImpl.java:3794)
//                at android.view.ViewRootImpl.deliverKeyEvent(ViewRootImpl.java:3716)
//                at android.view.ViewRootImpl.deliverInputEvent(ViewRootImpl.java:3248)
//                at android.view.ViewRootImpl.doProcessInputEvents(ViewRootImpl.java:4385)
//                at android.view.ViewRootImpl.enqueueInputEvent(ViewRootImpl.java:4364)
//                at android.view.ViewRootImpl$WindowInputEventReceiver.onInputEvent(ViewRootImpl.java:4456)
//                at android.view.InputEventReceiver.dispatchInputEvent(InputEventReceiver.java:179)
//                at android.os.MessageQueue.nativePollOnce(Native Method)
//                at android.os.MessageQueue.next(MessageQueue.java:125)
//                at android.os.Looper.loop(Looper.java:124)
//                at android.app.ActivityThread.main(ActivityThread.java:5071)
//                at java.lang.reflect.Method.invokeNative(Native Method)
//                at java.lang.reflect.Method.invoke(Method.java:511)
//                at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:812)
//                at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:579)
//                at dalvik.system.NativeStart.main(Native Method)
                finish();
            }
        }
    }

    /**
     * 通过反射销毁所有Activity；
     *
     * 使用场景：Avtivity所在进程崩溃时，5.0以上系统会显示当前Activity的前一个Activity并且卡住点击界面没反应；
     */
    public static void finishAllActivity() {
        finishAllActivity(null);
    }

    /**
     * 通过反射获取系统Activity栈中的所有存在的Activity；
     * 注意：需要在UI线程调用，否则会有数据同步问题；
     *
     * @return
     */
    public static List<Activity> getAllActivity() {
        List<Activity> activities = new ArrayList<>();
        try {
            Class activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
            activitiesField.setAccessible(true);
            Map activityMap = (Map) activitiesField.get(activityThread);
            if (activityMap != null) {
                for (Object activityRecord : activityMap.values()) {
                    Class activityRecordClass = activityRecord.getClass();
                    Field activityField = activityRecordClass.getDeclaredField("activity");
                    activityField.setAccessible(true);
                    Activity activity = (Activity) activityField.get(activityRecord);
                    activities.add(activity);
                }
            }
        } catch (Throwable e) {
        }
        return activities;
    }

    public static void finishAllActivity(Set<Activity> excludeActivity){
        try {
            List<Activity> activities = getAllActivity();
            for (Activity activity : activities) {
                if (excludeActivity != null && excludeActivity.contains(activity))
                    continue;

                activity.moveTaskToBack(true);
                activity.finish();
            }
        } catch (Throwable e) {
        }
    }

    public static boolean isActivitiesEmpty() {
        try {
            List<Activity> activities = getAllActivity();
            return activities == null || activities.isEmpty();
        } catch (Throwable e) {
        }
        return false;
    }

    /**
     * 提高AlarmManager执行的准确性
     * 参考：https://developer.android.com/training/monitoring-device-state/doze-standby.html
     *
     * @param context
     * @param alarmType
     * @param triggerAtMillis
     * @param pendingIntent
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static void alarmManagerSet(Context context, int alarmType, long triggerAtMillis, PendingIntent pendingIntent) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT < AndroidVersionCodes.KITKAT) {
            alarmManager.set(alarmType, triggerAtMillis, pendingIntent);
        } else if (Build.VERSION.SDK_INT < AndroidVersionCodes.M) {
            try {
                alarmManager.setExact(alarmType, triggerAtMillis, pendingIntent);
            } catch (NoSuchMethodError e) { // AOSP on Flo型号手机sdk版本号判断有问题，添加保护
                alarmManager.set(alarmType, triggerAtMillis, pendingIntent);
            }
        } else {
            try { // Android 6.0开始有Doze模式（低电耗模式），如果需要设置在该模式下触发的闹铃，需要使用 setAndAllowWhileIdle() 或 setExactAndAllowWhileIdle()；
                ReflectUtils.invokeMethod(
                        alarmManager,
                        "setExactAndAllowWhileIdle",
                        new Class[]{int.class, long.class, PendingIntent.class},
                        alarmType, triggerAtMillis, pendingIntent);
            } catch (Throwable e) {
                alarmManager.setExact(alarmType, triggerAtMillis, pendingIntent);
            }
        }
    }

    /**
     * 强制清理{@link Resources}类中缓存的资源引用，从而可以释放内存中Bitmap、byte[]等类型的资源;
     * 一般适用于“按两次返回键”退出应用主界面时调用；
     * 效果：Android6.0.1能降低70%内存占用；
     * @param context
     */
    public static void forceClearResources(Context context) {
        try {
            Resources resources = context.getResources();
            Configuration configuration = resources.getConfiguration();
            int keyboardHidden = configuration.keyboardHidden;
            configuration.keyboardHidden = Configuration.KEYBOARD_UNDEFINED;
            resources.updateConfiguration(configuration, resources.getDisplayMetrics()); // Configuration.needNewResources(configChanges, interestingChanges)

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) { // Android2.3.7
                Object sPreloadedDrawables = ReflectUtils.getFieldValue(resources, "sPreloadedDrawables"); // LongSparseArray<Drawable.ConstantState>
                ReflectUtils.invokeMethod(sPreloadedDrawables, "clear", null);
            } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) { // Android4.0.4
                Object sPreloadedDrawables = ReflectUtils.getFieldValue(resources, "sPreloadedDrawables"); // LongSparseArray<Drawable.ConstantState>
                ReflectUtils.invokeMethod(sPreloadedDrawables, "clear", null);
                Object sPreloadedColorDrawables = ReflectUtils.getFieldValue(resources, "sPreloadedColorDrawables"); // LongSparseArray<Drawable.ConstantState>
                ReflectUtils.invokeMethod(sPreloadedColorDrawables, "clear", null);
            } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR1) { // Android4.2.2
                Object sPreloadedDrawables = ReflectUtils.getFieldValue(resources, "sPreloadedDrawables"); // LongSparseArray<Drawable.ConstantState>
                ReflectUtils.invokeMethod(sPreloadedDrawables, "clear", null);
                Object sPreloadedColorDrawables = ReflectUtils.getFieldValue(resources, "sPreloadedColorDrawables"); // LongSparseArray<Drawable.ConstantState>
                ReflectUtils.invokeMethod(sPreloadedColorDrawables, "clear", null);
                Object sPreloadedColorStateLists = ReflectUtils.getFieldValue(resources, "sPreloadedColorStateLists"); // LongSparseArray<ColorStateList>
                ReflectUtils.invokeMethod(sPreloadedColorStateLists, "clear", null);
            } else if (Build.VERSION.SDK_INT <= AndroidVersionCodes.M) { // Android6.0.1
                Object[] sPreloadedDrawables = (Object[]) ReflectUtils.getFieldValue(resources, "sPreloadedDrawables"); // LongSparseArray<ConstantState>[]
                for (Object sPreloadedDrawable : sPreloadedDrawables) {
                    ReflectUtils.invokeMethod(sPreloadedDrawable, "clear", null);
                }
                Object sPreloadedColorDrawables = ReflectUtils.getFieldValue(resources, "sPreloadedColorDrawables"); // LongSparseArray<ConstantState>
                ReflectUtils.invokeMethod(sPreloadedColorDrawables, "clear", null);
                Object sPreloadedColorStateLists = ReflectUtils.getFieldValue(resources, "sPreloadedColorStateLists"); // LongSparseArray<ColorStateList>、LongSparseArray<android.content.res.ConstantState<ColorStateList>>
                ReflectUtils.invokeMethod(sPreloadedColorStateLists, "clear", null);
            } else if (Build.VERSION.SDK_INT <= AndroidVersionCodes.N) { // Android7.0.0
                Object mResourcesImpl = ReflectUtils.invokeMethod(resources, "getImpl", null);
                Object[] sPreloadedDrawables = (Object[]) ReflectUtils.getFieldValue(mResourcesImpl, "sPreloadedDrawables"); // LongSparseArray<Drawable.ConstantState>[]
                for (Object sPreloadedDrawable : sPreloadedDrawables) {
                    ReflectUtils.invokeMethod(sPreloadedDrawable, "clear", null);
                }
                Object sPreloadedColorDrawables = ReflectUtils.getFieldValue(mResourcesImpl, "sPreloadedColorDrawables"); // LongSparseArray<Drawable.ConstantState>
                ReflectUtils.invokeMethod(sPreloadedColorDrawables, "clear", null);
                Object sPreloadedColorStateLists = ReflectUtils.getFieldValue(mResourcesImpl, "sPreloadedComplexColors"); // LongSparseArray<android.content.res.ConstantState<ComplexColor>>
                ReflectUtils.invokeMethod(sPreloadedColorStateLists, "clear", null);
            }
            resources.flushLayoutCache();
        } catch (Throwable e) {
            if (LogUtils.isDebug()) {
                LogUtils.e(TAG, "forceClearResources", e);
            }
        }
    }

    public static Intent getLaunchIntentForPackageCompat(Context context, String pkgName) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(pkgName);
        if(intent != null)
            return intent;

        intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setPackage(pkgName);
        List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent, 0);
        if (list != null) {
            for (ResolveInfo info : list) {
                ActivityInfo activityInfo = info.activityInfo;
                if (activityInfo != null && pkgName.equals(activityInfo.packageName)) {
                    intent.setComponent(new ComponentName(pkgName, activityInfo.name));
                    return intent;
                }
            }
        }

        intent.removeCategory(Intent.CATEGORY_LAUNCHER);
        list = context.getPackageManager().queryIntentActivities(intent, 0);
        if (list != null) {
            for (ResolveInfo info : list) {
                ActivityInfo activityInfo = info.activityInfo;
                if (activityInfo != null && pkgName.equals(activityInfo.packageName)) {
                    intent.setComponent(new ComponentName(pkgName, activityInfo.name));
                    return intent;
                }
            }
        }

        return intent;
    }

    /**
     * Android4.0.4，解决：设置了android:ellipsize="end"，但“...”不显示的问题；
     * Android4.4，解决：文本两边不能贴边的问题；
     *
     * @param textView
     */
    public static void fixedTextViewEllipsizeEnd(TextView textView) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            textView.setHorizontallyScrolling(true);
        }
    }

    /**
     * 解决：同一个进程内，2个Activity界面都引用桌面壁纸背景图，如果背景图被误recycled会导致异常：java.lang.RuntimeException: Canvas: trying to use a recycled bitmap；
     */
    public static Drawable getWallpaperDrawableCompat(Context context){
        Drawable drawable = null;
        try {
            drawable = getUsableWallpaperDrawable(context);
            if (drawable == null) {
                WallpaperManager.getInstance(context).forgetLoadedWallpaper(); // 清除bitmap.isRecycled的图片内存缓存；
                drawable = getUsableWallpaperDrawable(context);
                if (drawable != null && drawable instanceof BitmapDrawable) {
                    drawable = new BitmapDrawable(BitmapUtils.copyBitmap(((BitmapDrawable) drawable).getBitmap()));
                }
            }
        } catch (Throwable e) {
            if (LogUtils.isDebug()) {
                LogUtils.e(TAG, "getWallpaperDrawableCompat", e);
            }
        }
        return drawable;
    }

    private static Drawable getUsableWallpaperDrawable(Context context){
        Drawable drawable = WallpaperManager.getInstance(context).getDrawable();
        if (drawable != null && drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            if (bitmap != null && !bitmap.isRecycled()) {
                return drawable;
            }
        } else {
            return drawable;
        }
        return null;
    }
}