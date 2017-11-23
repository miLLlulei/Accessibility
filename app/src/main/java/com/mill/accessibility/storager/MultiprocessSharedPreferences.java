/*
 * 创建日期：2014年9月12日 下午0:0:02
 */
package com.mill.accessibility.storager;

import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.support.annotation.NonNull;
import android.util.Log;

import com.mill.accessibility.utils.LogUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * 使用ContentProvider实现多进程SharedPreferences读写;<br>
 * 1、ContentProvider天生支持多进程访问；<br>
 * 2、使用内部私有BroadcastReceiver实现多进程OnSharedPreferenceChangeListener监听；<br>
 * 
 * 使用方法：AndroidManifest.xml中添加provider申明：<br>
 * <pre>
 * &lt;provider android:name="com.android.zgj.utils.MultiprocessSharedPreferences"
 * android:authorities="com.android.zgj.MultiprocessSharedPreferences"
 * android:process="com.android.zgj.MultiprocessSharedPreferences"
 * android:exported="false" /&gt;
 * &lt;!-- authorities属性里面最好使用包名做前缀，apk在安装时authorities同名的provider需要校验签名，否则无法安装；--!/&gt;<br>
 * </pre>
 * 
 * ContentProvider方式实现要注意：<br>
 * 1、当ContentProvider所在进程android.os.Process.killProcess(pid)时，会导致整个应用程序完全意外退出或者ContentProvider所在进程重启；<br>
 * 重启报错信息：Acquiring provider <processName> for user 0: existing object's process dead；<br>
 * 2、如果设备处在“安全模式”下，只有系统自带的ContentProvider才能被正常解析使用，因此put值时默认返回false，get值时默认返回null；<br>
 * 
 * 其他方式实现SharedPreferences的问题：<br>
 * 使用FileLock和FileObserver也可以实现多进程SharedPreferences读写，但是维护成本高，需要定期对照系统实现更新新的特性；
 * 
 */
public class MultiprocessSharedPreferences extends ContentProvider implements SharedPreferences {
	private static final String TAG = "MultiprocessSharedPreferences";
	public static final boolean DEBUG = LogUtils.isDebug();
	private Context mContext;
	private String mName;
	private int mMode;
	private boolean mIsSafeMode;
	private static final Object CONTENT = new Object();
	private WeakHashMap<OnSharedPreferenceChangeListener, Object> mListeners;
	private BroadcastReceiver mReceiver;

	private static String AUTHORITY;
	private static volatile Uri AUTHORITY_URI;
	private UriMatcher mUriMatcher;
	private static final String KEY = "value";
	private static final String KEY_NAME = "name";
	private static final String PATH_WILDCARD = "*/";
	private static final String PATH_GET_ALL = "getAll";
	private static final String PATH_GET_STRING = "getString";
	private static final String PATH_GET_INT = "getInt";
	private static final String PATH_GET_LONG = "getLong";
	private static final String PATH_GET_FLOAT = "getFloat";
	private static final String PATH_GET_BOOLEAN = "getBoolean";
	private static final String PATH_CONTAINS = "contains";
	private static final String PATH_APPLY = "apply";
	private static final String PATH_COMMIT = "commit";
	private static final String PATH_REGISTER_ON_SHARED_PREFERENCE_CHANGE_LISTENER = "registerOnSharedPreferenceChangeListener";
	private static final String PATH_UNREGISTER_ON_SHARED_PREFERENCE_CHANGE_LISTENER = "unregisterOnSharedPreferenceChangeListener";
	private static final String PATH_GET_STRING_SET = "getStringSet";
	private static final int GET_ALL = 1;
	private static final int GET_STRING = 2;
	private static final int GET_INT = 3;
	private static final int GET_LONG = 4;
	private static final int GET_FLOAT = 5;
	private static final int GET_BOOLEAN = 6;
	private static final int CONTAINS = 7;
	private static final int APPLY = 8;
	private static final int COMMIT = 9;
	private static final int REGISTER_ON_SHARED_PREFERENCE_CHANGE_LISTENER = 10;
	private static final int UNREGISTER_ON_SHARED_PREFERENCE_CHANGE_LISTENER = 11;
	private static final int GET_STRING_SET = 12;
	private HashMap<String, Integer> mListenersCount;

	private static class ReflectionUtil {

		public static ContentValues contentValuesNewInstance(HashMap<String, Object> values) {
			try {
				Constructor<ContentValues> c = ContentValues.class.getDeclaredConstructor(new Class[] { HashMap.class }); // hide
				c.setAccessible(true);
				return c.newInstance(values);
			} catch (IllegalArgumentException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			} catch (InstantiationException e) {
				throw new RuntimeException(e);
			}
		}

