package com.mill.accessibility.utils.process;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Parcel;

import java.io.File;
import java.io.IOException;

/**
 * Created by lizhenzhen on 2016-05-25.
 */
public class AndroidAppProcess extends AndroidProcess {
    private static final boolean SYS_SUPPORTS_SCHEDGROUPS = (new File("/dev/cpuctl/tasks")).exists();
    public final boolean foreground;
    public final int uid;
    public static final Creator<AndroidAppProcess> CREATOR = new Creator() {
        public AndroidAppProcess createFromParcel(Parcel source) {
            return new AndroidAppProcess(source);
        }

        public AndroidAppProcess[] newArray(int size) {
            return new AndroidAppProcess[size];
        }
    };

    public AndroidAppProcess(int pid) throws IOException, AndroidAppProcess.NotAndroidAppProcessException {
        super(pid);
        boolean foreground;
        int uid;
        if(SYS_SUPPORTS_SCHEDGROUPS) {
            Cgroup stat1 = this.cgroup();
            ControlGroup status1 = stat1.getGroup("cpuacct");
            ControlGroup cpu = stat1.getGroup("cpu");
            if(Build.VERSION.SDK_INT >= 21) {
                if(cpu == null || status1 == null || !status1.group.contains("pid_")) {
                    throw new AndroidAppProcess.NotAndroidAppProcessException(pid);
                }

                foreground = !cpu.group.contains("bg_non_interactive");

                try {
                    uid = Integer.parseInt(status1.group.split("/")[1].replace("uid_", ""));
                } catch (Exception var9) {
                    uid = this.status().getUid();
                }
            } else {
                if(cpu == null || status1 == null || !cpu.group.contains("apps")) {
                    throw new AndroidAppProcess.NotAndroidAppProcessException(pid);
                }

                foreground = !cpu.group.contains("bg_non_interactive");

                try {
                    uid = Integer.parseInt(status1.group.substring(status1.group.lastIndexOf("/") + 1));
                } catch (Exception var8) {
                    uid = this.status().getUid();
                }
            }
        } else {
            if(this.name.startsWith("/") || !(new File("/data/data", this.getPackageName())).exists()) {
                throw new AndroidAppProcess.NotAndroidAppProcessException(pid);
            }

            Stat stat = this.stat();
            Status status = this.status();
            foreground = stat.policy() == 0;
            uid = status.getUid();
        }

        this.foreground = foreground;
        this.uid = uid;
    }

    public String getPackageName() {
        return this.name.split(":")[0];
    }

    public PackageInfo getPackageInfo(Context context, int flags) throws PackageManager.NameNotFoundException {
        return context.getPackageManager().getPackageInfo(this.getPackageName(), flags);
    }

    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeByte((byte)(this.foreground?1:0));
        dest.writeInt(this.uid);
    }

    protected AndroidAppProcess(Parcel in) {
        super(in);
        this.foreground = in.readByte() != 0;
        this.uid = in.readInt();
    }

    public static final class NotAndroidAppProcessException extends Exception {
        public NotAndroidAppProcessException(int pid) {
            super(String.format("The process %d does not belong to any application", new Object[]{Integer.valueOf(pid)}));
        }
    }
}
