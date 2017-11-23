package com.mill.accessibility.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class DeviceUtils {
    private static final String TAG = "DeviceUtils";
    private static final String DEFAULT_IMEI = "360_DEFAULT_IMEI"; // MD5值：3b3ae7a8818fc1d8c4a2448d2bd371e7
    public static final int LEVEL_LOW = 1;
    public static final int LEVEL_MIDDLE = 2;
    public static final int LEVEL_HIGH = 3;
    private static int CURRENT_LEVEL = -1;
    private static String sImei;
    private static String sImei_md5;
    private static String sImei2;
    private static String sStatus2;
    private static String sVersionName;
    private static String sVersionCode;
    private static final String APP_STORE_IMEI = "app_store_imei0_new";
    private static final String APP_STORE_IMEI2 = "app_store_imei";
    private static final String APP_STORE_IMEI_MD5 = "app_store_imei_md5";
    private static final String APP_STORE_STATUS = "app_store_status";
    private static final String UPDATED_IMEIS_FLAG = "update_imeis_flag";
    private static String sAbi;
    private static String sAbi2;
    private static String sAndroidId;
    private static int statusBarHeight;
    private static String miuiVersionName;
    private static String romName;
    private static String archName;
    private static int carrier_id = -1;

    // 手机品牌判断
    private static final int ANDROID_SDK_VERSION = Build.VERSION.SDK_INT;
    private static final String PRODUCT = Build.PRODUCT.toLowerCase();
    private static final String MODEL = Build.MODEL.toLowerCase();
    private static final String BRAND = Build.BRAND.toLowerCase();
    private static final String MANUFACTURER = Build.MANUFACTURER.toLowerCase();
    private static final String HOST = Build.HOST.toLowerCase();
    private static final String DISPLAY = Build.DISPLAY.toLowerCase();
    private static final String FINGERPRINT = Build.FINGERPRINT.toLowerCase();

    public static int Key1 = 122;
    public static int Key2 = 115;
    public static int Key3 = 104;
    public static int Key4 = 84;
    public static int Key5 = 116;
    public static int Key6 = 112;
    public static int Key7 = 94;
    public static int Key8 = 49;

    @SuppressWarnings("deprecation")
    public static String getScreenSize(Context ctx) {
        Display screenSize = ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int width = screenSize.getWidth();
        int height = screenSize.getHeight();

        return width + "_" + height;
    }

    public static int screen_width = 0;
    public static int screen_height = 0;

    public static boolean isMeizuM9() {
        return PRODUCT.contains("meizu_m9") && MODEL.contains("m9");
    }

    public static boolean isMeizuMX() {
        return PRODUCT.contains("meizu_mx");
    }

    public static boolean isMeizuMX2() {
        return PRODUCT.contains("meizu_mx2");
    }

    public static boolean isMeizuMX3() {
        return PRODUCT.contains("meizu_mx3");
    }

    public static boolean isMeizu() {
        return PRODUCT.contains("meizu");
    }

    public static boolean isGTN7100WithMokeeRom() {
        return MODEL.contains("gt-n7100") && HOST.contains("mokee");
    }

    public static boolean isMiOne() {
        return MODEL.startsWith("mi-one");
    }

    public static boolean isHuaweiP6() {
        return MODEL.contains("huawei") && MODEL.contains("p6");
    }

    public static boolean isHuawei() {
        return MODEL.contains("huawei") || BRAND.contains("honor") || MODEL.contains("honor") || BRAND.contains("huawei");
    }

    public static boolean isCoolpad() {
        return MODEL.contains("coolpad") || BRAND.contains("coolpad");
    }

    public static boolean isZTE() {
        return BRAND.equals("zte");
    }

    public static boolean isHtcDevice() {
        return MODEL.contains("htc") || MODEL.contains("desire");
    }

    public static boolean isLephoneDevice() {
        return PRODUCT.contains("lephone");
    }

    public static boolean isZTEU880() {
        return MANUFACTURER.equals("zte") && MODEL.contains("blade");
    }

    public static boolean isZTEUV880() {
        return MANUFACTURER.equals("zte") && MODEL.contains("zte-u v880");
    }

    public static boolean isZTEU950() {
        return MANUFACTURER.equals("zte") && MODEL.contains("zte u950");
    }

    public static boolean isZTEU985() {
        return MANUFACTURER.equals("zte") && MODEL.contains("zte u985");
    }

    public static boolean isHTCHD2() {
        return MANUFACTURER.equals("htc") && MODEL.contains("hd2");
    }

    public static boolean isHTCOneX() {
        return MANUFACTURER.equals("htc") && MODEL.contains("htc one x");
    }

    public static boolean isSamsung() {
        return MANUFACTURER.contains("samsung") || BRAND.contains("samsung") ;
    }

    public static boolean isI9100() {
        return MANUFACTURER.equals("samsung") && MODEL.equals("gt-i9100");
    }

    public static boolean isGtS5830() {
        return MODEL.equalsIgnoreCase("gt-s5830");
    }

    public static boolean isGtS5830i() {
        return MODEL.equalsIgnoreCase("gt-s5830i");
    }

    public static boolean isGtS5838() {
        return MODEL.startsWith("gt-s5838");
    }

    public static boolean isSMG9008W() {
        return MODEL.equalsIgnoreCase("sm-g9008w");
    }

    public static boolean isGTP1000() {
        return MODEL.equalsIgnoreCase("gt-p1000");
    }

    public static boolean isMb525() {
        return MODEL.startsWith("mb525");
    }

    public static boolean isMe525() {
        return MODEL.startsWith("me525");
    }

    public static boolean isMb526() {
        return MODEL.startsWith("mb526");
    }

    public static boolean isMe526() {
        return MODEL.startsWith("me526");
    }

    public static boolean isMe860() {
        return MODEL.startsWith("me860");
    }

    public static boolean isMe865() {
        return MODEL.startsWith("me865");
    }

    public static boolean isXT882() {
        return MODEL.startsWith("xt882");
    }

    public static boolean isYulong() {
        return MANUFACTURER.equalsIgnoreCase("yulong");
    }

    public static boolean isKindleFire() {
        return MODEL.contains("kindle fire");
    }

    public static boolean isLGP970() {
        return MODEL.startsWith("lg-p970");
    }

    public static boolean isLG() {
        return BRAND.equalsIgnoreCase("lge");
    }

    public static boolean isU8800() {
        return MODEL.startsWith("u8800");
    }

    public static boolean isU9200() {
        return MODEL.startsWith("u9200");
    }

    public static boolean isMt15i() {
        return MODEL.startsWith("mt15i");
    }

    public static boolean isDEOVOV5() {
        return MODEL.equalsIgnoreCase("deovo v5");
    }

    public static boolean isMilestone() {
        return MODEL.equalsIgnoreCase("milestone");
    }

    public static boolean isMilestoneXT720() {
        return MODEL.equalsIgnoreCase("milestone xt720");
    }

    public static boolean isXT702() {
        return MODEL.equalsIgnoreCase("xt702");
    }

    public static boolean isC8500() {
        return MODEL.equalsIgnoreCase("c8500");
    }

    public static boolean isAmoiN807() {
        return MODEL.equalsIgnoreCase("amoi n807");
    }

    public static boolean isE15I() {
        return MODEL.equalsIgnoreCase("e15i");
    }

    public static boolean isZTE_CN600() {
        return MODEL.equalsIgnoreCase("zte-c n600");
    }

    private static final String TOUCH3MODEL = "k-touch tou ch3";
    public static boolean isTouch3() {
        return TOUCH3MODEL.equals(MODEL);
    }

    public static String getScreenSizeEx(Context ctx) {
        if (screen_width == 0 || screen_height == 0) {
            Display dis = ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            screen_width = dis.getWidth();
            screen_height = dis.getHeight();
        }
        if (screen_width > screen_height) {
            return screen_height + "*" + screen_width;
        }
        return screen_width + "*" + screen_height;
    }


    @SuppressWarnings("deprecation")
    public static int getScreenWidth(Context ctx) {
        if (screen_width == 0) {
            Display screenSize = ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            screen_width = screenSize.getWidth();
        }

        return screen_width;
    }

    /**
     * 横竖屏切换时，修改一下保存的宽高值
     * @param width
     */
    public static void setScreenWidth(int width){
        screen_width = width;
    }

    public static void setScreenHeight(int height){
        screen_height = height;
    }

    @SuppressWarnings("deprecation")
    public static int getScreenHeight(Context ctx) {
        if (screen_height == 0) {
            Display screenSize = ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            screen_height = screenSize.getHeight();
        }
        return screen_height;
    }

    public static float getScreenDensity(Context ctx) {
        DisplayMetrics dsm = new DisplayMetrics();
        ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(dsm);
        return dsm.density;
    }

    public static Display getScreenDisplaySize(Context ctx) {
        return ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
    }

    /**
     * 用于获取状态栏的高度
     *
     * @return 返回状态栏高度的像素值。
     */
    public static int getStatusBarHeight(Context context) {
        if (statusBarHeight != 0)
            return statusBarHeight;

        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
        }
        return statusBarHeight;
    }

    public static String getVersionName(Context context) {
        String versionName;
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            versionName = String.format("%s", pi.versionName);
            if (versionName != null && versionName.length() != 0) {
                return versionName;
            }
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 是否是默认IMEI值（获取系统IMEI失败）
     *
     * @return
     */
    private static boolean isDefaultIMEI() {
        return DEFAULT_IMEI.equals(sImei);
    }

    public static String getModel() {
        String model = null;
        try {
            model = URLEncoder.encode(Build.MODEL, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return model;
    }

    public static String getVersionName() {
        try {
            if (null == sVersionName || sVersionName.length() == 0) {
                try {
                    sVersionName = URLEncoder.encode(DeviceUtils.getVersionName(ContextUtils.getApplicationContext()), "utf-8");
                } catch (UnsupportedEncodingException e) {
                    sVersionName = "";
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            sVersionName = "";
            e.printStackTrace();
        }
        return sVersionName;
    }

    public static boolean isDebuggable() {
        try {
            PackageManager pm = ContextUtils.getApplicationContext().getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(ContextUtils.getApplicationContext().getPackageName(), 0);
            return (0 != (packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static int getTargetSdkVersion() {
        try {
            PackageManager pm = ContextUtils.getApplicationContext().getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(ContextUtils.getApplicationContext().getPackageName(), 0);
            return packageInfo.applicationInfo.targetSdkVersion;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private static final String PID_PATH = "/sys/class/android_usb/android0/idProduct";
    private static final String VID_PATH = "/sys/class/android_usb/android0/idVendor";

    public static String getVID() {
        return readFile(VID_PATH);
    }

    public static String getPID() {
        return readFile(PID_PATH);
    }

    private static String readFile(String file) {
        File cmdLineFile = new File(file);
        if (!cmdLineFile.exists()) {
            return "";
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(cmdLineFile)));
            String line;
            while ((line = reader.readLine()) != null) {
                return line.trim();
            }
        } catch (Exception e) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                }
            }
        }

        return "";
    }

    public static String getCpuInfo(Context c) {
        String cpuinfo = "/proc/cpuinfo";

        FileReader fr;
        BufferedReader br = null;
        try {
            fr = new FileReader(cpuinfo);
            br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null) {
                String lline = line.toLowerCase();
                if (lline.contains("hardware")) {
                    String[] split = lline.split(":");
                    if (split.length >= 2) {
                        String value = split[1].toLowerCase().trim();
                        if (isMediatekPlatform()) {
                            return checkSupportGemini(c, value);
                        } else {
                            return value;
                        }
                    }
                }
            }
        } catch (Exception e) {

        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {

                }
            }
        }

        return "";
    }

    public static String checkSupportGemini(Context c, String value) {
        String enableGemini = ReflectUtils.getSystemProperties("ro.mediatek.gemini_support", "true");
        if ("true".equals(enableGemini)) {
            return value;
        } else {// 如果不是双卡版的，就返回非mtk的cpu型号
            return "null";
        }
    }

    /**
     * Check is or not Mediatek platfrom
     *
     * http://blog.csdn.net/fanmengke_im/article/details/28400815
     * 可以读取下面的三个MTK 平台独有的system property， 有即是MTK 平台了，并且可以获取具体的MTK 平台释放资讯。
     * ro.mediatek.platform          对应MTK IC， 注意不区分2G，3G， 如MT6575/MT6515 都统一会是MT6575
     * ro.mediatek.version.branch    对应MTK 内部branch， 如ALPS.ICS.MP,  ALPS.ICS2.MP, ALPS.JB.MP 等之类
     * ro.mediatek.version.release   对应MTK 内部branch 的释放版本，如ALPS.ICS.MP.V2.47, ALPS.JB2.MP.V1.9
     */
    public static boolean isMediatekPlatform(){
        String platform = ReflectUtils.getSystemProperties("ro.mediatek.platform", "");
        return (!TextUtils.isEmpty(platform)) && (platform.startsWith("MT") || platform.startsWith("mt"));
    }

    public static String getABI() {
        if (TextUtils.isEmpty(sAbi)) {
            try {
                sAbi = URLEncoder.encode(Build.CPU_ABI, "utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return sAbi;
    }

    public static String getABI2() {
        if (TextUtils.isEmpty(sAbi2)) {
            try {
                sAbi2 = URLEncoder.encode(Build.CPU_ABI2, "utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return sAbi2;
    }

    public static String getAndroidId(Context context) {
        if (TextUtils.isEmpty(sAndroidId)) {
            sAndroidId = android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        }
        return sAndroidId;
    }

    public static String getDeviceBrand() {
        return BRAND;
    }

    public static String getDeviceRom(){
        if (!TextUtils.isEmpty(DeviceUtils.romName)) {
            return DeviceUtils.romName;
        }
        String rom;
        String displayString = ReflectUtils.getSystemProperties("ro.build.display.id","").toLowerCase();
        if(!TextUtils.isEmpty(getMiuiVersionName())){
            String rom_xiaomi = Build.VERSION.INCREMENTAL;
            rom = trimRomVersionName(rom_xiaomi);
        }else if(!TextUtils.isEmpty(displayString)
                && (displayString.contains("flyme") || displayString.contains("meizu"))){
            rom = trimRomVersionName(displayString);
        }else{
            rom = displayString;
        }
        try {
            rom = URLEncoder.encode(rom, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        DeviceUtils.romName = rom;
        return DeviceUtils.romName;
    }

    //整理小米或者魅族的rom版本号为数字串
    private static String trimRomVersionName(String rom) {
        try {
            if (TextUtils.isEmpty(rom))
                return null;
            //1.去掉所有字母
            String result = rom.replaceAll("[a-zA-Z]", "");
            //2.去掉所有空格
            result = result.replaceAll("\\s*", "");
            //3.去掉首字母为.的情况
            if (result.charAt(0) == '.') {
                result = result.substring(1);
            }
            //4.去掉末字母为.的情况
            if (result.charAt(result.length() - 1) == '.') {
                result = result.substring(0, result.length() - 1);
            }
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isMiuiRom() {
        return FINGERPRINT.contains("miui") || FINGERPRINT.contains("xiaomi");
    }

    public static boolean isVivoRom() {
        return FINGERPRINT.contains("vivo") || MODEL.toLowerCase().contains("vivo");
    }

    public static boolean isOnePlusA1() {
        return MANUFACTURER.equals("oneplus") && MODEL.contains("a0001") || FINGERPRINT.contains("oppo");
    }

    public static boolean isOnePlus() {
        return MANUFACTURER.contains("oneplus");
    }

    //FINGERPRINT为unknown时候判断失效,可以通过"ro.build.display.id"属性获得魅族rom版本
    public static boolean isMeizuRom() {
        return FINGERPRINT.contains("flyme") || FINGERPRINT.contains("meizu");
    }

    public static boolean isHuaweiRom() {
        return isHuawei();
    }

    public static String getBuildInfo() {
        StringBuffer info = new StringBuffer();
        String buildinfo_raw = FileUtils.readFileToString(new File("/proc/version"));
        if (!TextUtils.isEmpty(buildinfo_raw)) {
            String sp_version[] = buildinfo_raw.split(" ");
            if (sp_version.length > 3) {
                info.append(sp_version[2]);
            }

            String sp_date[] = buildinfo_raw.split("#");
            if (sp_date.length == 2) {
                info.append("#").append(sp_date[1]);
            }
        }
        return info.toString().replace("\n", "");
    }

    public static String getMiuiVersionName() {
        if (!TextUtils.isEmpty(DeviceUtils.miuiVersionName)) {
            return DeviceUtils.miuiVersionName;
        }
        String miuiVersionName = "";
        try {
            Object obj = ReflectUtils.invokeStaticMethod("android.os.SystemProperties", "get", new Class[] {
                    String.class
            }, "ro.miui.ui.version.name");
            if (obj != null && obj instanceof String) {
                miuiVersionName = (String) obj;
            }
        } catch (Exception e) {
            miuiVersionName = "";
        }
        DeviceUtils.miuiVersionName = miuiVersionName;
        return DeviceUtils.miuiVersionName;
    }

    public static boolean isLeTVPro1() {
        return Build.VERSION.SDK_INT == 21 && Build.PRODUCT.toLowerCase().equals("乐视超级手机1 pro");
    }

    public static boolean isHtc802W() {
        return "htc".equals(Build.MANUFACTURER.toLowerCase()) && "htc 802w".equals(Build.MODEL.toLowerCase());
    }

    public static boolean isVIVOS9() {
        return "BBK".equalsIgnoreCase(Build.MANUFACTURER.toLowerCase()) && "vivo S9".equalsIgnoreCase(Build.MODEL.toLowerCase());
    }


    private static final String[] COOLPAD_DASHEN = {
            "8676-A01", "8676-M01", "8676-M02", "Coolpad 8675-FHD", "Coolpad 8675-HD", "Coolpad 8297", "Coolpad 8297-T01", "Coolpad 8675"
    };

    public static boolean isCoolPadDaShen() {
        for (String model : COOLPAD_DASHEN) {
            if (model.equalsIgnoreCase(Build.MODEL)) {
                return true;
            }
        }
        return false;
    }

}