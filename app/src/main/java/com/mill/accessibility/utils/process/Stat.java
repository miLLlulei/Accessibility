package com.mill.accessibility.utils.process;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.IOException;

/**
 * Created by lizhenzhen on 2016-05-25.
 */
public final class Stat extends ProcFile {
    private final String[] fields;
    public static final Parcelable.Creator<Stat> CREATOR = new Parcelable.Creator() {
        public Stat createFromParcel(Parcel source) {
            return new Stat(source);
        }

        public Stat[] newArray(int size) {
            return new Stat[size];
        }
    };

    public static Stat get(int pid) throws IOException {
        return new Stat(String.format("/proc/%d/stat", new Object[]{Integer.valueOf(pid)}));
    }

    private Stat(String path) throws IOException {
        super(path);
        this.fields = this.content.split("\\s+");
    }

    private Stat(Parcel in) {
        super(in);
        this.fields = in.createStringArray();
    }

    public int getPid() {
        return Integer.parseInt(this.fields[0]);
    }

    public String getComm() {
        if (fields != null && fields.length >= 2) {
            return this.fields[1].replace("(", "").replace(")", "");
        }
        return "";
    }

    public char state() {
        return this.fields[2].charAt(0);
    }

    public int policy() {
        return Integer.parseInt(this.fields[40]);
    }

    public int session() {
        return Integer.parseInt(this.fields[5]);
    }

    public long priority() {
        return Long.parseLong(this.fields[17]);
    }

    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeStringArray(this.fields);
    }
}
