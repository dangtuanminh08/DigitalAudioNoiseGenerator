package com.example.audioplayer;

import android.os.Parcel;
import android.os.Parcelable;

public class Item implements Parcelable {
    private final String title;
    private final String artist;
    private final String path;

    public Item(String title, String artist, String path) {
        this.title = title;
        this.artist = artist;
        this.path = path;
    }

    protected Item(Parcel in) {
        title = in.readString();
        artist = in.readString();
        path = in.readString();
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(artist);
        dest.writeString(path);
    }
}