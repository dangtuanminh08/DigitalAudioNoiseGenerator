package com.example.audioplayer;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {

    private List<Item> itemList;
    private final Context context;
    private String currentSongPath = null; // Stores the path of the currently playing song



    public ItemAdapter(Context context, List<Item> itemList) {
        this.context = context;
        this.itemList = itemList;
    }

    public void returnSong(String songPath, View v) {
        MainActivity.playSong(songPath, v.getContext());
        updateCurrentSong(songPath); // Update current song index!
    }

    public void updateCurrentSong(String songPath) {
        currentSongPath = songPath;
        notifyDataSetChanged();
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
            returnSong(itemName, v);
        });

        if (item.getTitle().equals(currentSongPath)) {
            Log.d("ItemAdapter", "Current: " + item.getTitle());
            holder.titleTextView.setTextColor(ContextCompat.getColor(context, R.color.purple_200));
        } else {
            holder.titleTextView.setTextColor(ContextCompat.getColor(context, R.color.white));
        }
    }

    @Override
    public int getItemCount() {
        return itemList.size();
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
