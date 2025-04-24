package com.example.audioplayer;

import android.content.Context;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;

import java.util.ArrayList;


//"Global" instance of the ExoPlayer.
public class PlayerManager {
    private static ExoPlayer player;
    private static ArrayList<String> queue = new ArrayList<>();
    private static NotificationManager notificationManager;

    public static ExoPlayer getPlayer(Context context) {
        if (player == null) {
            player = new ExoPlayer.Builder(context).build();
            Log.d("NotificationManager", player.toString());
            MediaSessionCompat mediaSession = new MediaSessionCompat(context, "Minh");
            notificationManager = new NotificationManager(context, mediaSession, player, queue);
            notificationManager.createNotification();
        }
        return player;
    }

    public static void playSong(ArrayList<String> queue) {
        PlayerManager.queue = queue;
        player.clearMediaItems();
        for (String path : queue) {
            MediaItem mediaItem = MediaItem.fromUri(path);
            player.addMediaItem(mediaItem);
        }
        player.prepare();
    }

    public static ArrayList<String> getQueue() {
        return queue;
    }

    public static int getCurrentSongIndex() {
        if (player != null) {
            return player.getCurrentMediaItemIndex();
        }
        return -1;
    }

    @OptIn(markerClass = UnstableApi.class)
    public static void showMediaNotification(String title, String artist) {
        notificationManager.showNotification(title, artist);
    }

    public static void release() {
        player.release();
        player = null;
    }


}