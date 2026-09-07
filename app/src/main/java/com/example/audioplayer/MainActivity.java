package com.example.audioplayer;

import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.Manifest.permission.POST_NOTIFICATIONS;
import static android.Manifest.permission.READ_MEDIA_AUDIO;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;

    private ExoPlayer player;
    private MusicViewModel viewModel;
    private final List<Item> queue = new ArrayList<>();

    private LinearLayout miniPlayer;
    private TextView miniSongTitle, miniSongArtist;
    private GifImageView miniPlayPause;
    private GifDrawable playGif, pauseGif;

    private final Observer<List<Item>> musicFilesObserver = items -> {
        queue.clear();
        queue.addAll(items);
        ((TextView) findViewById(R.id.textView)).setText(
                String.format(Locale.CANADA, "Number of songs: %d", queue.size())
        );
    };

    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        player = PlayerManager.getPlayer(this);
        if (ForegroundService.getNotification() == null) {
            PlayerManager.showMediaNotification(getString(R.string.unknown_title), getString(R.string.beginning_notif_message));
        }
        if (!ForegroundService.isRunning()) {
            Intent serviceIntent = new Intent(this, ForegroundService.class);
            ContextCompat.startForegroundService(this, serviceIntent);
        }

        viewModel = new ViewModelProvider(this).get(MusicViewModel.class);
        setupInsets();
        setupFragment();
        setupMiniPlayerView();
        initGifs();

        viewModel.getMusicFiles().observe(this, musicFilesObserver);

        checkAndRequestPermissions();
        setupPlayerListeners();
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
    //Sets up mini player at the bottom
    @OptIn(markerClass = UnstableApi.class)
    private void setupMiniPlayerView() {
        miniPlayer = findViewById(R.id.mini_player);
        miniSongTitle = findViewById(R.id.mini_song_title);
        miniSongArtist = findViewById(R.id.mini_song_artist);
        miniPlayPause = findViewById(R.id.mini_play_pause);

        miniPlayPause.setOnClickListener(v -> togglePlayback());

        miniPlayer.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, PlayerActivity.class);
            intent.putStringArrayListExtra("SONGS", PlayerManager.getQueue());
            intent.putExtra("CURRENT_INDEX", PlayerManager.getCurrentSongIndex());
            ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(context, R.anim.slide_up, 0);
            context.startActivity(intent, options.toBundle());
        });
    }

    //Loads GIFs for use
    private void initGifs() {
        try {
            playGif = new GifDrawable(getResources(), R.drawable.play);
            pauseGif = new GifDrawable(getResources(), R.drawable.pause);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load GIFs", e);
        }
    }

    private void togglePlayback() {
        if (player == null) return;
        if (player.isPlaying()) {
            player.pause();
            miniPlayPause.setImageDrawable(pauseGif);
            pauseGif.start();
        } else {
            player.play();
            miniPlayPause.setImageDrawable(playGif);
            playGif.start();
        }
    }

    private void setupPlayerListeners() {
        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                miniPlayPause.setImageDrawable(isPlaying ? playGif : pauseGif);
                if (isPlaying) playGif.start();
            }

            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                updateMiniPlayer();
            }
        });
    }

    //Checks for permissions and requests if needed
    private void checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(READ_MEDIA_AUDIO);
        }

        if (ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(POST_NOTIFICATIONS);
        }

        if (ContextCompat.checkSelfPermission(this, BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(BLUETOOTH_CONNECT);
        }

        if (permissionsNeeded.isEmpty()) {
            viewModel.loadMusicFiles();
        } else {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        }
    }

    //Handles permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                viewModel.loadMusicFiles();
            } else {
                Toast.makeText(this, "No music for you then!! 😂", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMiniPlayer();
        if (player != null && miniPlayer.getVisibility() == View.VISIBLE) {
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
        if (player == null || player.getCurrentMediaItem() == null || queue.isEmpty()) {
            miniPlayer.setVisibility(View.GONE);
            return;
        }

        int index = player.getCurrentMediaItemIndex();
        if (index >= queue.size()) index = 0;
        Item currentItem = queue.get(index);

        if (currentItem.getTitle().contentEquals(miniSongTitle.getText()) &&
                currentItem.getArtist().contentEquals(miniSongArtist.getText())) {
            return; // No update needed
        }

        miniSongTitle.setText(currentItem.getTitle());
        miniSongArtist.setText(currentItem.getArtist());
        miniPlayer.setVisibility(View.VISIBLE);
        miniPlayPause.setImageDrawable(player.isPlaying() ? playGif : pauseGif);
    }
}
