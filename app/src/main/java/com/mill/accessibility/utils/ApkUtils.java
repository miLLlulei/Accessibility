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

}
