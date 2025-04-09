package com.example.audioplayer;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MusicRepository {

    public List<Item> getMusicFiles(Context context) {
        List<Item> songList = new ArrayList<>();

        String[] projection = {MediaStore.Audio.Media.DATA}; // Gets the file path
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"; // Filters for music files only

        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null);
        if ((cursor != null) && cursor.moveToFirst()) {
            int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            do {
                String filePath = cursor.getString(dataColumn);
                String fileName = getFileNameFromPath(filePath);
                fileName = fileName.replaceFirst("[.][^.]+$", "");
                songList.add(new Item(fileName, "Description", filePath));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return songList;
    }

    private String getFileNameFromPath(String filePath) {
        return new File(filePath).getName();
    }
}