package com.example.audioplayer;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TabViewFragment extends Fragment implements
        ItemAdapter.OnSongClickListener,
        ItemAdapter.OnRenameRequestListener,
        ItemAdapter.OnDeleteRequestListener {

    private Context context;
    private final List<Item> itemList = new ArrayList<>();
    public static ItemAdapter adapter;

    private ActivityResultLauncher<IntentSenderRequest> renameLauncher;
    private ActivityResultLauncher<IntentSenderRequest> deleteLauncher;

    private Uri fileToRename, fileToDelete;
    private String newFileName;

    private static final String ARG_TAB_TYPE = "tab_type";
    private String tabType; // "songs" or "playlists"

    public static TabViewFragment newInstance(String tabType) {
        TabViewFragment fragment = new TabViewFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TAB_TYPE, tabType);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        context = requireContext();
        View view = inflater.inflate(R.layout.recycler_view, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(adapter = new ItemAdapter(itemList, this, this, this));
        recyclerView.setVerticalScrollBarEnabled(false);
        recyclerView.addItemDecoration(new TopSpacingItemDecoration(64));

        tabType = getArguments() != null ? getArguments().getString(ARG_TAB_TYPE, "songs") : "songs";

        setupViewModel();
        setupRenameLauncher();
        setupDeleteLauncher();

        return view;
    }

    private void setupViewModel() {
        MusicViewModel viewModel = new ViewModelProvider(requireActivity()).get(MusicViewModel.class);
        viewModel.getMusicFiles().observe(getViewLifecycleOwner(), items -> {
            itemList.clear();
            if ("songs".equals(tabType)) {
                if (items != null) itemList.addAll(items);
            } else if ("playlists".equals(tabType)) {
                itemList.add(new Item("In Development", "This feature is not implemented yet.", "", null));
            }
            adapter.notifyDataSetChanged();
        });
    }

    //Creates the permission launcher for renaming files
    private void setupRenameLauncher() {
        renameLauncher = registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && fileToRename != null && newFileName != null) {
                String actualPath = MediaFileManager.getRealPathFromUri(context, fileToRename);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaFileManager.renameAudioFile(context, fileToRename, newFileName);
                }

                for (Item item : itemList) {
                    if (item.getPath().equals(actualPath)) {
                        String oldName = actualPath.substring(actualPath.lastIndexOf('/') + 1, actualPath.lastIndexOf('.'));
                        item.setPath(actualPath.replace(oldName, newFileName));
                        item.setTitle(newFileName);
                        adapter.notifyDataSetChanged();
                        break;
                    }
                }
                newFileName = null;
                fileToRename = null;
            }
        });
    }

    //Creates the permission launcher for deleting files
    private void setupDeleteLauncher() {
        deleteLauncher = registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && fileToDelete != null) {
                for (int i = 0; i < itemList.size(); i++) {
                    if (fileToDelete.equals(itemList.get(i).getUri())) {
                        itemList.remove(i);
                        adapter.notifyItemRemoved(i);
                        break;
                    }
                }
                fileToDelete = null;
                Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRenameRequested(Uri fileUri, String proposedName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                PendingIntent pi = MediaStore.createWriteRequest(context.getContentResolver(), Collections.singletonList(fileUri));
                renameLauncher.launch(new IntentSenderRequest.Builder(pi.getIntentSender()).build());

                fileToRename = fileUri;
                newFileName = proposedName;
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(context, "Rename permission failed", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, "Rename requires Android 11+", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeleteRequested(Uri fileUri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                PendingIntent pi = MediaStore.createDeleteRequest(context.getContentResolver(), Collections.singletonList(fileUri));
                deleteLauncher.launch(new IntentSenderRequest.Builder(pi.getIntentSender()).build());
                fileToDelete = fileUri;
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(context, "Delete permission failed", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, "Delete requires Android 11+", Toast.LENGTH_SHORT).show();
        }
    }

    public static class TopSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private final int topSpacing;

        public TopSpacingItemDecoration(int topSpacing) {
            this.topSpacing = topSpacing;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                   @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            if (parent.getChildAdapterPosition(view) == 0) {
                outRect.top = topSpacing;
            }
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onSongClick(String songName, String songArtist) {
        ExoPlayer player = PlayerManager.getPlayer(context);
        ArrayList<String> queue = PlayerManager.getQueue();
        queue.clear();

        for (Item item : itemList) {
            queue.add(item.getPath());
            player.addMediaItem(MediaItem.fromUri(item.getPath()));
        }

        int position = getSongPosition(songName);
        player.seekTo(position, 0);

        Intent intent = new Intent(context, PlayerActivity.class);
        intent.putStringArrayListExtra("SONGS", queue);
        intent.putExtra("CURRENT_INDEX", position);
        context.startActivity(intent);

        player.prepare();
        player.play();
    }

    private int getSongPosition(String songName) {
        for (int i = 0; i < itemList.size(); i++) {
            if (itemList.get(i).getTitle().equals(songName)) {
                return i;
            }
        }
        return 0;
    }
}
