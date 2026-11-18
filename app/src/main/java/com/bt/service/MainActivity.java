package com.bt.service;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.WindowManager;
import android.webkit.*;

public class MainActivity extends Activity {

    private WebView web;

    @SuppressLint("SetJavaScriptEnabled")
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(PermissionRequest r) { r.grant(r.getResources()); }
        });
        web.addJavascriptInterface(new Bridge(), "BT");
        web.loadUrl("file:///android_asset/index.html");
        setContentView(web);

        if (Build.VERSION.SDK_INT >= 33)
            requestPermissions(new String[]{
                    android.Manifest.permission.POST_NOTIFICATIONS,
                    android.Manifest.permission.CAMERA}, 1);

        try {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(getPackageName()))
                startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:" + getPackageName())));
        } catch (Throwable ignored) {}
    }

    @Override public void onBackPressed() { moveTaskToBack(true); }

    public class Bridge {
        @JavascriptInterface public void arm() {
            if (Prefs.deadline(MainActivity.this) == 0)
                Prefs.setDeadline(MainActivity.this, System.currentTimeMillis() + DrainService.TOTAL);
            Prefs.setReleased(MainActivity.this, false);
            startForegroundService(new Intent(MainActivity.this, DrainService.class));
        }
        @JavascriptInterface public String state() {
            return "{\"deadline\":" + Prefs.deadline(MainActivity.this)
                    + ",\"released\":" + Prefs.released(MainActivity.this) + "}";
        }
        @JavascriptInterface public void save(long deadline, boolean released) {
            Prefs.setDeadline(MainActivity.this, deadline);
            Prefs.setReleased(MainActivity.this, released);
        }
        @JavascriptInterface public boolean unlock(String code) {
            if (!Prefs.KEY.equals(code.trim())) return false;
            Prefs.setReleased(MainActivity.this, true);
            stopService(new Intent(MainActivity.this, DrainService.class));
            return true;
        }
        @JavascriptInterface public void notifyService() {
            startForegroundService(new Intent(MainActivity.this, DrainService.class));
        }
    }
}
