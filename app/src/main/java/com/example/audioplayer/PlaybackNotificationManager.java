package com.example.audioplayer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.app.NotificationCompat.MediaStyle;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;

import java.util.Objects;

public class PlaybackNotificationManager {

    static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "media_playback_channel";
    private static final String CUSTOM_ACTION_RELEASE = "ACTION_RELEASE";
    private static final int LARGE_ICON_MAX_PX = 512;

    private final Context context;
    private final MediaSessionCompat mediaSession;
    private final ExoPlayer player;
    private final Bitmap largeIcon;
    private final PlaybackStateCompat.CustomAction releaseAction;

    private boolean notificationPosted = false;
    @Nullable
    private String postedTitle;
    @Nullable
    private String postedArtist;
    private boolean postedPlaying = false;

    private boolean metadataApplied = false;
    @Nullable
    private String metaTitle;
    @Nullable
    private String metaArtist;
    private long metaDuration = C.TIME_UNSET;

    public PlaybackNotificationManager(Context context, MediaSessionCompat mediaSession, ExoPlayer player) {
        this.context = context.getApplicationContext();
        this.mediaSession = mediaSession;
        this.player = player;
        this.largeIcon = decodeScaled(this.context, R.drawable.notification_image);
        this.releaseAction = new PlaybackStateCompat.CustomAction.Builder(
                CUSTOM_ACTION_RELEASE, this.context.getString(R.string.action_release), R.drawable.exit)
                .build();
    }

    void start() {
        createNotificationChannel();
        mediaSession.setCallback(new MediaSessionCallback());
        mediaSession.setActive(true);
        updatePlaybackState();
    }

