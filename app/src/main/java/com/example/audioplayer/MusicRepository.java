package com.example.audioplayer;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MusicRepository {

    private static final String TAG = "MusicRepository";

    public List<Item> getMusicFiles(Context context) {
        List<Item> musicItemList = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        ArtistOverrideStore artistOverrides = new ArtistOverrideStore(context);
        String fallbackArtist = context.getString(R.string.unknown_artist);

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        try (Cursor cursor = contentResolver.query(musicUri, projection, selection, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
                int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);

                do {
                    long id = cursor.getLong(idColumn);
                    Uri fileUri = ContentUris.withAppendedId(musicUri, id);
                    String filePath = cursor.getString(dataColumn);

                    String title = cursor.getString(titleColumn);
                    if (title == null) title = "";
                    int dotIndex = title.lastIndexOf('.');
                    if (dotIndex > 0) {
                        title = title.substring(0, dotIndex);
                    }

                    String artist = artistOverrides.get(id);
                    if (artist == null) {
                        String rawArtist = cursor.getString(artistColumn);
                        artist = (rawArtist == null || MediaStore.UNKNOWN_STRING.equals(rawArtist))
                                ? fallbackArtist
                                : rawArtist;
                    }

                    String formattedDuration = formatDuration(cursor.getLong(durationColumn));

                    musicItemList.add(new Item(title, artist, filePath, formattedDuration, fileUri));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Toast.makeText(context, R.string.error_retrieving, Toast.LENGTH_SHORT).show();
        }

        musicItemList.sort((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()));
        return musicItemList;
    }

    private String formatDuration(long millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format(Locale.CANADA, "%d:%02d", minutes, seconds);
    }
}