		public static String contentProvidermAuthority(ContentProvider contentProvider) {
			try {
				Field mAuthority = ContentProvider.class.getDeclaredField("mAuthority"); // Android 5.0
				mAuthority.setAccessible(true);
				return (String) mAuthority.get(contentProvider);
			} catch (NoSuchFieldException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
	}

	// 如果设备处在“安全模式”下，只有系统自带的ContentProvider才能被正常解析使用；
	private boolean isSafeMode(Context context) {
		boolean isSafeMode = false;
		try {
			isSafeMode = context.getPackageManager().isSafeMode();
		} catch (Throwable e) {
			if (!isPackageManagerHasDiedException(e)
					&& !isApplicationPackageManagerNullPointerException(e)) {
				throw e;
			}
		}
		return isSafeMode;
	}
	
	/**
	 * （可选）设置AUTHORITY，不用在初始化时遍历程序的AndroidManifest.xml文件获取android:authorities的值，减少初始化时间提高运行速度；
	 *
	 * @param authority
	 */
	public static void setAuthority(String authority) {
		AUTHORITY = authority;
	}

	private boolean checkInitAuthority(Context context) {
		if (AUTHORITY_URI == null) {
			synchronized (MultiprocessSharedPreferences.this) {
				if (AUTHORITY_URI == null) {
					if(AUTHORITY == null) {
						if (Build.VERSION.SDK_INT >= 21 && this instanceof ContentProvider) { // Android5.0.1
							AUTHORITY = ReflectionUtil.contentProvidermAuthority(this);
						} else {
							PackageInfo packageInfos = null;
							try {
								packageInfos = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PROVIDERS);
							} catch (PackageManager.NameNotFoundException e) {
								if (DEBUG) {
									e.printStackTrace();
								}
							} catch (RuntimeException e) {
								if (!isPackageManagerHasDiedException(e)) {
									throw new RuntimeException("checkInitAuthority", e);
								}
							}
							if (packageInfos != null && packageInfos.providers != null) {
								for (ProviderInfo providerInfo : packageInfos.providers) {
									if (providerInfo.name.equals(MultiprocessSharedPreferences.class.getName())) {
										AUTHORITY = providerInfo.authority;
										break;
									}
								}
							}
						}
					}
					if (DEBUG) {
						if (AUTHORITY == null) {
							throw new IllegalArgumentException("'AUTHORITY' initialize failed, Unable to find explicit provider class " + MultiprocessSharedPreferences.class.getName() + "; have you declared this provider in your AndroidManifest.xml?");
						} else {
							LogUtils.d(TAG, "checkInitAuthority.AUTHORITY = " + AUTHORITY);
						}
					}
					AUTHORITY_URI = Uri.parse(ContentResolver.SCHEME_CONTENT + "://" + AUTHORITY);
				}
			}
		}
		return AUTHORITY_URI != null;
	}

	private boolean isPackageManagerHasDiedException(Throwable e) {
//		1、packageManager.getPackageInfo
//		java.lang.RuntimeException: Package manager has died
//		at android.app.ApplicationPackageManager.getPackageInfo(ApplicationPackageManager.java:80)
//		...
//		Caused by: android.os.DeadObjectException
//		at android.os.BinderProxy.transact(Native Method)
//		at android.content.pm.IPackageManager$Stub$Proxy.getPackageInfo(IPackageManager.java:1374)

//		2、contentResolver.query
//		java.lang.RuntimeException: Package manager has died
//		at android.app.ApplicationPackageManager.resolveContentProvider(ApplicationPackageManager.java:636)
//		at android.app.ActivityThread.acquireProvider(ActivityThread.java:4750)
//		at android.app.ContextImpl$ApplicationContentResolver.acquireUnstableProvider(ContextImpl.java:2234)
//		at android.content.ContentResolver.acquireUnstableProvider(ContentResolver.java:1425)
//		at android.content.ContentResolver.query(ContentResolver.java:445)
//		at android.content.ContentResolver.query(ContentResolver.java:404)
//		at com.qihoo.storager.MultiprocessSharedPreferences.getValue(AppStore:502)
//		...
//		Caused by: android.os.TransactionTooLargeException
//		at android.os.BinderProxy.transact(Native Method)
//		at android.content.pm.IPackageManager$Stub$Proxy.resolveContentProvider(IPackageManager.java:2500)
//		at android.app.ApplicationPackageManager.resolveContentProvider(ApplicationPackageManager.java:634)

//		3、ApplicationPackageManager.isSafeMode
// 		java.lang.RuntimeException: Package manager has died
// 		at android.app.ApplicationPackageManager.isSafeMode(ApplicationPackageManager.java:820)
		if (e instanceof  RuntimeException
				&& e.getMessage() != null
				&& e.getMessage().contains("Package manager has died")) {
			Throwable cause = getLastCause(e);
			if (cause instanceof DeadObjectException
					|| cause.getClass().getName().equals("android.os.TransactionTooLargeException")) {
				return true;
			}
		}
		return false;
	}

	private boolean isApplicationPackageManagerNullPointerException(Throwable e) {
//		Xiaomi MI PAD 2 5.1(22)、asus ASUS_Z00ADB 5.0(21)、Teclast X16 Plus(D3A5) 5.1.1(22)、Lenovo YOGA Tablet 2-1050F 5.0.1(21)
//		java.lang.ExceptionInInitializerError
//		at com.qihoo.appstore.base.AppStoreApplication.onCreateMainProcess(AppStore:531)
//		at com.qihoo.appstore.base.AppStoreApplication.onCreateProcess(AppStore:305)
//		at com.qihoo.appstore.base.AppStoreApplication.onCreate(AppStore:259)
//		at android.app.Instrumentation.callApplicationOnCreate(Instrumentation.java:1011)
//		at android.app.ActivityThread.handleBindApplication(ActivityThread.java:4561)
//		at android.app.ActivityThread.access$1500(ActivityThread.java:147)
//		at android.app.ActivityThread$H.handleMessage(ActivityThread.java:1342)
//		at android.os.Handler.dispatchMessage(Handler.java:102)
//		at android.os.Looper.loop(Looper.java:135)
//		at android.app.ActivityThread.main(ActivityThread.java:5264)
//		at java.lang.reflect.Method.invoke(Native Method)
//		at java.lang.reflect.Method.invoke(Method.java:372)
//		at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:900)
//		at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:695)
//		Caused by: java.lang.NullPointerException: Attempt to write to field 'int android.app.ApplicationPackageManager.mCachedSafeMode' on a null object reference
//		at android.app.ApplicationPackageManager.isSafeMode(ApplicationPackageManager.java:975)
//		at com.qihoo.storager.MultiprocessSharedPreferences.isSafeMode(AppStore:149)
//		at com.qihoo.storager.MultiprocessSharedPreferences.<init
		Throwable cause = getLastCause(e);
		if (cause instanceof NullPointerException
				&& cause.getMessage() != null
				&& cause.getMessage().contains("Attempt to write to field 'int android.app.ApplicationPackageManager.mCachedSafeMode' on a null object reference")) {
			return true;
		}
		return false;
	}

