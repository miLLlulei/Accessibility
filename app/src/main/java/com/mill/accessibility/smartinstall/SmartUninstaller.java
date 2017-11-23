package com.mill.accessibility.smartinstall;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityNodeInfo;

import com.mill.accessibility.accessibility.MyAccessibility;
import com.mill.accessibility.accessibility.BaseAccessibility;
import com.mill.accessibility.accessibility.UninstallResidueAccessbility;
import com.mill.accessibility.utils.ContextUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 */
public class SmartUninstaller extends InstallAccessibility {

    private static final Map<String, SmartUnInstallItem> maps = new ConcurrentHashMap<>();

    public static void onReceive(Context context, Intent intent) {
        if (intent != null) {
            String intentAction = intent.getAction();
            final String sPackageName = intent.getDataString();
            if (TextUtils.isEmpty(sPackageName)) {
                return;
            }
            final String pkgName = sPackageName.substring(sPackageName.indexOf(':') + 1);
            if (TextUtils.isEmpty(pkgName)) {
                return;
            }
            if (Intent.ACTION_PACKAGE_REMOVED.equals(intentAction)) {
                SmartUnInstallItem item = getValueBykey(pkgName);
                if (item != null) {
                    SmartUninstaller.updateInstallState(pkgName, SmartUnInstallItem.uninstalled);
                    logPrint("onReceive  pkgName=" + pkgName + "  maps.size()=" + maps.size());
                    if (!SmartUninstaller.haveUnInstaingItem()) {
//                        SmartInstallerFloatWindow.getInstances().dismissWindow();
                    }
                }

                UninstallResidueAccessbility.getInstance().startResidueCancelMission(10 * 1000);
            }
        }
    }

    public static void updateInstallState(String key, int state) {
        if (maps.containsKey(key)) {
            SmartUnInstallItem item = maps.get(key);
            if (item != null) {
                item.uninstallState = state;
            }
        }
    }

    public static void addKey(PackageInfo info, boolean showFloating) {
        if (info != null) {
            String key = info.packageName;
            if (TextUtils.isEmpty(key)) {
                return;
            }
            accessModel = MyAccessibility.ACCESS_MODEL_UNINSTALL;

            //调用小apk里的方法智能卸载
            BaseAccessibility.setAccessMode(MyAccessibility.ACCESS_MODEL_UNINSTALL);
            maps.remove(key);
            SmartUnInstallItem item = new SmartUnInstallItem();
            item.key = key;
            item.label = getApkLabel(info);
            item.showFloatingWindow = showFloating;
            item.addMapsTime = System.currentTimeMillis();
            maps.put(key, item);
            logPrint("addKey key=" + key);
        }
    }

    public static void addKey(PackageInfo info) {
        addKey(info, true);
    }


    private static String getApkLabel(PackageInfo info) {
        Context context = ContextUtils.getApplicationContext();
        PackageManager pm = context.getPackageManager();
        return pm.getApplicationLabel(info.applicationInfo).toString();
    }

    private static boolean isContainsKey(String key) {
        return maps.containsKey(key);
    }

    private static SmartUnInstallItem getValueBykey(String key) {
        return maps.get(key);
    }

    private static String getNameByPackage(String key) {
        SmartUnInstallItem item = maps.get(key);
        if(item != null){
            return item.label;
        }
        return null;
    }

    public static void removeKey(String key) {
        maps.remove(key);
        logPrint("removeKey KEY=" + key);
    }

    public static void removeAllKey() {
        maps.clear();
        logPrint("removeAllKey-----------");
    }

    public static class SmartUnInstallItem {
        public String key;
        public long addMapsTime = 0;
        public String label;
        public boolean showFloatingWindow = true;
        public long clickUnInstallBtnTime = 0;
        public int uninstallState = inituninstall;
        public static final int inituninstall = 0;
        public static final int uninstalled = 1;
        public static final int uninstalling = 2;

        @Override
        public String toString() {
            return "  uninstallState=" + uninstallState + "  addMapsTime=" + addMapsTime +"  label="+label;
        }
    }

    public static void updateClickunIntstallBtnTime(String key, long time) {
        if (maps.containsKey(key)) {
            SmartUnInstallItem item = maps.get(key);
            if (item != null) {
                item.clickUnInstallBtnTime = time;
            }
        }
    }

    public static boolean haveUnInstaingItem() {
        Iterator it = maps.entrySet().iterator();
        String key;
        Object value;
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            key = (String) entry.getKey();
            value = entry.getValue();
            if (value != null) {
                logPrint("havaInstaingItem key=" + key + "  installState=" + ((SmartUnInstallItem) value).uninstallState);
                if (((SmartUnInstallItem) value).uninstallState == SmartUnInstallItem.uninstalling) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean needCloseFloatWindow() {
        Iterator it = maps.entrySet().iterator();
        String key;
        Object value;
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            key = (String) entry.getKey();
            value = entry.getValue();
            if (value != null) {
                SmartUnInstallItem item = ((SmartUnInstallItem) value);
                long clickUnInstallBtnTime = item.clickUnInstallBtnTime;
                long tempTime = System.currentTimeMillis() - clickUnInstallBtnTime;
                long installOutTime = 1 * 50 * 1000;
                logPrint("needCloseFloatWindow key=" + key + "  uninstallState=" + item.uninstallState + "  clickUnInstallBtnTime=" + clickUnInstallBtnTime
                        + "  tempTime=" + tempTime +  " isOutTime="+(tempTime > installOutTime));
                if (item.uninstallState == SmartUnInstallItem.uninstalling) {
                    return tempTime > installOutTime;
                }
            }
        }
        return true;
    }

    @SuppressLint("NewApi")
    public static String showUnInstallGuide(AccessibilityNodeInfo accessibilityNodeInfo, long time, boolean[] bShowFloatingWindow) {
        if (accessibilityNodeInfo == null) {
            return null;
        }

        Iterator it = maps.entrySet().iterator();
        String key;
        SmartUnInstallItem value;
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            key = (String) entry.getKey();
            value = (SmartUnInstallItem) entry.getValue();
            String label = value.label;
            if (!TextUtils.isEmpty(label) && (value != null && value.showFloatingWindow)) {
                logPrint("showUnInstallGuide :  key=" + key + "  value=" + value + "  label=" + label);
                List<AccessibilityNodeInfo> accessibilityNodeInfos = accessibilityNodeInfo.findAccessibilityNodeInfosByText(label);
                if (accessibilityNodeInfos != null && !accessibilityNodeInfos.isEmpty()) {
                    return key;
                }else if(adaptationCoolPad()){
                        List<AccessibilityNodeInfo> coolpadInfos = accessibilityNodeInfo.findAccessibilityNodeInfosByText(uninstall_text);
                        if(coolpadInfos != null && !coolpadInfos.isEmpty()){
                            return key;
                        }
                }
            } else {
                if (value != null && !value.showFloatingWindow) {
                    bShowFloatingWindow[0] = false;
                    return key;
                }
            }
        }
        return null;
    }

    private static final String uninstall_text = "要卸载此应用吗";

    //适配coolpad机型
    private static boolean adaptationCoolPad(){
        String br = Build.BRAND;
        if("coolpad".equalsIgnoreCase(br)){
            return true;
        }
        return false;
    }
}
