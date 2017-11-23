package com.mill.accessibility.utils;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;


public class SDCardUtils {

    private SDCardUtils() {
        throw new UnsupportedOperationException("cannot be instantiated");
    }

    public static boolean isSDCardMounted() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);

    }

    /**
     * 获取SD卡的剩余容量 单位byte
     */
    @SuppressWarnings("deprecation")
    public static long getSDCardAvalableSize() {
        if (isSDCardMounted()) {
            return FileUtils.getAvailableBytes(new File(PathUtils.getSDCardPath()));
        }
        return 0;
    }


    /**
     * 获取指定路径所在空间的剩余可用容量字节数，单位byte
     *
     * @param filePath
     * @return 容量字节 SDCard可用空间，内部存储可用空间
     */
    @SuppressWarnings("deprecation")
    public static long getFreeBytes(String filePath) {
        // 如果是sd卡的下的路径，则获取sd卡可用容量
        if (filePath.startsWith(PathUtils.getSDCardPath())) {
            filePath = PathUtils.getSDCardPath();
        } else {// 如果是内部存储的路径，则获取内存存储的可用容量
            filePath = Environment.getDataDirectory().getAbsolutePath();
        }
        StatFs stat = new StatFs(filePath);
        long availableBlocks = (long) stat.getAvailableBlocks() - 4;
        return stat.getBlockSize() * availableBlocks;
    }

    private static final String MAGIC_STR = ".android_secure";
    private static final boolean DEBUG = false;
    private static final String TAG = "MultiSdcardHelper";

    public static String getAppMovableSdcardPath() {
        List<Mntent.mntent> mntents = Mntent.queryAll();
        for (Mntent.mntent mntent : mntents) {
            if ("tmpfs".equalsIgnoreCase(mntent.mnt_fsname) && !TextUtils.isEmpty(mntent.mnt_dir)) {
                if (mntent.mnt_dir.endsWith(MAGIC_STR) && mntent.mnt_dir.length() > MAGIC_STR.length()) {
                    String path = mntent.mnt_dir.substring(0, mntent.mnt_dir.length() - MAGIC_STR.length());
                    if (path.endsWith("/")) {
                        path = path.substring(0, path.length() - 1);
                    }
                    return path;
                }
            }
        }
        return null;
    }



    public static File getLegacyExternalStorageDirectory() {
        try {
            Method getLegacyExternalStorageDirectory = Environment.class.getMethod("getLegacyExternalStorageDirectory");
            Object object = getLegacyExternalStorageDirectory.invoke(Environment.class);
            if (object instanceof File) {
                return (File) object;
            } else if (object instanceof String) {
                return new File((String) object);
            } else {
                return Environment.getExternalStorageDirectory();
            }
        } catch (Throwable e) {
            return Environment.getExternalStorageDirectory();
        }
    }

    public static File getEmulatedStorageSource() {
        try {
            Method getEmulatedStorageSource = Environment.class.getMethod("getEmulatedStorageSource", int.class);
            Object object = getEmulatedStorageSource.invoke(Environment.class, myUserId());
            if (object instanceof File) {
                return (File) object;
            } else if (object instanceof String) {
                return new File((String) object);
            }
        } catch (Throwable e) {
            try {
                File emulatedStorageSource = getEmulatedStorageSource(myUserId());
                if (emulatedStorageSource != null) {
                    return emulatedStorageSource;
                }
            } catch (Throwable e1) {
                //DONOTHING
            }
        }
        return Environment.getExternalStorageDirectory();
    }

    private static final String ENV_EMULATED_STORAGE_SOURCE = "EMULATED_STORAGE_SOURCE";

    private static File getEmulatedStorageSource(int userId) {
        // /mnt/shell/emulated/0
        String env = System.getenv(ENV_EMULATED_STORAGE_SOURCE);
        File file = new File(env, String.valueOf(userId));
        if (!TextUtils.isEmpty(env) && file.exists()) {
            return file;
        } else {
            return null;
        }
    }

    private static int myUserId() {
        try {
            Class<?> class1 = Class.forName("android.os.UserHandle");
            Method myUserId = class1.getMethod("myUserId");
            return (Integer) myUserId.invoke(null);
        } catch (Exception e) {
            return 0;
        }
    }

    public static class Sdcard {
        public String path;
        public String state;
        public boolean isEmulated;
        public boolean isRemovable;
        public String filesystem;

        @Override
        public boolean equals(Object o) {
            if (o instanceof Sdcard) {
                Sdcard sdcard1 = (Sdcard) o;
                return TextUtils.equals(path, sdcard1.path);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return path == null ? super.hashCode() : path.hashCode();
        }

        @Override
        public String toString() {
            return "path:" + path + "," + "state:" + state + "," + "isEmulated:" + isEmulated + "," + "isRemovable:" + isRemovable + "," + "filesystem:" + filesystem;
        }
    }

    private static boolean isExternalStorageRemovable() {
        try {
            Method isExternalStorageRemovable = Environment.class.getMethod("isExternalStorageRemovable");
            return (Boolean) isExternalStorageRemovable.invoke(null);
        } catch (IllegalArgumentException e) {
            return true;
        } catch (NoSuchMethodException e) {
            return true;
        } catch (IllegalAccessException e) {
            return true;
        } catch (InvocationTargetException e) {
            return true;
        }
    }

    private static boolean isExternalStorageEmulated() {
        try {
            Method isExternalStorageEmulated = Environment.class.getMethod("isExternalStorageEmulated");
            return (Boolean) isExternalStorageEmulated.invoke(null);
        } catch (IllegalArgumentException e) {
            return false;
        } catch (NoSuchMethodException e) {
            return false;
        } catch (IllegalAccessException e) {
            return false;
        } catch (InvocationTargetException e) {
            return false;
        }
    }

    public static String getSDCardPath() {
        return Environment
                .getExternalStorageDirectory().getPath();
    }


    private static String getStorageVolumeStats(Object service, Object storageVolume, String path) {
        try {
            Class<? extends Object> StorageVolumeClass = storageVolume.getClass();
            Method getState = StorageVolumeClass.getMethod("getState");
            return (String) getState.invoke(storageVolume);
        } catch (Exception e) {
            try {
                Class IMountService = service.getClass();
                Method getVolumeState = IMountService.getMethod("getVolumeState", String.class);
                return (String) getVolumeState.invoke(service, path);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return Environment.MEDIA_UNMOUNTABLE;
    }

    public static long GetAvailableSpace(Context context) {
        long ret;
        if (SDCardUtils.isSDCardMounted()) {
            ret = getSDCardAvalableSize();

        } else {
            ret = getApplicationAvailableSize(context);
        }
        return ret;
    }

    public static long getApplicationAvailableSize(Context context) {

        File root = context.getCacheDir();
        if (!FileUtils.IsFileExist(root.getParent())) {
            return 1024;
        }

        return FileUtils.getAvailableBytes(new File(root.getParent()));
    }

    /**
     * 获取Rom内存或SD卡上总空间的大小
     *
     * @param path
     * @return
     */
    public static long getStorageTotal(String path) {
        if (TextUtils.isEmpty(path)) {
            return -1;
        }
        long blockSize;
        long blocksTotal;
        long storageTotalSize;

        try {
            StatFs stat = new StatFs(path);
            blockSize = stat.getBlockSize();
            blocksTotal = stat.getBlockCount();
            storageTotalSize = blocksTotal * blockSize;
        } catch (Exception e) {
            storageTotalSize = -1;
        }

        return storageTotalSize;
    }

    /**
     * 获取所有存储设备空间只和
     *
     * @return
     */
    public static long getAllStorageTotal(Context context) {
        long ret = 0;
        if (SDCardUtils.isSDCardMounted()) {
            ret = getStorageTotal(PathUtils.getSDCardPath());
        } else {
            ret += getStorageTotal(context.getCacheDir().getParent());
        }
        return ret;
    }


    /**
     * 检查部分高端机型是否是在内部存储器中虚拟的外部储存设备。如：三星I9250就是内部存储器中虚拟的SD
     * 如果是True，说明它没有真正的外部存储设置。 解决部分高端机虚拟SD卡，禁止使用搬家功能。
     *
     * @return boolean
     */
    public static boolean isEmulatedExternalStorage() {
        return false;
    }
}
