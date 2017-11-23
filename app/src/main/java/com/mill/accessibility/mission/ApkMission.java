package com.mill.accessibility.mission;

import android.content.Context;

/**
 */
public abstract class ApkMission implements Runnable, Comparable<ApkMission> {
    public static final int PRIORITY_LOW = 1;
    public static final int PRIORITY_NORMAL = 2;
    public static final int PRIORITY_HIGH = 3;
    protected StatusCallback mCallback;
    protected Context mContext;
    protected int missionType;
    private int priority;

    public interface StatusCallback {
        void callback(Object object, int type, int status);
    }

    public ApkMission(int priority) {
        this.priority = priority;
    }

    protected void changeStatus(Object mInfo, int status) {
        if (mCallback != null) {
            mCallback.callback(mInfo, missionType, status);
        }
    }

    public void setStatusCallback(StatusCallback callback) {
        this.mCallback = callback;
    }

    public int getMissionType(){
        return missionType;
    }

    @Override
    public int compareTo(ApkMission o) {
        return this.priority < o.priority ? 1 : this.priority > o.priority ? -1 : 0; // 数字大，优先级高
//        return this.priority > o.priority ? 1 : this.priority < o.priority ? -1 : 0; // 数字小，优先级高
    }

    @Override
    public abstract void run();
}
