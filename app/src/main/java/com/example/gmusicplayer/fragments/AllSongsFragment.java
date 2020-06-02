package com.example.gmusicplayer.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.commonsware.cwac.merge.MergeAdapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.gmusicplayer.R;
import com.example.gmusicplayer.SongModel;
import com.example.gmusicplayer.adapters.CustomAdapter;
import com.example.gmusicplayer.utils.SongsUtils;

import java.util.ArrayList;

public class AllSongsFragment extends Fragment {

    ListView list;
    CustomAdapter adapter;
    ArrayList<SongModel> CustomListViewValuesArr = new ArrayList<>();
    static int selectedRow;
    SongsUtils songsUtils;
    Context context;
    LayoutInflater mInflater;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context =  getActivity();
        mInflater = LayoutInflater.from(context);
    }

    @SuppressLint({ "SdCardPath", "SimpleDateFormat" })
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_songs, container, false);

        songsUtils = new SongsUtils(getActivity());

        setListData();

        list = v.findViewById( R.id.listView1 );
        //list.addHeaderView(getActivity().getLayoutInflater().inflate(R.layout.listview_header, null));
        adapter=new CustomAdapter( getActivity(), CustomListViewValuesArr);

        MergeAdapter myMergeAdapter = new MergeAdapter();
        //View Header =  mInflater.inflate(R.layout.heading, null);
        //TextView textView = (TextView) Header.findViewById(R.id.textView1);
        //textView.setText(R.string.all_songs_a_z);
        //myMergeAdapter.addView(Header);
        myMergeAdapter.addAdapter(adapter);

        list.setAdapter(myMergeAdapter);
        try {
            list.setSelectionFromTop(selectedRow, 0);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

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

//    Boolean isVisible = false;
//
//    @Override
//    public void setMenuVisibility(final boolean visible) {
//        super.setMenuVisibility(visible);
//        isVisible = visible;
//        if (isVisible) {
//            try {
//                ((MainActivity) Objects.requireNonNull(getActivity())).showHideFabWithScroll(list);
//            }
//            catch (Exception e) {
//                Log.i("Show/HideTitle:AllSongs", "Safe from Crash.");
//            }
//        }
//    }



    public void setListData()
    {
        CustomListViewValuesArr.clear();
        CustomListViewValuesArr = new ArrayList<>(songsUtils.allSongs());
    }



}

