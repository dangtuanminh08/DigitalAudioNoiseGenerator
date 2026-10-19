package com.example.audioplayer;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

@UnstableApi
public class PlayerActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isUserSeeking = false;
    private ExoPlayer player;
    private PlayerView playerView;

    private TextView songTitle, artistName, currentTime, totalTime;
    private SeekBar lengthBar;
    private GifDrawable playGif, pauseGif;
    private GifImageView btnPlayPause;
    private ImageButton btnShuffle, btnRepeat;
    private PlaybackSpeedManager playbackSpeedManager;

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (player != null && player.isPlaying()) {
                if (!isUserSeeking) {
                    syncProgress();
                }
                handler.postDelayed(this, 1000);
            }
        }
    };

    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            updatePlaybackIcons(isPlaying);
        }

        @Override
        public void onPlaybackStateChanged(int state) {
            if (state == Player.STATE_READY) {
                updateDuration();
            } else if (state == Player.STATE_ENDED) {
                handler.removeCallbacks(updateRunnable);
            }
        }

        @Override
        public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
            updateSongInfo();
        }

        @Override
        public void onTimelineChanged(@NonNull Timeline timeline, int reason) {
            updateSongInfo();
        }

        @Override
        public void onPositionDiscontinuity(@NonNull Player.PositionInfo oldPos,
                                            @NonNull Player.PositionInfo newPos, int reason) {
            if (!isUserSeeking) {
                syncProgress();
            }
        }
    };

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        initViews();
        initPlayer();
        initSeekBar();
        player.addListener(playerListener);
        updateSongInfo();
    }

    private void initViews() {
        songTitle = findViewById(R.id.song_title);
        artistName = findViewById(R.id.artist_name);
        lengthBar = findViewById(R.id.seek_bar);
        currentTime = findViewById(R.id.current_time);
        totalTime = findViewById(R.id.total_time);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnShuffle = findViewById(R.id.btn_shuffle);
        btnRepeat = findViewById(R.id.btn_repeat);
        ImageButton btnNext = findViewById(R.id.btn_next);
        ImageButton btnPrev = findViewById(R.id.btn_prev);
        ImageButton backButton = findViewById(R.id.back_button);
        ImageButton playerMenu = findViewById(R.id.player_menu);

        try {
            playGif = new GifDrawable(getResources(), R.drawable.play);
            pauseGif = new GifDrawable(getResources(), R.drawable.pause);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        btnPlayPause.setOnClickListener(v -> PlayerManager.togglePlayPause());
        btnNext.setOnClickListener(v -> PlayerManager.skipToNext());
        btnPrev.setOnClickListener(v -> PlayerManager.skipToPrevious());
        btnShuffle.setOnClickListener(v -> toggleShuffle());
        btnRepeat.setOnClickListener(v -> toggleRepeat());
        backButton.setOnClickListener(v -> exitActivity());

        if (playerMenu != null) {
            playerMenu.setOnClickListener(v -> showBottomSheet());
        }
    }

    private void initPlayer() {
        player = PlayerManager.getPlayer(this);
        playerView = findViewById(R.id.player_view);
        playerView.setPlayer(player);
        playbackSpeedManager = new PlaybackSpeedManager(player);

        updateShuffleButton();
        updateRepeatButton();
    }

    private void initSeekBar() {
        lengthBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserSeeking = false;
                player.seekTo(seekBar.getProgress());
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentTime.setText(formatTime(progress));
                }
            }
        });
    }

    private void updatePlaybackIcons(boolean isPlaying) {
        GifDrawable gif = isPlaying ? playGif : pauseGif;
        btnPlayPause.setImageDrawable(gif);
        gif.start();

        if (isPlaying) {
            startUpdatingSeekBar();
        } else {
            handler.removeCallbacks(updateRunnable);
        }
    }

    // Title and artist come from the queue item, so renames show up and the artist is no longer always "unknown".
    private void updateSongInfo() {
        CharSequence title = PlayerManager.getCurrentTitle();
        CharSequence artist = PlayerManager.getCurrentArtist();

        songTitle.setText(title != null ? title : "");
        artistName.setText(artist != null && artist.length() > 0 ? artist : getString(R.string.unknown_artist));

        updateDuration();
        syncProgress();
    }

    private void updateDuration() {
        long duration = player.getDuration();
        if (duration <= 0) {
            lengthBar.setMax(0);
            totalTime.setText(formatTime(0));
            return;
        }
        lengthBar.setMax((int) duration);
        totalTime.setText(formatTime(duration));
    }

    private void syncProgress() {
        long position = player.getCurrentPosition();
        lengthBar.setProgress((int) position);
        currentTime.setText(formatTime(position));
    }

    private void startUpdatingSeekBar() {
        // Remove first so repeated calls never stack parallel loops.
        handler.removeCallbacks(updateRunnable);
        handler.post(updateRunnable);
    }

    private void toggleShuffle() {
        boolean enabled = !player.getShuffleModeEnabled();
        player.setShuffleModeEnabled(enabled);
        updateShuffleButton();
        Toast.makeText(this, enabled ? R.string.shuffle_on : R.string.shuffle_off, Toast.LENGTH_SHORT).show();
    }

    private void toggleRepeat() {
        int mode = (player.getRepeatMode() + 1) % 3;
        player.setRepeatMode(mode);
        updateRepeatButton();
        Toast.makeText(this, getRepeatModeText(mode), Toast.LENGTH_SHORT).show();
    }

    private void updateShuffleButton() {
        btnShuffle.setColorFilter(player.getShuffleModeEnabled() ? Color.WHITE : Color.argb(255, 186, 186, 186));
    }

    private void updateRepeatButton() {
        int mode = player.getRepeatMode();
        int icon = mode == Player.REPEAT_MODE_ONE ? R.drawable.repeat_one :
                mode == Player.REPEAT_MODE_ALL ? R.drawable.repeat : R.drawable.order;
        btnRepeat.setImageResource(icon);
    }

    @StringRes
    private int getRepeatModeText(int mode) {
        return mode == Player.REPEAT_MODE_ONE ? R.string.repeat_song :
                mode == Player.REPEAT_MODE_ALL ? R.string.repeat_queue : R.string.repeat_off;
    }

    private void exitActivity() {
        handler.removeCallbacks(updateRunnable);
        finish();
    }

    private String formatTime(long millis) {
        long min = TimeUnit.MILLISECONDS.toMinutes(millis);
        long sec = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format(Locale.CANADA, "%d:%02d", min, sec);
    }

    private void showBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        ViewGroup root = findViewById(android.R.id.content);
        View view = LayoutInflater.from(this).inflate(R.layout.player_settings, root, false);

        TextView speedText = view.findViewById(R.id.speed_text);
        TextView pitchText = view.findViewById(R.id.pitch_text);
        SeekBar speedBar = view.findViewById(R.id.speed_bar);
        SeekBar pitchBar = view.findViewById(R.id.pitch_bar);

        speedBar.setMax(20);
        pitchBar.setMax(20);
        speedBar.setMin(1);
        pitchBar.setMin(1);

        // Round, not truncate: 1.1f * 10f can land just under 11.
        speedBar.setProgress(Math.round(playbackSpeedManager.getPlaybackSpeed() * 10f));
        pitchBar.setProgress(Math.round(playbackSpeedManager.getPlaybackPitch() * 10f));

        speedText.setText(getString(R.string.playback_speed_label, speedBar.getProgress() / 10f));
        pitchText.setText(getString(R.string.playback_pitch_label, pitchBar.getProgress() / 10f));

        speedBar.setOnSeekBarChangeListener(createSpeedPitchListener(speedText, true));
        pitchBar.setOnSeekBarChangeListener(createSpeedPitchListener(pitchText, false));

        dialog.setContentView(view);
        dialog.show();
    }

    private SeekBar.OnSeekBarChangeListener createSpeedPitchListener(TextView label, boolean isSpeed) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                float value = progress / 10f;
                if (isSpeed) playbackSpeedManager.setPlaybackSpeed(value);
                else playbackSpeedManager.setPlaybackPitch(value);
                label.setText(getString(isSpeed ? R.string.playback_speed_label : R.string.playback_pitch_label, value));
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
            }
        };
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(updateRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSongInfo();
        updatePlaybackIcons(player.isPlaying());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateRunnable);
        player.removeListener(playerListener);
        // PlayerView registers its own listener on the singleton player. Detach it or it leaks this activity.
        playerView.setPlayer(null);
        if (playGif != null) playGif.recycle();
        if (pauseGif != null) pauseGif.recycle();
    }
}