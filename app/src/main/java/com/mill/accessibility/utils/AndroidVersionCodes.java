package com.mill.accessibility.utils;

public class AndroidVersionCodes extends android.os.Build.VERSION_CODES {

	/**
	 * Android 6.0
	 * M is for Marshmallow!
	 *
	 * <p>Applications targeting this or a later release will get these
	 * new changes in behavior:</p>
	 * <ul>
	 * <li> Runtime permissions.  Dangerous permissions are no longer granted at
	 * install time, but must be requested by the application at runtime through
	 * {@link android.app.Activity#requestPermissions}.</li>
	 * <li> Bluetooth and Wi-Fi scanning now requires holding the location permission.</li>
	 * <li> {@link android.app.AlarmManager#setTimeZone AlarmManager.setTimeZone} will fail if
	 * the given timezone is non-Olson.</li>
	 * <li> Activity transitions will only return shared
	 * elements mapped in the returned view hierarchy back to the calling activity.</li>
	 * <li> {@link android.view.View} allows a number of behaviors that may break
	 * existing apps: Canvas throws an exception if restore() is called too many times,
	 * widgets may return a hint size when returning UNSPECIFIED measure specs, and it
	 * will respect the attributes {@link android.R.attr#foreground},
	 * {@link android.R.attr#foregroundGravity}, {@link android.R.attr#foregroundTint}, and
	 * {@link android.R.attr#foregroundTintMode}.</li>
	 * <li> {@link android.view.MotionEvent#getButtonState MotionEvent.getButtonState}
	 * will no longer report {@link android.view.MotionEvent#BUTTON_PRIMARY}
	 * and {@link android.view.MotionEvent#BUTTON_SECONDARY} as synonyms for
	 * {@link android.view.MotionEvent#BUTTON_STYLUS_PRIMARY} and
	 * {@link android.view.MotionEvent#BUTTON_STYLUS_SECONDARY}.</li>
	 * <li> {@link android.widget.ScrollView} now respects the layout param margins
	 * when measuring.</li>
	 * </ul>
	 */
	public static final int M = 23;

	/**
	 * Android 7.0
	 * N is for Nougat.
	 *
	 * <p>Applications targeting this or a later release will get these
	 * new changes in behavior:</p>
	 * <ul>
	 * <li> {@link android.app.DownloadManager.Request#setAllowedNetworkTypes
	 * DownloadManager.Request.setAllowedNetworkTypes}
	 * will disable "allow over metered" when specifying only
	 * {@link android.app.DownloadManager.Request#NETWORK_WIFI}.</li>
	 * <li> {@link android.app.DownloadManager} no longer allows access to raw
	 * file paths.</li>
	 * <li> {@link android.app.Notification.Builder#setShowWhen
	 * Notification.Builder.setShowWhen}
	 * must be called explicitly to have the time shown, and various other changes in
	 * {@link android.app.Notification.Builder Notification.Builder} to how notifications
	 * are shown.</li>
	 * <li>{@link android.content.Context#MODE_WORLD_READABLE} and
	 * {@link android.content.Context#MODE_WORLD_WRITEABLE} are no longer supported.</li>
	 * <li>{@link android.os.FileUriExposedException} will be thrown to applications.</li>
	 * <li>Applications will see global drag and drops as per
	 * {@link android.view.View#DRAG_FLAG_GLOBAL}.</li>
	 * <li>{@link android.webkit.WebView#evaluateJavascript WebView.evaluateJavascript}
	 * will not persist state from an empty WebView.</li>
	 * <li>{@link android.animation.AnimatorSet} will not ignore calls to end() before
	 * start().</li>
	 * <li>{@link android.app.AlarmManager#cancel(android.app.PendingIntent)
	 * AlarmManager.cancel} will throw a NullPointerException if given a null operation.</li>
	 * <li>{@link android.app.FragmentManager} will ensure fragments have been created
	 * before being placed on the back stack.</li>
	 * <li>{@link android.app.FragmentManager} restores fragments in
	 * {@link android.app.Fragment#onCreate Fragment.onCreate} rather than after the
	 * method returns.</li>
	 * <li>{@link android.R.attr#resizeableActivity} defaults to true.</li>
	 * <li>{@link android.graphics.drawable.AnimatedVectorDrawable} throws exceptions when
	 * opening invalid VectorDrawable animations.</li>
	 * <li>{@link android.view.ViewGroup.MarginLayoutParams} will no longer be dropped
	 * when converting between some types of layout params (such as
	 * {@link android.widget.LinearLayout.LayoutParams LinearLayout.LayoutParams} to
	 * {@link android.widget.RelativeLayout.LayoutParams RelativeLayout.LayoutParams}).</li>
	 * <li>Your application processes will not be killed when the device density changes.</li>
	 * <li>Drag and drop. After a view receives the
	 * {@link android.view.DragEvent#ACTION_DRAG_ENTERED} event, when the drag shadow moves into
	 * a descendant view that can accept the data, the view receives the
	 * {@link android.view.DragEvent#ACTION_DRAG_EXITED} event and won’t receive
	 * {@link android.view.DragEvent#ACTION_DRAG_LOCATION} and
	 * {@link android.view.DragEvent#ACTION_DROP} events while the drag shadow is within that
	 * descendant view, even if the descendant view returns <code>false</code> from its handler
	 * for these events.</li>
	 * </ul>
	 */
	public static final int N = 24;

	/**
	 * Android 7.1.1
	 * N MR1: Nougat++.
	 */
	public static final int N_MR1 = 25;
}