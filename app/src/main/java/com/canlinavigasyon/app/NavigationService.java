package com.canlinavigasyon.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NavigationService extends Service {

    private static final String CHANNEL_ID = "canli_navigasyon";
    private static final int NOTIFICATION_ID = 77;

    private TextToSpeech tts;
    private Location lastLocation;
    private final Map<String, Long> lastSpeech = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification("GPS arka plan hizmeti aktif"));

        tts = new TextToSpeech(getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("tr", "TR"));
                tts.setSpeechRate(1.0f);
            }
        });

        requestLocationUpdates();
    }

    private void requestLocationUpdates() {
        android.location.LocationManager lm =
                (android.location.LocationManager) getSystemService(LOCATION_SERVICE);

        try {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return;
            }

            lm.requestLocationUpdates(
                    android.location.LocationManager.GPS_PROVIDER,
                    1000L,
                    2.0f,
                    location -> {
                        lastLocation = location;
                        checkHazards(location);
                    }
            );
        } catch (Exception ignored) {
        }
    }

    private void checkHazards(Location current) {
        SharedPreferences prefs = getSharedPreferences("nav", MODE_PRIVATE);
        String json = prefs.getString("hazards", "[]");

        Pattern p = Pattern.compile(
                "\\{[^{}]*?lat\\s*:\\s*([-0-9.]+)[^{}]*?lng\\s*:\\s*([-0-9.]+)[^{}]*?(?:type|tur)\\s*:\\s*[\\\"']([^\\\"']+)[\\\"'][^{}]*?\\}",
                Pattern.CASE_INSENSITIVE
        );

        Matcher m = p.matcher(json);
        while (m.find()) {
            try {
                double lat = Double.parseDouble(m.group(1));
                double lng = Double.parseDouble(m.group(2));
                String type = m.group(3);

                float[] result = new float[1];
                Location.distanceBetween(
                        current.getLatitude(), current.getLongitude(),
                        lat, lng, result
                );

                float distance = result[0];
                float speed = Math.max(0f, current.getSpeed());
                float dynamicMax = Math.max(300f, speed * 8f);

                if (distance <= dynamicMax && distance >= 5f) {
                    String key = lat + "," + lng;
                    long now = System.currentTimeMillis();
                    long last = lastSpeech.getOrDefault(key, 0L);

                    if (now - last > 30000L) {
                        String rounded = String.valueOf(Math.round(distance));
                        speak("Dikkat! " + rounded + " metre ileride " + type + ".");
                        lastSpeech.put(key, now);
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void speak(String text) {
        if (tts != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nav_warning");
            } else {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }

    private Notification buildNotification(String text) {
        Intent launchIntent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(
                this, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT |
                (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("🧭 Canlı Navigasyon")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_map)
                .setOngoing(true)
                .setContentIntent(pi)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Canlı Navigasyon",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Arka plan GPS navigasyon hizmeti");
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        android.location.LocationManager lm =
                (android.location.LocationManager) getSystemService(LOCATION_SERVICE);

        try {
            lm.removeUpdates(location -> {});
        } catch (Exception ignored) {
        }

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
