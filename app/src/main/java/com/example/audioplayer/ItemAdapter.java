package com.example.audioplayer;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.media3.exoplayer.ExoPlayer;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {

    private List<Item> itemList;
    private Context context;
    private String currentSongPath = null; // Stores the path of the currently playing song



    public ItemAdapter(Context context, List<Item> itemList) {
        this.context = context;
        this.itemList = itemList;
    }

    public void ReturnSong(String songName, View v) {
        MainActivity.playSong(songName, v.getContext());
        getSong(songName); // Update current song index!
    }

    public void getSong(String songPath) {
        currentSongPath = songPath;  // Update the song path
        notifyDataSetChanged(); // Refresh RecyclerView
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item layout (take XML and parse it to create view from elements)
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_layout, parent, false);

        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = itemList.get(position);
        String fileName = new File(item.getTitle()).getName();
        String songName = fileName.replaceFirst("[.][^.]+$", "");
        holder.titleTextView.setText(songName);
        holder.artistTextView.setText(item.getArtist());
        holder.itemView.setOnClickListener(v -> {
            String itemName = item.getTitle();
            String itemDescription = item.getArtist();
            ReturnSong(itemName, v);
        });

        if (item.getTitle().equals(currentSongPath)) {
            Log.d("ItemAdapter", "Set");
            holder.titleTextView.setTextColor(ContextCompat.getColor(context, R.color.purple_200));
        } else {
            Log.d("ItemAdapter", "Not set");
            holder.titleTextView.setTextColor(ContextCompat.getColor(context, R.color.white));
        }
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    private String formatTime(long millis) {
        return String.format("%d:%02d", TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) % 60);
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, artistTextView;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            artistTextView = itemView.findViewById(R.id.artistTextView);
        }
    }
}
