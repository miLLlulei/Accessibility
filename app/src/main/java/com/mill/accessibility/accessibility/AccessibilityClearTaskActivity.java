package com.mill.accessibility.accessibility;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import com.mill.accessibility.smartinstall.InstallAccessibility;

/**
 */
public class AccessibilityClearTaskActivity extends Activity {
    private boolean mFirstResume = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //兼容开启“不保留活动（用户离开后即销毁每个活动）”开关情况下自动回收activity的问题。
        if(savedInstanceState != null){
            mFirstResume = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        autoFinish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void autoFinish(){
        if(!mFirstResume){
//            SmartInstallerSettingFloatingWindow.getInstances().dismissWindow();
            finish();
            AppstoreAccessibility.needClearTask = false;
        }else{
            mFirstResume = false;
            openSettingAccessibilityNeedTips(this);
        }
    }

    private void openSettingAccessibilityNeedTips(Context context) {
        try{
            boolean enable = InstallAccessibility.autoInstallServerEnable();
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            if(!(context instanceof Activity)){
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
            if (!enable) {
//                SmartInstallerSettingFloatingWindow.getInstances().showWindow();
//                SmartInstallerSettingFloatingWindow.getInstances().dismissWindowDelayed(10 * 1000);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
