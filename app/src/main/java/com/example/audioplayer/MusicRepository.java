package com.example.audioplayer;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MusicRepository {

    private static final String TAG = "MusicRepository";

    public List<Item> getMusicFiles(Context context) {
        List<Item> musicItemList = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        try (Cursor cursor = contentResolver.query(musicUri, projection, selection, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);

                do {
                    String filePath = cursor.getString(dataColumn);
                    String title = cursor.getString(titleColumn);
                    String artist = cursor.getString(artistColumn);
                    long duration = cursor.getLong(durationColumn);
                    String formattedDuration = formatDuration(duration);

                    musicItemList.add(new Item(title, String.format("%s • %s", artist, formattedDuration), filePath));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving music files", e);
        }

        return musicItemList;
    }

    private String formatDuration(long millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format(Locale.CANADA, "%d:%02d", minutes, seconds);
    }
}