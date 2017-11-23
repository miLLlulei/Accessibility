package com.mill.accessibility.utils;

import android.content.Context;

/**
 * cyc
 *只保留主进程的context 引用 不做其他逻辑
 *比较进程的方式有点耗时 先不处理
 * 2015-5-7
 */
public class ContextUtils {

	private static Context mContext;

	public static void init(Context context) {
		synchronized (ContextUtils.class) {
			mContext = context;
		}
	}
	
	public static Context getApplicationContext() {
		if (mContext != null) {
			return mContext;
		}
		synchronized (ContextUtils.class) {
			if (mContext == null) {
				mContext = ReflectUtils.getApplicationContext();
			}
		}
		return mContext;
	}
}
