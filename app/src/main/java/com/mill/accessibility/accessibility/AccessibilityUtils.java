package com.mill.accessibility.accessibility;

import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;

import java.util.HashSet;
import java.util.Set;

/**
 */
public class AccessibilityUtils {


    public static void getAccessibilityStatus(Context context) {
        long start = System.currentTimeMillis();
        try {
            Set<ComponentName> enabledServices = new HashSet<>();
            final String enabledServicesSetting = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            final TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
            colonSplitter.setString(enabledServicesSetting);
            while (colonSplitter.hasNext()) {
                final String componentNameString = colonSplitter.next();
                final ComponentName enabledService = ComponentName.unflattenFromString(componentNameString);
                BaseAccessibility.logPrint("getAccessibilityStatus= " + componentNameString + "  enabledService=" + (enabledService == null ? false : true));
                if (enabledService != null) {
                    enabledServices.add(enabledService);
                }
            }
            ComponentName appstoreComponent = new ComponentName(context, AppstoreAccessibility.class);
            BaseAccessibility.isEnable.set(enabledServices.contains(appstoreComponent));
            BaseAccessibility.logPrint("getAccessibilityStatus status:" + BaseAccessibility.isEnable.get() + "  enabledServicesSetting=" + enabledServicesSetting);
        } catch (Throwable e) {
            BaseAccessibility.logPrint(e.getMessage());
            e.printStackTrace();
        }
        BaseAccessibility.logPrint("getAccessibilityStatus= " + "getAccessibilityStatus cost:" + (System.currentTimeMillis() - start) + ",status:" + BaseAccessibility.isEnable.get());
    }

    /**
     * 通过AccessibilityManager获取是否开启辅助功能
     * @param context
     * @param className
     */
    /*public static void getAccessibilityStatus(Context context, String className){
        AccessibilityManager manager = (AccessibilityManager)context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if(Build.VERSION.SDK_INT >= AndroidVersionCodes.ICE_CREAM_SANDWICH){
            List<AccessibilityServiceInfo> enableInfos = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN);
            List<AccessibilityServiceInfo> installInfos = manager.getInstalledAccessibilityServiceList();
        }else{
            List<ServiceInfo> infos = manager.getAccessibilityServiceList();
        }
    }*/
}
