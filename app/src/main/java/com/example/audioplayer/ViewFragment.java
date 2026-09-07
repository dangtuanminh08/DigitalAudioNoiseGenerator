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

import androidx.media3.common.Player;

public class ViewFragment extends Fragment implements
        ItemAdapter.OnSongClickListener,
        ItemAdapter.OnRenameRequestListener,
        ItemAdapter.OnDeleteRequestListener {

    private Context context;
    private final List<Item> itemList = new ArrayList<>();
    public static ItemAdapter adapter;

    private ActivityResultLauncher<IntentSenderRequest> renameLauncher;
    private ActivityResultLauncher<IntentSenderRequest> deleteLauncher;

    private Uri fileToRename, fileToDelete;
    private String newFileName, newFileArtist;

    private Player.Listener playbackHighlightListener;
    private ExoPlayer highlightPlayer;
    public static ViewFragment newInstance() {
        ViewFragment fragment = new ViewFragment();
        Bundle args = new Bundle();
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

        setupViewModel();
        setupRenameLauncher();
        setupDeleteLauncher();
        setupPlaybackHighlightListener();

        return view;
    }

    private void setupViewModel() {
        MusicViewModel viewModel = new ViewModelProvider(requireActivity()).get(MusicViewModel.class);
        viewModel.getMusicFiles().observe(getViewLifecycleOwner(), items -> {
            if (items == null || items.equals(itemList)) return;

            itemList.clear();
            itemList.addAll(items);
            adapter.notifyDataSetChanged();
        });
    }

    //Creates the permission launcher for renaming files
    private void setupRenameLauncher() {
        renameLauncher = registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && fileToRename != null && newFileName != null) {
                String actualPath = MediaFileManager.getRealPathFromUri(context, fileToRename);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaFileManager.renameAudioFile(context, fileToRename, newFileName, newFileArtist);
                }

                for (Item item : itemList) {
                    if (item.getPath().equals(actualPath)) {
                        int dotIndex = actualPath.lastIndexOf('.');
                        String extension = dotIndex > 0 ? actualPath.substring(dotIndex) : "";
                        String parentDir = actualPath.substring(0, actualPath.lastIndexOf('/') + 1);
                        item.setPath(parentDir + newFileName + extension);
                        item.setTitle(newFileName);
                        item.setArtist(newFileArtist);
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
    public void onRenameRequested(Uri fileUri, String proposedName, String proposedArtist) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                PendingIntent pi = MediaStore.createWriteRequest(context.getContentResolver(), Collections.singletonList(fileUri));
                renameLauncher.launch(new IntentSenderRequest.Builder(pi.getIntentSender()).build());

                fileToRename = fileUri;
                newFileName = proposedName;
                newFileArtist = proposedArtist;

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

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onSongClick(String songName, String songArtist) {
        ArrayList<String> queue = new ArrayList<>();
        for (Item item : itemList) {
            queue.add(item.getPath());
        }

        PlayerManager.prepareQueue(queue);
        ExoPlayer player = PlayerManager.getPlayer(context);

        int position = getSongPosition(songName);
        player.seekTo(position, 0);

        Intent intent = new Intent(context, PlayerActivity.class);
        intent.putStringArrayListExtra("SONGS", queue);
        intent.putExtra("CURRENT_INDEX", position);
        context.startActivity(intent);

        player.prepare();
        player.play();
    }

    private void setupPlaybackHighlightListener() {
        highlightPlayer = PlayerManager.getPlayer(context);
        ItemAdapter thisTabAdapter = adapter;

        playbackHighlightListener = new Player.Listener() {
            @Override
            public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                thisTabAdapter.setCurrentPlayingSong(highlightPlayer.getCurrentMediaItemIndex());
            }
        };
        highlightPlayer.addListener(playbackHighlightListener);
    }

    private int getSongPosition(String songName) {
        for (int i = 0; i < itemList.size(); i++) {
            if (itemList.get(i).getTitle().equals(songName)) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (highlightPlayer != null && playbackHighlightListener != null) {
            highlightPlayer.removeListener(playbackHighlightListener);
        }
    }
}