	private boolean isUnstableCountException(Throwable e) {
//		java.lang.RuntimeException: java.lang.IllegalStateException: unstableCount < 0: -1
//		at com.android.zgj.utils.MultiprocessSharedPreferences.getValue(AppStore:595)
//		at com.android.zgj.utils.MultiprocessSharedPreferences.getString(AppStore:374)
//		...
//		Caused by: java.lang.IllegalStateException: unstableCount < 0: -1
//		at android.os.Parcel.readException(Parcel.java:1433)
//		at android.os.Parcel.readException(Parcel.java:1379)
//		at android.app.ActivityManagerProxy.refContentProvider(ActivityManagerNative.java:2596)
//		at android.app.ActivityThread.releaseProvider(ActivityThread.java:4632)
//		at android.app.ContextImpl$ApplicationContentResolver.releaseUnstableProvider(ContextImpl.java:2007)
//		at android.content.ContentResolver.query(ContentResolver.java:404)
//		at android.content.ContentResolver.query(ContentResolver.java:315)
//		at com.qihoo.storager.MultiprocessSharedPreferences.getValue(AppStore:583)

//		java.lang.IllegalStateException: ref counts can't go to zero here: stable=0 unstable=0
//		at android.os.Parcel.readException(Parcel.java:1433)
//		at android.os.Parcel.readException(Parcel.java:1379)
//		at android.app.ActivityManagerProxy.refContentProvider(ActivityManagerNative.java:2573)
//		at android.app.ActivityThread.releaseProvider(ActivityThread.java:4453)
//		at android.app.ContextImpl$ApplicationContentResolver.releaseProvider(ContextImpl.java:1830)
//		at android.content.ContentResolver$CursorWrapperInner.close(ContentResolver.java:1843)
//		at com.qihoo.storager.MultiprocessSharedPreferences.getValue(AppStore:613)
//		at com.qihoo.storager.MultiprocessSharedPreferences.getString(AppStore:372)
//		at com.qihoo.appstore.utils.ApplicationConfig.getString(AppStore:435)
//		at com.qihoo.appstore.rooter.RootManager.isInterceptRootStart(AppStore:319)
//		at com.qihoo.appstore.rooter.RootManager$1.run(AppStore:96)
//		at java.lang.Thread.run(Thread.java:856)
//		at com.qihoo.utils.thread.PriorityThreadFactory$2.run(AppStore:54)
		if (e instanceof  IllegalStateException
				&& e.getMessage() != null
				&& (e.getMessage().contains("unstableCount < 0: -1")
				|| e.getMessage().contains("ref counts can't go to zero here: stable=0 unstable=0"))) {
			return true;
		}
		return false;
	}

	private boolean isDeadSystemException(Throwable e) {
//		java.lang.RuntimeException: android.os.DeadSystemException
//		at android.app.ActivityThread.acquireProvider(ActivityThread.java:5577)
//		at android.app.ContextImpl$ApplicationContentResolver.acquireUnstableProvider(ContextImpl.java:2249)
//		at android.content.ContentResolver.acquireUnstableProvider(ContentResolver.java:1518)
//		at android.content.ContentResolver.query(ContentResolver.java:516)
//		at android.content.ContentResolver.query(ContentResolver.java:473)
//		at com.qihoo.storager.MultiprocessSharedPreferences.getValue(AppStore:610)
//		... 18 more
//		Caused by: android.os.DeadSystemException
//		... 24 more

//		java.lang.RuntimeException: java.lang.RuntimeException: android.os.DeadSystemException
//		at com.qihoo.storager.MultiprocessSharedPreferences$EditorImpl.setValue(AppStore:605)
//		at com.qihoo.storager.MultiprocessSharedPreferences$EditorImpl.commit(AppStore:584)
//		at com.qihoo.appstore.utils.ApplicationConfig.setString(AppStore:454)
//		at com.qihoo.appstore.rooter.RootManager.setPermToken(AppStore:219)
//		at com.qihoo.appstore.rooter.RootManager.startImpl(AppStore:161)
//		at com.qihoo.appstore.rooter.RootManager.access$100(AppStore:24)
//		at com.qihoo.appstore.rooter.RootManager$1.run(AppStore:103)
//		at java.lang.Thread.run(Thread.java:761)
//		at com.qihoo.utils.thread.PriorityThreadFactory$2.run(AppStore:54)
//		Caused by: java.lang.RuntimeException: android.os.DeadSystemException
//		at android.app.ActivityThread.acquireProvider(ActivityThread.java:5513)
//		at android.app.ContextImpl$ApplicationContentResolver.acquireProvider(ContextImpl.java:2365)
//		at android.content.ContentResolver.acquireProvider(ContentResolver.java:1471)
//		at android.content.ContentResolver.update(ContentResolver.java:1404)
//		at com.qihoo.storager.MultiprocessSharedPreferences$EditorImpl.setValue(AppStore:595)
//	... 9 more
//		Caused by: android.os.DeadSystemException
//	... 14 more
		if (e instanceof  RuntimeException
				&& e.getCause() != null
				&& e.getCause().getClass().getName().equals("android.os.DeadSystemException")
				&& e.getMessage() != null
				&& e.getMessage().contains("android.os.DeadSystemException")) {
			return true;
		}
		return false;
	}

