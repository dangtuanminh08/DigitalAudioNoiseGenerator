package com.example.audioplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.app.NotificationCompat.MediaStyle;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;

import java.util.List;

public class NotificationManager {
    private final Context context;
    private static ExoPlayer player;
    private static MediaSessionCompat mediaSession;
    private final List<String> queue;

    private final Bitmap largeIcon;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final int NOTIFICATION_ID = 1;
    private final String CHANNEL_ID = "media_playback_channel";

    private static Notification notification;


    private final Runnable updateProgress = new Runnable() {
        @Override
        public void run() {
            if (player.isPlaying()) {
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
                handler.postDelayed(this, 1000);
            }
        }
    };

    private static final PlaybackStateCompat.CustomAction releaseAction = new PlaybackStateCompat.CustomAction.Builder("ACTION_RELEASE", "Release", R.drawable.exit)
            .build();

    public NotificationManager(Context context, MediaSessionCompat mediaSession, ExoPlayer player, List<String> queue) {
        this.context = context.getApplicationContext();
        NotificationManager.mediaSession = mediaSession;
        NotificationManager.player = player;
        this.queue = queue;
        this.largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.notification_image);
    }

    public void createNotification() {
        createNotificationChannel();
        mediaSession.setCallback(new MediaSessionCallback());
        mediaSession.setActive(true);
        updatePlaybackState(player.isPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "DANG! Playback",
                    android.app.NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Audio Player Channel");
            android.app.NotificationManager notificationManager =
                    (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @UnstableApi
    public void showNotification(String title, String artist) {
        if (context == null || mediaSession == null || player == null) return;

        PendingIntent playPauseIntent = createPendingIntent("ACTION_PLAY_PAUSE");
        PendingIntent nextIntent = createPendingIntent("ACTION_NEXT");
        PendingIntent prevIntent = createPendingIntent("ACTION_PREVIOUS");
        PendingIntent releaseIntent = createPendingIntent("ACTION_RELEASE");

        String playPauseLabel = player.isPlaying() ? "Pause" : "Play";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setLargeIcon(largeIcon)
                .setContentTitle(title)
                .setContentText(artist)
                .setStyle(new MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2, 3))
                .addAction(R.drawable.pause, playPauseLabel, playPauseIntent)
                .addAction(R.drawable.prev_song, "Previous", prevIntent)
                .addAction(R.drawable.next_song, "Next", nextIntent)
                .addAction(androidx.media3.session.R.drawable.media3_icon_block, "Release", releaseIntent)
                .setOngoing(true);

        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, player.getDuration())
                .build());

        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
        }

        notification = builder.build();
    }

    public static Notification getNotification() {
        return notification;
    }

    @OptIn(markerClass = UnstableApi.class)
    private PendingIntent createPendingIntent(String action) {
        Intent intent = new Intent(context, PlayerActivity.class);
        intent.setAction(action);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void updatePlaybackState(int state) {
        long pos = player.getCurrentPosition();
        long bufferPos = player.getBufferedPosition();
        float speed = state == PlaybackStateCompat.STATE_PLAYING ? 1f : 0f;

        PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY_PAUSE |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                PlaybackStateCompat.ACTION_SEEK_TO
                )
                .addCustomAction(releaseAction)
                .setState(state, pos, speed)
                .setBufferedPosition(bufferPos)
                .build();

        mediaSession.setPlaybackState(playbackState);
    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            player.play();
            handler.post(updateProgress);
        }

        @Override
        public void onPause() {
            player.pause();
            handler.removeCallbacks(updateProgress);
        }

        @Override
        public void onSkipToNext() {
            if (player.getCurrentMediaItemIndex() == queue.size() - 1) {
                player.seekTo(0, 0);
            } else {
                player.seekToNextMediaItem();
            }
            player.play();
        }

        @Override
        public void onSkipToPrevious() {
            long pos = player.getCurrentPosition();
            if ((player.getCurrentMediaItemIndex() == 0 && pos < 7000) || pos < 7000) {
                player.seekTo((player.getCurrentMediaItemIndex() == 0) ? queue.size() - 1 : player.getCurrentMediaItemIndex() - 1, 0);
            } else {
                player.seekTo(0);
            }
            player.play();
        }

        @Override
        public void onSeekTo(long pos) {
            player.seekTo(pos);
            updatePlaybackState(player.isPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED);
        }

        @Override
        public void onCustomAction(@NonNull String action, Bundle extras) {
            if ("ACTION_RELEASE".equals(action)) {
                mediaSession.release();
                PlayerManager.release();
                handler.removeCallbacksAndMessages(null);
                NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID);

            }
        }
    }
}
