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

import com.androidplayer.MusicPlayerService;
import com.androidplayer.MusicPlayerServiceProvider;
import com.androidplayer.R;
import com.androidplayer.SearchResultsActivity;
import com.androidplayer.adapters.StringListAdapter;

public class GenreListFragment extends Fragment implements FragmentInterface {

	private static ListView listView;

	private static StringListAdapter adapter;
	private static TreeMap<String, LinkedList<Song>> genreMap;

	private static MusicPlayerServiceProvider musicPlayerServiceProvider;
	private static MusicPlayerService musicPlayerService;

	private View rootView;

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
		constructList();
		adapter = new StringListAdapter(getActivity(),
				R.layout.listview_row_layout, genreMap.keySet().toArray(
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
				Intent genreSongsIntent = new Intent(getActivity(),
						SearchResultsActivity.class);
				genreSongsIntent
						.setAction(SearchResultsActivity.KEY_SELECT_ACTION);
				genreSongsIntent.putExtra(SearchResultsActivity.SONG_LIST,
						genreMap.get(itemValue));
				startActivity(genreSongsIntent);
			}
		});
	}

	private void constructList() {
		genreMap = new TreeMap<String, LinkedList<Song>>();
		for (Song song : musicPlayerService.getSongs()) {
			String genre = song.getTag().genre;
			if (genre.length() > 0) {
				LinkedList<Song> list = genreMap.get(genre);
				if (list == null) {
					list = new LinkedList<Song>();
					genreMap.put(genre, list);
				}
				list.add(song);
			}
		}
	}
}
