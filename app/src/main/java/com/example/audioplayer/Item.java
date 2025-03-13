package com.example.audioplayer;

import android.os.Parcel;
import android.os.Parcelable;

public class Item implements Parcelable {
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
    private final String title;
    private final String artist;

    public Item(String title, String artist) {
        this.title = title;
        this.artist = artist;
    }

    protected Item(Parcel in) {
        title = in.readString();
        artist = in.readString();
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(artist);
    }
}
