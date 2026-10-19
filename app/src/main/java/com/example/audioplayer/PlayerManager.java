package com.example.audioplayer;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.content.ContextCompat;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;

import java.util.ArrayList;
import java.util.List;

public class PlayerManager {

    private static final boolean ENABLE_AUDIO_OFFLOAD = false;

    private static final long PREVIOUS_RESTART_THRESHOLD_MS = 7000;

    private static ExoPlayer player;
    @SuppressLint("StaticFieldLeak")
    private static Context appContext;
    private static PlaybackNotificationManager notificationManager;
    private static SessionListener sessionListener;

    private PlayerManager() {}

    public static ExoPlayer getPlayer(Context context) {
        if (player == null) {
            appContext = context.getApplicationContext();

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build();

            player = new ExoPlayer.Builder(appContext)
                    .setAudioAttributes(audioAttributes, true)
                    .setHandleAudioBecomingNoisy(true)
                    .setWakeMode(C.WAKE_MODE_LOCAL)
                    .build();

            if (ENABLE_AUDIO_OFFLOAD) {
                enableAudioOffload(player);
            }

            MediaSessionCompat mediaSession = new MediaSessionCompat(appContext, "Minh");
            notificationManager = new PlaybackNotificationManager(appContext, mediaSession, player);
            notificationManager.start();

            sessionListener = new SessionListener();
            player.addListener(sessionListener);
        }
        return player;
    }

    @OptIn(markerClass = UnstableApi.class)
    private static void enableAudioOffload(ExoPlayer exoPlayer) {
        exoPlayer.setTrackSelectionParameters(
                exoPlayer.getTrackSelectionParameters().buildUpon()
                        .setAudioOffloadPreferences(
                                new TrackSelectionParameters.AudioOffloadPreferences.Builder()
                                        .setAudioOffloadMode(TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED)
                                        .setIsGaplessSupportRequired(true)
                                        .setIsSpeedChangeSupportRequired(true)
                                        .build())
                        .build());
    }
    public static void playQueue(Context context, List<Item> items, int startIndex) {
        ExoPlayer exoPlayer = getPlayer(context);
        List<MediaItem> mediaItems = new ArrayList<>(items.size());
        for (Item item : items) {
            mediaItems.add(toMediaItem(item));
        }
        exoPlayer.setMediaItems(mediaItems, startIndex, 0L);
        exoPlayer.prepare();
        play();
    }

    public static MediaItem toMediaItem(Item item) {
        return new MediaItem.Builder()
                .setMediaId(item.getMediaId())
                .setUri(item.getUri())
                .setMediaMetadata(new MediaMetadata.Builder()
                        .setTitle(item.getTitle())
                        .setArtist(item.getArtist())
                        .build())
                .build();
    }

    public static void updateItemMetadata(Item item) {
        if (player == null) return;
        int index = indexOfMediaId(item.getMediaId());
        if (index < 0) return;
        player.replaceMediaItem(index, toMediaItem(item));
        if (notificationManager != null) {
            notificationManager.refresh();
        }
    }

    public static void removeItem(int index) {
        if (player == null || index < 0 || index >= player.getMediaItemCount()) return;
        player.removeMediaItem(index);
    }

    public static int indexOfMediaId(String mediaId) {
        if (player == null) return -1;
        for (int i = 0; i < player.getMediaItemCount(); i++) {
            if (mediaId.equals(player.getMediaItemAt(i).mediaId)) return i;
        }
        return -1;
    }

    @Nullable
    public static String getCurrentMediaId() {
        MediaItem item = player != null ? player.getCurrentMediaItem() : null;
        return item != null ? item.mediaId : null;
    }

    @Nullable
    public static CharSequence getCurrentTitle() {
        MediaItem item = player != null ? player.getCurrentMediaItem() : null;
        return item != null ? item.mediaMetadata.title : null;
    }

    @Nullable
    public static CharSequence getCurrentArtist() {
        MediaItem item = player != null ? player.getCurrentMediaItem() : null;
        return item != null ? item.mediaMetadata.artist : null;
    }

    public static void play() {
        if (player == null) return;
        int state = player.getPlaybackState();
        if (state == Player.STATE_ENDED) {
            player.seekTo(0, 0);
        } else if (state == Player.STATE_IDLE && player.getMediaItemCount() > 0) {
            player.prepare();
        }
        player.play();
    }

    public static void pause() {
        if (player != null) player.pause();
    }

    public static void togglePlayPause() {
        if (player == null) return;
        if (player.isPlaying()) {
            player.pause();
        } else {
            play();
        }
    }

    public static void skipToNext() {
        if (player == null || player.getMediaItemCount() == 0) return;
        int next = player.getNextMediaItemIndex();
        player.seekTo(next == C.INDEX_UNSET ? 0 : next, 0);
        play();
    }

    public static void skipToPrevious() {
        if (player == null || player.getMediaItemCount() == 0) return;
        if (player.getCurrentPosition() < PREVIOUS_RESTART_THRESHOLD_MS) {
            int previous = player.getPreviousMediaItemIndex();
            player.seekTo(previous == C.INDEX_UNSET ? player.getMediaItemCount() - 1 : previous, 0);
        } else {
            player.seekTo(0);
        }
        play();
    }

    public static Notification buildNotification(Context context) {
        getPlayer(context);
        return notificationManager.buildNotification();
    }

    private static void startPlaybackService() {
        if (appContext == null) return;
        try {
            ContextCompat.startForegroundService(appContext, new Intent(appContext, ForegroundService.class));
        } catch (IllegalStateException e) {
            // Background foreground service start was refused. Playback continues with no foreground service
        }
    }

    public static void shutdown(Context context) {
        Context app = context.getApplicationContext();
        if (player != null) {
            if (sessionListener != null) player.removeListener(sessionListener);
            player.release();
        }
        if (notificationManager != null) {
            notificationManager.release();
        }
        player = null;
        notificationManager = null;
        sessionListener = null;
        app.stopService(new Intent(app, ForegroundService.class));
    }

    private static void refreshSession() {
        if (notificationManager == null) return;
        notificationManager.refresh();
        notificationManager.updatePlaybackState();
    }

    private static class SessionListener implements Player.Listener {
        private int consecutiveErrors = 0;

        @Override
        public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
            if (playWhenReady) startPlaybackService();
            refreshSession();
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            refreshSession();
        }

        @Override
        public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
            refreshSession();
        }

        @Override
        public void onTimelineChanged(@NonNull Timeline timeline, int reason) {
            refreshSession();
        }

        @Override
        public void onPlaybackStateChanged(int playbackState) {
            if (playbackState == Player.STATE_READY) consecutiveErrors = 0;
            refreshSession();
        }

        @Override
        public void onPlaybackParametersChanged(@NonNull PlaybackParameters playbackParameters) {
            if (notificationManager != null) notificationManager.updatePlaybackState();
        }

        @Override
        public void onPositionDiscontinuity(@NonNull Player.PositionInfo oldPosition,
                                            @NonNull Player.PositionInfo newPosition, int reason) {
            if (notificationManager != null) notificationManager.updatePlaybackState();
        }

        @Override
        public void onPlayerError(@NonNull PlaybackException error) {
            ExoPlayer exoPlayer = player;
            if (exoPlayer == null) return;

            consecutiveErrors++;
            if (consecutiveErrors >= exoPlayer.getMediaItemCount()) return;

            int next = exoPlayer.getNextMediaItemIndex();
            exoPlayer.seekTo(next == C.INDEX_UNSET ? 0 : next, 0);
            exoPlayer.prepare();
            exoPlayer.play();
        }
    }
}