	private boolean isUnableToFindAppForCallerSecurityException(Throwable e) {
//		java.lang.RuntimeException: java.lang.SecurityException: msg[Unable to find app for caller android.app.ApplicationThreadProxy@42c05400 (pid=3917) when getting content provider com.qihoo.appstore.MultiprocessSharedPreferences],args[[android.app.ActivityThread$ApplicationThread@422f3bb8, com.qihoo.appstore.MultiprocessSharedPreferences, 0, true]]
//		at com.qihoo.storager.MultiprocessSharedPreferences$EditorImpl.setValue(AppStore:628)
//		at com.qihoo.storager.MultiprocessSharedPreferences$EditorImpl.apply(AppStore:600)
//		at com.qihoo.appstore.utils.ApplicationConfig.setLong(AppStore:492)
//		at com.qihoo.appstore.statInner.NewOnlineStatManager$4.onResponse(AppStore:234)
//		at com.qihoo.appstore.statInner.NewOnlineStatManager$4.onResponse(AppStore:231)
//		at com.android.volley.toolbox.JsonRequest.deliverResponse(AppStore:65)
//		at com.android.volley.ExecutorDelivery$ResponseDeliveryRunnable.run(AppStore:99)
//		at android.os.Handler.handleCallback(Handler.java:733)
//		at android.os.Handler.dispatchMessage(Handler.java:95)
//		at android.os.Looper.loop(Looper.java:136)
//		at android.app.ActivityThread.main(ActivityThread.java:5113)
//		at java.lang.reflect.Method.invokeNative(Native Method)
//		at java.lang.reflect.Method.invoke(Method.java:515)
//		at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:793)
//		at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:609)
//		at dalvik.system.NativeStart.main(Native Method)
//		Caused by: java.lang.SecurityException: msg[Unable to find app for caller android.app.ApplicationThreadProxy@42c05400 (pid=3917) when getting content provider com.qihoo.appstore.MultiprocessSharedPreferences],args[[android.app.ActivityThread$ApplicationThread@422f3bb8, com.qihoo.appstore.MultiprocessSharedPreferences, 0, true]]
//		at com.morgoo.droidplugin.hook.proxy.IActivityManagerHook.invoke(AppStore:66)
//		at $Proxy1.getContentProvider(Native Method)
//		at android.app.ActivityThread.acquireProvider(ActivityThread.java:4511)
//		at android.app.ContextImpl$ApplicationContentResolver.acquireProvider(ContextImpl.java:2235)
//		at android.content.ContentResolver.acquireProvider(ContentResolver.java:1378)
//		at android.content.ContentResolver.update(ContentResolver.java:1310)
//		at com.qihoo.storager.MultiprocessSharedPreferences$EditorImpl.setValue(AppStore:616)
//		... 16 more
//		Caused by: java.lang.SecurityException: Unable to find app for caller android.app.ApplicationThreadProxy@42c05400 (pid=3917) when getting content provider com.qihoo.appstore.MultiprocessSharedPreferences
//		at android.os.Parcel.readException(Parcel.java:1489)
//		at android.os.Parcel.readException(Parcel.java:1443)
//		at android.app.ActivityManagerProxy.getContentProvider(ActivityManagerNative.java:2865)
//		at java.lang.reflect.Method.invokeNative(Native Method)
//		at java.lang.reflect.Method.invoke(Method.java:515)
//		at com.morgoo.droidplugin.hook.HookedMethodHandler.doHookInner(AppStore:63)
//		at com.morgoo.droidplugin.hook.proxy.ProxyHook.invoke(AppStore:60)
//		at com.morgoo.droidplugin.hook.proxy.IActivityManagerHook.invoke(AppStore:63)
//		... 22 more


		if (e instanceof  SecurityException
				&& e.getMessage() != null
				&& e.getMessage().contains("Unable to find app for caller")) {
			return true;
		}
		return false;
	}

	/**
	 * 获取异常栈中最底层的 Throwable Cause；
	 *
	 * @param tr
	 * @return
     */
	private Throwable getLastCause(Throwable tr) {
		Throwable cause = tr.getCause();
		Throwable causeLast = null;
		while (cause != null) {
			causeLast = cause;
			cause = cause.getCause();
		}
		if (causeLast == null) {
			causeLast = new Throwable();
		}
		return causeLast;
	}

	/**
	 * mode不使用{@link Context#MODE_MULTI_PROCESS}特可以支持多进程了；
	 * 
	 * @param mode
	 * 
	 * @see Context#MODE_PRIVATE
	 * @see Context#MODE_WORLD_READABLE
	 * @see Context#MODE_WORLD_WRITEABLE
	 */
	public static SharedPreferences getSharedPreferences(Context context, String name, int mode) {
		return new MultiprocessSharedPreferences(context, name, mode);
	}

	/**
	 * @deprecated 此默认构造函数只用于父类ContentProvider在初始化时使用；
	 */
	@Deprecated
	public MultiprocessSharedPreferences() {

	}

	private MultiprocessSharedPreferences(Context context, String name, int mode) {
		mContext = context;
		mName = name;
		mMode = mode;
		mIsSafeMode = isSafeMode(mContext);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, ?> getAll() {
		Map<String, ?> value = (Map<String, ?>) getValue(PATH_GET_ALL, null, null);
		return value == null ? new HashMap<String, Object>() : value;
	}

	@Override
	public String getString(String key, String defValue) {
		try {
			return (String) getValue(PATH_GET_STRING, key, defValue);
		} catch (ClassCastException e) {
//			java.lang.ClassCastException: java.lang.Integer cannot be cast to java.lang.String
//			at com.qihoo.storager.MultiprocessSharedPreferences.getString(AppStore:372)
//			at com.qihoo.appstore.utils.ApplicationConfig.getString(AppStore:435)
//			at com.qihoo.appstore.rooter.RootManager.isInterceptRootStart(AppStore:319)
//			at com.qihoo.appstore.rooter.RootManager$1.run(AppStore:96)
//			at java.lang.Thread.run(Thread.java:838)
//			at com.qihoo.utils.thread.PriorityThreadFactory$2.run(AppStore:54)
			return null;
		}
	}

	@Override
	public Set<String> getStringSet(String key, Set<String> defValues) {
		return (Set<String>) getValue(PATH_GET_STRING_SET, key, defValues);
	}

	@Override
	public int getInt(String key, int defValue) {
		return (Integer) getValue(PATH_GET_INT, key, defValue);
	}

	@Override
	public long getLong(String key, long defValue) {
		return (Long) getValue(PATH_GET_LONG, key, defValue);
	}

	@Override
	public float getFloat(String key, float defValue) {
		return (Float) getValue(PATH_GET_FLOAT, key, defValue);
	}

	@Override
	public boolean getBoolean(String key, boolean defValue) {
		return (Boolean) getValue(PATH_GET_BOOLEAN, key, defValue);
	}

	@Override
	public boolean contains(String key) {
		return (Boolean) getValue(PATH_CONTAINS, key, false);
	}

	@Override
	public Editor edit() {
		return new EditorImpl();
	}

	@Override
	public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
		synchronized (this) {
			if (mListeners == null) {
				mListeners = new WeakHashMap<OnSharedPreferenceChangeListener, Object>();
			}
			Boolean result = (Boolean) getValue(PATH_REGISTER_ON_SHARED_PREFERENCE_CHANGE_LISTENER, null, false);
			if (result != null && result) {
				mListeners.put(listener, CONTENT);
				if (mReceiver == null) {
					mReceiver = new BroadcastReceiver() {
						@Override
						public void onReceive(Context context, Intent intent) {
							String name = intent.getStringExtra(KEY_NAME);
							@SuppressWarnings("unchecked")
							List<String> keysModified = (List<String>) intent.getSerializableExtra(KEY);
							if (mName.equals(name) && keysModified != null) {
								Set<OnSharedPreferenceChangeListener> listeners = new HashSet<OnSharedPreferenceChangeListener>(mListeners.keySet());
								for (int i = keysModified.size() - 1; i >= 0; i--) {
									final String key = keysModified.get(i);
									for (OnSharedPreferenceChangeListener listener : listeners) {
										if (listener != null) {
											listener.onSharedPreferenceChanged(MultiprocessSharedPreferences.this, key);
										}
									}
								}
							}
						}
					};
					mContext.registerReceiver(mReceiver, new IntentFilter(makeAction(mName)));
				}
			}
		}
	}

