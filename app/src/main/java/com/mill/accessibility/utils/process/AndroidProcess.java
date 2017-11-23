package com.mill.accessibility.utils.process;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.mill.accessibility.utils.ConvertUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by lizhenzhen on 2016-05-25.
 */
public class AndroidProcess implements Parcelable {
    public final String name;
    public final int pid;
    public static final Creator<AndroidProcess> CREATOR = new Creator() {
        public AndroidProcess createFromParcel(Parcel source) {
            return new AndroidProcess(source);
        }

        public AndroidProcess[] newArray(int size) {
            return new AndroidProcess[size];
        }
    };

    static String getProcessName(int pid) throws IOException {
        String cmdline = null;

        try {
            cmdline = ProcFile.readFile(String.format("/proc/%d/cmdline", new Object[]{Integer.valueOf(pid)})).trim();
        } catch (IOException var3) {
            ;
        }

        return TextUtils.isEmpty(cmdline)? Stat.get(pid).getComm():cmdline;
    }

    public AndroidProcess(int pid) throws IOException {
        this.pid = pid;
        this.name = getProcessName(pid);
    }

    public String read(String filename) throws IOException {
        if (filename.equals("oom_adj")) {
            File adjFile = new File(String.format("/proc/%d/%s", new Object[]{Integer.valueOf(this.pid), filename}));
            if (adjFile.canRead()) {
                return ProcFile.readFile(String.format("/proc/%d/%s", new Object[]{Integer.valueOf(this.pid), filename}));
            }
            else {
                return "-2";
            }
        }
        return ProcFile.readFile(String.format("/proc/%d/%s", new Object[]{Integer.valueOf(this.pid), filename}));
    }

    public Cgroup cgroup() throws IOException {
        return Cgroup.get(this.pid);
    }

    public Stat stat() throws IOException {
        return Stat.get(this.pid);
    }

    public Status status() throws IOException {
        return Status.get(this.pid);
    }

    public int oom_score() throws IOException {
        try {
            return Integer.parseInt(this.read("oom_score"));
        }catch (NumberFormatException e) {
            return -1000;
        }
    }

    public int oom_adj() throws IOException {
        return Integer.parseInt(this.read("oom_adj"));
    }

    public int oom_score_adj() throws IOException {
        return ConvertUtils.string2Int(read("oom_score_adj"));
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeInt(this.pid);
    }

    protected AndroidProcess(Parcel in) {
        this.name = in.readString();
        this.pid = in.readInt();
    }
}
