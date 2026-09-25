package com.canlinavigasyon.app;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_REQUEST = 1001;
    private static final int NOTIFICATION_REQUEST = 1002;
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setGeolocationEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(
                    String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }
        });

        webView.addJavascriptInterface(new AndroidNavBridge(), "AndroidNav");
        webView.loadUrl("file:///android_asset/index.html");

        requestPermissionsIfNeeded();
    }

    private void requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean fine = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;

            if (!fine) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        },
                        LOCATION_REQUEST
                );
            }
        }

        if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_REQUEST
            );
        }
    }

    public class AndroidNavBridge {

        @JavascriptInterface
        public void startService() {
            Intent intent = new Intent(MainActivity.this, NavigationService.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(MainActivity.this, intent);
            } else {
                startService(intent);
            }
        }

        @JavascriptInterface
        public void stopService() {
            stopService(new Intent(MainActivity.this, NavigationService.class));
        }

        @JavascriptInterface
        public void saveHazards(String json) {
            SharedPreferences prefs = getSharedPreferences("nav", MODE_PRIVATE);
            prefs.edit().putString("hazards", json).apply();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
