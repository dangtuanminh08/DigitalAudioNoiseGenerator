package com.example.audioplayer;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.MyViewHolder> {

    private final List<Item> itemList;
    private final OnSongClickListener listener;
    private int selectedPosition = -1;

    public interface OnSongClickListener {
        void onSongClick(String songName, String songArtist);
    }

    public ItemAdapter(List<Item> itemList, OnSongClickListener listener) {
        this.itemList = itemList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_layout, parent, false); // Replace item_layout with your layout file
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Item currentItem = itemList.get(position);
        holder.titleTextView.setText(currentItem.getTitle());
        holder.artistTextView.setText(currentItem.getArtist());

        if (position == selectedPosition) {
            holder.titleTextView.setTextColor(Color.rgb(255, 150, 255));
        } else {
            holder.titleTextView.setTextColor(Color.WHITE);
        }

        holder.itemView.setOnClickListener(v -> {
            int previousSelectedPosition = selectedPosition;
            selectedPosition = holder.getLayoutPosition();
            notifyItemChanged(previousSelectedPosition);
            notifyItemChanged(selectedPosition);

            String itemName = currentItem.getTitle();
            String itemArtist = currentItem.getArtist();
            if (listener != null) {
                listener.onSongClick(itemName, itemArtist);
            }
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView titleTextView;
        public TextView artistTextView;

        public MyViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView); // Replace item_title with your view ID
            artistTextView = itemView.findViewById(R.id.artistTextView); // Replace item_artist with your view ID
        }
    }
}