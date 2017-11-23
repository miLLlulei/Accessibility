package com.mill.accessibility.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 */
public class Mntent {

    private static final String FILE_MOUNTS = "/proc/mounts";
	private static final boolean DEBUG = false;
    private static final String TAG = "AppManger";

    /**
     * 
     */
    private Mntent() {
    }

    public final static class mntent {
        public String mnt_fsname;
        public String mnt_dir;
        public String mnt_type;
        public String mnt_opts;
        public int mnt_freq;
        public int mnt_passno;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(mntent.class.getSimpleName());
            sb.append("[");
            sb.append("mnt_fsname:").append(mnt_fsname);
            sb.append(",mnt_dir:").append(mnt_dir);
            sb.append(",mnt_type:").append(mnt_type);
            sb.append(",mnt_opts:").append(mnt_opts);
            sb.append(",mnt_freq:").append(mnt_freq);
            sb.append(",mnt_passno:").append(mnt_passno);
            sb.append("]");
            return sb.toString();
        }
    }

    public static List<mntent> queryAll() {
        List<mntent> mntents = new ArrayList<mntent>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(FILE_MOUNTS));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }
                int indexOf = line.indexOf('#');
                if (indexOf >= 0) {
                    line = line.substring(0, indexOf);
                }
                StringTokenizer tokenizer = new StringTokenizer(line);
                if (tokenizer.countTokens() != 6) {
                    continue;
                }
                mntent m = new mntent();
                m.mnt_fsname = tokenizer.nextToken();
                m.mnt_dir = tokenizer.nextToken();
                m.mnt_type = tokenizer.nextToken();
                m.mnt_opts = tokenizer.nextToken();
                try {
                    m.mnt_freq = Integer.parseInt(tokenizer.nextToken());
                } catch (Exception e) {
                }
                try {
                    m.mnt_passno = Integer.parseInt(tokenizer.nextToken());
                } catch (Exception e) {
                }
                mntents.add(m);
            }
        } catch (Exception e) {
			if (DEBUG) {
				Log.e(TAG, "queryAll", e);
			}
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                }
            }
        }
        return mntents;
    }

    public static mntent queryByDir(String dir) {
        BufferedReader reader = null;
		mntent bestM = null;
        try {
            reader = new BufferedReader(new FileReader(FILE_MOUNTS));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }
                int indexOf = line.indexOf('#');
                if (indexOf >= 0) {
                    line = line.substring(0, indexOf);
                }
                StringTokenizer tokenizer = new StringTokenizer(line);
                if (tokenizer.countTokens() != 6) {
                    continue;
                }
                mntent m = new mntent();
                m.mnt_fsname = tokenizer.nextToken();
                m.mnt_dir = tokenizer.nextToken();
                m.mnt_type = tokenizer.nextToken();
                m.mnt_opts = tokenizer.nextToken();
                try {
                    m.mnt_freq = Integer.parseInt(tokenizer.nextToken());
                } catch (Exception e) {
                }
                try {
                    m.mnt_passno = Integer.parseInt(tokenizer.nextToken());
                } catch (Exception e) {
                }
				if (dir.startsWith(m.mnt_dir)) {
					if (bestM == null
							|| bestM.mnt_dir.length() < m.mnt_dir.length()) {
						bestM = m;
					}
                }
            }
        } catch (Exception e) {
			if (DEBUG) {
				Log.e(TAG, "queryByDir", e);
			}
			bestM = null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                }
            }
        }
		return bestM;
    }
}
