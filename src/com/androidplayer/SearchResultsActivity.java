package com.androidplayer;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import tags.Song;
import android.app.ActionBar;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.androidplayer.adapters.SongListAdapter;

public class SearchResultsActivity extends Activity {

	private static ListView listView;

	private static SongListAdapter adapter;
	private static LinkedList<Song> searchList;

	private static MusicPlayer musicPlayer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search_results);
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		musicPlayer = MusicPlayer.getInstance(this);
		searchList = new LinkedList<Song>();

		listView = (ListView) findViewById(R.id.searchList);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Song itemValue = (Song) listView.getItemAtPosition(position);
				musicPlayer.setCurrent(itemValue);
				try {
					musicPlayer.playSong(itemValue, true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		handleIntent(getIntent());
	}

	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		handleIntent(intent);
	}

	private void handleIntent(Intent intent) {
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String search = intent.getStringExtra(SearchManager.QUERY);
			List<Song> list = musicPlayer.getSongs();
			for (Song s : list) {
				String concat = s.getTag().title + "\t" + s.getTag().artist + "\t" + s.getTag().album; 
				if (concat.length() >= search.length()) {
					if (concat.toUpperCase(Locale.getDefault())
							.contains(search.toUpperCase(Locale.getDefault()))) {
						searchList.add(s);
					}
				}
			}
			adapter = new SongListAdapter(this, R.layout.listview_row_layout, searchList.toArray(new Song[] {}));
			listView.setAdapter(adapter);
		}
	}
}