package com.mill.accessibility.utils.process;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by lizhenzhen on 2016-05-25.
 */
public class ProcFile extends File implements Parcelable {
    public final String content;
    public static final Creator<ProcFile> CREATOR = new Creator() {
        public ProcFile createFromParcel(Parcel in) {
            return new ProcFile(in);
        }

        public ProcFile[] newArray(int size) {
            return new ProcFile[size];
        }
    };

    public static String readFile(String path) throws IOException {
        BufferedReader reader = null;

        try {
            StringBuilder output = new StringBuilder();
            reader = new BufferedReader(new FileReader(path));
            String line = reader.readLine();

            for(String newLine = ""; line != null && !line.trim().equals(""); line = reader.readLine()) {
                output.append(newLine).append(line);
                newLine = "\n";
            }

            line = output.toString();
            return line;
        } finally {
            if(reader != null) {
                reader.close();
            }

        }

    }

    protected ProcFile(String path) throws IOException {
        super(path);
        this.content = readFile(path);
    }

    protected ProcFile(Parcel in) {
        super(in.readString());
        this.content = in.readString();
    }

    public long length() {
        return (long)this.content.length();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.getAbsolutePath());
        dest.writeString(this.content);
    }
}
