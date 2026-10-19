package com.example.audioplayer;

import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

public class ForegroundService extends Service {

    static final String ACTION_PLAY_PAUSE = "com.example.audioplayer.action.PLAY_PAUSE";
    static final String ACTION_NEXT = "com.example.audioplayer.action.NEXT";
    static final String ACTION_PREVIOUS = "com.example.audioplayer.action.PREVIOUS";

    private ExoPlayer boundPlayer;
    private boolean isForeground = false;

    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
            updateForegroundState();
        }

        @Override
        public void onPlaybackStateChanged(int playbackState) {
            updateForegroundState();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        boundPlayer = PlayerManager.getPlayer(getApplicationContext());
        boundPlayer.addListener(playerListener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                    PlaybackNotificationManager.NOTIFICATION_ID,
                    PlayerManager.buildNotification(this),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        }
        isForeground = true;

        String action = intent != null ? intent.getAction() : null;
        if (ACTION_PLAY_PAUSE.equals(action)) {
            PlayerManager.togglePlayPause();
        } else if (ACTION_NEXT.equals(action)) {
            PlayerManager.skipToNext();
        } else if (ACTION_PREVIOUS.equals(action)) {
            PlayerManager.skipToPrevious();
        }

        updateForegroundState();
        return START_NOT_STICKY;
    }

    private void updateForegroundState() {
        if (!isForeground || boundPlayer == null) return;

        int state = boundPlayer.getPlaybackState();
        boolean active = boundPlayer.getPlayWhenReady()
                && state != Player.STATE_IDLE
                && state != Player.STATE_ENDED;

        if (!active) {
            boolean queueEmpty = boundPlayer.getCurrentMediaItem() == null;
            stopForeground(queueEmpty ? STOP_FOREGROUND_REMOVE : STOP_FOREGROUND_DETACH);
            isForeground = false;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (boundPlayer != null) {
            boundPlayer.removeListener(playerListener);
            boundPlayer = null;
        }
        isForeground = false;
    }
}