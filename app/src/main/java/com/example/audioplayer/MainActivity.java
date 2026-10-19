package com.example.audioplayer;

import static android.Manifest.permission.POST_NOTIFICATIONS;
import static android.Manifest.permission.READ_MEDIA_AUDIO;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;

import java.io.IOException;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class MainActivity extends AppCompatActivity {

    static final String EXTRA_RELEASE_PLAYER = "com.example.audioplayer.extra.RELEASE_PLAYER";

    private ExoPlayer player;
    private MusicViewModel viewModel;

    private LinearLayout miniPlayer;
    private TextView miniSongTitle, miniSongArtist, songCountText;
    private GifImageView miniPlayPause;
    private GifDrawable playGif, pauseGif;

    private final ActivityResultLauncher<String> audioPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    viewModel.loadMusicFiles();
                } else {
                    Toast.makeText(this, R.string.permission_denied_toast, Toast.LENGTH_SHORT).show();
                }
                requestNotificationPermission();
            });

    private final ActivityResultLauncher<String> notificationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                // No notification will be shown. Playback will still work
            });
    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            setPlayPauseIcon(isPlaying);
        }

        @OptIn(markerClass = UnstableApi.class)
        @Override
        public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
            updateMiniPlayer();
        }

        @Override
        public void onTimelineChanged(@NonNull Timeline timeline, int reason) {
            updateMiniPlayer();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null && getIntent().getBooleanExtra(EXTRA_RELEASE_PLAYER, false)) {
            getIntent().removeExtra(EXTRA_RELEASE_PLAYER);
            PlayerManager.shutdown(this);
        }

        player = PlayerManager.getPlayer(this);
        viewModel = new ViewModelProvider(this).get(MusicViewModel.class);
        songCountText = findViewById(R.id.textView);

        setupInsets();
        setupFragment();
        setupMiniPlayerView();
        initGifs();

        viewModel.getMusicFiles().observe(this, items ->
                songCountText.setText(getString(R.string.song_count, items == null ? 0 : items.size())));

        checkAndRequestPermissions();
    }

    private void setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupFragment() {
        if (getSupportFragmentManager().findFragmentById(R.id.fragment_container) == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, ViewFragment.newInstance())
                    .commit();
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    private void setupMiniPlayerView() {
        miniPlayer = findViewById(R.id.mini_player);
        miniSongTitle = findViewById(R.id.mini_song_title);
        miniSongArtist = findViewById(R.id.mini_song_artist);
        miniPlayPause = findViewById(R.id.mini_play_pause);

        miniPlayPause.setOnClickListener(v -> PlayerManager.togglePlayPause());

        miniPlayer.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, PlayerActivity.class);
            ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(context, R.anim.slide_up, 0);
            context.startActivity(intent, options.toBundle());
        });
    }

    private void initGifs() {
        try {
            playGif = new GifDrawable(getResources(), R.drawable.play);
            pauseGif = new GifDrawable(getResources(), R.drawable.pause);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load GIFs", e);
        }
    }

    private void setPlayPauseIcon(boolean isPlaying) {
        GifDrawable gif = isPlaying ? playGif : pauseGif;
        miniPlayPause.setImageDrawable(gif);
        gif.start();
    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            if (viewModel.getMusicFiles().getValue() == null) {
                viewModel.loadMusicFiles();
            }
            requestNotificationPermission();
        } else {
            audioPermissionLauncher.launch(READ_MEDIA_AUDIO);
        }
    }

    private void requestNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(POST_NOTIFICATIONS);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        player.addListener(playerListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        player.removeListener(playerListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMiniPlayer();
        if (miniPlayer.getVisibility() == View.VISIBLE) {
            (player.isPlaying() ? playGif : pauseGif).start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        playGif.stop();
        pauseGif.stop();
    }

    private void updateMiniPlayer() {
        MediaItem current = player.getCurrentMediaItem();
        if (current == null) {
            miniPlayer.setVisibility(View.GONE);
            return;
        }

        CharSequence title = current.mediaMetadata.title;
        CharSequence artist = current.mediaMetadata.artist;

        if (miniPlayer.getVisibility() == View.VISIBLE
                && TextUtils.equals(title, miniSongTitle.getText())
                && TextUtils.equals(artist, miniSongArtist.getText())) {
            return;
        }

        miniSongTitle.setText(title);
        miniSongArtist.setText(artist);
        miniPlayer.setVisibility(View.VISIBLE);
        setPlayPauseIcon(player.isPlaying());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (playGif != null) playGif.recycle();
        if (pauseGif != null) pauseGif.recycle();
    }
}