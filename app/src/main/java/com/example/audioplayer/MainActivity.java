package com.example.audioplayer;

import static android.Manifest.permission.READ_MEDIA_AUDIO;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.session.MediaSession;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;


@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class MainActivity extends AppCompatActivity implements ItemAdapter.OnSongClickListener {

    private ItemAdapter adapter;
    private ExoPlayer player;
    private MusicViewModel viewModel;
    private List<Item> songs;
    private List<Item> playlists;
    private MediaSession mediaSession;
    private final String[] permissionList = {READ_MEDIA_AUDIO};
    private static final int MY_PERMISSIONS_REQUEST_READ_MEDIA_AUDIO = 0;
    private LinearLayout miniPlayer;
    private TextView miniSongTitle, miniSongArtist;
    private GifImageView miniPlayPause;

    @SuppressLint("NotifyDataSetChanged")
    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);

        viewModel = new ViewModelProvider(this).get(MusicViewModel.class);

        songs = new ArrayList<>();
        playlists = new ArrayList<>();
        playlists.add(new Item("In Development", "This feature is not implemented yet.", ""));

        adapter = new ItemAdapter(songs, this);

        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), getLifecycle(), songs, playlists);
        viewPager.setAdapter(viewPagerAdapter);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Songs");
            } else if (position == 1) {
                tab.setText("Playlists");
            }
        }).attach();

        viewModel.getMusicFiles().observe(this, items -> {
            songs.clear();
            songs.addAll(items);
            adapter.notifyDataSetChanged();
            TextView testTextView = findViewById(R.id.textView);
            testTextView.setText(String.format(Locale.CANADA, "Number of songs: %d", songs.size()));
        });

        miniPlayer = findViewById(R.id.mini_player);
        miniSongTitle = findViewById(R.id.mini_song_title);
        miniSongArtist = findViewById(R.id.mini_song_artist);
        miniPlayPause = findViewById(R.id.mini_play_pause);

        GifDrawable playGif;
        GifDrawable pauseGif;
        try {
            playGif = new GifDrawable(getResources(), R.drawable.play);
            pauseGif = new GifDrawable(getResources(), R.drawable.pause);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        GifDrawable finalPlayGif = playGif;
        GifDrawable finalPauseGif = pauseGif;

        miniPlayPause.setOnClickListener(v -> {
            if (player.isPlaying()) {
                player.pause();
                miniPlayPause.setImageDrawable(finalPauseGif);
                finalPauseGif.start();

            } else {
                player.play();
                miniPlayPause.setImageDrawable(finalPlayGif);
                finalPlayGif.start();
            }
        });

        // Opens full PlayerActivity when clicked
        miniPlayer.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, PlayerActivity.class);

            intent.putStringArrayListExtra("SONGS", PlayerManager.getQueue());
            intent.putExtra("CURRENT_INDEX", PlayerManager.getCurrentSongIndex());

            ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(context, R.anim.slide_up, 0);
            context.startActivity(intent, options.toBundle());
        });

        player = PlayerManager.getPlayer(this);
        player.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                updateMiniPlayer();
            }
        });

        if (ContextCompat.checkSelfPermission(this, READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissionList, MY_PERMISSIONS_REQUEST_READ_MEDIA_AUDIO);
        } else {
            // Permissions already granted, load music files
            viewModel.loadMusicFiles();
        }
    }

    /*
    Requests permissions and only displays the song list if the permission is granted.
    If permission is denied, the app will request every time it is opened until permission is granted.
    */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_READ_MEDIA_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                viewModel.loadMusicFiles();
            } else {
                Toast.makeText(this, "No music for you then!! 😂 (Reopen the app to accept permissions)", Toast.LENGTH_SHORT).show();
            }
        }
    }

    protected void onResume() {
        super.onResume();
        updateMiniPlayer();
    }

    private void updateMiniPlayer() {
        if (player.getCurrentMediaItem() != null) {
            Item currentItem = songs.get(player.getCurrentMediaItemIndex());

            miniSongTitle.setText(currentItem.getTitle());
            miniSongArtist.setText(currentItem.getArtist());
            miniPlayer.setVisibility(View.VISIBLE); // Shows mini player when a song is playing

            if (player.isPlaying()) {
                miniPlayPause.setImageResource(R.drawable.play);
            } else {
                miniPlayPause.setImageResource(R.drawable.pause);
            }
        } else {
            miniPlayer.setVisibility(View.GONE); // Hides mini player if no song is playing
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onSongClick(String songName, String songArtist) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
    }
}

//Add pop-up menu in player
//Make speed/pitch menu