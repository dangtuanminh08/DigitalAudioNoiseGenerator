package com.example.audioplayer;

import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.app.NotificationCompat.MediaStyle;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;

import java.util.ArrayList;

public class NotificationManager {
    private final Context context;
    private static MediaSessionCompat mediaSession;
    private static ExoPlayer player;
    private static ArrayList<String> queue = new ArrayList<>();
    private static final String CHANNEL_ID = "media_playback_channel";
    private static final int NOTIFICATION_ID = 1;

    private final Handler handler = new Handler();
    private final Runnable updateProgress = new Runnable() {
        @Override
        public void run() {
            if (player != null && player.isPlaying()) {
                setPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                handler.postDelayed(this, 1000); // update every 1s
            }
        }
    };

    private static final PlaybackStateCompat.CustomAction releaseAction = new PlaybackStateCompat.CustomAction.Builder("ACTION_RELEASE", "Release",
            androidx.media3.session.R.drawable.media3_icon_block)
            .build();

    public NotificationManager(Context context, MediaSessionCompat mediaSession, ExoPlayer player, ArrayList<String> queue) {
        this.context = context;
        NotificationManager.mediaSession = mediaSession;
        NotificationManager.player = player;
        NotificationManager.queue = queue;
    }

    public void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "DANG! Channel";
            String descriptionText = "Channel for audio player notifications from: DANG!";
            int importance = android.app.NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(descriptionText);

            android.app.NotificationManager notificationManager =
                    (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void createNotification() {
        mediaSession.setActive(true);
        setPlaybackState(PlaybackStateCompat.STATE_PLAYING);
        mediaSession.setCallback(new MySessionCallback());

        if (context == null) {
            throw new IllegalStateException("Context is null in createNotificationChannel");
        }
    }

    public static void setPlaybackState(int playbackState) {
        long currentPosition = player.getCurrentPosition();
        long bufferedPosition = player.getBufferedPosition();

        mediaSession.setPlaybackState(
                new PlaybackStateCompat.Builder()
                        .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE
                                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                                | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                                | PlaybackStateCompat.ACTION_SEEK_TO
                        )
                        .addCustomAction(releaseAction)
                        .setState(
                                playbackState,
                                currentPosition,
                                (playbackState == PlaybackStateCompat.STATE_PLAYING) ? 1f : 0f
                        )
                        .setBufferedPosition(bufferedPosition)
                        .build()
        );
    }

    @UnstableApi
    public void showNotification(String title, String artist) {
        createNotificationChannel();

        Intent playPauseIntent = new Intent(context, PlayerActivity.class);
        playPauseIntent.setAction("ACTION_PLAY_PAUSE");
        PendingIntent playPausePendingIntent = PendingIntent.getActivity(context, 0, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent nextIntent = new Intent(context, PlayerActivity.class);
        nextIntent.setAction("ACTION_NEXT");
        PendingIntent nextPendingIntent = PendingIntent.getActivity(context, 0, nextIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent previousIntent = new Intent(context, PlayerActivity.class);
        previousIntent.setAction("ACTION_PREVIOUS");
        PendingIntent previousPendingIntent = PendingIntent.getActivity(context, 0, previousIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent releaseIntent = new Intent(context, PlayerActivity.class);
        releaseIntent.setAction("ACTION_RELEASE");
        PendingIntent releasePendingIntent = PendingIntent.getBroadcast(context, 0, releaseIntent, PendingIntent.FLAG_IMMUTABLE);

        String playPauseLabel = player.isPlaying() ? "Pause" : "Play";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.notificationicon)
                .setContentTitle(title)
                .setContentText(artist)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.notificationimage))
                .setStyle(new MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2, 3))
                .addAction(new NotificationCompat.Action(R.drawable.pause, playPauseLabel, playPausePendingIntent))
                .addAction(new NotificationCompat.Action(R.drawable.prev_song, "Previous", previousPendingIntent))
                .addAction(new NotificationCompat.Action(R.drawable.next_song, "Next", nextPendingIntent))
                .addAction(new NotificationCompat.Action(androidx.media3.session.R.drawable.media3_icon_block, "Release", releasePendingIntent))
                .setOngoing(true);


        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, player.getDuration())
                .build()
        );

        setPlaybackState(player.isPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(NOTIFICATION_ID, builder
                    .build());
        }
    }

    private class MySessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            player.play();
            handler.post(updateProgress);
            setPlaybackState(PlaybackStateCompat.STATE_PLAYING);
        }

        @Override
        public void onPause() {
            player.pause();
            handler.removeCallbacks(updateProgress);
            setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
        }

        @Override
        public void onSkipToNext() {
            if (player.getCurrentMediaItemIndex() == queue.size() - 1) {
                player.seekTo(0, 0);
                player.play();
            } else {
                player.seekToNextMediaItem();
            }
        }

        @Override
        public void onSkipToPrevious() {
            if ((player.getCurrentMediaItemIndex() == 0) && player.getCurrentPosition() < 7000) {
                int lastIndex = queue.size() - 1;
                player.seekTo(lastIndex, 0);
                player.play();
            } else if (player.getCurrentPosition() < 7000) {
                player.seekToPreviousMediaItem();
            } else {
                player.seekTo(0);
            }
        }

        @Override
        public void onSeekTo(long pos) {
            player.seekTo(pos);
            setPlaybackState(player.isPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED);
        }

        @Override
        public void onCustomAction(@NonNull String action, Bundle extras) {
            if (releaseAction.getAction().equals(action)) {
                mediaSession.release();
                PlayerManager.release();
                NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID);
            }
        }
    }
}