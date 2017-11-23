package com.mill.accessibility.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.mill.accessibility.forcestop.ForceStopAccessbility;
import com.mill.accessibility.smartinstall.InstallAccessibility;
import com.mill.accessibility.smartinstall.SmartInstaller;
import com.mill.accessibility.smartinstall.SmartUninstaller;
import com.mill.accessibility.utils.DeviceUtils;
import com.mill.accessibility.utils.LogUtils;

/**
 */
@SuppressLint("NewApi")
public class AppstoreAccessibility extends AccessibilityService {
    public static final int ACCESS_MODEL_INSTALL = 1;
    public static final int ACCESS_MODEL_UNINSTALL = 2;
    public static final int ACCESS_MODEL_FORCESTOP = 3;
    public static final int ACCESS_MODEL_UNINSTALL_CLEAR = 4;
    public static final String[] packages = new String[]{"com.android.packageinstaller", "com.google.android.packageinstaller", "com.android.settings",
            "com.qihoo360.mobilesafe", "com.qihoo.cleandroid_cn", "com.samsung.android.packageinstaller",
            "com.cleanmaster.mguard_cn", "com.huawei.systemmanager", "com.lenovo.safecenter"}; //解决卸载残留
    public static boolean needClearTask = true;

    private String TAG = AppstoreAccessibility.class.getSimpleName();

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        this.processAccessibilityEnvent(event);
    }

    private void processAccessibilityEnvent(AccessibilityEvent event) {

        SmartInstaller.logPrint("processAccessibilityEnvent thread name=" + Thread.currentThread().getName());

        if (!SmartInstaller.sdkIsSupport()) {
            return;
        }

        if (event.getSource() == null) {
        } else {
            try {
                switch (BaseAccessibility.accessModel) {
                    case ACCESS_MODEL_INSTALL:
                        processInstallApplication(event);
                        break;
                    case ACCESS_MODEL_UNINSTALL:
                        processUninstallApplication(event);
                        break;
                    case ACCESS_MODEL_FORCESTOP:
                        ForceStopAccessbility.processInstallApplication(this, event);
                        break;
                    case ACCESS_MODEL_UNINSTALL_CLEAR:
                        UninstallResidueAccessbility.getInstance().processUninstallResidue(this, event);
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void onServiceConnected() {
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED |
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                | AccessibilityEvent.TYPE_VIEW_SCROLLED
                | AccessibilityEvent.TYPE_VIEW_CLICKED;

        info.packageNames = packages;

        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN;

        info.notificationTimeout = 100;

        this.setServiceInfo(info);

        BaseAccessibility.isEnable.set(true);
        if (needClearTask) {
            InstallAccessibility.openSettingAccessibilityNeedTips(this);
            AppstoreAccessibility.needClearTask = false;
            LocalAccessibilityManager.getInstance().notifyAccessibilityChanged(true);
        }

//        StatHelper.onSmartInstallEvent(StatFieldConst.SmartInstaller.Action.ACTION_SMARTINSC);
        LogUtils.d("BaseAccessibility", "onServiceConnected:" + BaseAccessibility.isEnable.get());

        SmartInstaller.logPrint("processAccessibilityEnvent onServiceConnected= " + BaseAccessibility.isEnable.get()
                + " autoClickNumber=" + BaseAccessibility.autoClickNumber);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        SmartInstaller.logPrint("onUnbind--------------");
        return super.onUnbind(intent);
    }


    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        return true;
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        SmartInstaller.logPrint("onCreate-----------");
        BaseAccessibility.autoClickNumber = SmartInstaller.getAutoInstallNumber();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        BaseAccessibility.isEnable.set(false);
        LocalAccessibilityManager.getInstance().notifyAccessibilityChanged(false);
        LogUtils.d("SmartInstaller", "onDestroy:" + BaseAccessibility.isEnable.get());
        SmartInstaller.removeAllKey();
//        SmartInstallerFloatWindow.getInstances().dismissWindow();
        SmartInstaller.logPrint("onDestroy-----------");
    }

    private static final String[] install = new String[]{"安装", "Install"};
    private static final String[] next = new String[]{"下一步", "继续安装", "继续", "Next"};
    private static final String[] next2 = new String[]{"继续安装"};
    private static final String[] open = new String[]{"打开", "Open"};
    private static final String[] complete = new String[]{"完成", "Done"};
    private static final String[] completeAfter = new String[]{"取消", "Cancel", "删除", "Delete"};
    private static final String[] Failure = new String[]{"失败"};
    private static final String[] ok = new String[]{"确定", "Ok"};
    private static final String[] uninstall = new String[]{"卸载", "Uninstall"};

    private boolean isContainerPck(String pckName) {
        for (String name : packages) {
            if (name.equalsIgnoreCase(pckName)) {
                return true;
            }
        }
        return false;
    }

    private void processUninstallApplication(AccessibilityEvent event) {
        AccessibilityNodeInfo nodeInfo = event.getSource();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            nodeInfo = getRootInActiveWindow();
        }

        boolean[] bShowFloatingWindow = {false};
        String key = SmartUninstaller.showUnInstallGuide(nodeInfo, System.currentTimeMillis(), bShowFloatingWindow);
        if (key == null && bShowFloatingWindow[0]) {
            return;
        }

        boolean execUninstall = false;
        //查找并处理确定节点
        if (BaseAccessibility.findAndAccessAccessibilityNode(nodeInfo, ok)) {
            execUninstall = true;
        }

        //查找并处理安装节点
        if (BaseAccessibility.findAndAccessAccessibilityNode(nodeInfo, uninstall)) {
            execUninstall = true;
        }

        BaseAccessibility.logPrint("auto_uninstall  number= " + BaseAccessibility.autoClickNumber + "  key=" + key + "  execUninstall=" + execUninstall);

        InstallAccessibility.setAutoInstallNumber(BaseAccessibility.autoClickNumber);

        if (execUninstall) {
            if (bShowFloatingWindow[0]) {
//                SmartInstallerFloatWindow.getInstances().showWindow(ContextUtils.getApplicationContext().getString(R.string.auto_uninstall_show_content));
                SmartUninstaller.updateClickunIntstallBtnTime(key, System.currentTimeMillis());
                SmartUninstaller.updateInstallState(key, SmartUninstaller.SmartUnInstallItem.uninstalling);
            }
        }
        if (bShowFloatingWindow[0]) {
//            SmartInstallerFloatWindow.getInstances().updateAutoInstallNumber(BaseAccessibility.autoClickNumber);
        }
    }

    private void processInstallApplication(AccessibilityEvent event) {

        boolean isClickInstall = false;
        boolean isClickNext = false;
        boolean isInstallComplete = false;

        if (isContainerPck(event.getPackageName().toString())) {

            AccessibilityNodeInfo nodeInfo = event.getSource();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {//大于15api增加了新的方法
                nodeInfo = getRootInActiveWindow();
            }

            boolean[] bShowFloatingWindow = {false};
            String key = SmartInstaller.showAutoInstallPage(nodeInfo, System.currentTimeMillis(), bShowFloatingWindow);
            if (key == null && bShowFloatingWindow[0]) {
                return;
            }

            //查找并处理确定节点
            if (BaseAccessibility.findAndAccessAccessibilityNode(nodeInfo, ok)) {
                isClickInstall = true;
            }

            //查找并处理安装节点
            if (BaseAccessibility.findAndAccessAccessibilityNode(nodeInfo, install)) {
                isClickInstall = true;
            } else if (DeviceUtils.isCoolpad() && BaseAccessibility.findAndAccessAccessibilityNodeCompat(nodeInfo, install)) {
                isClickInstall = true;
            }

            //查找并处理下一步节点
            if (BaseAccessibility.findAndAccessAccessibilityNode(nodeInfo, next)) {
                isClickNext = true;
            }

            if (BaseAccessibility.findAndAccessAccessibilityNode(nodeInfo, next2, true)) {
                isClickNext = true;
            }

            //查找并处理完成节点
            if (nodeInfo != null) {
                if (BaseAccessibility.findAndAccessAccessibilityNode(nodeInfo, complete)) {
                    isInstallComplete = true;
                } else if (DeviceUtils.isCoolpad() && BaseAccessibility.findAndAccessAccessibilityNodeCompat(nodeInfo, complete)) {
                    isInstallComplete = true;
                }
            }

            //查找并处理失败节点
            if (nodeInfo != null) {
                if (BaseAccessibility.findAndAccessAccessibilityNode(nodeInfo, Failure)) {
                    isInstallComplete = true;
                }
            }

            BaseAccessibility.logPrint("auto_install  number= " + BaseAccessibility.autoClickNumber + "  key=" +
                    key + "  isClickInstall=" + isClickInstall + "  isClickNext=" + isClickNext + "  isInstallComplete=" + isInstallComplete);

            InstallAccessibility.setAutoInstallNumber(BaseAccessibility.autoClickNumber);

            if (isInstallComplete) {
                BaseAccessibility.resetAccessModel();
                SmartInstaller.removeKey(key);
                if (!SmartInstaller.haveInstaingItem()) {
                    if (bShowFloatingWindow[0]) {
//                        SmartInstallerFloatWindow.getInstances().dismissWindow();
                    }
                }
            }
            if (isClickInstall) {
                if (bShowFloatingWindow[0]) {
                    SmartInstaller.updateClickIntstallBtnTime(key, System.currentTimeMillis());
                    SmartInstaller.updateInstallState(key, SmartInstaller.SmartInstallItem.installing);
//                    SmartInstallerFloatWindow.getInstances().showWindow(ContextUtils.getApplicationContext().getString(R.string.auto_install_show_content));
                }
            }
            if (bShowFloatingWindow[0]) {
//                SmartInstallerFloatWindow.getInstances().updateAutoInstallNumber(BaseAccessibility.autoClickNumber);
            }
        }
    }

    private void clickStat(String action) {
//        StatHelper.onSmartInstallEvent(action);
    }

}
