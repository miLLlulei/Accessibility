package com.mill.accessibility.utils;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author jinmeng
 *         对于android.permission.PACKAGE_USAGE_STATS权限的处理
 */
public class PackageUsageHelper {

    public static boolean usagePermissionCheck(Context context) {
        boolean granted = false;
        if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
                int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.getPackageName());
                if (mode == AppOpsManager.MODE_DEFAULT) {
                    String permissionUsage = "android.permission.PACKAGE_USAGE_STATS";
                    granted = (context.checkCallingOrSelfPermission(permissionUsage) == PackageManager.PERMISSION_GRANTED);
                } else {
                    granted = (mode == AppOpsManager.MODE_ALLOWED);
                }
            } catch (Throwable e) {
            }
        }
        return granted;
    }

    public static final String EXTRA_SHOW_FRAGMENT = ":android:show_fragment";

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static Intent getIntent(Context context) {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        PackageManager packageManager = context.getPackageManager();
        if (packageManager.resolveActivity(intent, 0) != null) {
            return intent;
        } else {
            intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
//            ResolveInfo info = packageManager.resolveActivity(intent, 0);
//            if (info != null) {
////                intent = new Intent();
//                intent.setClassName(info.activityInfo.packageName, info.activityInfo.name);
//            }
//            intent.setClassName("com.android.settings", "com.android.settings.Settings");
//            intent.putExtra(EXTRA_SHOW_FRAGMENT, "com.android.settings.UsageAccessSettings");
            return intent;
        }
    }

    public static void toOpenPkgUsagePermission(Context context) {
        if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Intent intent = getIntent(context);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static String getTopPackageName(Context context, long beginTime) {
        String topPackageName = null;
        if (usagePermissionCheck(context)) {
            UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            UsageEvents events = null; // 优先使用queryEvents，因为其时间是queryUsageStats的一半，同时beginTime越小耗费的时间越久；
            try {
                events = usageStatsManager.queryEvents(beginTime, System.currentTimeMillis());
            } catch (Throwable e) {
//                LeEco Le X620 6.0(23)、Letv Letv X500 6.0(23)、Coolpad C106-9 6.0.1(23)、Letv X900 6.0.1(23)
//                java.lang.IllegalStateException: String '7.1.5' is not in the string pool
//                at android.os.Parcel.readException(Parcel.java:1607)
//                at android.os.Parcel.readException(Parcel.java:1552)
//                at android.app.usage.IUsageStatsManager$Stub$Proxy.queryEvents(IUsageStatsManager.java:233)
//                at android.app.usage.UsageStatsManager.queryEvents(UsageStatsManager.java:245)
//                at com.qihoo.utils.PackageUsageHelper.getTopPackageName(AppStore:79)
//                at com.qihoo.utils.AndroidUtilsCompat.getTopPkgName(AppStore:295)
//                at com.qihoo.utils.AndroidUtilsCompat.getTopPkgName(AppStore:291)
//                at com.qihoo360.mobilesafe.util.OSUtils.isLauncherTop(AppStore:620)
//                at com.qihoo360.mobilesafe.util.OSUtils.isLauncherTopApp(AppStore:628)
//                at com.qihoo.receiver.charge.ChargeScreenBrocastReceiver$1.run(AppStore:107)

//                LeEco Le X620 6.0(23)、Coolpad C106-9 6.0.1(23)、Letv X900 6.0.1(23)、Letv Letv X500 6.0(23)
//                java.lang.NullPointerException: rhs == null
//                at android.os.Parcel.readException(Parcel.java:1626)
//                at android.os.Parcel.readException(Parcel.java:1573)
//                at android.app.usage.IUsageStatsManager$Stub$Proxy.queryEvents(IUsageStatsManager.java:233)
//                at android.app.usage.UsageStatsManager.queryEvents(UsageStatsManager.java:245)
//                at com.qihoo.utils.PackageUsageHelper.getTopPackageName(AppStore:81)
//                at com.qihoo.utils.AndroidUtilsCompat.getTopPkgName(AppStore:295)
//                at com.qihoo.utils.AndroidUtilsCompat.getTopPkgName(AppStore:291)
//                at com.qihoo.utils.LauncherHelper.checkLauncherTop(AppStore:41)
//                at com.qihoo.utils.LauncherHelper.isLauncherTop(AppStore:30)
//                at com.qihoo.receiver.charge.ChargeScreenBrocastReceiver$1.run(AppStore:107)
                if (LogUtils.isDebug()) {
                    LogUtils.e(PackageUsageHelper.class.getName(), "getTopPackageName", e);
                }
            }
            if (events != null) {
                SortedMap<Long, UsageEvents.Event> sortedMap = new TreeMap<>();
                UsageEvents.Event event = new UsageEvents.Event();
                while (events.hasNextEvent()) {
                    if (events.getNextEvent(event) && event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                        sortedMap.put(event.getTimeStamp(), event);
                    }
                }
                if (!sortedMap.isEmpty()) {
                    topPackageName = sortedMap.get(sortedMap.lastKey()).getPackageName();
                }
                sortedMap.clear();
            } else { // queryEvents异常后用queryUsageStats再获取一次
                List<UsageStats> stats = null;
                try {
                    stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginTime, System.currentTimeMillis());
                } catch (Throwable e) {
                    if (LogUtils.isDebug()) {
                        LogUtils.e(PackageUsageHelper.class.getName(), "getTopPackageName", e);
                    }
                }
                if (stats != null) {
                    SortedMap<Long, UsageStats> sortedMap = new TreeMap<>();
                    for (UsageStats usageStats : stats) {
                        sortedMap.put(usageStats.getLastTimeUsed(), usageStats);
                    }
                    if (!sortedMap.isEmpty()) {
                        topPackageName = sortedMap.get(sortedMap.lastKey()).getPackageName();
                    }
                    sortedMap.clear();
                }
            }
        }
        return topPackageName;
    }

    /**
     * 获取系统中所有应用的使用数据,不一定能读全
     * @param context
     * @return
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static List<UsageStats> getPackageUsageStats(Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            long endt = calendar.getTimeInMillis();//结束时间
            calendar.add(Calendar.DAY_OF_YEAR, -2);//时间间隔为两年
            long statt = calendar.getTimeInMillis();//开始时间
            UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            List<UsageStats> queryUsageStats = null;
            try {
                queryUsageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_YEARLY, statt, endt);
            } catch (Throwable e) {
//                360 1509-A00 6.0.1(23)、QiKU 8676-A01 5.1(22)、360 1505-A02 6.0.1(23)
//                java.lang.NullPointerException: Attempt to invoke interface method 'boolean android.os.IBinder.transact(int, android.os.Parcel, android.os.Parcel, int)' on a null object reference
//                at android.content.pm.ParceledListSlice.(ParceledListSlice.java:97)
//                at android.content.pm.ParceledListSlice.(ParceledListSlice.java:41)
//                at android.content.pm.ParceledListSlice$2.createFromParcel(ParceledListSlice.java:200)
//                at android.content.pm.ParceledListSlice$2.createFromParcel(ParceledListSlice.java:198)
//                at android.app.usage.IUsageStatsManager$Stub$Proxy.queryUsageStats(IUsageStatsManager.java:184)
//                at android.app.usage.UsageStatsManager.queryUsageStats(UsageStatsManager.java:133)
//                at com.qihoo.utils.PackageUsageHelper.getPackageUsageStats(AppStore:161)
//                at com.qihoo.utils.PackageUsageHelper.getPackageUsageTime(AppStore:188)
//                at com.qihoo.express.mini.support.ApkWatcherData.getApkShowTimes(AppStore:87)
//                at com.qihoo.appstore.localapkinfo.LocalApkInfoDb.initLocalApkSimpleInfo(AppStore:268)
//                at com.qihoo.appstore.localapkinfo.LocalApkInfoDb.LoadAllSimpleApkInfo(AppStore:314)
//                at com.qihoo.appstore.localapkinfo.LocalApkInfoDb.loadAllApkInfo(AppStore:100)
//                at com.qihoo.appstore.localapkinfo.LocalApkMgr$5.run(AppStore:442)
//                at com.qihoo.utils.thread.LooperThread.run(AppStore:38)
                if (LogUtils.isDebug()) {
                    LogUtils.e(PackageUsageHelper.class.getName(), "getPackageUsageStats", e);
                }
            }
            return queryUsageStats;
        }else{
            return null;
        }
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static long getPackageStartTime(Context context,String packageName){
        long cTime = System.currentTimeMillis();
        List<UsageStats> result = getPackageUsageStats(context);
        if (result != null) {
            for (UsageStats stat : result) {
                if (packageName.equalsIgnoreCase(stat.getPackageName())) {
                    if (cTime - stat.getLastTimeUsed() < 5) {
                        return stat.getLastTimeUsed();
                    }
                }
            }
        }
        return 0;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static long getPackageUsageTime(Context context,String packageName){
        long usageTime = 0;
        List<UsageStats> result = getPackageUsageStats(context);
        if (result != null) {
            for (UsageStats stat : result) {
                if (packageName.equalsIgnoreCase(stat.getPackageName())) {
                    try {
                       usageTime = (int)ReflectUtils.getField(stat,"mLaunchCount");
                    } catch (NoSuchFieldException e) {
                        usageTime = -1;
                    } catch (IllegalAccessException e) {
                        usageTime = -1;
                    }
                }
            }
        }
        return usageTime;
    }

}
