package com.example.audioplayer;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class TabViewFragment extends Fragment {

    private static final String ARG_ITEMS = "items";
    private Context context;

    public static TabViewFragment newInstance(ArrayList<Item> items) {
        TabViewFragment fragment = new TabViewFragment();

        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_ITEMS, items);
        fragment.setArguments(args);
        return fragment;
    }

    public Context getMainContext() {
        return context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.recycler_view, container, false); // Use the same fragment layout for all


        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        List<Item> itemList = new ArrayList<>();
        if (getArguments() != null) {
            itemList = getArguments().getParcelableArrayList(ARG_ITEMS);
        }
        context = getContext();
        ItemAdapter adapter = new ItemAdapter(context, itemList);
        Log.d("ItemAdapter", getContext().toString());
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
}