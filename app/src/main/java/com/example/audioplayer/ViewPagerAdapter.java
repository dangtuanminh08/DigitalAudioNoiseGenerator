package com.example.audioplayer;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.List;

public class ViewPagerAdapter extends FragmentStateAdapter {

    private final ArrayList<Item> songs;
    private final ArrayList<Item> playlists;

    public ViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle, List<Item> songs, List<Item> playlists) {
        super(fragmentManager, lifecycle);
        this.songs = (ArrayList<Item>) songs;
        this.playlists = (ArrayList<Item>) playlists;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return TabViewFragment.newInstance(songs);
            case 1:
                return TabViewFragment.newInstance(playlists);
            default:
                return new Fragment(); // Default case (empty fragment)
        }
    }

    @Override
    public int getItemCount() {
        return 2; // DO NOT CHANGE or there will be no tabs :(
    }
}