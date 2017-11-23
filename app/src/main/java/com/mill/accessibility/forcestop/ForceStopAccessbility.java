package com.mill.accessibility.forcestop;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.mill.accessibility.accessibility.BaseAccessibility;
import com.mill.accessibility.mission.ApkMission;
import com.mill.accessibility.utils.LogUtils;


/**
 */

public class ForceStopAccessbility extends BaseAccessibility {
    private static String curPkg;
    private static ApkMission.StatusCallback curCallback;
    private static boolean isClickForceStop = false;
    private static boolean isClickOk = false;

    private static final String[] ok =new String[]{"确定", "确认", "好", "Ok", "ok", "OK"};
    private static final String[] forceStop = new String[]{"结束运行", "强行停止", "停止运行", "强制运行"};
    private static final String[] packages = new String[]{"com.android.settings"};
    private static Handler handler = new Handler(Looper.getMainLooper());



    @SuppressLint("NewApi")
    public static void  processInstallApplication(final AccessibilityService service, AccessibilityEvent event) {

        boolean hasForceStopView;
        boolean isForceStopEnable;

        if (isRegistPkg(event.getPackageName().toString()) && AccessibilityEvent.TYPE_VIEW_CLICKED != event.getEventType()) {

            AccessibilityNodeInfo nodeInfo = event.getSource();
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
                nodeInfo = service.getRootInActiveWindow();
            }

            //查找并处理确定按钮
            if(!isClickOk){
                isClickOk = findAndAccessAccessibilityNode(nodeInfo, ok);
                if(isClickOk){
                    return;
                }
            }

            //查找并处理强行停止节点
            hasForceStopView = hasAccessibilityNode(nodeInfo, forceStop);
            isForceStopEnable = isAccessibilityNodeEnable(nodeInfo, forceStop);
            if(hasForceStopView && isForceStopEnable && !isClickForceStop){
                isClickForceStop = findAndAccessAccessibilityNode(nodeInfo, forceStop);
            }

            //点击过了强行停止的确定按钮或者强行停止按钮不可点击
            if (hasForceStopView && !isForceStopEnable || isClickOk) {
                BaseAccessibility.resetAccessModel();
//                BatteryCallbackManager.getInstance().onForceStopStatusChanged(curPkg, StatusConst.FORCESTOP_SUCCESS);

                if(curCallback != null) {
                    curCallback.callback(curPkg, 0, StatusConst.FORCESTOP_SUCCESS);
                }
            }
        }
    }

    private static boolean isRegistPkg(String pckName) {
        for (String name : packages) {
            if (name.equalsIgnoreCase(pckName)) {
                return true;
            }
        }
        return false;
    }

    public static void finishPage(Context context){
        Intent intent = new Intent(context, ForceStopActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static void forceStop(Context context, String pkgName, ApkMission.StatusCallback callback){
        initStatus(pkgName, callback);
        Intent intent = new Intent(context, ForceStopActivity.class);
        if(!(context instanceof Activity)){
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        LogUtils.d("ForceStopActivity","forceStop:" + pkgName);
        intent.putExtra(ForceStopActivity.EXTRA_PKG, pkgName);
        context.startActivity(intent);
    }


    private static void initStatus(String pkgName, ApkMission.StatusCallback callback){
        isClickForceStop = false;
        isClickOk = false;
        curPkg = pkgName;
        curCallback = callback;
    }

}
