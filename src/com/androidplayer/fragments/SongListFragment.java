package com.androidplayer.fragments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import tags.Song;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.androidplayer.MusicPlayerService;
import com.androidplayer.MusicPlayerServiceProvider;
import com.androidplayer.R;
import com.androidplayer.adapters.SongListAdapter;

public class SongListFragment extends Fragment implements FragmentInterface {

	private static ListView listView;

	private static SongListAdapter adapter;
	private static ArrayList<Song> list;

	private static MusicPlayerServiceProvider musicPlayerServiceProvider;
	private static MusicPlayerService musicPlayerService;

	private View rootView;

	private static boolean isListConstructed = false;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		this.rootView = inflater.inflate(R.layout.listview_fragment, container,
				false);

		musicPlayerServiceProvider = new MusicPlayerServiceProvider(
				getActivity());
		musicPlayerServiceProvider.doBindService();
		musicPlayerService = musicPlayerServiceProvider.getMusicPlayerService();
		createView();
		setRetainInstance(true);
		return rootView;
	}

	@Override
	public void onDestroy() {
		musicPlayerServiceProvider.doUnbindService();
		super.onDestroy();
	}

	public void createView() {
		constructControls();
		registerListeners();
	}

	private void constructControls() {
		listView = (ListView) rootView.findViewById(R.id.listView);
		listView.setFastScrollEnabled(true);
		if (!isListConstructed) {
			constructList();
			isListConstructed = true;
		}
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
				musicPlayerService.setCurrent(itemValue);
				try {
					musicPlayerService.playSong(itemValue, true);
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		});
	}

	private void constructList() {
		list = new ArrayList<Song>(musicPlayerService.getSongs().size());
		list.addAll(musicPlayerService.getSongs());
		Collections.sort(list, new Comparator<Song>() {
			@Override
			public int compare(Song arg0, Song arg1) {
				return arg0.toString().compareTo(arg1.toString());
			}
		});
	}

}
