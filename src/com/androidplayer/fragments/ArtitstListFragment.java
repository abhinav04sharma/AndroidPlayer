package com.androidplayer.fragments;

import java.util.LinkedList;
import java.util.TreeMap;

import tags.Song;
import android.content.Intent;
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
import com.androidplayer.SearchResultsActivity;
import com.androidplayer.adapters.StringListAdapter;

public class ArtitstListFragment extends Fragment implements FragmentInterface {

	private static ListView listView;

	private static StringListAdapter adapter;
	private static TreeMap<String, LinkedList<Song>> artistMap;

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
		adapter = new StringListAdapter(getActivity(),
				R.layout.listview_row_layout, artistMap.keySet().toArray(
						new String[] {}));
		listView.setAdapter(adapter);
	}

	private void registerListeners() {

		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				String itemValue = (String) listView
						.getItemAtPosition(position);
				Intent artistSongsIntent = new Intent(getActivity(),
						SearchResultsActivity.class);
				artistSongsIntent
						.setAction(SearchResultsActivity.KEY_SELECT_ACTION);
				artistSongsIntent.putExtra(SearchResultsActivity.SONG_LIST,
						artistMap.get(itemValue));
				startActivity(artistSongsIntent);
			}
		});
	}

	private void constructList() {
		artistMap = new TreeMap<String, LinkedList<Song>>();
		for (Song song : musicPlayer.getSongs()) {
			String artist = song.getTag().artist;
			if (artist.length() > 0) {
				LinkedList<Song> list = artistMap.get(artist);
				if (list == null) {
					list = new LinkedList<Song>();
					artistMap.put(artist, list);
				}
				list.add(song);
			}
		}
	}
}
