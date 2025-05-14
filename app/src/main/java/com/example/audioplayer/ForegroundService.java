package com.example.audioplayer;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;


public class ForegroundService extends Service {

    private static Notification notification;

    @Override
    public void onCreate() {
        super.onCreate();
        PlayerManager.getPlayer(getApplicationContext());
    }

    public static Notification getNotification() {
        return notification;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        notification = NotificationManager.getNotification();

        if (notification != null) {
            startForeground(1, notification);
        } else {
            stopSelf(); //Stop if notification is not ready
        }

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PlayerManager.release();
    }
}
