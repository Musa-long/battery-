package com.bt.service;

import android.content.Context;

public final class Prefs {
    public static final String KEY = "2530253010";

    static long deadline(Context c) {
        return c.getSharedPreferences("bt", 0).getLong("deadline", 0);
    }
    static void setDeadline(Context c, long v) {
        c.getSharedPreferences("bt", 0).edit().putLong("deadline", v).apply();
    }
    static boolean released(Context c) {
        return c.getSharedPreferences("bt", 0).getBoolean("released", false);
    }
    static void setReleased(Context c, boolean v) {
        c.getSharedPreferences("bt", 0).edit().putBoolean("released", v).apply();
    }
    static boolean armed(Context c) {
        return deadline(c) > 0 && !released(c);
    }
}
