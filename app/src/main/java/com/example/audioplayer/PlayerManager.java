package com.example.audioplayer;

import android.content.Context;
import android.media.session.MediaSession;

import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;

import java.util.ArrayList;

//"Global" instance of the ExoPlayer.
public class PlayerManager {
    private static ExoPlayer player;
    private static MediaSession mediaSession;
    private static ArrayList<String> queue = new ArrayList<>();

    public static ExoPlayer getPlayer(Context context) {
        if (player == null) {
            player = new ExoPlayer.Builder(context).build();
            mediaSession = new MediaSession(context, "Minh");
        }
        mediaSession.setActive(true);
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
}