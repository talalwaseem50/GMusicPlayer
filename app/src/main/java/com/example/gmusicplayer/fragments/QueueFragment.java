package com.example.gmusicplayer.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gmusicplayer.R;
import com.example.gmusicplayer.adapters.QueueCustomAdapter;
import com.example.gmusicplayer.callbacks.SimpleItemTouchHelperCallback;
import com.example.gmusicplayer.utils.SharedPrefsUtils;

import java.util.Objects;

public class QueueFragment extends Fragment implements QueueCustomAdapter.MyFragmentCallback {

    QueueCustomAdapter adapter;
    RecyclerView recyclerView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_queue, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        Resources res = getResources();
        recyclerView = view.findViewById(R.id.recyclerView);

        adapter = new QueueCustomAdapter(getActivity(), res);
        adapter.setMyFragmentCallback(this);
        recyclerView.setAdapter(adapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
        Objects.requireNonNull(recyclerView.getLayoutManager()).scrollToPosition(
                (new SharedPrefsUtils(getContext()).readSharedPrefsInt("musicID",0)));

        ItemTouchHelper.Callback callback =
                new SimpleItemTouchHelperCallback(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);

    }

    public void notifyFragmentQueueUpdate() {
        adapter.notifyAdapterDataSetChanged();
    }

    public interface MyFragmentCallbackOne {
        void viewPagerRefreshOne();
    }

    MyFragmentCallbackOne callback;

    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity) {
            callback = (MyFragmentCallbackOne) context;
        }
    }

    private void addToViewPager() {
        callback.viewPagerRefreshOne();
    }

    @Override
    public void viewPagerRefresh() {
        addToViewPager();
    }
}