	@Override
	public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
		synchronized (this) {
			getValue(PATH_UNREGISTER_ON_SHARED_PREFERENCE_CHANGE_LISTENER, null, false); // WeakHashMap
			if (mListeners != null) {
				mListeners.remove(listener);
				if (mListeners.isEmpty() && mReceiver != null) {
					mContext.unregisterReceiver(mReceiver);
				}
			}
		}
	}

	public final class EditorImpl implements Editor {
		private final Map<String, Object> mModified = new HashMap<String, Object>();
		private boolean mClear = false;

		@Override
		public Editor putString(String key, String value) {
			synchronized (this) {
				mModified.put(key, value);
				return this;
			}
		}

		@Override
		public Editor putStringSet(String key, Set<String> values) {
			synchronized (this) {
				mModified.put(key, (values == null) ? null : new HashSet<String>(values));
				return this;
			}
		}

		@Override
		public Editor putInt(String key, int value) {
			synchronized (this) {
				mModified.put(key, value);
				return this;
			}
		}

		@Override
		public Editor putLong(String key, long value) {
			synchronized (this) {
				mModified.put(key, value);
				return this;
			}
		}

		@Override
		public Editor putFloat(String key, float value) {
			synchronized (this) {
				mModified.put(key, value);
				return this;
			}
		}

		@Override
		public Editor putBoolean(String key, boolean value) {
			synchronized (this) {
				mModified.put(key, value);
				return this;
			}
		}

		@Override
		public Editor remove(String key) {
			synchronized (this) {
				mModified.put(key, null);
				return this;
			}
		}

		@Override
		public Editor clear() {
			synchronized (this) {
				mClear = true;
				return this;
			}
		}

		@Override
		public void apply() {
			setValue(PATH_APPLY);
		}

		@Override
		public boolean commit() {
			return setValue(PATH_COMMIT);
		}

		private boolean setValue(String pathSegment) {
			boolean result = false;
			if (!mIsSafeMode && checkInitAuthority(mContext)) { // 如果设备处在“安全模式”，返回false；
				String[] selectionArgs = new String[] { String.valueOf(mMode), String.valueOf(mClear) };
				synchronized (this) {
					Uri uri = Uri.withAppendedPath(Uri.withAppendedPath(AUTHORITY_URI, mName), pathSegment);
					ContentValues values = ReflectionUtil.contentValuesNewInstance((HashMap<String, Object>) mModified);
					try {
						result = mContext.getContentResolver().update(uri, values, null, selectionArgs) > 0;
					} catch (IllegalArgumentException e) {
						// 解决ContentProvider所在进程被杀时的抛出的异常：
						// java.lang.IllegalArgumentException: Unknown URI content://xxx.xxx.xxx/xxx/xxx
						// at android.content.ContentResolver.update(ContentResolver.java:1312)
						if (DEBUG) {
							e.printStackTrace();
						}
					} catch (RuntimeException e) {
						if (!isPackageManagerHasDiedException(e)
								&& !isUnstableCountException(e)
								&& !isDeadSystemException(e)
								&& !isUnableToFindAppForCallerSecurityException(e)) {
							throw new RuntimeException(e);
						}
					}
				}
			}
			if (DEBUG) {
				LogUtils.d(TAG, "setValue.mName = " + mName + ", pathSegment = " + pathSegment + ", mModified.size() = " + mModified.size());
			}
			return result;
		}
	}

