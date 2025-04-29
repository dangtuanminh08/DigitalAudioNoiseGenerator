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
    private static ArrayList<String> queue;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isUserSeeking = false;
    private ExoPlayer player;
    private TextView songTitle;
    private TextView artistName;
    private TextView currentTime;
    private TextView totalTime;
    private SeekBar lengthBar;
    private GifDrawable playGif, pauseGif;

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

    private ImageButton btnShuffle;
    private ImageButton btnRepeat;
    private GifImageView btnPlayPause;
    private PlaybackSpeedManager playbackSpeedManager;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        PlayerView playerView = findViewById(R.id.player_view);
        songTitle = findViewById(R.id.song_title);
        artistName = findViewById(R.id.artist_name);
        lengthBar = findViewById(R.id.seek_bar);
        currentTime = findViewById(R.id.current_time);
        totalTime = findViewById(R.id.total_time);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        ImageButton btnNext = findViewById(R.id.btn_next);
        ImageButton btnPrev = findViewById(R.id.btn_prev);
        btnShuffle = findViewById(R.id.btn_shuffle);
        btnRepeat = findViewById(R.id.btn_repeat);
        ImageButton backButton = findViewById(R.id.back_button);
        ImageButton playerMenu = findViewById(R.id.player_menu);

        //GifDrawable must be declared this way
        try {
            playGif = new GifDrawable(getResources(), R.drawable.play);
            pauseGif = new GifDrawable(getResources(), R.drawable.pause);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        player = PlayerManager.getPlayer(this);
        playerView.setPlayer(player);
        playbackSpeedManager = new PlaybackSpeedManager(player);

        if (playerMenu != null) {
            playerMenu.setOnClickListener(v -> showBottomSheet());
        }

        // Exits PlayerActivity
        backButton.setOnClickListener(v -> finish());

        //The intent from ItemAdapter
        Intent intent = getIntent();
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

        //Keeping repeat/shuffle states
        updateShuffleButton();
        updateRepeatButton();

        //Playback controls
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnNext.setOnClickListener(v -> nextSong());
        btnPrev.setOnClickListener(v -> prevSong());
        btnRepeat.setOnClickListener(v -> toggleRepeat());
        btnShuffle.setOnClickListener(v -> toggleShuffle());

        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying) {
                    btnPlayPause.setImageDrawable(playGif);
                    playGif.start();
                    NotificationManager.setPlaybackState(3);
                    startUpdatingSeekBar();
                } else {
                    btnPlayPause.setImageDrawable(pauseGif);
                    pauseGif.start();
                    NotificationManager.setPlaybackState(2);
                    handler.removeCallbacks(updateRunnable);
                }
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
            public void onPositionDiscontinuity(@NonNull Player.PositionInfo oldPosition,
                                                @NonNull Player.PositionInfo newPosition, int reason) {
                if (!isUserSeeking) {
                    lengthBar.setProgress((int) player.getCurrentPosition());
                    currentTime.setText(formatTime(player.getCurrentPosition()));
                }
            }
        });

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

    //BottomSheet for player speed/pitch
    private void showBottomSheet() {
        BottomSheetDialog playerSettings = new BottomSheetDialog(this);
        View playerSettingsView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.player_settings, null);

        TextView speedText = playerSettingsView.findViewById(R.id.speed_text);
        TextView pitchText = playerSettingsView.findViewById(R.id.pitch_text);
        SeekBar speedSeekBar = playerSettingsView.findViewById(R.id.speed_bar);
        SeekBar pitchSeekBar = playerSettingsView.findViewById(R.id.pitch_bar);

        speedSeekBar.incrementProgressBy(1);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            speedSeekBar.setMin(1);
        }
        speedSeekBar.setMax(20);
        speedSeekBar.setProgress((int) playbackSpeedManager.getPlaybackSpeed());
        speedText.setText(String.format(Locale.CANADA, "Playback Speed: %.1fx", speedSeekBar.getProgress() / 10f));

        pitchSeekBar.incrementProgressBy(1);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pitchSeekBar.setMin(1);
        }
        pitchSeekBar.setMax(20);
        pitchSeekBar.setProgress((int) playbackSpeedManager.getPlaybackPitch());
        pitchText.setText(String.format(Locale.CANADA, "Playback Pitch: %.1fx", pitchSeekBar.getProgress() / 10f));

        speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar lengthBar, int progress, boolean fromUser) {
                float speed = (float) progress / 10.0f;
                playbackSpeedManager.setPlaybackSpeed(speed);
                speedText.setText(String.format(Locale.CANADA, "Playback Speed: %.1fx", speed));
            }

            @Override
            public void onStartTrackingTouch(SeekBar lengthBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar lengthBar) {
            }
        });

        pitchSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar lengthBar, int progress, boolean fromUser) {
                float pitch = (float) progress / 10.0f;
                playbackSpeedManager.setPlaybackPitch(pitch);
                pitchText.setText(String.format(Locale.CANADA, "Playback Pitch: %.1fx", pitch));
            }

            @Override
            public void onStartTrackingTouch(SeekBar lengthBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar lengthBar) {
            }
        });

        playerSettings.setContentView(playerSettingsView);
        playerSettings.show();
    }

    //Helper functions
    private void togglePlayPause() {
        if (player.isPlaying()) {
            player.pause();
        } else {
            player.play();
        }
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
            if (player.getCurrentMediaItemIndex() == 0) {
                player.seekTo(queue.size() - 1, 0);
            } else {
                player.seekToPreviousMediaItem();
            }
        } else {
            player.seekTo(0);
        }
        updateSongHighlight();
    }

    private void toggleShuffle() {
        boolean shuffleEnabled = !player.getShuffleModeEnabled();
        player.setShuffleModeEnabled(shuffleEnabled);
        updateShuffleButton();
        Toast.makeText(this, shuffleEnabled ? "Shuffle ON" : "Shuffle OFF", Toast.LENGTH_SHORT).show();
    }

    private void toggleRepeat() {
        int mode = player.getRepeatMode();
        if (mode == Player.REPEAT_MODE_OFF) {
            player.setRepeatMode(Player.REPEAT_MODE_ALL);
        } else if (mode == Player.REPEAT_MODE_ALL) {
            player.setRepeatMode(Player.REPEAT_MODE_ONE);
        } else {
            player.setRepeatMode(Player.REPEAT_MODE_OFF);
        }
        updateRepeatButton();
        Toast.makeText(this, getRepeatModeText(), Toast.LENGTH_SHORT).show();
    }

    private void updateShuffleButton() {
        int color = player.getShuffleModeEnabled() ? Color.WHITE : Color.argb(255, 186, 186, 186);
        btnShuffle.setColorFilter(color);
    }

    private void updateRepeatButton() {
        int repeatMode = player.getRepeatMode();
        if (repeatMode == Player.REPEAT_MODE_ONE) {
            btnRepeat.setImageResource(R.drawable.repeat_one);
        } else if (repeatMode == Player.REPEAT_MODE_ALL) {
            btnRepeat.setImageResource(R.drawable.repeat);
        } else {
            btnRepeat.setImageResource(R.drawable.order);
        }
    }

    private String getRepeatModeText() {
        switch (player.getRepeatMode()) {
            case Player.REPEAT_MODE_ONE:
                return "Repeat song";
            case Player.REPEAT_MODE_ALL:
                return "Repeat queue";
            default:
                return "Repeat off";
        }
    }

    //Makes sure the total time never displays C.TIME_UNSET (a gigantic negative number)
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

    //Part of the highlight updating
    private void updateSongHighlight() {
        if (player != null) {
            String currentSongPath = queue.get(player.getCurrentMediaItemIndex());

            Intent intent = new Intent();
            intent.putExtra("CURRENT_SONG", currentSongPath);
            setResult(RESULT_OK, intent);
        }
    }

    //Changes the text views to reflect the data of the currently playing song
    private void setPlayerDisplayText() {
        String songPath = queue.get(player.getCurrentMediaItemIndex());
        File file = new File(songPath);
        String songFile = file.getName();
        String songName = songFile.replaceFirst("[.][^.]+$", "");
        songTitle.setText(songName);
        artistName.setText(R.string.unknown_artist);

        updateSeekBarAndTime();
        startUpdatingSeekBar();

        PlayerManager.showMediaNotification(songName, "Unknown Artist");
    }

    private void startUpdatingSeekBar() {
        handler.post(updateRunnable);
    }

    //Converts milliseconds to minutes and seconds
    private String formatTime(long millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format(Locale.CANADA, "%d:%02d", minutes, seconds);
    }

    //Destroy :(
    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateRunnable);
    }
}
