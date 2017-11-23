package com.mill.accessibility.utils;

import android.os.SystemClock;

import java.util.Random;

public class RandomUtils {
    private static final Random sRandom = new Random(SystemClock.uptimeMillis());

    public static Random getRandom() {
        return sRandom;
    }

    public static int getRandomInt(int n) {
        return RandomUtils.getRandom().nextInt(n);
    }

    public static String getRandomString(int len) {
        String ret = "";
        for (int n = 0; n < len; n++) {
            int randInt = getRandomInt(16);
            char ch = (char) (randInt % 2 == 1 ? 'A' : 'a' + randInt);
            ret = ret + ch;
        }
        return ret;
    }

    public static String getRandomPath() {
        return getRandomString(getRandomInt(16) + 5);
    }
}