	private Object getValue(String pathSegment, String key, Object defValue) {
		Object v = null;
		if (!mIsSafeMode && checkInitAuthority(mContext)) { // 如果设备处在“安全模式”，返回defValue；
			Uri uri = Uri.withAppendedPath(Uri.withAppendedPath(AUTHORITY_URI, mName), pathSegment);
			String[] projection = null;
			if (PATH_GET_STRING_SET.equals(pathSegment) && defValue != null) {
				@SuppressWarnings("unchecked")
				Set<String> set = (Set<String>) defValue;
				projection = new String[set.size()];
				set.toArray(projection);
			}
			String[] selectionArgs = new String[] { String.valueOf(mMode), key, defValue == null ? null : String.valueOf(defValue) };
			Cursor cursor = null;
			try {
				cursor = mContext.getContentResolver().query(uri, projection, null, selectionArgs, null);
			} catch (SecurityException e) {
				// 解决崩溃：
				// java.lang.SecurityException: Permission Denial: reading com.qihoo.storager.MultiprocessSharedPreferences uri content://com.qihoo.appstore.MultiprocessSharedPreferences/LogUtils/getBoolean from pid=2446, uid=10116 requires the provider be exported, or grantUriPermission()
				// at android.content.ContentProvider$Transport.enforceReadPermission(ContentProvider.java:332)
				// ...
				// at android.content.ContentResolver.query(ContentResolver.java:317)
				if (DEBUG) {
					e.printStackTrace();
				}
			} catch (NullPointerException e) {
				if ((e.getMessage() != null && e.getMessage().contains("Null pointer exception during instruction 'monitor-enter v10'"))
							|| Log.getStackTraceString(e).contains("android.app.ActivityThread.acquireProvider")) {
//					OPPO R7Plusm 5.1.1(22)、OPPO	OPPO A33 5.1.1(22)、OPPO	OPPO A53 5.1.1(22)
//					java.lang.NullPointerException: Null pointer exception during instruction 'monitor-enter v10'
//					at android.os.Parcel.readException(Parcel.java:1565)
//					at android.os.Parcel.readException(Parcel.java:1512)
//					at android.app.ActivityManagerProxy.getContentProvider(ActivityManagerNative.java:3436)
//					at android.app.ActivityThread.acquireProvider(ActivityThread.java:4851)
//					at android.app.ContextImpl$ApplicationContentResolver.acquireUnstableProvider(ContextImpl.java:2573)
//					at android.content.ContentResolver.acquireUnstableProvider(ContentResolver.java:1491)
//					at android.content.ContentResolver.query(ContentResolver.java:511)
//					at android.content.ContentResolver.query(ContentResolver.java:471)
//					at com.qihoo.storager.MultiprocessSharedPreferences.getValue(AppStore:581)

//					Coolpad Coolpad 8675-FHD 4.4.4(19)、Lenovo Lenovo A320t 4.4.2(19)、Xiaomi MI 2S 4.1.1(16)、Huawei HUAWEI G750-T01 4.2.2(17)、OPPO R8207 4.4.4(19)
//					java.lang.NullPointerException
//					at android.app.ActivityThread.acquireProvider(ActivityThread.java:4579)
//					at android.app.ContextImpl$ApplicationContentResolver.acquireUnstableProvider(ContextImpl.java:2262)
//					at android.content.ContentResolver.acquireUnstableProvider(ContentResolver.java:1503)
//					at android.content.ContentResolver.query(ContentResolver.java:469)
//					at android.content.ContentResolver.query(ContentResolver.java:416)
//					at com.qihoo.storager.MultiprocessSharedPreferences.getValue(AppStore:610)
//					at com.qihoo.storager.MultiprocessSharedPreferences.contains(AppStore:432)
//					at com.qihoo.appstore.utils.ApplicationConfig.contains(AppStore:480)
//					at com.qihoo.appstore.stablenotification.NotificationResources.isShowStableNotification(AppStore:60)
//					at com.qihoo.appstore.push.TransitService.onHandleIntent(AppStore:168)
//					at android.app.IntentService$ServiceHandler.handleMessage(IntentService.java:65)
//					at android.os.Handler.dispatchMessage(Handler.java:102)
//					at android.os.Looper.loop(Looper.java:136)
//					at android.os.HandlerThread.run(HandlerThread.java:61)

					LogUtils.d(TAG, "getValue NullPointerException", e);
				} else {
					throw e;
				}
			} catch (RuntimeException e) {
				if (!isPackageManagerHasDiedException(e)
						&& !isUnstableCountException(e)
						&& !isDeadSystemException(e)) {
					throw e;
				}
			}
			if (cursor != null) {
				Bundle bundle = null;
				try {
					bundle = cursor.getExtras();
				} catch (RuntimeException e) {
					// 解决ContentProvider所在进程被杀时的抛出的异常：
					// java.lang.RuntimeException: android.os.DeadObjectException
					// at android.database.BulkCursorToCursorAdaptor.getExtras(BulkCursorToCursorAdaptor.java:173)
					// at android.database.CursorWrapper.getExtras(CursorWrapper.java:94)
					if (DEBUG) {
						e.printStackTrace();
					}
				}
				if (bundle != null) {
					v = bundle.get(KEY);
					bundle.clear();
				}
				try {
					cursor.close();
				} catch (IllegalStateException e) {
//					java.lang.IllegalStateException: ref counts can't go to zero here: stable=0 unstable=0
//					at android.os.Parcel.readException(Parcel.java:1433)
//					at android.os.Parcel.readException(Parcel.java:1379)
//					at android.app.ActivityManagerProxy.refContentProvider(ActivityManagerNative.java:2405)
//					at java.lang.reflect.Method.invokeNative(Native Method)
//					at java.lang.reflect.Method.invoke(Method.java:511)
//					at com.morgoo.droidplugin.hook.proxy.ProxyHook.invoke(AppStore:62)
//					at com.morgoo.droidplugin.hook.proxy.IActivityManagerHook.invoke(AppStore:63)
//					at $Proxy1.refContentProvider(Native Method)
//					at android.app.ActivityThread.releaseProvider(ActivityThread.java:4486)
//					at android.app.ContextImpl$ApplicationContentResolver.releaseProvider(ContextImpl.java:1730)
//					at android.content.ContentResolver$CursorWrapperInner.close(ContentResolver.java:1841)
//					at com.qihoo.storager.MultiprocessSharedPreferences.getValue(AppStore:660)
//					at com.qihoo.storager.MultiprocessSharedPreferences.getString(AppStore:390)
//					at com.qihoo.appstore.utils.ApplicationConfig.getString(AppStore:446)
//					at com.qihoo.appstore.rooter.RootManager.isInterceptRootStart(AppStore:322)
//					at com.qihoo.appstore.rooter.RootManager$1.run(AppStore:98)
//					at java.lang.Thread.run(Thread.java:856)
//					at com.qihoo.utils.thread.PriorityThreadFactory$2.run(AppStore:54)
					if (!isUnstableCountException(e)) {
						throw new RuntimeException(e);
					}
				} catch (NullPointerException e) {
					if (e.getMessage() != null && e.getMessage().contains("Attempt to write to field 'boolean android.app.ActivityThread$ProviderRefCount.removePending' on a null object reference")) {
//						asus ASUS_Z00ADA 5.0(21)、asus ASUS_Z00ADB 5.0(21)、Teclast X80 Pro(E3E7) 5.1(22)
//						java.lang.NullPointerException: Attempt to write to field 'boolean android.app.ActivityThread$ProviderRefCount.removePending' on a null object reference
//						at android.app.ActivityThread.releaseProvider(ActivityThread.java:4824)
//						at android.app.ContextImpl$ApplicationContentResolver.releaseProvider(ContextImpl.java:2474)
//						at android.content.ContentResolver$CursorWrapperInner.close(ContentResolver.java:2453)
//						at com.qihoo.storager.MultiprocessSharedPreferences.getValue(AppStore:613)
//						at com.qihoo.storager.MultiprocessSharedPreferences.getInt(AppStore:383)
//						at com.qihoo.appstore.utils.ApplicationConfig.getInt(AppStore:443)
//						at com.qihoo.appstore.rooter.RadicalRoot.isForeGroundType(AppStore:139)
//						at com.qihoo.appstore.hackqualcomm.HackQualcommActivity$2.run(AppStore:60)
//						at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:422)
//						at java.util.concurrent.FutureTask.run(FutureTask.java:237)
//						at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.access$201(ScheduledThreadPoolExecutor.java:152)
//						at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:265)
//						at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1112)
						LogUtils.d(TAG, "getValue cursor.close() NullPointerException", e);
					} else {
						throw e;
					}
				}
			}
		}
		if (DEBUG) {
			LogUtils.d(TAG, "getValue.mName = " + mName + ", pathSegment = " + pathSegment + ", key = " + key + ", defValue = " + defValue);
		}
		return v == null ? defValue : v;
	}

