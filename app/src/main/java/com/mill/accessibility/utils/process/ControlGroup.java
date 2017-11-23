package com.mill.accessibility.utils.process;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by lizhenzhen on 2016-05-25.
 */
public class ControlGroup implements Parcelable {
    public final int id;
    public final String subsystems;
    public final String group;
    public static final Creator<ControlGroup> CREATOR = new Creator() {
        public ControlGroup createFromParcel(Parcel source) {
            return new ControlGroup(source);
        }

        public ControlGroup[] newArray(int size) {
            return new ControlGroup[size];
        }
    };

    protected ControlGroup(String line) throws NumberFormatException, IndexOutOfBoundsException {
        String[] fields = line.split(":");
        this.id = Integer.parseInt(fields[0]);
        this.subsystems = fields[1];
        this.group = fields[2];
    }

    protected ControlGroup(Parcel in) {
        this.id = in.readInt();
        this.subsystems = in.readString();
        this.group = in.readString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeString(this.subsystems);
        dest.writeString(this.group);
    }

    public String toString() {
        return String.format("%d:%s:%s", new Object[]{Integer.valueOf(this.id), this.subsystems, this.group});
    }
}
