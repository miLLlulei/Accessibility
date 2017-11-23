package com.mill.accessibility.forcestop;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.mill.accessibility.accessibility.AppstoreAccessibility;
import com.mill.accessibility.accessibility.BaseAccessibility;
import com.mill.accessibility.mission.ApkMission;
import com.mill.accessibility.mission.InstallConsts;

/**
 */
public class ForceStopMission extends ApkMission {
    private Context mContext;
    private String mPkgName;

    public ForceStopMission(Context context, String pkgName){
        super(PRIORITY_HIGH);
        mContext = context;
        mPkgName = pkgName;
        missionType= InstallConsts.MissionType.MissionTypeForceStop;
    }

    @Override
    public void run() {
        boolean allowUseRoot = false;
        boolean result;
        changeStatus(mPkgName, StatusConst.FORCESTOP_RUNNING);
        if(allowUseRoot){
//            if(!RootManager.getInstance().isRootRunning()){
//                RootManager.getInstance().startSync(mContext,false);
//            }
//            result = RootForceStopUtils.rootForceStopPackage(mContext, mPkgName);
//            changeStatus(mPkgName, result ? StatusConst.FORCESTOP_SUCCESS : StatusConst.FORCESTOP_FAILED);
        }else{
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    BaseAccessibility.accessModel = AppstoreAccessibility.ACCESS_MODEL_FORCESTOP;
                    ForceStopAccessbility.forceStop(mContext, mPkgName, mCallback);

//                    BaseAccessibility.setAccessMode(AppstoreAccessibility.ACCESS_MODEL_FORCESTOP);
//                    BaseAccessibility.forceStop(mPkgName, new SmallApkCallbackImp());
                }
            });
        }
    }

}
