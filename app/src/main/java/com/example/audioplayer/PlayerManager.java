package com.example.audioplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.annotation.OptIn;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;

import java.util.ArrayList;


//"Global" instance of the ExoPlayer.
public class PlayerManager {
    private static ExoPlayer player;
    private static ArrayList<String> queue = new ArrayList<>();
    @SuppressLint("StaticFieldLeak")
    private static NotificationManager notificationManager;

    public static ExoPlayer getPlayer(Context context) {
        if (player == null) {
            player = new ExoPlayer.Builder(context.getApplicationContext()).build();
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build();

            player.setAudioAttributes(audioAttributes, true);
            MediaSessionCompat mediaSession = new MediaSessionCompat(context, "Minh");
            notificationManager = new NotificationManager(context, mediaSession, player, queue);
            notificationManager.createNotification();
        }
        return player;
    }

    public static void prepareQueue(ArrayList<String> queue) {
        PlayerManager.queue = queue;
        player.clearMediaItems();
        for (String path : queue) {
            MediaItem mediaItem = MediaItem.fromUri(path);
            player.addMediaItem(mediaItem);
        }
    }

    public static ArrayList<String> getQueue() {
        return queue;
    }

    public static int getCurrentSongIndex() {
        return (player != null) ? player.getCurrentMediaItemIndex() : -1;
    }

    @OptIn(markerClass = UnstableApi.class)
    public static void showMediaNotification(String title, String artist) {
        notificationManager.showNotification(title, artist);
    }

    public static void release() {
        if (player != null) {
            player.release();
            player = null;
        }
    }
}