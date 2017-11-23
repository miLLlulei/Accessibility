package com.mill.accessibility;

import android.app.Application;
import android.content.Context;

import com.mill.accessibility.accessibility.AccessibilityUtils;
import com.mill.accessibility.thread.BackgroundExecutors;
import com.mill.accessibility.utils.ContextUtils;

/**
 * Created by lulei-ms on 2017/10/10.
 */

public class MyApplicatin extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        ContextUtils.init(this);

        BackgroundExecutors.getGlobalExecutor().postDelayed(new Runnable() {
            @Override
            public void run() {
                //读取智能安装开关
                AccessibilityUtils.getAccessibilityStatus(MyApplicatin.this);
                // 子线程执行，可以耗时。
            }
        }, 10);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