    void release() {
        mediaSession.setActive(false);
        mediaSession.release();
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID);
    }

    private void createNotificationChannel() {
        NotificationChannel channel;
        channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setSound(null, null);
        channel.enableVibration(false);
        channel.setDescription(context.getString(R.string.channel_description));

        NotificationManagerCompat.from(context).createNotificationChannel(channel);
    }

    void refresh() {
        updateSessionMetadata();

        MediaItem item = player.getCurrentMediaItem();
        if (item == null) {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID);
            notificationPosted = false;
            return;
        }

        String title = toStringOrNull(item.mediaMetadata.title);
        String artist = toStringOrNull(item.mediaMetadata.artist);
        boolean playing = player.isPlaying();

        if (notificationPosted
                && playing == postedPlaying
                && Objects.equals(title, postedTitle)
                && Objects.equals(artist, postedArtist)) {
            return;
        }

        notificationPosted = true;
        postedTitle = title;
        postedArtist = artist;
        postedPlaying = playing;
        post(buildNotification());
    }

    @SuppressLint("MissingPermission")
    private void post(Notification notification) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification);
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    Notification buildNotification() {
        MediaItem item = player.getCurrentMediaItem();
        CharSequence title = item != null ? item.mediaMetadata.title : null;
        CharSequence artist = item != null ? item.mediaMetadata.artist : null;
        if (TextUtils.isEmpty(title)) title = context.getString(R.string.unknown_artist);
        if (TextUtils.isEmpty(artist)) artist = context.getString(R.string.unknown_artist);

        boolean isPlaying = player.isPlaying();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setLargeIcon(largeIcon)
                .setContentTitle(title)
                .setContentText(artist)
                .setContentIntent(launchPendingIntent())
                .setStyle(new MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .addAction(isPlaying ? R.drawable.pause : R.drawable.play,
                        context.getString(isPlaying ? R.string.action_pause : R.string.action_play),
                        servicePendingIntent(ForegroundService.ACTION_PLAY_PAUSE, 1))
                .addAction(R.drawable.prev_song,
                        context.getString(R.string.action_previous),
                        servicePendingIntent(ForegroundService.ACTION_PREVIOUS, 2))
                .addAction(R.drawable.next_song,
                        context.getString(R.string.action_next),
                        servicePendingIntent(ForegroundService.ACTION_NEXT, 3))
                // Dismissable while paused; the service detaches from the foreground on pause.
                .setOngoing(isPlaying);

        PendingIntent releaseIntent = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            releaseIntent = releasePendingIntent();
        }
        if (releaseIntent != null) {
            builder.addAction(androidx.media3.session.R.drawable.media3_icon_block,
                    context.getString(R.string.action_release), releaseIntent);
        }

        return builder.build();
    }

    private PendingIntent servicePendingIntent(String action, int requestCode) {
        Intent intent = new Intent(context, ForegroundService.class).setAction(action);
        return PendingIntent.getForegroundService(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @Nullable
    private PendingIntent launchPendingIntent() {
        Intent launch = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (launch == null) return null;
        return PendingIntent.getActivity(context, 10, launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Nullable
    private PendingIntent releasePendingIntent() {
        Intent launch = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (launch == null) return null;
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        launch.putExtra(MainActivity.EXTRA_RELEASE_PLAYER, true);
        return PendingIntent.getActivity(context, 11, launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    void updatePlaybackState() {
        int state = getState();

        float speed = state == PlaybackStateCompat.STATE_PLAYING
                ? player.getPlaybackParameters().speed
                : 0f;

        PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                PlaybackStateCompat.ACTION_SEEK_TO
                )
                .addCustomAction(releaseAction)
                .setState(state, player.getCurrentPosition(), speed)
                .setBufferedPosition(player.getBufferedPosition())
                .build();

        mediaSession.setPlaybackState(playbackState);
    }

    private int getState() {
        int state;
        int exoState = player.getPlaybackState();
        if (player.getCurrentMediaItem() == null) {
            state = PlaybackStateCompat.STATE_NONE;
        } else if (player.isPlaying()) {
            state = PlaybackStateCompat.STATE_PLAYING;
        } else if (player.getPlayWhenReady() && exoState == Player.STATE_BUFFERING) {
            state = PlaybackStateCompat.STATE_BUFFERING;
        } else if (exoState == Player.STATE_ENDED) {
            state = PlaybackStateCompat.STATE_STOPPED;
        } else {
            state = PlaybackStateCompat.STATE_PAUSED;
        }
        return state;
    }

    /** Title, artist and duration for system media controls. Skips the call when nothing changed. */
    void updateSessionMetadata() {
        MediaItem item = player.getCurrentMediaItem();
        String title = item != null ? toStringOrNull(item.mediaMetadata.title) : null;
        String artist = item != null ? toStringOrNull(item.mediaMetadata.artist) : null;
        long duration = player.getDuration();
        if (duration < 0) duration = C.TIME_UNSET;

        if (metadataApplied
                && Objects.equals(title, metaTitle)
                && Objects.equals(artist, metaArtist)
                && duration == metaDuration) {
            return;
        }

        metadataApplied = true;
        metaTitle = title;
        metaArtist = artist;
        metaDuration = duration;

        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
        if (title != null) builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, title);
        if (artist != null) builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist);
        if (duration != C.TIME_UNSET) builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration);
        mediaSession.setMetadata(builder.build());
    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            PlayerManager.play();
        }

        @Override
        public void onPause() {
            PlayerManager.pause();
        }

        @Override
        public void onSkipToNext() {
            PlayerManager.skipToNext();
        }

        @Override
        public void onSkipToPrevious() {
            PlayerManager.skipToPrevious();
        }

        @Override
        public void onSeekTo(long pos) {
            player.seekTo(pos);
        }

        @Override
        public void onCustomAction(@NonNull String action, Bundle extras) {
            if (CUSTOM_ACTION_RELEASE.equals(action)) {
                PlayerManager.shutdown(context);
            }
        }
    }

    @Nullable
    private static String toStringOrNull(@Nullable CharSequence value) {
        return value != null ? value.toString() : null;
    }

    @Nullable
    private static Bitmap decodeScaled(Context context, int resId) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(context.getResources(), resId, options);

        int largest = Math.max(options.outWidth, options.outHeight);
        int sampleSize = 1;
        while (largest / sampleSize > PlaybackNotificationManager.LARGE_ICON_MAX_PX) {
            sampleSize *= 2;
        }

        options.inJustDecodeBounds = false;
        options.inSampleSize = sampleSize;
        options.inScaled = false;
        return BitmapFactory.decodeResource(context.getResources(), resId, options);
    }
}