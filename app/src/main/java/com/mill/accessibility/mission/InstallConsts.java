package com.mill.accessibility.mission;

public class InstallConsts {

    public class InstallDelegateManagerStatus {
        public static final int PROCESS_BEGIN = 100; // 开始处理
        public static final int PROCESS_CONTINUE = 101; //
        public static final int PROCESS_END = 102; // 结束
    }

    public class InstallStatus {

        public static final int STATUS_NOT_START = -1;
        public static final int STATUS_START = 1;
        public static final int STATUS_INSTALLING = 2;
        public static final int STATUS_SUCESS = 200;
        public static final int STATUS_Fail = 500;

        public static final int INSTALL_WAITING = 201;
        public static final int INSTALL_INSTALLING = 202;
        public static final int INSTALL_FILE_FAILED = 203;
        public static final int INSTALL_AUTODOWNLOAD = 204;
        public static final int INSTALL_FINISH = 205;
        public static final int INSTALL_SILENTINSTALL_SUCCESS = 206;
        public static final int INSTALL_SPACE_NOT_ENOUGH = 207;
        public static final int INSTALL_SILENTINSTALL_FAILED = 208;
        public static final int INSTALL_ONLY_SILENTINSTALL = 209;
        public static final int INSTALL_PROCESS_END = 210; // 结束
        public static final int INSTALL_FAILED_VERSION_DOWNGRADE = 211;
        public static final int INSTALL_INSTALLED_AND_UNINSTALLED = 230; //安装了又卸载了
    }

    public class UninstallStatus{
        public static final int STATUS_SILENT_SUCCESS = 0;
        public static final int STATUS_NORMAL_UNINSTALL = 1;
        public static final int STATUS_ONLY_SLIENTLY_UNINSTALL_FAILED=2;
        public static final int STATUS_SYSTEM_APP_SILENT_SUCCESS=3;
    }

    public class InstallType {
        public static final int ALLOW_SLIENT_INSTALL = 1;
        public static final int ONLY_SILENT_INSTALL = 2;

        public static final int UPDATE_NONE = 0;
        public static final int UPDATE_INSTALL = 1;
        public static final int FIRST_INSTALL = 2;
    }

    public class MissionType {
        public static final int MissionTypeInstall = 0;
        public static final int MissionTypeUninstall = 1;
        public static final int MissionTypeRestore = 2;
        public static final int MissionTypeDisable = 3;
        public static final int MissionTypeForceStop = 4;
        public static final int MissionTypeEnable = 5;
    }

    private static long setInstallTypeImp(long installType, boolean allow, int flag) {
        if (allow) {
            installType |= flag;
        } else {
            installType &= ~flag;
        }
        return installType;
    }

    public static boolean isInstallSuccess(int installType){
        return installType == InstallStatus.STATUS_SUCESS
                || installType == InstallStatus.INSTALL_FINISH
                || installType == InstallStatus.INSTALL_SILENTINSTALL_SUCCESS;
    }

    public static boolean isAllowSlientInstall(long installType) {
        return (installType & InstallConsts.InstallType.ALLOW_SLIENT_INSTALL) != 0;
    }

    public static long setAllowSlientInstall(long installType, boolean val) {
        return setInstallTypeImp(installType, val, InstallConsts.InstallType.ALLOW_SLIENT_INSTALL);
    }

    public static boolean isOnlySilentInstall(long installType) {
        return (installType & InstallConsts.InstallType.ONLY_SILENT_INSTALL) != 0;
    }

    public static long setOnlySilentInstall(long installType, boolean val) {
        return setInstallTypeImp(installType, val, InstallConsts.InstallType.ONLY_SILENT_INSTALL);
    }


    public class InstallErrCode {
        // 静默安装失败错误码, 1000 - 2000
        public static final int INSTALL_SUCCESS = 1000;
        public static final int INSTALL_FAILED_ALREADY_EXISTS = 1000 + 1;
        public static final int INSTALL_FAILED_INVALID_APK = 1000 + 2;
        public static final int INSTALL_FAILED_INSUFFICIENT_STORAGE = 1000 + 3;
        public static final int INSTALL_FAILED_DUPLICATE_PACKAGE = 1000 + 4;
        public static final int INSTALL_FAILED_UPDATE_INCOMPATIBLE = 1000 + 5;
        public static final int INSTALL_FAILED_OLDER_SDK = 1000 + 6;
        public static final int INSTALL_FAILED_NEWER_SDK = 1000 + 7;
        public static final int INSTALL_FAILED_MISSING_FEATURE = 1000 + 8;
        public static final int INSTALL_FAILED_MEDIA_UNAVAILABLE = 1000 + 9;
        public static final int INSTALL_PARSE_FAILED_NOT_APK = 1000 + 10;
        public static final int INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES = 1000 + 11;
        // public static final int device not found = 1000 + 12;
        public static final int SPACE_LEFT_ON = 1000 + 13;
        public static final int INSTALL_FAILED_NO_SHARED_USER = 1000 + 14;
        public static final int INSTALL_FAILED_SHARED_USER_INCOMPATIBLE = 1000 + 15;
        public static final int INSTALL_FAILED_MISSING_SHARED_LIBRARY = 1000 + 16;
        public static final int INSTALL_FAILED_REPLACE_COULDNT_DELETE = 1000 + 17;
        public static final int INSTALL_FAILED_DEXOPT = 1000 + 18;
        public static final int INSTALL_FAILED_CONFLICTING_PROVIDER = 1000 + 19;
        public static final int INSTALL_FAILED_TEST_ONLY = 1000 + 20;
        public static final int INSTALL_FAILED_CPU_ABI_INCOMPATIBLE = 1000 + 21;
        public static final int INSTALL_FAILED_CONTAINER_ERROR = 1000 + 22;
        public static final int INSTALL_FAILED_INVALID_INSTALL_LOCATION = 1000 + 23;
        public static final int INSTALL_PARSE_FAILED_BAD_MANIFEST = 1000 + 24;
        public static final int INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION = 1000 + 25;
        public static final int INSTALL_PARSE_FAILED_NO_CERTIFICATES = 1000 + 26;
        public static final int INSTALL_PARSE_FAILED_CERTIFICATE_ENCODING = 1000 + 27;
        public static final int INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME = 1000 + 28;
        public static final int INSTALL_PARSE_FAILED_BAD_SHARED_USER_ID = 1000 + 29;
        public static final int INSTALL_PARSE_FAILED_MANIFEST_MALFORMED = 1000 + 30;
        public static final int INSTALL_PARSE_FAILED_MANIFEST_EMPTY = 1000 + 31;
        public static final int INSTALL_FAILED_INTERNAL_ERROR = 1000 + 32;
    }
}
