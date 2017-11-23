package com.mill.accessibility.smartinstall;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityNodeInfo;

import com.mill.accessibility.accessibility.AppstoreAccessibility;
import com.mill.accessibility.accessibility.BaseAccessibility;
import com.mill.accessibility.utils.ContextUtils;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 */
public class SmartInstaller extends InstallAccessibility {

    private static final String AUTO_INSTALL_GUIDE_NEED_SHOW = "AUTO_INSTALL_GUIDE_NEED_SHOW";

    private static final Map<String, SmartInstallItem> maps = new ConcurrentHashMap<>();

    private static final String[] sensitiveWords = new String[]{"替换应用程序", "替换应用", "风险提示"};

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
            if (Intent.ACTION_PACKAGE_ADDED.equals(intentAction) || Intent.ACTION_PACKAGE_REPLACED.equals(intentAction) || Intent.ACTION_PACKAGE_REMOVED.equals(intentAction)) {
                boolean contains = isContainsKey(pkgName);
                logPrint("onReceive  pkgName=" + pkgName + "  isContainsKey=" + contains);
                if (contains) {
                    updateInstallState(pkgName, SmartInstallItem.installComplete);
                }
            }
        }
    }

    public static class SmartInstallItem {
        public String infoName;
        public String key;
        public String indexId;
        public boolean showFloatingWindow = true;
        public long addMapsTime = 0;
        public long clickInstallBtnTime = 0;
        public int installState = no_install;
        public static final int no_install = 0;
        public static final int installing = 1;
        public static final int installComplete = 2;

        @Override
        public String toString() {
            return "infoName=" + infoName + "  installState=" + installState + "  addMapsTime=" + addMapsTime;
        }
    }

    public static void setAutoInstallGuideShow() {
//        ApplicationConfig.getInstance().setBoolean(AUTO_INSTALL_GUIDE_NEED_SHOW, false);
    }

    public static boolean isAutoInstallGuideShow() {
//        return ApplicationConfig.getInstance().getBoolean(AUTO_INSTALL_GUIDE_NEED_SHOW, true);
        return true;
    }

    private static long lastShowSysInstallPageTime = 0;

    public static void addKey(String key, String labelName, String indexId) {
        addKey(key, labelName, indexId, true);
    }

    public static void addKey(String key, String labelName, String indexId, boolean showFloatingWindow) {
        if (TextUtils.isEmpty(key) || ContextUtils.getApplicationContext().getPackageName().equals(key)) {
            return;
        }
        BaseAccessibility.accessModel = AppstoreAccessibility.ACCESS_MODEL_INSTALL;

        BaseAccessibility.setAccessMode(AppstoreAccessibility.ACCESS_MODEL_INSTALL);
        maps.remove(key);
        SmartInstallItem item = new SmartInstallItem();
        item.infoName = labelName;
        item.key = key;
        item.indexId = indexId;
        item.addMapsTime = System.currentTimeMillis();
        item.showFloatingWindow = showFloatingWindow;
        maps.put(key, item);
        lastShowSysInstallPageTime = System.currentTimeMillis();
        logPrint("addKey key=" + key + "  name=" + labelName + "  lastShowSysInstallPageTime=" + lastShowSysInstallPageTime);
    }

    private static boolean isContainsKey(String key) {
        return maps.containsKey(key);
    }

    public static void removeKey(String key) {
        maps.remove(key);
        logPrint("removeKey KEY=" + key);
    }

    public static void removeAllKey() {
        maps.clear();
        logPrint("removeAllKey-----------");
    }

    public static void updateInstallState(String key, int state) {
        if (maps.containsKey(key)) {
            SmartInstallItem item = maps.get(key);
            if (item != null) {
                item.installState = state;
            }
        }
    }

    public static void updateClickIntstallBtnTime(String key, long time) {
        if (maps.containsKey(key)) {
            SmartInstallItem item = maps.get(key);
            if (item != null) {
                item.clickInstallBtnTime = time;
            }
        }
    }

    public static boolean haveInstaingItem() {
        Iterator it = maps.entrySet().iterator();
        String key;
        Object value;
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            key = (String) entry.getKey();
            value = entry.getValue();
            if (value != null) {
                logPrint("havaInstaingItem key=" + key + "  installState=" + ((SmartInstallItem) value).installState);
                if (((SmartInstallItem) value).installState == SmartInstallItem.installing) {
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
                SmartInstallItem item = ((SmartInstallItem) value);
                long clickInstallBtnTime = item.clickInstallBtnTime;
                long tempTime = System.currentTimeMillis() - clickInstallBtnTime;
                long installOutTime = 1 * 30 * 1000;
                logPrint("needCloseFloatWindow key=" + key + "  installState=" + item.installState + "  clickInstallBtnTime=" + clickInstallBtnTime
                        + "  tempTime=" + tempTime + " isOutTime=" + (tempTime > installOutTime));
                if (item.installState == SmartInstallItem.installing) {
                    return tempTime > installOutTime;
                }
            }
        }
        return true;
    }

    @SuppressLint("NewApi")
    public static String showAutoInstallPage(AccessibilityNodeInfo accessibilityNodeInfo, long time, boolean[] bShowFloatingWindow) {
        if (accessibilityNodeInfo == null) {
            return null;
        }

//        Iterator it = maps.entrySet().iterator();
//        String key;
//        SmartInstallItem value;
//        while (it.hasNext()) {
//            Map.Entry entry = (Map.Entry) it.next();
//            key = (String) entry.getKey();
//            value = (SmartInstallItem) entry.getValue();
//            QHDownloadResInfo info = DownloadObjs.downloadInfoMgr.getDownloadInfoById(value.indexId);
//            if (key != null && info != null && (value != null && value.showFloatingWindow)) {
//                logPrint("showAutoInstallPage :  key=" + key + "  value=" + value + "  infoname=" + info.resName);
//                if (!TextUtils.isEmpty(value.infoName) && !"-1".equals(value.infoName)) {
//                    List<AccessibilityNodeInfo> accessibilityNodeInfos = accessibilityNodeInfo.findAccessibilityNodeInfosByText(value.infoName);
//                    if (accessibilityNodeInfos != null && !accessibilityNodeInfos.isEmpty()) {
//                        return key;
//                    }
//                    //这里是适配vivo手机更新软件
//                    for (String word : sensitiveWords) {
//                        accessibilityNodeInfos = accessibilityNodeInfo.findAccessibilityNodeInfosByText(word);
//                        if (accessibilityNodeInfos != null && !accessibilityNodeInfos.isEmpty()) {
//                            return key;
//                        }
//                    }
//                } else {
//                    String name = info.resName;
//                    if (TextUtils.isEmpty(name)) {
//                        return null;
//                    }
//                    List<AccessibilityNodeInfo> accessibilityNodeInfos = accessibilityNodeInfo.findAccessibilityNodeInfosByText(name);
//                    if (accessibilityNodeInfos != null && !accessibilityNodeInfos.isEmpty()) {
//                        return key;
//                    }
//                }
//            } else {
//                if (value != null && !value.showFloatingWindow) {
//                    bShowFloatingWindow[0] = false;
//                    return key;
//                }
//            }
//        }
        return null;
    }

    public static void onCreate() {
        Iterator it = maps.entrySet().iterator();
        String key;
        Object value;
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            key = (String) entry.getKey();
            value = entry.getValue();
            if (value != null) {
                logPrint("onCreate :  havaInstaingItem key=" + key + "  installState=" + ((SmartInstallItem) value).installState);
                if (((SmartInstallItem) value).installState == SmartInstallItem.installComplete) {
                    removeKey(key);
                }
            }
        }
    }

    public static void onPause() {
    }

    public static void onDestory() {
//        SmartInstallerFloatWindow.getInstances().dismissWindow();
        logPrint("onDestory:--------------");
    }
}
