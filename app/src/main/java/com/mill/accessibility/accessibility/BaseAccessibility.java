package com.mill.accessibility.accessibility;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityNodeInfo;

import com.mill.accessibility.utils.ContextUtils;
import com.mill.accessibility.utils.LogUtils;
import com.mill.accessibility.utils.SPUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 */
@SuppressLint("NewApi")
public class BaseAccessibility {
    protected static boolean isHaveAccessibilitySettings = true;
    public static AtomicBoolean isEnable = new AtomicBoolean(false);
    public static long autoClickNumber = 0;
    public static int accessModel = 0;

    private static boolean mBound = false;

    public static boolean autoInstallServerEnable() {
        return isEnable.get() || isSmallApkEnbale();
    }

    static {
        try {
            final PackageManager packageManager = ContextUtils.getApplicationContext().getPackageManager();
            List<ResolveInfo> list = packageManager.queryIntentActivities(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
                    PackageManager.GET_ACTIVITIES);
            isHaveAccessibilitySettings = list.size() > 0;
        } catch (Exception ignored) {
        }
    }

    public static void resetAccessModel() {
        accessModel = 0;
    }

    public static boolean sdkIsSupport() {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1;
    }

    public static void logPrint(String msg) {
        LogUtils.d("BaseAccessibility", "msg=" + msg);
    }

    public static boolean findAndAccessAccessibilityNode(AccessibilityNodeInfo info, String[] texts) {
        return findAndAccessAccessibilityNode(info, texts, false);
    }

    public static boolean findAndAccessAccessibilityNode(AccessibilityNodeInfo info, String[] texts, boolean fuzzy) {
        if (texts != null) {
            for (String textItem : texts) {
                List<AccessibilityNodeInfo> nodes = info.findAccessibilityNodeInfosByText(textItem);
                if (nodes != null && !nodes.isEmpty()) {
                    AccessibilityNodeInfo node;
                    for (int i = 0; i < nodes.size(); i++) {
                        node = nodes.get(i);
                        BaseAccessibility.logPrint("find text=" + textItem + "  enable=" + node.isEnabled());
                        CharSequence text = node.getText();

                        if (!TextUtils.isEmpty(text)) {
                            if (textItem.equals(text.toString()) || (fuzzy && text.toString().contains(textItem))) {
                                if (node.isEnabled() && node.isClickable()) {
                                    if (node.getClassName().equals("android.widget.Button")
                                            || node.getClassName().equals("android.widget.TextView")
                                            || node.getClassName().equals("android.widget.CheckBox")) {
                                        autoClickNumber++;
                                        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean findAndAccessAccessibilityNodeCompat(AccessibilityNodeInfo info, String[] texts) {
        if (texts != null) {
            for (String textItem : texts) {
                List<AccessibilityNodeInfo> nodes = info.findAccessibilityNodeInfosByText(textItem);
                if (nodes != null && !nodes.isEmpty()) {
                    AccessibilityNodeInfo node;
                    for (int i = 0; i < nodes.size(); i++) {
                        node = nodes.get(i);
                        CharSequence text = node.getText();
                        if (text != null && textItem.equals(text.toString())) {
                            if (!node.isClickable()) {
                                List<AccessibilityNodeInfo> nodeParents = info.findAccessibilityNodeInfosByViewId("com.yulong.android:id/widget_bottombar_actionview");
                                if (nodeParents != null && !nodeParents.isEmpty()) {
                                    for (AccessibilityNodeInfo nodeParent : nodeParents) {
                                        int childCount = nodeParent.getChildCount();
                                        boolean find = false;
                                        for (int j = 0; j < childCount; j++) {
                                            AccessibilityNodeInfo nodeChild = nodeParent.getChild(j);
                                            CharSequence text2 = nodeChild.getText();
                                            if (text2 != null && textItem.equals(text2.toString())) {
                                                find = true;
                                                break;
                                            }
                                        }
                                        if (find) {
                                            for (int j = 0; j < childCount; j++) {
                                                AccessibilityNodeInfo nodeChild = nodeParent.getChild(j);
                                                if (nodeChild.isEnabled() && nodeChild.isClickable()) {
                                                    autoClickNumber++;
                                                    return nodeChild.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                                }
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean hasAccessibilityNode(AccessibilityNodeInfo info, String[] texts) {
        if (texts != null) {
            for (String textItem : texts) {
                List<AccessibilityNodeInfo> nodes = info.findAccessibilityNodeInfosByText(textItem);
                if (nodes != null && !nodes.isEmpty()) {
                    AccessibilityNodeInfo node;
                    for (int i = 0; i < nodes.size(); i++) {
                        node = nodes.get(i);
                        BaseAccessibility.logPrint("find text=" + textItem + "  enable=" + node.isEnabled());
                        CharSequence text = node.getText();
                        if (TextUtils.equals(text, textItem)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean isAccessibilityNodeEnable(AccessibilityNodeInfo info, String[] texts) {
        if (texts != null) {
            for (String textItem : texts) {
                List<AccessibilityNodeInfo> nodes = info.findAccessibilityNodeInfosByText(textItem);
                if (nodes != null && !nodes.isEmpty()) {
                    AccessibilityNodeInfo node;
                    for (int i = 0; i < nodes.size(); i++) {
                        node = nodes.get(i);
                        BaseAccessibility.logPrint("find text=" + textItem + "  enable=" + node.isEnabled());
                        CharSequence text = node.getText();
                        if (TextUtils.equals(text, textItem) && node.isEnabled()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean containAccessibilityNode(AccessibilityNodeInfo info, String[] texts) {
        if (texts != null) {
            for (String textItem : texts) {
                List<AccessibilityNodeInfo> nodes = info.findAccessibilityNodeInfosByText(textItem);
                if (nodes != null && !nodes.isEmpty()) {
                    AccessibilityNodeInfo node;
                    for (int i = 0; i < nodes.size(); i++) {
                        node = nodes.get(i);
                        BaseAccessibility.logPrint("find text=" + textItem + "  enable=" + node.isEnabled());
                        CharSequence text = node.getText();
                        if (!TextUtils.isEmpty(text)) {
                            if (text.toString().contains(textItem)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean isSmallApkEnbale() {
            return false;
    }

    public static void setAccessMode(final int mode) {

    }
}
