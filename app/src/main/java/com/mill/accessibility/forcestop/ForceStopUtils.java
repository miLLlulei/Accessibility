package com.mill.accessibility.forcestop;

import android.content.Context;

import com.mill.accessibility.accessibility.BaseAccessibility;
import com.mill.accessibility.mission.ApkMission;
import com.mill.accessibility.utils.DeviceUtils;

/**
 */
public class ForceStopUtils {
    public static boolean isSupportBatterySaving() {
        return isSupportRootOrAccessibility()
                && !DeviceUtils.isOnePlus();
    }

    public static boolean isSupportRootOrAccessibility() {
        boolean isSupportRoot = false;
        if(isSupportRoot){
            return true;
        }else if(BaseAccessibility.isEnable.get() || BaseAccessibility.isSmallApkEnbale()){
            return true;
        }else if(BaseAccessibility.sdkIsSupport()){
            return true;
        }else{
            return false;
        }
    }

    public static void forceStopPkg(final Context context, String pkgName) {
        ForceStopMission mission = new ForceStopMission(context, pkgName);
        mission.setStatusCallback(new ApkMission.StatusCallback() {
            @Override
            public void callback(Object object, int type, int status) {
                if(status == StatusConst.FORCESTOP_SUCCESS) {
                    //用于清除强行停止后可能残余的应用页面
                    ForceStopAccessbility.finishPage(context);
                }
            }
        });
        mission.run();
    }

}
