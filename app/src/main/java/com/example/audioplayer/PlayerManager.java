package com.example.audioplayer;

import android.content.Context;
import android.media.session.MediaSession;
import android.net.Uri;

import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;

import java.io.File;
import java.util.ArrayList;

public class PlayerManager {
    private static ExoPlayer player;
    private static MediaSession mediaSession;

    public static ExoPlayer getPlayer(Context context) {
        if (player == null) {
            player = new ExoPlayer.Builder(context).build();
            mediaSession = new MediaSession(context, "Minh");
        }
        mediaSession.setActive(true);
        return player;
    }

    public static void playSong(ArrayList<String> queue) {
        if (player != null) {
            player.clearMediaItems();
            for (String item : queue) {
                Uri fileUri = Uri.fromFile(new File(item));
                player.addMediaItem(MediaItem.fromUri(fileUri));
            }
            player.prepare();
            player.setPlayWhenReady(true);
        }
    }
}