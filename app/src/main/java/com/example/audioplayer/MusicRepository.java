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

    //Reads all data from all music files in internal storage
    public List<Item> getMusicFiles(Context context) {
        List<Item> musicItemList = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

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
                    Uri fileUri = Uri.withAppendedPath(musicUri, String.valueOf(id));
                    String filePath = cursor.getString(dataColumn);
                    String title = cursor.getString(titleColumn);
                    title = title.substring(0, title.lastIndexOf('.'));
                    String artist = cursor.getString(artistColumn).equals("<unknown>") ? "( •̀ ω •́ )" : cursor.getString(artistColumn);
                    Log.d("TAG", artist);
                    long duration = cursor.getLong(durationColumn);
                    String formattedDuration = formatDuration(duration);

                    musicItemList.add(new Item(title, artist, filePath, formattedDuration, fileUri));
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