package com.example.audioplayer;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return TabViewFragment.newInstance("songs");
            case 1:
                return TabViewFragment.newInstance("playlists");
            default:
                return new Fragment(); // Default case (empty fragment)
        }
    }

    @Override
    public int getItemCount() {
        return 2; // DO NOT CHANGE or there will be no tabs :(
    }
}