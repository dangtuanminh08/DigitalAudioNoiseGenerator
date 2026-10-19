package com.example.audioplayer;

import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;

public class PlaybackSpeedManager {
    private final ExoPlayer player;

    public PlaybackSpeedManager(ExoPlayer player) {
        this.player = player;
    }

    public void setPlaybackSpeed(float speed) {
        player.setPlaybackParameters(player.getPlaybackParameters().withSpeed(speed));
    }

    @OptIn(markerClass = UnstableApi.class)
    public void setPlaybackPitch(float pitch) {
        player.setPlaybackParameters(player.getPlaybackParameters().withPitch(pitch));
    }

    public float getPlaybackSpeed() {
        return player.getPlaybackParameters().speed;
    }

    public float getPlaybackPitch() {
        return player.getPlaybackParameters().pitch;
    }
}
