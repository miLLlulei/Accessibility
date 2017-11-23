package com.mill.accessibility.utils.process;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.IOException;

/**
 * Created by lizhenzhen on 2016-05-25.
 */
public final class Status extends ProcFile {
    public static final Parcelable.Creator<Status> CREATOR = new Parcelable.Creator() {
        public Status createFromParcel(Parcel source) {
            return new Status(source);
        }

        public Status[] newArray(int size) {
            return new Status[size];
        }
    };

    public static Status get(int pid) throws IOException {
        return new Status(String.format("/proc/%d/status", new Object[]{Integer.valueOf(pid)}));
    }

    private Status(String path) throws IOException {
        super(path);
    }

    private Status(Parcel in) {
        super(in);
    }

    public String getValue(String fieldName) {
        String[] lines = this.content.split("\n");
        String[] var3 = lines;
        int var4 = lines.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            String line = var3[var5];
            if(line.startsWith(fieldName + ":")) {
                return line.split(fieldName + ":")[1].trim();
            }
        }

        return null;
    }

    public int getUid() {
        try {
            return Integer.parseInt(this.getValue("Uid").split("\\s+")[0]);
        } catch (Exception var2) {
            return -1;
        }
    }

    public int getGid() {
        try {
            return Integer.parseInt(this.getValue("Gid").split("\\s+")[0]);
        } catch (Exception var2) {
            return -1;
        }
    }
}
