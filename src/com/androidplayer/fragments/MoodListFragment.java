package com.androidplayer.fragments;

import java.util.ArrayList;

import tags.Song;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.androidplayer.MusicPlayer;
import com.androidplayer.R;
import com.androidplayer.adapters.SongListAdapter;

public class MoodListFragment extends Fragment implements FragmentInterface {

	private static ListView listView;

	private static SongListAdapter adapter;
	private static ArrayList<Song> list;

	private static MusicPlayer musicPlayer;

	private View rootView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		this.rootView = inflater.inflate(R.layout.listview_fragment, container,
				false);
		musicPlayer = MusicPlayer.getInstance(getActivity()
				.getApplicationContext());
		createView();
		setRetainInstance(true);
		return rootView;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	public void createView() {
		constructControls();
		registerListeners();
	}

	private void constructControls() {
		listView = (ListView) rootView.findViewById(R.id.listView);
		listView.setFastScrollEnabled(true);
		constructList();
		adapter = new SongListAdapter(getActivity(),
				R.layout.listview_row_layout, list.toArray(new Song[] {}));
		listView.setAdapter(adapter);
	}

	private void registerListeners() {

		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// ListView Clicked item value
				Song itemValue = (Song) listView.getItemAtPosition(position);
				musicPlayer.setCurrent(itemValue);
				try {
					musicPlayer.playSong(itemValue, true);
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		});
	}

	private void constructList() {
		list = new ArrayList<Song>(musicPlayer.getSongs().size());
		list.addAll(musicPlayer.getSongs().subList(0, 20));
	}

}
