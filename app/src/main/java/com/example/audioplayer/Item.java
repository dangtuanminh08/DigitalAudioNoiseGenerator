package com.example.audioplayer;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class Item implements Parcelable {
    private String title;
    private String artist;
    private String path;
    private final String duration;
    private final Uri uri;

    public Item(String title, String artist, String path, String duration, Uri uri) {
        this.title = title;
        this.artist = artist;
        this.path = path;
        this.duration = duration;
        this.uri = uri;
    }

    protected Item(Parcel in) {
        title = in.readString();
        artist = in.readString();
        path = in.readString();
        duration = in.readString();
        uri = in.readParcelable(Uri.class.getClassLoader());
    }

    public static final Creator<Item> CREATOR = new Creator<Item>() {
        @Override
        public Item createFromParcel(Parcel in) {
            return new Item(in);
        }

        @Override
        public Item[] newArray(int size) {
            return new Item[size];
        }
    };

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getPath() {
        return path;
    }

    public String getDuration() {
        return duration;
    }

    public Uri getUri() {
        return uri;
    }

    public void setTitle(String newTitle) {
        this.title = newTitle;
    }

    public void setArtist(String newArtist) {
        this.artist = newArtist;
    }

    public void setPath(String newPath) {
        this.path = newPath;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(artist);
        dest.writeString(path);
        dest.writeString(duration);
        dest.writeParcelable(uri, flags);
    }
}