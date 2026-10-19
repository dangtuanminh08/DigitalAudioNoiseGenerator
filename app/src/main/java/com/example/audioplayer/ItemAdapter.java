package com.example.audioplayer;

import android.app.AlertDialog;
import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Objects;

public class ItemAdapter extends ListAdapter<Item, ItemAdapter.MyViewHolder> {

    private static final DiffUtil.ItemCallback<Item> DIFF_CALLBACK = new DiffUtil.ItemCallback<Item>() {
        @Override
        public boolean areItemsTheSame(@NonNull Item oldItem, @NonNull Item newItem) {
            return oldItem.getUri().equals(newItem.getUri());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Item oldItem, @NonNull Item newItem) {
            return oldItem.equals(newItem);
        }
    };

    private final OnSongClickListener listener;
    private final OnRenameRequestListener renameListener;
    private final OnDeleteRequestListener deleteListener;
    private final int[] imageResId = {R.drawable.item_pic1, R.drawable.item_pic2, R.drawable.item_pic3};

    @Nullable
    private String playingMediaId;

    private int primaryTextColor;
    private boolean primaryTextColorResolved = false;

    public interface OnSongClickListener {
        void onSongClick(int position);
    }

    public interface OnRenameRequestListener {
        void onRenameRequested(android.net.Uri fileUri, String proposedName, String proposedArtist);
    }

    public interface OnDeleteRequestListener {
        void onDeleteRequested(android.net.Uri fileUri);
    }

    public ItemAdapter(OnSongClickListener listener,
                       OnRenameRequestListener renameListener,
                       OnDeleteRequestListener deleteListener,
                       @Nullable String initialPlayingMediaId) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.renameListener = renameListener;
        this.deleteListener = deleteListener;
        this.playingMediaId = initialPlayingMediaId;
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        final TextView titleTextView, artistTextView;
        final ImageView imageView;
        final ImageButton menuButton;

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
        MyViewHolder holder = new MyViewHolder(itemView);

        holder.itemView.setOnClickListener(v -> {
            int position = holder.getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION && listener != null) {
                listener.onSongClick(position);
            }
        });

        holder.menuButton.setOnClickListener(v -> {
            int position = holder.getBindingAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return;
            showPopupMenu(holder.menuButton, getItem(position));
        });

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Item item = getItem(position);
        Context context = holder.itemView.getContext();

        holder.titleTextView.setText(item.getTitle());
        holder.artistTextView.setText(String.format("%s • %s", item.getArtist(), item.getDuration()));
        holder.imageView.setImageResource(imageResId[position % imageResId.length]);

        boolean isPlaying = item.getMediaId().equals(playingMediaId);
        holder.titleTextView.setTextColor(
                isPlaying
                        ? ContextCompat.getColor(context, R.color.primary)
                        : getPrimaryTextColor(context)
        );
    }

    public void setCurrentPlayingMediaId(@Nullable String mediaId) {
        if (Objects.equals(mediaId, playingMediaId)) return;

        String previous = playingMediaId;
        playingMediaId = mediaId;

        List<Item> items = getCurrentList();
        for (int i = 0; i < items.size(); i++) {
            String id = items.get(i).getMediaId();
            if (id.equals(previous) || id.equals(mediaId)) {
                notifyItemChanged(i);
            }
        }
    }

    private void showPopupMenu(View anchor, Item item) {
        Context context = anchor.getContext();
        PopupMenu popupMenu = new PopupMenu(context, anchor);
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
    }

    private void showRenameDialog(Item item, Context context) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.alert_dialog, null);
        EditText editTitle = dialogView.findViewById(R.id.new_title);
        EditText editArtist = dialogView.findViewById(R.id.new_artist);

        editTitle.setText(item.getTitle());
        editArtist.setText(item.getArtist());

        new AlertDialog.Builder(context)
                .setTitle(R.string.rename_title)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String newTitle = editTitle.getText().toString().trim();
                    String newArtist = editArtist.getText().toString().trim();
                    if (!newTitle.isEmpty() && renameListener != null) {
                        renameListener.onRenameRequested(item.getUri(), newTitle, newArtist);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmDeletion(Item item, Context context) {
        new AlertDialog.Builder(context)
                .setMessage(context.getString(R.string.delete_confirm, item.getTitle()))
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    if (deleteListener != null) {
                        deleteListener.onDeleteRequested(item.getUri());
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private int getPrimaryTextColor(Context context) {
        if (!primaryTextColorResolved) {
            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
            primaryTextColor = ContextCompat.getColor(context, typedValue.resourceId);
            primaryTextColorResolved = true;
        }
        return primaryTextColor;
    }
}