	private String makeAction(String name) {
		return String.format("%1$s_%2$s", MultiprocessSharedPreferences.class.getName(), name);
	}

	@Override
	public boolean onCreate() {
		/*if (KillSelfHelper.DEBUG) {
			LogUtils.d(KillSelfHelper.TAG, TAG + ".onCreate = " + android.os.Process.myPid());
		}*/
		if (checkInitAuthority(getContext())) {
			mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
			mUriMatcher.addURI(AUTHORITY, PATH_WILDCARD + PATH_GET_ALL, GET_ALL);
			mUriMatcher.addURI(AUTHORITY, PATH_WILDCARD + PATH_GET_STRING, GET_STRING);
			mUriMatcher.addURI(AUTHORITY, PATH_WILDCARD + PATH_GET_INT, GET_INT);
			mUriMatcher.addURI(AUTHORITY, PATH_WILDCARD + PATH_GET_LONG, GET_LONG);
			mUriMatcher.addURI(AUTHORITY, PATH_WILDCARD + PATH_GET_FLOAT, GET_FLOAT);
			mUriMatcher.addURI(AUTHORITY, PATH_WILDCARD + PATH_GET_BOOLEAN, GET_BOOLEAN);
			mUriMatcher.addURI(AUTHORITY, PATH_WILDCARD + PATH_CONTAINS, CONTAINS);
			mUriMatcher.addURI(AUTHORITY, PATH_WILDCARD + PATH_APPLY, APPLY);
			mUriMatcher.addURI(AUTHORITY, PATH_WILDCARD + PATH_COMMIT, COMMIT);
			mUriMatcher.addURI(AUTHORITY, PATH_WILDCARD + PATH_REGISTER_ON_SHARED_PREFERENCE_CHANGE_LISTENER, REGISTER_ON_SHARED_PREFERENCE_CHANGE_LISTENER);
			mUriMatcher.addURI(AUTHORITY, PATH_WILDCARD + PATH_UNREGISTER_ON_SHARED_PREFERENCE_CHANGE_LISTENER, UNREGISTER_ON_SHARED_PREFERENCE_CHANGE_LISTENER);
			mUriMatcher.addURI(AUTHORITY, PATH_WILDCARD + PATH_GET_STRING_SET, GET_STRING_SET);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		/*if (KillSelfHelper.DEBUG) {
			LogUtils.d(KillSelfHelper.TAG, TAG + ".query = " + android.os.Process.myPid() + ", uri = " + uri + ", selectionArgs = " + Arrays.toString(selectionArgs));
		}*/
		String name = uri.getPathSegments().get(0);
		int mode = Integer.parseInt(selectionArgs[0]);
		String key = selectionArgs[1];
		String defValue = selectionArgs[2];
		Bundle bundle = new Bundle();
		switch (mUriMatcher.match(uri)) {
			case GET_ALL:
				bundle.putSerializable(KEY, (HashMap<String, ?>) getSystemSharedPreferences(name, mode).getAll());
				break;
			case GET_STRING:
				bundle.putString(KEY, getSystemSharedPreferences(name, mode).getString(key, defValue));
				break;
			case GET_INT:
				bundle.putInt(KEY, getSystemSharedPreferences(name, mode).getInt(key, Integer.parseInt(defValue)));
				break;
			case GET_LONG:
				bundle.putLong(KEY, getSystemSharedPreferences(name, mode).getLong(key, Long.parseLong(defValue)));
				break;
			case GET_FLOAT:
				bundle.putFloat(KEY, getSystemSharedPreferences(name, mode).getFloat(key, Float.parseFloat(defValue)));
				break;
			case GET_BOOLEAN:
				bundle.putBoolean(KEY, getSystemSharedPreferences(name, mode).getBoolean(key, Boolean.parseBoolean(defValue)));
				break;
			case CONTAINS:
				bundle.putBoolean(KEY, getSystemSharedPreferences(name, mode).contains(key));
				break;
			case REGISTER_ON_SHARED_PREFERENCE_CHANGE_LISTENER: {
				checkInitListenersCount();
				Integer countInteger = mListenersCount.get(name);
				int count = (countInteger == null ? 0 : countInteger) + 1;
				mListenersCount.put(name, count);
				countInteger = mListenersCount.get(name);
				bundle.putBoolean(KEY, count == (countInteger == null ? 0 : countInteger));
			}
				break;
			case UNREGISTER_ON_SHARED_PREFERENCE_CHANGE_LISTENER: {
				checkInitListenersCount();
				Integer countInteger = mListenersCount.get(name);
				int count = (countInteger == null ? 0 : countInteger) - 1;
				if (count <= 0) {
					mListenersCount.remove(name);
					bundle.putBoolean(KEY, !mListenersCount.containsKey(name));
				} else {
					mListenersCount.put(name, count);
					countInteger = mListenersCount.get(name);
					bundle.putBoolean(KEY, count == (countInteger == null ? 0 : countInteger));
				}
			}
				break;
			case GET_STRING_SET: {
				Set<String> set = null;
				if (projection != null) {
					set = new HashSet<String>(Arrays.asList(projection));
				}
				bundle.putSerializable(KEY, (HashSet<String>) getSystemSharedPreferences(name, mode).getStringSet(key, set));
			}
			default:
				if (DEBUG) {
					throw new IllegalArgumentException("At query, This is Unknown Uri：" + uri + ", AUTHORITY = " + AUTHORITY);
				}
		}
		return new BundleCursor(bundle);
	}

	@SuppressWarnings("unchecked")
	@Override
	public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		/*if (KillSelfHelper.DEBUG) {
			LogUtils.d(KillSelfHelper.TAG, TAG + ".update = " + android.os.Process.myPid() + ", uri = " + uri + ", selectionArgs = " + Arrays.toString(selectionArgs));
		}*/
		int result = 0;
		String name = uri.getPathSegments().get(0);
		int mode = Integer.parseInt(selectionArgs[0]);
		SharedPreferences preferences = getSystemSharedPreferences(name, mode);
		int match = mUriMatcher.match(uri);
		switch (match) {
			case APPLY:
			case COMMIT:
				boolean hasListeners = mListenersCount != null && mListenersCount.get(name) != null && mListenersCount.get(name) > 0;
				ArrayList<String> keysModified = null;
				Map<String, Object> map = null;
				if (hasListeners) {
					keysModified = new ArrayList<String>();
					map = (Map<String, Object>) preferences.getAll();
				}
				Editor editor = preferences.edit();
				boolean clear = Boolean.parseBoolean(selectionArgs[1]);
				if (clear) {
					if (hasListeners && !map.isEmpty()) {
						for (Map.Entry<String, Object> entry : map.entrySet()) {
							keysModified.add(entry.getKey());
						}
					}
					editor.clear();
				}
				for (Map.Entry<String, Object> entry : values.valueSet()) {
					String k = entry.getKey();
					Object v = entry.getValue();
					// Android 5.L_preview : "this" is the magic value for a removal mutation. In addition,
					// setting a value to "null" for a given key is specified to be
					// equivalent to calling remove on that key.
					if (v instanceof EditorImpl || v == null) {
						editor.remove(k);
						if (hasListeners && map.containsKey(k)) {
							keysModified.add(k);
						}
					} else {
						if (hasListeners && (!map.containsKey(k) || (map.containsKey(k) && !v.equals(map.get(k))))) {
							keysModified.add(k);
						}
					}

					if (v instanceof String) {
						editor.putString(k, (String) v);
					} else if (v instanceof Set) {
						editor.putStringSet(k, (Set<String>) v);
					} else if (v instanceof Integer) {
						editor.putInt(k, (Integer) v);
					} else if (v instanceof Long) {
						editor.putLong(k, (Long) v);
					} else if (v instanceof Float) {
						editor.putFloat(k, (Float) v);
					} else if (v instanceof Boolean) {
						editor.putBoolean(k, (Boolean) v);
					}
				}
				if (hasListeners && keysModified.isEmpty()) {
					result = 1;
				} else {
					switch (match) {
						case APPLY:
							editor.apply();
							result = 1;
							// Okay to notify the listeners before it's hit disk
							// because the listeners should always get the same
							// SharedPreferences instance back, which has the
							// changes reflected in memory.
							notifyListeners(name, keysModified);
							break;
						case COMMIT:
							if (editor.commit()) {
								result = 1;
								notifyListeners(name, keysModified);
							}
							break;
						default:
							break;
					}
				}
				values.clear();
				break;
			default:
				if (DEBUG) {
					throw new IllegalArgumentException("At update, This is Unknown Uri：" + uri + ", AUTHORITY = " + AUTHORITY);
				}
		}
		return result;
	}

