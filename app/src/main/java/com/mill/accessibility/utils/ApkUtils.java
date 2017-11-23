package com.mill.accessibility.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.text.TextUtils;

import java.io.File;

public class ApkUtils {

    private final static String TAG = "ApkUtils";


    public static PackageInfo getInstalledApp(Context context, String packageName) {
        return getInstalledApp(context, packageName, 0);
    }

    public static PackageInfo getInstalledApp(Context context, String packageName, int flags) {
        PackageInfo info = null;

        PackageManager pm = context.getPackageManager();
        try {
            if (pm != null) {
                info = pm.getPackageInfo(packageName, flags);
            }
        } catch (NameNotFoundException e) {
//            e.printStackTrace();
        }

        return info;
    }


    public static boolean install(Context context, String apkFilePath) {
        if (TextUtils.isEmpty(apkFilePath)) {
            return false;
        }
        File targetFile = new File(apkFilePath);
        if (targetFile.getAbsolutePath().startsWith(
                PathUtils.getCacheDir(context))) {
            FileUtils.changeFileMode(targetFile, "755");
        }

        try {
            Intent installIntent = new Intent("android.intent.action.VIEW");
            Uri localUri = Uri.parse("file://" + apkFilePath);
            installIntent.setDataAndType(localUri, "application/vnd.android.package-archive");
            //三星N9150 调系统安装会出现闪屏页 加个FLAG_ACTIVITY_NO_ANIMATION标记
            if (context instanceof Activity && !apkFilePath.contains(context.getPackageName())) {
                installIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            } else {
                installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            context.startActivity(installIntent);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }



    public static void uninstall(Context mContext, String packageName) {
        Intent intent;
        Uri uri;
        try {
            uri = Uri.fromParts("package", packageName, null);
            intent = new Intent(Intent.ACTION_DELETE, uri);

            if (mContext instanceof Activity) {
                ((Activity) mContext).startActivity(intent);
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    /**
     * 使用 LocalApkMgr.getInstance().isApkInstalled 代替
     */
    @Deprecated
    public static boolean isApkInstalled(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        if (packageName.equals(context.getPackageName())) {
            return true;
        }
        boolean result = false;

        try {
            result = context.getPackageManager().getPackageInfo(packageName,
                    PackageManager.GET_DISABLED_COMPONENTS) != null;
        } catch (NameNotFoundException | RuntimeException e) {
//            java.lang.RuntimeException: Package manager has died
//            at android.app.ApplicationPackageManager.getPackageInfo(ApplicationPackageManager.java:77)
//            at com.qihoo.utils.ApkUtils.isApkInstalled(AppStore:191)
//            at com.qihoo.appstore.base.AppStoreApplication$13.run(AppStore:576)
//            at android.os.Handler.handleCallback(Handler.java:725)
//            at android.os.Handler.dispatchMessage(Handler.java:92)
//            at android.os.Looper.loop(Looper.java:137)
//            at android.os.HandlerThread.run(HandlerThread.java:60)
//            Caused by: android.os.TransactionTooLargeException
//            at android.os.BinderProxy.transact(Native Method)
//            at android.content.pm.IPackageManager$Stub$Proxy.getPackageInfo(IPackageManager.java:1362)
//            at java.lang.reflect.Method.invokeNative(Native Method)
//            at java.lang.reflect.Method.invoke(Method.java:511)
//            at com.morgoo.droidplugin.hook.HookedMethodHandler.doHookInner(AppStore:51)
//            at com.morgoo.droidplugin.hook.proxy.ProxyHook.invoke(AppStore:60)
//            at $Proxy8.getPackageInfo(Native Method)
//            at android.app.ApplicationPackageManager.getPackageInfo(ApplicationPackageManager.java:72)
        }

        return result;
    }

}
