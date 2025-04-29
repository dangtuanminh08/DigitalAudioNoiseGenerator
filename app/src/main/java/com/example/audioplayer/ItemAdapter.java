package com.example.audioplayer;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.MyViewHolder> {

    private final List<Item> itemList;
    private final OnSongClickListener listener;
    private int selectedPosition = -1;
    private final int[] imageResId = {R.drawable.item_pic1, R.drawable.item_pic2, R.drawable.item_pic3};

    public interface OnSongClickListener {
        void onSongClick(String songName, String songArtist);
    }

    public ItemAdapter(List<Item> itemList, OnSongClickListener listener) {
        this.itemList = itemList;
        this.listener = listener;
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView titleTextView;
        public TextView artistTextView;
        public ImageView imageView;

        public MyViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            artistTextView = itemView.findViewById(R.id.artistTextView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_layout, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Item currentItem = itemList.get(position);
        holder.titleTextView.setText(currentItem.getTitle());
        holder.artistTextView.setText(currentItem.getArtist());
        holder.imageView.setImageResource(imageResId[position % imageResId.length]);
        if (position == selectedPosition) {
            holder.titleTextView.setTextColor(Color.argb(255, 255, 120, 190));
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
}