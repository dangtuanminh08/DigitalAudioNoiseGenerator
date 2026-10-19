package com.example.audioplayer;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
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
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

public class ViewFragment extends Fragment implements
        ItemAdapter.OnSongClickListener,
        ItemAdapter.OnRenameRequestListener,
        ItemAdapter.OnDeleteRequestListener {

    private static final String TAG = "ViewFragment";

    private ItemAdapter adapter;
    private MusicViewModel viewModel;

    private ActivityResultLauncher<IntentSenderRequest> renameLauncher;
    private ActivityResultLauncher<IntentSenderRequest> deleteLauncher;


    private Uri fileToRename, fileToDelete;
    private String newFileName, newFileArtist;

    private Player.Listener playbackHighlightListener;
    private ExoPlayer highlightPlayer;

    public static ViewFragment newInstance() {
        ViewFragment fragment = new ViewFragment();
        fragment.setArguments(new Bundle());
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MusicViewModel.class);

        setupRenameLauncher();
        setupDeleteLauncher();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.recycler_view, container, false);

        adapter = new ItemAdapter(this, this, this, PlayerManager.getCurrentMediaId());

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.setVerticalScrollBarEnabled(false);

        viewModel.getMusicFiles().observe(getViewLifecycleOwner(), items -> {
            if (items != null) adapter.submitList(items);
        });

        setupPlaybackHighlightListener();
        return view;
    }

    private void setupRenameLauncher() {
        renameLauncher = registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
            Uri uri = fileToRename;
            String title = newFileName;
            String artist = newFileArtist;
            fileToRename = null;
            newFileName = null;
            newFileArtist = null;

            if (result.getResultCode() != Activity.RESULT_OK || uri == null || title == null) return;

            Item updated = viewModel.renameItem(uri, title, artist != null ? artist : "");
            if (updated != null) {
                PlayerManager.updateItemMetadata(updated);
            }
        });
    }

    private void setupDeleteLauncher() {
        deleteLauncher = registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
            Uri uri = fileToDelete;
            fileToDelete = null;

            if (result.getResultCode() != Activity.RESULT_OK || uri == null) return;

            // Remove from the player by id so the queue and list cannot drift apart.
            int playerIndex = PlayerManager.indexOfMediaId(uri.toString());
            if (playerIndex >= 0) {
                PlayerManager.removeItem(playerIndex);
            }
            viewModel.removeItem(uri);
            Toast.makeText(requireContext(), R.string.deleted_successfully, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onRenameRequested(Uri fileUri, String proposedName, String proposedArtist) {
        fileToRename = fileUri;
        newFileName = proposedName;
        newFileArtist = proposedArtist;
        try {
            PendingIntent pi = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pi = MediaStore.createWriteRequest(
                        requireContext().getContentResolver(), Collections.singletonList(fileUri));
            }
            assert pi != null;
            renameLauncher.launch(new IntentSenderRequest.Builder(pi.getIntentSender()).build());
        } catch (Exception e) {
            Log.e(TAG, "Rename permission request failed", e);
            fileToRename = null;
            newFileName = null;
            newFileArtist = null;
            Toast.makeText(requireContext(), R.string.rename_permission_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeleteRequested(Uri fileUri) {
        fileToDelete = fileUri;
        try {
            PendingIntent pi = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pi = MediaStore.createDeleteRequest(
                        requireContext().getContentResolver(), Collections.singletonList(fileUri));
            }
            assert pi != null;
            deleteLauncher.launch(new IntentSenderRequest.Builder(pi.getIntentSender()).build());
        } catch (Exception e) {
            Log.e(TAG, "Delete permission request failed", e);
            fileToDelete = null;
            Toast.makeText(requireContext(), R.string.delete_permission_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onSongClick(int position) {
        List<Item> items = adapter.getCurrentList();
        if (position < 0 || position >= items.size()) return;

        PlayerManager.playQueue(requireContext(), items, position);
        startActivity(new Intent(requireContext(), PlayerActivity.class));
    }

    private void setupPlaybackHighlightListener() {
        highlightPlayer = PlayerManager.getPlayer(requireContext());
        ItemAdapter boundAdapter = adapter;

        playbackHighlightListener = new Player.Listener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                boundAdapter.setCurrentPlayingMediaId(mediaItem != null ? mediaItem.mediaId : null);
            }
        };
        highlightPlayer.addListener(playbackHighlightListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (highlightPlayer != null && playbackHighlightListener != null) {
            highlightPlayer.removeListener(playbackHighlightListener);
        }
        highlightPlayer = null;
        playbackHighlightListener = null;
        adapter = null;
    }
}