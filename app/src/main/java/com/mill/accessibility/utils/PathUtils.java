package com.mill.accessibility.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PathUtils {
    public static final String RECOVERY_DIRECTORY = "recovery";
    public static final String FILENAME_SEQUENCE_SEPARATOR = "-";
    private static final Pattern CONTENT_DISPOSITION_PATTERN = Pattern
            .compile("attachment;\\s*filename\\s*=\\s*\"([^\"]*)\"");

    private static String mDataDataPath;
    private static String mDataDataProductPath;
    private static File mDataDir;


    // /mnt/sdcard
    public static String getSDCardPath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    public static String getInternalPath() {
        return Environment.getDataDirectory().getAbsolutePath();
    }

    // /data/data
    public static String getDataDataPath() {
        if (mDataDataPath == null) {
            synchronized (PathUtils.class) {
                if (mDataDataPath == null) {
                    mDataDataPath = Environment.getDataDirectory().getAbsolutePath() + "/data";
                }
            }
        }
        return mDataDataPath;
    }

    public static String getDataDataProductPath() {
        if (mDataDataProductPath == null) {
            synchronized (PathUtils.class) {
                if (mDataDataProductPath == null) {
                    mDataDataProductPath = getDataDir().getAbsolutePath();
                }
            }
        }
        return mDataDataProductPath;
    }

    /**
     * 获取data私有目录根目录，如：“/data/data/<packageName>/”、“/data/user/0/<packageName>/”；
     *
     * 注意：
     * 使用{@link Context#getDatabasePath(String)}代替{@link Context#getCacheDir()}，
     * {@link Context#getCacheDir()}源码中，当"cache"子目录创建失败时会返回为null；
     *
     * @return
     */
    public static File getDataDir() {
        if (mDataDir == null) {
            synchronized (PathUtils.class) {
                if (mDataDir == null) {
                    try {
                        Context contextImpl = (Context) ReflectUtils.invokeMethod(ContextUtils.getApplicationContext(), "getBaseContext", null);
                        if (Build.VERSION.SDK_INT < 24) { // Android7.0
                            mDataDir = ((File) ReflectUtils.invokeMethod(contextImpl, "getDataDirFile", null));
                        } else {
                            mDataDir = ((File) ReflectUtils.invokeMethod(contextImpl, "getDataDir", null));
                        }
                    } catch (Throwable e) {
                        mDataDir = ContextUtils.getApplicationContext().getDatabasePath(" ").getParentFile();
                    }
                }
            }
        }
        return mDataDir;
    }

    // /data/data/com.qihoo.appstore/cache
    public static String getCacheDir(Context context) {
        return context.getCacheDir().getAbsolutePath();
    }

    public static boolean isFilenameValid(Context context, File file) {
        final File[] whiteList;
        try {
            file = file.getCanonicalFile();
            whiteList = new File[]{
                    context.getFilesDir().getCanonicalFile(),
                    context.getCacheDir().getCanonicalFile(),
                    Environment.getDownloadCacheDirectory().getCanonicalFile(),
                    Environment.getExternalStorageDirectory()
                            .getCanonicalFile(),};
        } catch (IOException e) {
            return false;
        }

        for (File testDir : whiteList) {
            if (contains(testDir, file)) {
                return true;
            }
        }

        return false;
    }

    private static boolean contains(File dir, File file) {
        if (file == null)
            return false;
        String dirPath = dir.getAbsolutePath();
        String filePath = file.getAbsolutePath();
        if (dirPath.equals(filePath)) {
            return true;
        }
        if (!dirPath.endsWith("/")) {
            dirPath += "/";
        }
        return filePath.startsWith(dirPath);
    }

    public static File getFilesystemRoot(String path) {
        File cache = Environment.getDownloadCacheDirectory();
        if (path.startsWith(cache.getPath())) {
            return cache;
        }
        File external = Environment.getExternalStorageDirectory();
        if (path.startsWith(external.getPath())) {
            return external;
        }
        throw new IllegalArgumentException(
                "Cannot determine filesystem root for " + path);
    }

    public static String buildValidFatFilename(String name) {
        if (TextUtils.isEmpty(name) || ".".equals(name) || "..".equals(name)) {
            return "(invalid)";
        }
        final StringBuilder res = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            final char c = name.charAt(i);
            if (isValidFatFilenameChar(c)) {
                res.append(c);
            } else {
                res.append('_');
            }
        }
        return res.toString();
    }

    private static boolean isValidFatFilenameChar(char c) {
        if ((c <= 0x1f)) {
            return false;
        }
        switch (c) {
            case '"':
            case '*':
            case '/':
            case ':':
            case '<':
            case '>':
            case '?':
            case '\\':
            case '|':
            case 0x7F:
                return false;
            default:
                return true;
        }
    }

    private static boolean isFilenameAvailableLocked(File[] parents, String name) {
        if (RECOVERY_DIRECTORY.equalsIgnoreCase(name))
            return false;

        for (File parent : parents) {
            if (new File(parent, name).exists()) {
                return false;
            }
        }

        return true;
    }

    public static String generateAvailableFilenameLocked(File[] parents,
                                                         String prefix, String suffix) throws IOException {
        String name = prefix + suffix;
        if (isFilenameAvailableLocked(parents, name)) {
            return name;
        }

		/*
         * This number is used to generate partially randomized filenames to
		 * avoid collisions. It starts at 1. The next 9 iterations increment it
		 * by 1 at a time (up to 10). The next 9 iterations increment it by 1 to
		 * 10 (random) at a time. The next 9 iterations increment it by 1 to 100
		 * (random) at a time. ... Up to the point where it increases by
		 * 100000000 at a time. (the maximum value that can be reached is
		 * 1000000000) As soon as a number is reached that generates a filename
		 * that doesn't exist, that filename is used. If the filename coming in
		 * is [base].[ext], the generated filenames are [base]-[sequence].[ext].
		 */
        int sequence = 1;
        for (int magnitude = 1; magnitude < 1000000000; magnitude *= 10) {
            for (int iteration = 0; iteration < 9; ++iteration) {
                name = prefix + FILENAME_SEQUENCE_SEPARATOR
                        + sequence + suffix;
                if (isFilenameAvailableLocked(parents, name)) {
                    return name;
                }
                sequence += RandomUtils.getRandomInt(magnitude) + 1;
            }
        }

        throw new IOException("Failed to generate an available filename");
    }


    private static String parseContentDisposition(String contentDisposition) {
        try {
            Matcher m = CONTENT_DISPOSITION_PATTERN.matcher(contentDisposition);
            if (m.find()) {
                return m.group(1);
            }
        } catch (IllegalStateException ex) {
        }
        return null;
    }

    public static String chooseFilename(String url, String contentDisposition,
                                        String contentLocation, String defFileName) {
        String filename = null;

        // If we couldn't do anything with the hint, move toward the content
        // disposition
        if (contentDisposition != null) {
            filename = parseContentDisposition(contentDisposition);
            if (filename != null) {
                int index = filename.lastIndexOf('/') + 1;
                if (index > 0) {
                    filename = filename.substring(index);
                }
            }
        }

        // If we still have nothing at this point, try the content location
        if (filename == null && contentLocation != null) {
            String decodedContentLocation = Uri.decode(contentLocation);
            if (decodedContentLocation != null
                    && !decodedContentLocation.endsWith("/")
                    && decodedContentLocation.indexOf('?') < 0) {
                int index = decodedContentLocation.lastIndexOf('/') + 1;
                if (index > 0) {
                    filename = decodedContentLocation.substring(index);
                } else {
                    filename = decodedContentLocation;
                }
            }
        }

        // If all the other http-related approaches failed, use the plain uri
        if (filename == null) {
            String decodedUrl = Uri.decode(url);
            if (decodedUrl != null && !decodedUrl.endsWith("/")
                    && decodedUrl.indexOf('?') < 0) {
                int index = decodedUrl.lastIndexOf('/') + 1;
                if (index > 0) {
                    filename = decodedUrl.substring(index);
                }
            }
        }

        // Finally, if couldn't get filename from URI, get a generic filename
        if (filename == null) {
            filename = defFileName;
        }

        filename = PathUtils.buildValidFatFilename(filename);

        return filename;
    }

    //TODO 需要做版本适配。4.X以上系统无此函数
    //4.4.4r1 有此函数
    //http://www.grepcode.com/file/repository.grepcode.com/java/ext/com.google.android/android/4.4.4_r1/android/os/FileUtils.java#FileUtils.isFilenameSafe%28java.io.File%29
    public static boolean isFilenameSafe(File file) {
        try {
            return (Boolean) Class.forName("android.os.FileUtils")
                    .getDeclaredMethod("isFilenameSafe", File.class)
                    .invoke(null, file);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 判断file是否是data/data/com.qihoo.appstore/cache的子目录或文件.
     */
    public static boolean isCacheSubFile(Context context, File file) {
        return file != null && file.getAbsolutePath().startsWith(context.getCacheDir().getAbsolutePath());
    }

    public static String replaceSdCardPath(String path) {
        String newPath = SDCardUtils.getSDCardPath();
        String tmp = path.toLowerCase();
        if (tmp.startsWith("sdcard") && tmp.length() > 6) {
            newPath += path.substring(6);
        }
        return newPath;
    }

    public static long getDirSize()
    {
        return 0;
    }
}
