package com.example.audioplayer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class TabViewFragment extends Fragment implements ItemAdapter.OnSongClickListener {

    private static final String ARG_ITEMS = "items";
    private Context context;
    private List<Item> itemList;

    public static TabViewFragment newInstance(ArrayList<Item> items) {
        TabViewFragment fragment = new TabViewFragment();

        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_ITEMS, items);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        context = getContext();
        View view = inflater.inflate(R.layout.recycler_view, container, false); // Use the same fragment layout for all

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        itemList = new ArrayList<>();
        if (getArguments() != null) {
            itemList = getArguments().getParcelableArrayList(ARG_ITEMS);
        }

        recyclerView.setVerticalScrollBarEnabled(false);

        ItemAdapter adapter = new ItemAdapter(itemList, this);
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new TopSpacingItemDecoration(64));
        return view;
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
        ArrayList<String> queue = PlayerManager.getQueue();
        if (queue.isEmpty()) {
            for (Item i : itemList) {
                queue.add(i.getPath());
            }
        }
        PlayerManager.playSong(queue);
        int position = getSongPosition(songName);
        PlayerManager.getPlayer(context).seekTo(position, 0);
        PlayerManager.getPlayer(context).play();

        // Start PlayerActivity
        Intent intent = new Intent(context, PlayerActivity.class);
        intent.putStringArrayListExtra("SONGS", queue);
        intent.putExtra("CURRENT_INDEX", position);
        context.startActivity(intent);
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