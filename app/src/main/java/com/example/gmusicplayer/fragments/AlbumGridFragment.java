package com.example.gmusicplayer.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.gmusicplayer.GlobalDetailActivity;
import com.example.gmusicplayer.R;
import com.example.gmusicplayer.adapters.AlbumGridAdapter;
import com.example.gmusicplayer.utils.SongsUtils;

import java.util.ArrayList;
import java.util.HashMap;

public class AlbumGridFragment  extends Fragment {

    GridView gridView;
    SongsUtils songsUtils;
    ArrayList<HashMap<String,String>> CustomArray;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View v = inflater.inflate(R.layout.fragment_album_grid, container, false);
        songsUtils = new SongsUtils(getActivity());
        CustomArray = songsUtils.albums();
        gridView = v.findViewById(R.id.gridView1);

        gridView.setAdapter(new AlbumGridAdapter(getActivity(), CustomArray));

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                Intent intent = new Intent(getActivity(), GlobalDetailActivity.class);
                intent.putExtra("id", position);
                intent.putExtra("name", songsUtils.albums().get(position).get("album"));
                intent.putExtra("field", "albums");
                startActivity(intent);
            }
        });
        return v;
    }



    boolean _areLecturesLoaded = false;
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && !_areLecturesLoaded ) {

            _areLecturesLoaded = true;
        }
    }

}
