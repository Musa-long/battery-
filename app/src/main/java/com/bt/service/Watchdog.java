package com.bt.service;

import android.app.*;
import android.content.*;

public class Watchdog extends BroadcastReceiver {
    private static final int REQ = 7717;

    static void schedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = PendingIntent.getBroadcast(c, REQ,
                new Intent(c, Watchdog.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 15 * 60 * 1000, pi);
    }

    @Override public void onReceive(Context ctx, Intent i) {
        if (Prefs.armed(ctx)) {
            ctx.startForegroundService(new Intent(ctx, DrainService.class));
            schedule(ctx);
        }
    }
}
