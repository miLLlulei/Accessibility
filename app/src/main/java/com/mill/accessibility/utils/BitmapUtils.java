package com.mill.accessibility.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class BitmapUtils {

    public static int getBitmapSize(Bitmap bitmap){
        if(bitmap != null){
            try{
                if(Build.VERSION.SDK_INT >= AndroidVersionCodes.KITKAT){
                    return bitmap.getAllocationByteCount();
                }else if(Build.VERSION.SDK_INT >= AndroidVersionCodes.HONEYCOMB_MR1){
                    return bitmap.getByteCount();
                }else{
                    return bitmap.getRowBytes() * bitmap.getHeight();
                }
            }catch (Exception e){
            }
        }
        return 0;
    }


    public static Bitmap getBitmapFromFile(File dst, int width, int height) {
        if (null != dst && dst.exists()) {
            BitmapFactory.Options opts = null;
            if (width > 0 && height > 0) {
                opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(dst.getPath(), opts);
                final int minSideLength = Math.min(width, height);
                opts.inSampleSize = computeSampleSize(opts, minSideLength, width * height);
                opts.inJustDecodeBounds = false;
                opts.inInputShareable = true;
                opts.inPurgeable = true;
            }
            try {
                return BitmapFactory.decodeFile(dst.getPath(), opts);
            } catch (OutOfMemoryError e) {
                try {
                    return BitmapFactory.decodeFile(dst.getPath(), opts);
                } catch (OutOfMemoryError e2) {
                    e2.printStackTrace();
                }
            }
        }
        return null;
    }


    public static Bitmap getBitmapFromFileAutoScale(File dst) {
        if (null != dst && dst.exists()) {
            InputStream fileInput = null;
            try {
                 fileInput = new FileInputStream(dst);
                TypedValue typedValue = new TypedValue();
                typedValue.density = DisplayMetrics.DENSITY_XHIGH;
                return BitmapFactory.decodeResourceStream(ContextUtils.getApplicationContext().getResources(),typedValue,fileInput,null,null);
            } catch (OutOfMemoryError e) {
                try {
                    return BitmapFactory.decodeFile(dst.getPath(), null);
                } catch (OutOfMemoryError e2) {
                    e2.printStackTrace();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                if(fileInput != null){
                    try {
                        fileInput.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }


    public static Bitmap drawableToBitamp(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = ((BitmapDrawable) drawable);
            return bitmapDrawable.getBitmap();
        } else {
            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(),
                    drawable.getOpacity() != PixelFormat.OPAQUE ? Config.ARGB_8888 : Config.RGB_565);

            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            drawable.draw(canvas);
            return bitmap;
        }
    }

    public static Bitmap getBitmapFromResource(Resources resources, int id, int width, int height) {
        BitmapFactory.Options opts = null;
        if (width > 0 && height > 0) {
            opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(resources, id, opts);
            final int minSideLength = Math.min(width, height);
            opts.inSampleSize = computeSampleSize(opts, minSideLength, width * height);
            opts.inJustDecodeBounds = false;
            opts.inInputShareable = true;
            opts.inPurgeable = true;
        }
        try {
            return BitmapFactory.decodeResource(resources, id, opts);
        } catch (OutOfMemoryError e) {
            try {
                return BitmapFactory.decodeResource(resources, id, opts);
            } catch (OutOfMemoryError e2) {
                e2.printStackTrace();
            }
        }
        return null;
    }

    public static Bitmap getBitmapFromUri(Context context, Uri uri, int width, int height) throws IOException {
        BitmapFactory.Options opts = null;
        Bitmap bitmap = null;
        if (width > 0 && height > 0) {
            opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            InputStream inputStream =  context.getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(inputStream, null, opts);
            inputStream.close();
            final int minSideLength = Math.min(width, height);
            opts.inSampleSize = computeSampleSize(opts, minSideLength, width * height);
            opts.inJustDecodeBounds = false;
            opts.inInputShareable = true;
            opts.inPurgeable = true;
        }
        try {
            InputStream inputStream =  context.getContentResolver().openInputStream(uri);
            bitmap =  BitmapFactory.decodeStream(inputStream, null, opts);
            inputStream.close();
        } catch (OutOfMemoryError e) {
            try {
                InputStream inputStream =  context.getContentResolver().openInputStream(uri);
                bitmap =  BitmapFactory.decodeStream(inputStream, null, opts);
                inputStream.close();
            } catch (OutOfMemoryError e2) {
                e2.printStackTrace();
            }
        }

        return bitmap;
    }

    public static boolean verifyBitmap(InputStream input) {
        if (input == null) {
            return false;
        }
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        input = input instanceof BufferedInputStream ? input
                : new BufferedInputStream(input);
        BitmapFactory.decodeStream(input, null, options);
        try {
            input.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return (options.outHeight > 0) && (options.outWidth > 0);
    }

    public static Bitmap getBitmapFromResourceWithHighQuality(Resources resources, int id, int width, int height) {
        try {
            Bitmap b = BitmapFactory.decodeResource(resources, id, new BitmapFactory.Options());
            return b != null ? b : getBitmapFromResource(resources, id, width, height);
        } catch (Throwable e) {
            return getBitmapFromResource(resources, id, width, height);
        }
    }

    public static byte[] bitmapToBytes(final Bitmap bitmap, final boolean needRecycle) {
        return bitmapToBytes(bitmap, CompressFormat.PNG, needRecycle);
    }

    public static byte[] bitmapToBytes(final Bitmap bitmap, CompressFormat format, final boolean needRecycle) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] result = null;
        try {
            bitmap.compress(format, 100, output);
            result = output.toByteArray();
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            try {
                output.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (needRecycle) {
                bitmap.recycle();
            }
        }
        return result;
    }

    private static int computeSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(options, minSideLength, maxNumOfPixels);

        int roundedSize;
        if (initialSize <= 8) {
            roundedSize = 1;
            while (roundedSize < initialSize) {
                roundedSize <<= 1;
            }
        } else {
            roundedSize = (initialSize + 7) / 8 * 8;
        }

        return roundedSize;
    }

    public static int computeSampleSize1(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int roundedSize = 1;
        if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
            int sample1 = options.outWidth / reqWidth;
            int sample2 = options.outHeight / reqHeight;
            roundedSize = sample1 < sample2 ? sample1 : sample2;
        }
        if (roundedSize < 1) {
            roundedSize = 1;
        }
        return roundedSize;
    }

    private static int computeInitialSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels) {
        double w = options.outWidth;
        double h = options.outHeight;

        int lowerBound = maxNumOfPixels == -1 ? 1 : (int) Math.ceil(Math.sqrt(w * h / maxNumOfPixels));
        int upperBound = minSideLength == -1 ? 128 : (int) Math.min(Math.floor(w / minSideLength), Math.floor(h / minSideLength));

        if (upperBound < lowerBound) {
            return lowerBound;
        }

        if (maxNumOfPixels == -1 && minSideLength == -1) {
            return 1;
        } else if (minSideLength == -1) {
            return lowerBound;
        } else {
            return upperBound;
        }
    }

    public static boolean savePicToPath(Bitmap b, File path, int quality, CompressFormat format) {
        if (b == null || path == null) {
            return false;
        }
        boolean sdCardExist = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        if (!sdCardExist) {
            return false;
        }

        FileUtils.makeDir(path.getParentFile().getPath());


        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(path);
            boolean success = b.compress(format, quality, fos);
            fos.flush();
            return success;
        } catch (IOException e) {
            return false;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e2) {
                    if (LogUtils.isDebug()) {
                        e2.printStackTrace();
                    }
                }
            }
        }
    }


    public static void savePicToSdCardRootPath(Bitmap b, String fileName, int quality) {
        File sdDir = Environment.getExternalStorageDirectory();
        File path = new File(sdDir.getPath() + "/" + fileName);
        savePicToPath(b, path, quality, CompressFormat.PNG);
    }

    public static Bitmap getBitmapFromView(View view) {
        return getBitmapFromView(view, false);
    }

    private static Bitmap getBitmapFromView(View view, boolean forceHighQuality) {
        try {
            return getBitmapFromView(view, 1, forceHighQuality);
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    private static final int TRY_GET_BITMAP_FROM_VIEW_MAX_REPEAT_TIME = 2;

    private static Bitmap getBitmapFromView(View view, int tryTime, boolean forceHighQuality) {
        boolean willNotCacheDrawingBefore = view.willNotCacheDrawing();
        view.setWillNotCacheDrawing(false);

        int drawingCacheBackgroundColorBefore = view.getDrawingCacheBackgroundColor();
        view.setDrawingCacheBackgroundColor(0);
        int drawingCacheQualityBefore = view.getDrawingCacheQuality();
        if (drawingCacheBackgroundColorBefore != 0) {
            view.destroyDrawingCache();
        }
        if (tryTime > 1) {
            view.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
        }
        view.buildDrawingCache();
        Bitmap cacheBitmap = view.getDrawingCache();
        if (cacheBitmap == null || cacheBitmap.isRecycled()) {
            view.setDrawingCacheQuality(drawingCacheQualityBefore);
            view.setWillNotCacheDrawing(willNotCacheDrawingBefore);
            view.setDrawingCacheBackgroundColor(drawingCacheBackgroundColorBefore);

            if (tryTime < TRY_GET_BITMAP_FROM_VIEW_MAX_REPEAT_TIME) {
                handleOutOfMemory();
                return getBitmapFromView(view, tryTime + 1, forceHighQuality);
            }
            return null;
        }

        Bitmap bitmap = createBitmap(cacheBitmap, cacheBitmap.getWidth(), cacheBitmap.getHeight(), forceHighQuality || tryTime == 1 ? Config.ARGB_8888
                : Config.ARGB_4444);

        if (bitmap == cacheBitmap) {
            bitmap = createBitmap(cacheBitmap);
        }

        view.destroyDrawingCache();

        view.setDrawingCacheQuality(drawingCacheQualityBefore);
        view.setWillNotCacheDrawing(willNotCacheDrawingBefore);
        view.setDrawingCacheBackgroundColor(drawingCacheBackgroundColorBefore);

        return bitmap;
    }

    /**
     * 用于压缩时旋转图片
     *
     * @throws IOException
     * @throws OutOfMemoryError
     */
    public static Bitmap rotateBitmap(String srcFilePath, Bitmap bitmap) throws IOException, OutOfMemoryError {
        float degree = 0F;
        try {
            ExifInterface exif = new ExifInterface(srcFilePath);
            switch (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90F;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180F;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270F;
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
//            01-23 11:03:04.040 W/ExifInterface(29568): Invalid image.
//            01-23 11:03:04.040 W/ExifInterface(29568): java.io.IOException: Invalid marker: 89
//            01-23 11:03:04.040 W/ExifInterface(29568): 	at android.media.ExifInterface.getJpegAttributes(ExifInterface.java:1656)
//            01-23 11:03:04.040 W/ExifInterface(29568): 	at android.media.ExifInterface.loadAttributes(ExifInterface.java:1360)
//            01-23 11:03:04.040 W/ExifInterface(29568): 	at android.media.ExifInterface.<init>(ExifInterface.java:1064)
        }

        Matrix matrix = new Matrix();
        matrix.setRotate(degree, bitmap.getWidth(), bitmap.getHeight());
        Bitmap b2 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (bitmap != b2) {
            bitmap.recycle();
            bitmap = b2;
        }
        return bitmap;
    }

    private static Bitmap createBitmap(Bitmap src) {
        try {
            return Bitmap.createBitmap(src);
        } catch (OutOfMemoryError e) {
            handleOutOfMemory();
            return Bitmap.createBitmap(src);
        }
    }


    public static Bitmap copyBitmap(Bitmap source) {
        if (source == null) {
            return null;
        }
        try {
            return createBitmap(source, source.getWidth(), source.getHeight(), source.getConfig());
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            handleOutOfMemory();
            return null;
        }
    }

    private static Bitmap createBitmap(Bitmap source, int width, int height, Config config) {
        try {
            if (config == null) {
                config = Config.ARGB_4444;//不要改成565，要不然一些png图会丢失透明信息。
            }
            Bitmap target = createBitmap(width, height, config);
            target.setDensity(source.getDensity());
            Canvas canvas = new Canvas(target);
            Paint paint = new Paint();
            paint.setDither(true);
            paint.setAntiAlias(true);
            Rect src = new Rect(0, 0, source.getWidth(), source.getHeight());
            Rect dst = new Rect(0, 0, width, height);
            canvas.drawBitmap(source, src, dst, paint);
            return target;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return source;
        }
    }

    private static Bitmap createBitmap(int width, int height, Config config) {
        try {
            return Bitmap.createBitmap(width, height, config);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            handleOutOfMemory();
            return Bitmap.createBitmap(width, height, config);
        }
    }

    private static void handleOutOfMemory() {
        System.gc();
    }

}
