package com.example.audioplayer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

@UnstableApi
public class PlayerActivity extends AppCompatActivity {

    private ArrayList<String> queue;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isUserSeeking = false;
    private ExoPlayer player;

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
                lengthBar.setProgress((int) player.getCurrentPosition());
                currentTime.setText(formatTime(player.getCurrentPosition()));
                handler.postDelayed(this, 1000);
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
        handleIntent(getIntent());
        initSeekBar();
        setupPlayerListeners();
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

        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnNext.setOnClickListener(v -> nextSong());
        btnPrev.setOnClickListener(v -> prevSong());
        btnShuffle.setOnClickListener(v -> toggleShuffle());
        btnRepeat.setOnClickListener(v -> toggleRepeat());
        backButton.setOnClickListener(v -> finish());

        if (playerMenu != null) {
            playerMenu.setOnClickListener(v -> showBottomSheet());
        }
    }

    private void initPlayer() {
        player = PlayerManager.getPlayer(this);
        PlayerView playerView = findViewById(R.id.player_view);
        playerView.setPlayer(player);
        playbackSpeedManager = new PlaybackSpeedManager(player);

        updateShuffleButton();
        updateRepeatButton();
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.hasExtra("SONGS")) {
            queue = intent.getStringArrayListExtra("SONGS");
            int currentIndex = intent.getIntExtra("CURRENT_INDEX", 0);

            if (player.getCurrentMediaItemIndex() != currentIndex) {
                PlayerManager.prepareQueue(queue);
                player.seekTo(currentIndex, 0);
            } else {
                setPlayerDisplayText();
            }
        }
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

    private void setupPlayerListeners() {
        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                updatePlaybackIcons(isPlaying);
            }

            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    setPlayerDisplayText();
                    updateSeekBarAndTime();
                } else if (state == Player.STATE_ENDED) {
                    handler.removeCallbacks(updateRunnable);
                }
            }

            @Override
            public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                setPlayerDisplayText();
                updateSongHighlight();
            }

            @Override
            public void onPositionDiscontinuity(@NonNull Player.PositionInfo oldPos,
                                                @NonNull Player.PositionInfo newPos, int reason) {
                if (!isUserSeeking) {
                    lengthBar.setProgress((int) player.getCurrentPosition());
                    currentTime.setText(formatTime(player.getCurrentPosition()));
                }
            }
        });
    }

    //Helper function for UI

    private void updatePlaybackIcons(boolean isPlaying) {
        btnPlayPause.setImageDrawable(isPlaying ? playGif : pauseGif);
        (isPlaying ? playGif : pauseGif).start();
        NotificationManager.updatePlaybackState(isPlaying ? 3 : 2);

        if (isPlaying) {
            startUpdatingSeekBar();
        } else {
            handler.removeCallbacks(updateRunnable);
        }
    }

    //Helper functions for UX

    private void togglePlayPause() {
        if (player.isPlaying()) player.pause();
        else player.play();
    }

    private void nextSong() {
        if (player.getCurrentMediaItemIndex() == queue.size() - 1) {
            player.seekTo(0, 0);
        } else {
            player.seekToNextMediaItem();
        }
        updateSongHighlight();
    }

    private void prevSong() {
        if (player.getCurrentPosition() < 7000) {
            player.seekTo((player.getCurrentMediaItemIndex() == 0) ? queue.size() - 1 : player.getCurrentMediaItemIndex() - 1, 0);
        } else {
            player.seekTo(0);
        }
        updateSongHighlight();
    }

    private void toggleShuffle() {
        boolean enabled = !player.getShuffleModeEnabled();
        player.setShuffleModeEnabled(enabled);
        updateShuffleButton();
        Toast.makeText(this, enabled ? "Shuffle ON" : "Shuffle OFF", Toast.LENGTH_SHORT).show();
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

    private String getRepeatModeText(int mode) {
        return mode == Player.REPEAT_MODE_ONE ? "Repeat song" :
                mode == Player.REPEAT_MODE_ALL ? "Repeat queue" : "Repeat off";
    }

    private void updateSeekBarAndTime() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                long duration = player.getDuration();
                if (duration > 0) {
                    lengthBar.setMax((int) duration);
                    totalTime.setText(formatTime(duration));
                } else {
                    handler.postDelayed(this, 100);
                }
            }
        }, 100);
    }

    private void setPlayerDisplayText() {
        String path = player.getCurrentMediaItemIndex() >= queue.size() ? queue.get(0) : queue.get(player.getCurrentMediaItemIndex());
        String name = new File(path).getName().replaceFirst("[.][^.]+$", "");

        songTitle.setText(name);
        artistName.setText(R.string.unknown_artist);
        updateSeekBarAndTime();
        startUpdatingSeekBar();

        PlayerManager.showMediaNotification(name, "Unknown Artist");
    }

    private void updateSongHighlight() {
        ItemAdapter itemAdapter = TabViewFragment.adapter;
        String path = player.getCurrentMediaItemIndex() >= queue.size() ? queue.get(0) : queue.get(player.getCurrentMediaItemIndex());
        itemAdapter.setCurrentPlayingSong(queue.indexOf(path));
    }

    private void startUpdatingSeekBar() {
        handler.post(updateRunnable);
    }

    private String formatTime(long millis) {
        long min = TimeUnit.MILLISECONDS.toMinutes(millis);
        long sec = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format(Locale.CANADA, "%d:%02d", min, sec);
    }

    //Bottom sheet for playback speed and pitch
    private void showBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.player_settings, null);

        TextView speedText = view.findViewById(R.id.speed_text);
        TextView pitchText = view.findViewById(R.id.pitch_text);
        SeekBar speedBar = view.findViewById(R.id.speed_bar);
        SeekBar pitchBar = view.findViewById(R.id.pitch_bar);

        speedBar.setMax(20);
        pitchBar.setMax(20);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            speedBar.setMin(1);
            pitchBar.setMin(1);
        }

        speedBar.setProgress((int) playbackSpeedManager.getPlaybackSpeed());
        pitchBar.setProgress((int) playbackSpeedManager.getPlaybackPitch());

        speedText.setText(String.format(Locale.CANADA, "Playback Speed: %.1fx", speedBar.getProgress() / 10f));
        pitchText.setText(String.format(Locale.CANADA, "Playback Pitch: %.1fx", pitchBar.getProgress() / 10f));

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
                label.setText(String.format(Locale.CANADA, "%s: %.1fx", isSpeed ? "Playback Speed" : "Playback Pitch", value));
            }

            public void onStartTrackingTouch(SeekBar bar) {
            }

            public void onStopTrackingTouch(SeekBar bar) {
            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateRunnable);
    }
}
