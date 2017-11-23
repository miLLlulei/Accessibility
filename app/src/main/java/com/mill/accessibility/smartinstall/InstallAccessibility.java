package com.mill.accessibility.smartinstall;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.text.TextUtils;

import com.mill.accessibility.accessibility.AccessibilityClearTaskActivity;
import com.mill.accessibility.accessibility.MyAccessibility;
import com.mill.accessibility.accessibility.BaseAccessibility;
import com.mill.accessibility.utils.ContextUtils;
import com.mill.accessibility.utils.ReflectUtils;
import com.mill.accessibility.utils.SPUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 */
public class InstallAccessibility extends BaseAccessibility {
    private static List<String> blackListModle = null;
    static {
        loadData();
        registerReceiver();
    }

    private static synchronized void loadData() {
        String content = null;
        logPrint("loadData=" + content);
        if (!TextUtils.isEmpty(content)) {
            try {
                blackListModle = Collections.synchronizedList(new ArrayList<String>());
                JSONArray array = new JSONArray(content);
                int size = array.length();
                for (int i = 0; i < size; i++) {
                    blackListModle.add(array.optString(i));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private static void registerReceiver() {
        PackageReBroadcastReceiver reeceive = new PackageReBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addDataScheme("package");
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        Context context = ContextUtils.getApplicationContext();
        if (context != null) {
            context.registerReceiver(reeceive, filter);
        }
    }

    public static synchronized void updateData(String content) {
        if (!TextUtils.isEmpty(content)) {
            logPrint("updateData=" + content);
            if (!TextUtils.isEmpty(content)) {
                try {
                    if (blackListModle != null) {
                        blackListModle.clear();
                    } else {
                        blackListModle = Collections.synchronizedList(new ArrayList<String>());
                    }
                    JSONArray array = new JSONArray(content);
                    int size = array.length();
                    for (int i = 0; i < size; i++) {
                        blackListModle.add(array.optString(i));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static boolean modleIsInBlackList() {
        String model = Build.MODEL;
        logPrint("model=" + model);
        if (blackListModle != null) {
            for (String item : blackListModle) {
                if(TextUtils.isEmpty(item)){
                    continue;
                }
                if (item.equalsIgnoreCase(model)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean autoInstallEnableAndSupport() {
        return autoInstallServerEnable() && isSupportAccessibilityService();
    }

    public static void openSettingAccessibilityNeedTips(Context context){
        MyAccessibility.needClearTask = true;
        openSettingAccessibilityNeedTips(context, null);

//        BaseAccessibility.setSmallApkCallback(new SmallApkCallbackImp());
    }

    public static void openSettingAccessibilityNeedTips(Context context, String tipContent){
        Intent intent = new Intent(context, AccessibilityClearTaskActivity.class);
        if(!(context instanceof Activity)){
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        if (!TextUtils.isEmpty(tipContent)) {
            intent.putExtra("tipContent", tipContent);
        }
        context.startActivity(intent);
//        BackgroundStartActivity.startActivity(context,intent);
    }

    public static boolean isSupportAccessibilityService() {
        boolean open = true;
        boolean modleIsInBlackList = modleIsInBlackList();
        logPrint("isSupportAccessibilityService open=" + open + "   modleIsInBlackList=" + modleIsInBlackList);
        return sdkIsSupport() && open && !modleIsInBlackList && isHaveAccessibilitySettings;
    }

    //判断是否在系统安装界面
    @SuppressWarnings("deprecation")
    public static boolean isInSysInstallPage() {
        String strTopPName = null;
        Context context = ContextUtils.getApplicationContext();
        if (context == null) {
            return false;
        }
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        if (Build.VERSION.SDK_INT > 20) {
            final List<ActivityManager.RunningAppProcessInfo> processInfos = am.getRunningAppProcesses();
            if (processInfos != null && processInfos.size() > 0) {
                for (ActivityManager.RunningAppProcessInfo processInfo : processInfos) {
                    if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        if (ReflectUtils.getIntField(processInfo, "flags") == 4 && processInfo.pkgList.length == 1) {
                            strTopPName = processInfo.pkgList[0];
                            break;
                        }
                    }
                }
            }
        } else {
            //noinspection deprecation
            List<ActivityManager.RunningTaskInfo> runningTaskInfos = am.getRunningTasks(1);
            if (runningTaskInfos != null && !runningTaskInfos.isEmpty()) {
                ComponentName componentName = runningTaskInfos.get(0).topActivity;
                if (componentName != null && componentName.getPackageName() != null) {
                    strTopPName = componentName.getPackageName();
                }
            }
        }
        logPrint("isInSysInstallPage : strTopPName=" + strTopPName);
        return MyAccessibility.packages[0].equalsIgnoreCase(strTopPName);
    }



    private static final class PackageReBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            SmartUninstaller.onReceive(context,intent);
            SmartInstaller.onReceive(context,intent);
        }
    }

    private static final String AUTO_INSTALL_NUMBER = "AUTO_INSTALL_NUMBER";

    public static void setAutoInstallNumber(long number) {
        SPUtils.setString(SPUtils.FILE_NAME, ContextUtils.getApplicationContext(), AUTO_INSTALL_NUMBER, String.valueOf(number));
    }

    public static long getAutoInstallNumber() {
        String number = SPUtils.getString(SPUtils.FILE_NAME, ContextUtils.getApplicationContext(), AUTO_INSTALL_NUMBER, "0");
        return Long.valueOf(number);
    }

}
