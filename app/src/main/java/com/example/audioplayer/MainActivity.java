package com.example.audioplayer;

import static android.Manifest.permission.READ_MEDIA_AUDIO;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_READ_MEDIA_AUDIO = 1;
    private static ExoPlayer player;
    private static MediaSession mediaSession;
    private static ViewPager2 viewPager;
    private static ArrayList<String> songList;
    private static ArrayList<String> queue;
    private static ItemAdapter itemAdapter;
    private final String[] permissionList = {READ_MEDIA_AUDIO};

    @OptIn(markerClass = UnstableApi.class)
    public static void playSong(String songPath, Context context) {
        int index = songList.indexOf(songPath);
        if (index == -1) return;
        if (queue == null) queue = new ArrayList<>(songList);

        queue = new ArrayList<>(songList.subList(index, songList.size()));
        queue.addAll(songList.subList(0, index));


        Intent intent = new Intent(context, PlayerActivity.class);
        intent.putExtra("SONGS", queue);
        context.startActivity(intent);
    }

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


        viewPager = findViewById(R.id.viewPager);
        songList = new ArrayList<>();
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager(), getLifecycle(), songList);
        viewPager.setAdapter(adapter);

        player = PlayerManager.getPlayer(this);

        itemAdapter = new ItemAdapter(this, new ArrayList<>());
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Songs");
            } else if (position == 1) {
                tab.setText("Playlists");
            }
        }).attach();

        if (ContextCompat.checkSelfPermission(this, READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissionList, MY_PERMISSIONS_REQUEST_READ_MEDIA_AUDIO);
        } else {
            listMusicFiles(songList);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_PERMISSIONS_REQUEST_READ_MEDIA_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                listMusicFiles(songList);
            } else {
                Toast.makeText(this, "Permission denied, cannot load songs :(", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void listMusicFiles(List<String> songList) {
        String[] projection = {MediaStore.Audio.Media.DATA};
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        Cursor cursor = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null);
        if ((cursor != null) && cursor.moveToFirst()) {
            int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            String filePath;
            do {
                filePath = cursor.getString(dataColumn);
                songList.add(filePath);
                Log.d("Song", filePath);

            } while (cursor.moveToNext());
            cursor.close();
            TextView testTextView = findViewById(R.id.textView);
            testTextView.setText(String.format("Number of songs: %d", songList.size())); //delete this when app is done

            ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager(), getLifecycle(), (ArrayList<String>) songList);
            viewPager.setAdapter(adapter);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) { // 1 = request code for PlayerActivity
            if (data != null && data.hasExtra("CURRENT_SONG")) {
                String currentSongPath = data.getStringExtra("CURRENT_SONG");

                Log.d("DEBUG", "Received song update: " + currentSongPath);
                itemAdapter.updateCurrentSong(currentSongPath); // Update adapter
            }
        }
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
