package com.mill.accessibility;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.mill.accessibility.accessibility.AppstoreAccessibility;
import com.mill.accessibility.accessibility.BaseAccessibility;
import com.mill.accessibility.forcestop.ForceStopUtils;
import com.mill.accessibility.smartinstall.InstallAccessibility;
import com.mill.accessibility.utils.ApkUtils;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String PKG_NAME = "com.mill.accessibility";//修改自己的测试包名
    private String PKG_PATH = "/sdcard/apk.apk";//修改自己的测试apk
    private ServiceConnection mSConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        findViewById(R.id.btn1).setOnClickListener(this);
        findViewById(R.id.btn2).setOnClickListener(this);
        findViewById(R.id.btn3).setOnClickListener(this);
        findViewById(R.id.btn4).setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        if (mSConnection != null) {
            unbindService(mSConnection);
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn1) {
            if (!InstallAccessibility.autoInstallEnableAndSupport()) {
                InstallAccessibility.openSettingAccessibilityNeedTips(this, null);
                return;
            }
            bindService(new Intent(this, AppstoreAccessibility.class), mSConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {

                }

                @Override
                public void onServiceDisconnected(ComponentName name) {

                }
            }, Context.BIND_AUTO_CREATE);
        } else if (v.getId() == R.id.btn2) {
            BaseAccessibility.accessModel = AppstoreAccessibility.ACCESS_MODEL_INSTALL;
            ApkUtils.install(this, PKG_PATH);
        } else if (v.getId() == R.id.btn3) {
            BaseAccessibility.accessModel = AppstoreAccessibility.ACCESS_MODEL_UNINSTALL;
            ApkUtils.uninstall(this, PKG_NAME);
        } else if (v.getId() == R.id.btn4) {
            ForceStopUtils.forceStopPkg(this, PKG_NAME);
        }
    }
}
