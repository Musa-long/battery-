package com.bt.service;

import android.content.*;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context ctx, Intent intent) {
        if (Prefs.armed(ctx))
            ctx.startForegroundService(new Intent(ctx, DrainService.class));
    }
}
