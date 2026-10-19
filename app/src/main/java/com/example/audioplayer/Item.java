package com.example.audioplayer;

import android.net.Uri;

import androidx.annotation.Nullable;

import java.util.Objects;

public class Item {
    private final String title;
    private final String artist;
    @Nullable
    private final String path;
    private final String duration;
    private final Uri uri;

    public Item(String title, String artist, @Nullable String path, String duration, Uri uri) {
        this.title = title;
        this.artist = artist;
        this.path = path;
        this.duration = duration;
        this.uri = uri;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    @Nullable
    public String getPath() {
        return path;
    }

    public String getDuration() {
        return duration;
    }

    public Uri getUri() {
        return uri;
    }

    public String getMediaId() {
        return uri.toString();
    }

    public Item withDetails(String newTitle, String newArtist, @Nullable String newPath) {
        return new Item(newTitle, newArtist, newPath, duration, uri);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Item)) return false;
        Item item = (Item) o;
        return Objects.equals(title, item.title)
                && Objects.equals(artist, item.artist)
                && Objects.equals(path, item.path)
                && Objects.equals(duration, item.duration)
                && Objects.equals(uri, item.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, artist, path, duration, uri);
    }
}