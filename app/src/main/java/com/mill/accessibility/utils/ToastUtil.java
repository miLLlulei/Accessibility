package com.mill.accessibility.utils;

import android.content.Context;
import android.widget.Toast;

/**
 * 2015.4.20
 * 修改下Toast，避免一直弹不消失
 */
public class ToastUtil {
	private static Toast sToast;

	private ToastUtil() {
		throw new UnsupportedOperationException("cannot be instantiated");
	}

	/**
	 * 场景：当UI线程崩溃会导致toast一直显示不消失，部分手机（Coolpad 8675-FHD Android4.4.4、三星 SM-C1158 Android4.4.2）子线程崩溃也会导致toast一直显示不消失；
	 *
	 * 另外：如果使用Dialog套用Toast的window主题代替Toast显示，在部分手机（如：小米）上需要开启“悬浮窗权限”才能显示；
	 */
	public static void cancel() {
		if (sToast != null) {
			sToast.cancel();
		}
	}

	/**
	 * 短时间显示Toast
	 */
	public static void showShort(Context context, CharSequence message) {
		showToast(context, message, Toast.LENGTH_SHORT);
	}

	/**
	 * 短时间显示Toast
	 */
	public static void showShort(Context context, int message) {
		showToast(context, context.getString(message), Toast.LENGTH_SHORT);
	}

	/**
	 * 长时间显示Toast
	 */
	public static void showLong(Context context, CharSequence message) {
		showToast(context, message, Toast.LENGTH_LONG);
	}

	/**
	 * 长时间显示Toast
	 */
	public static void showLong(Context context, int message) {
		showToast(context, context.getString(message), Toast.LENGTH_LONG);
	}

	/**
	 * 自定义显示Toast时间
	 */
	public static void show(Context context, CharSequence message, int duration) {
		showToast(context, message, duration);
	}

	/**
	 * 自定义显示Toast时间
	 */
	public static void show(Context context, int message, int duration) {
		showToast(context, context.getString(message), duration);
	}

	private static void showToast(final Context context, final CharSequence msg, final int duration) {
		if (sToast == null) {
			// 使用context可能会有内存泄露，所以使用context.getApplicationContext()
			sToast = Toast.makeText(context.getApplicationContext(), msg, duration); // 需要在UI线程初始化；
		} else {
			sToast.setDuration(duration);
			sToast.setText(msg);
		}
		sToast.show();
	}

}