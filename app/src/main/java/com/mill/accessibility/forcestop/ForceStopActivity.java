package com.mill.accessibility.forcestop;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;

import com.mill.accessibility.utils.LogUtils;


/**
 * 此Activity用于清除强行停止后可能残余的应用页面
 */
public class ForceStopActivity extends Activity {
    private static final String SCHEME = "package";
    public static final String EXTRA_PKG = "pkg_name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtils.d("ForceStopActivity", "onCreate");
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        LogUtils.d("ForceStopActivity", "onNewIntent");
        handleIntent(intent);
    }

    private void handleIntent(Intent intent){
        if(intent != null){
            final String pkgName = intent.getStringExtra(EXTRA_PKG);
            if(!TextUtils.isEmpty(pkgName)){
                LogUtils.d("ForceStopActivity", "pkg:" + pkgName);
                startForceStop(ForceStopActivity.this, pkgName);

            }else{
                finish();
            }
        }
    }

    private void startForceStop(Context context, String pkgName){
        try {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts(SCHEME, pkgName, null);
            intent.setData(uri);
            context.startActivity(intent);
        } catch (Exception e) {
            if (LogUtils.isDebug()) {
                e.printStackTrace();
            }
        }
    }
}
