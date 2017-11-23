package com.mill.accessibility.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.mill.accessibility.utils.ApkUtils;
import com.mill.accessibility.utils.ContextUtils;
import com.mill.accessibility.utils.LogUtils;
import com.mill.accessibility.utils.ToastUtil;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.mill.accessibility.accessibility.MyAccessibility.packages;

/**
 */

public class UninstallResidueAccessbility extends BaseAccessibility{
    private static final String[] uninstall_text =new String[]{"卸载", "Uninstall", "uninstall"};
    private static final String[] residue_text =new String[]{"残留", "残余"};
    private static final String[] cancel_btn = new String[]{"取消", "Cancel", "cancel", "CANCEL",
                                                            "暂不删除",
                                                            "暂不", "确定"};
//    private static final String[] packages = new String[]{"com.qihoo360.mobilesafe",
//                                                            "com.qihoo.cleandroid_cn",
//                                                            "com.cleanmaster.mguard_cn",
//                                                            "com.miui.packageinstaller",
//                                                            "com.huawei.systemmanager"
//    };
    private Handler mMainHandler = new Handler(Looper.getMainLooper());
    private AtomicBoolean isProcessCancelFinish = new AtomicBoolean(true);
    private int mNeedClickCount = 0;
    private int mClickCount = 0;

    public static UninstallResidueAccessbility getInstance() {
        return mInstance;
    }

    public void startResidueCancelMission(long timeOut) {
        checkNeedClickCount();
        mMainHandler.removeCallbacks(timeoutRunnable);
        mMainHandler.postDelayed(timeoutRunnable, timeOut);

        isProcessCancelFinish.set(false);
        BaseAccessibility.accessModel = MyAccessibility.ACCESS_MODEL_UNINSTALL_CLEAR;
    }

    @SuppressLint("NewApi")
    public void processUninstallResidue(final AccessibilityService service, AccessibilityEvent event) {
        boolean hasResidueText;
        boolean isClickCancel = false;

        if (!isProcessCancelFinish.get()) {
            String pkgName = event.getPackageName().toString();
            if (isRegisterPkg(pkgName) && AccessibilityEvent.TYPE_VIEW_CLICKED != event.getEventType()) {
                AccessibilityNodeInfo nodeInfo = event.getSource();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    nodeInfo = ((MyAccessibility)service).getRootInActiveWindow(nodeInfo);
                }

                // 判断是否有残留文字
                hasResidueText = BaseAccessibility.containAccessibilityNode(nodeInfo, residue_text);

                // 查找取消按钮
                if (hasResidueText) {
                    if (BaseAccessibility.isAccessibilityNodeEnable(nodeInfo, cancel_btn)) {
                        isClickCancel = BaseAccessibility.findAndAccessAccessibilityNode(nodeInfo, cancel_btn);
                    }
                }

                // 处理过程完成
                if (isClickCancel) {
                    mClickCount++;
                }

                if (mClickCount >= mNeedClickCount) {
                    if (isProcessCancelFinish.compareAndSet(false, true)) {
                        mMainHandler.removeCallbacks(timeoutRunnable);
                        if (BaseAccessibility.accessModel == MyAccessibility.ACCESS_MODEL_UNINSTALL_CLEAR) {
                            BaseAccessibility.resetAccessModel();
                        }
                        mNeedClickCount = 0;
                        mClickCount = 0;
                    }
                }
            }
        }
    }

    private void checkNeedClickCount() {
        mNeedClickCount = 0;
        mClickCount = 0;
        for (int i = 0; i < packages.length; i++) {
            PackageInfo info = ApkUtils.getInstalledApp(ContextUtils.getApplicationContext(), packages[i]);
            if (info != null) {
                mNeedClickCount++;
            }
        }
    }

    private Runnable timeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (isProcessCancelFinish.compareAndSet(false, true)) {
                if (BaseAccessibility.accessModel == MyAccessibility.ACCESS_MODEL_UNINSTALL_CLEAR) {
                    BaseAccessibility.resetAccessModel();
                }
                mNeedClickCount = 0;
                mClickCount = 0;

                if (LogUtils.isDebug()) {
                    ToastUtil.show(ContextUtils.getApplicationContext(), "调试版本提示：卸载弹窗处理超时", Toast.LENGTH_SHORT);
                }
            }
        }
    };

    private static boolean isRegisterPkg(String pckName) {
        for (String name : packages) {
            if (name.equalsIgnoreCase(pckName)) {
                return true;
            }
        }
        return false;
    }

    private static UninstallResidueAccessbility mInstance = new UninstallResidueAccessbility();
}