	@Override
	public String getType(@NonNull Uri uri) {
		throw new UnsupportedOperationException("No external call");
	}

	@Override
	public Uri insert(@NonNull Uri uri, ContentValues values) {
		throw new UnsupportedOperationException("No external insert");
	}

	@Override
	public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
		throw new UnsupportedOperationException("No external delete");
	}

	private SharedPreferences getSystemSharedPreferences(String name, int mode) {
		return SharedPreferencesImpl.getSharedPreferences(getContext(), name, mode);
	}

	private void checkInitListenersCount() {
		if (mListenersCount == null) {
			mListenersCount = new HashMap<String, Integer>();
		}
	}

	private void notifyListeners(String name, ArrayList<String> keysModified) {
		if (keysModified != null && !keysModified.isEmpty()) {
			Intent intent = new Intent();
			intent.setAction(makeAction(name));
			intent.setPackage(getContext().getPackageName());
			intent.putExtra(KEY_NAME, name);
			intent.putExtra(KEY, keysModified);
			getContext().sendBroadcast(intent);
		}
	}

	private static final class BundleCursor extends MatrixCursor {
		private Bundle mBundle;

		public BundleCursor(Bundle extras) {
			super(new String[] {}, 0);
			mBundle = extras;
		}

		@Override
		public Bundle getExtras() {
			return mBundle;
		}

		@Override
		public Bundle respond(Bundle extras) {
			mBundle = extras;
			return mBundle;
		}
	}
}