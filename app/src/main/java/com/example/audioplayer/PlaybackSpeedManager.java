package com.example.audioplayer;

import androidx.media3.common.PlaybackParameters;
import androidx.media3.exoplayer.ExoPlayer;

public class PlaybackSpeedManager {
    private final ExoPlayer player;

    public PlaybackSpeedManager(ExoPlayer player) {
        this.player = player;
    }

    private float pitch = 1.0f;
    private float speed = 1.0f;

    public void setPlaybackSpeed(float speedIn) {
        speed = speedIn;
        PlaybackParameters playbackParameters = new PlaybackParameters(speed, pitch);
        player.setPlaybackParameters(playbackParameters);
    }

    public void setPlaybackPitch(float pitchIn) {
        pitch = pitchIn;
        PlaybackParameters playbackParameters = new PlaybackParameters(speed, pitch);
        player.setPlaybackParameters(playbackParameters);
    }

    public float getPlaybackSpeed() {
        float speed = player.getPlaybackParameters().speed;
        return speed * 10f;
    }

    public float getPlaybackPitch() {
        float pitch = player.getPlaybackParameters().pitch;
        return pitch * 10f;
    }
}
