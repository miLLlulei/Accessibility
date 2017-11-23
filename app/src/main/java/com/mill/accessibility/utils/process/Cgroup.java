package com.mill.accessibility.utils.process;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by lizhenzhen on 2016-05-25.
 */
public final class Cgroup extends ProcFile {
    public final ArrayList<ControlGroup> groups;
    public static final Parcelable.Creator<Cgroup> CREATOR = new Parcelable.Creator() {
        public Cgroup createFromParcel(Parcel source) {
            return new Cgroup(source);
        }

        public Cgroup[] newArray(int size) {
            return new Cgroup[size];
        }
    };

    public static Cgroup get(int pid) throws IOException {
        return new Cgroup(String.format("/proc/%d/cgroup", new Object[]{Integer.valueOf(pid)}));
    }

    private Cgroup(String path) throws IOException {
        super(path);
        String[] lines = this.content.split("\n");
        this.groups = new ArrayList();
        String[] var3 = lines;
        int var4 = lines.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            String line = var3[var5];

            try {
                this.groups.add(new ControlGroup(line));
            } catch (Exception var8) {
                ;
            }
        }

    }

    private Cgroup(Parcel in) {
        super(in);
        this.groups = in.createTypedArrayList(ControlGroup.CREATOR);
    }

    public ControlGroup getGroup(String subsystem) {
        Iterator var2 = this.groups.iterator();

        while(var2.hasNext()) {
            ControlGroup group = (ControlGroup)var2.next();
            String[] systems = group.subsystems.split(",");
            String[] var5 = systems;
            int var6 = systems.length;

            for(int var7 = 0; var7 < var6; ++var7) {
                String name = var5[var7];
                if(name.equals(subsystem)) {
                    return group;
                }
            }
        }

        return null;
    }

    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeTypedList(this.groups);
    }
}
