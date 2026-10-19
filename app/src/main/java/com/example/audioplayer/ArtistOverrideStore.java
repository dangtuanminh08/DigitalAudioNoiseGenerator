package com.example.audioplayer;

import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.Nullable;
public class ArtistOverrideStore {

    private static final String PREFS_NAME = "artist_overrides";

    private final SharedPreferences prefs;

    public ArtistOverrideStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Nullable
    public String get(long mediaId) {
        return prefs.getString(String.valueOf(mediaId), null);
    }

    public void set(Uri fileUri, String artist) {
        prefs.edit().putString(String.valueOf(ContentUris.parseId(fileUri)), artist).apply();
    }

    public void remove(Uri fileUri) {
        prefs.edit().remove(String.valueOf(ContentUris.parseId(fileUri))).apply();
    }
}