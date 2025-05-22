package com.example.audioplayer;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.MyViewHolder> {

    private final List<Item> itemList;
    private final OnSongClickListener listener;
    private final OnRenameRequestListener renameListener;
    private final OnDeleteRequestListener deleteListener;
    private int selectedPosition = -1;
    private final int[] imageResId = {R.drawable.item_pic1, R.drawable.item_pic2, R.drawable.item_pic3};

    public interface OnSongClickListener {
        void onSongClick(String songName, String songArtist);
    }

    public interface OnRenameRequestListener {
        void onRenameRequested(Uri fileUri, String proposedName, String proposedArtist);
    }

    public interface OnDeleteRequestListener {
        void onDeleteRequested(Uri fileUri);
    }

    public ItemAdapter(List<Item> itemList,
                       OnSongClickListener listener,
                       OnRenameRequestListener renameListener,
                       OnDeleteRequestListener deleteListener) {
        this.itemList = itemList;
        this.listener = listener;
        this.renameListener = renameListener;
        this.deleteListener = deleteListener;
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, artistTextView;
        ImageView imageView;
        ImageButton menuButton;

        public MyViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            artistTextView = itemView.findViewById(R.id.artistTextView);
            imageView = itemView.findViewById(R.id.imageView);
            menuButton = itemView.findViewById(R.id.menuButton);
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
        Item item = itemList.get(position);
        Context context = holder.itemView.getContext();

        holder.titleTextView.setText(item.getTitle());
        holder.artistTextView.setText(String.format("%s • %s", item.getArtist(), item.getDuration()));
        holder.imageView.setImageResource(imageResId[position % imageResId.length]);

        // Highlight the currently playing song (either user tapped or the one being played)
        int currentPlayingPosition = -1;
        int primaryTextColor = getThemeColor(context);
        holder.titleTextView.setTextColor(
                position == selectedPosition || position == currentPlayingPosition
                        ? Color.argb(255, 255, 133, 182)
                        : primaryTextColor
        );

        holder.itemView.setOnClickListener(v -> {
            String itemName = item.getTitle();
            String itemArtist = item.getArtist();
            if (listener != null) {
                listener.onSongClick(itemName, itemArtist);
            }
        });

        holder.menuButton.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(context, holder.menuButton);
            popupMenu.getMenuInflater().inflate(R.menu.context_menu, popupMenu.getMenu());

            popupMenu.setOnMenuItemClickListener(menuItem -> {
                int id = menuItem.getItemId();
                if (id == R.id.delete) {
                    confirmDeletion(item, context);
                    return true;
                } else if (id == R.id.rename) {
                    showRenameDialog(item, context);
                    return true;
                }
                return false;
            });

            popupMenu.show();
        });
    }

    // Track currently playing song position
    public void setCurrentPlayingSong(int position) {
        if (position == selectedPosition) return;

        int previousSelected = selectedPosition;
        selectedPosition = position;

        if (previousSelected != -1) notifyItemChanged(previousSelected);
        if (selectedPosition != -1) notifyItemChanged(selectedPosition);
    }

    private void showRenameDialog(Item item, Context context) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.alert_dialog, null);
        EditText editTitle = dialogView.findViewById(R.id.new_title);
        EditText editArtist = dialogView.findViewById(R.id.new_artist);

        editTitle.setText(item.getTitle());
        editArtist.setText(item.getArtist());

        new AlertDialog.Builder(context)
                .setTitle("Rename File")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newTitle = editTitle.getText().toString().trim();
                    String newArtist = editArtist.getText().toString().trim();
                    if (!newTitle.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        Uri uri = MediaFileManager.getAudioContentUriFromPath(context, item.getPath());
                        if (renameListener != null && uri != null) {
                            renameListener.onRenameRequested(uri, newTitle, newArtist);
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDeletion(Item item, Context context) {
        new AlertDialog.Builder(context)
                .setMessage("Are you sure you want to delete \"" + item.getTitle() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        Uri uri = item.getUri();
                        if (deleteListener != null && uri != null) {
                            deleteListener.onDeleteRequested(uri);
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    //Gets a specific color from theme attribute (dark or light)
    private int getThemeColor(Context context) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        return ContextCompat.getColor(context, typedValue.resourceId);
    }
}