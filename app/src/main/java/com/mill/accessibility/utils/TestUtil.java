/*
 * 文件名：	TestTimeRangeUtil.java
 * 创建日期：	2012-8-3
 */
package com.mill.accessibility.utils;

import android.os.SystemClock;
import android.util.Log;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 */
public class TestUtil {
	private static final String sTag = "TestUtil";
	private static final boolean DEBUG = false;
	private static final Map<String, Long> sTags = new ConcurrentHashMap<>();

	/**
	 * 计时开始
	 * @param tag
	 */
	public static void start(String tag, String... appendMsg) {
		if (DEBUG) {
			long startDate = SystemClock.elapsedRealtime();
			sTags.put(tag, startDate);
//			Log.d(sTag, "start.tag = " + tag + (appendMsg != null && appendMsg.length > 0 ? ", " + Arrays.toString(appendMsg) : ""));
		}
	}

	/**
	 * 计时结束
	 * @param tag
	 * @param appendMsg
	 */
	public static void end(String tag, String... appendMsg) {
		if (DEBUG) {
			Long startDateL = sTags.get(tag);
			if (startDateL != null) {
				sTags.remove(tag);
				long intervals = SystemClock.elapsedRealtime() - startDateL;
				Log.d(sTag, "end,tag = " + tag + (appendMsg != null && appendMsg.length > 0 ? ", " + Arrays.toString(appendMsg) : "") + ", time = " + intervals
						+ " ms");
			} else {
				Log.d(sTag, "end.tag = " + tag + ", not start.");
			}
		}
	}

	/**
	 * 让线程sleep指定毫秒(方便调试让http请求变慢).
	 */
	public static void sleep(long time) {
		if (DEBUG) {
			try {
				Thread.sleep(time);
			} catch (InterruptedException e) {
			}
		}
	}
}