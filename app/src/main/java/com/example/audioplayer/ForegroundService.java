package com.example.audioplayer;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class ForegroundService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = NotificationManager.getNotification();
        if (notification != null) {
            startForeground(1, notification);
            Log.d("ForegroundService", "Notification started");
        } else {
            Log.d("ForegroundService", "Notification is null");
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // We don't use binding
    }
}