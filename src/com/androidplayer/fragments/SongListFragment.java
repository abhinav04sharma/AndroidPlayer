package com.androidplayer.fragments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import tags.Song;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.androidplayer.MusicPlayer;
import com.androidplayer.R;
import com.androidplayer.adapters.TabsPagerAdapter;

public class SongListFragment extends Fragment {

	private static EditText searchBox;
	private static ListView listView;

	private static ArrayAdapter<Song> adapter;
	private static ArrayList<Song> list;
	private static ArrayList<Song> searchList;

	private static MusicPlayer musicPlayer;

	private View rootView;

	private static boolean initialized = false;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		this.rootView = inflater.inflate(R.layout.list_view_fragment,
				container, false);
		createView();
		setRetainInstance(true);
		return rootView;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	private void createView() {
		if (!initialized) {
			musicPlayer = MusicPlayer.getInstance(getActivity()
					.getApplicationContext());
			constructControls();
			registerListeners();
			initialized = true;
		}
	}

	private void constructControls() {
		searchBox = (EditText) rootView.findViewById(R.id.searchBox);

		listView = (ListView) rootView.findViewById(R.id.listView);

		list = new ArrayList<Song>(musicPlayer.getSongs().size());
		searchList = new ArrayList<Song>();

		for (Song s : musicPlayer.getSongs()) {
			list.add(s);
		}

		Collections.sort(list, new Comparator<Song>() {
			@Override
			public int compare(Song arg0, Song arg1) {
				return arg0.toString().compareTo(arg1.toString());
			}
		});

		adapter = new ArrayAdapter<Song>(getActivity(), R.layout.list_view,
				R.id.label, list);
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
				NowPlayingFragment npf = (NowPlayingFragment) TabsPagerAdapter
						.getFragment(R.id.now_playing_fragment);
				try {
					npf.playSong(itemValue, true);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					InputMethodManager imm = (InputMethodManager) getActivity()
							.getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(searchBox.getWindowToken(), 0);
				}

			}
		});

		searchBox
				.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView v, int actionId,
							KeyEvent event) {
						if (actionId == EditorInfo.IME_ACTION_SEARCH) {
							String search = searchBox.getText().toString();
							searchList.clear();
							for (Song s : list) {
								if (s.toString().length() >= search.length()) {
									if (s.toString()
											.toUpperCase(Locale.getDefault())
											.contains(search.toUpperCase())) {
										searchList.add(s);
									}
								}
							}
							adapter = new ArrayAdapter<Song>(getActivity(),
									R.layout.list_view, R.id.label, searchList);
							listView.setAdapter(adapter);
							InputMethodManager imm = (InputMethodManager) getActivity()
									.getSystemService(
											Context.INPUT_METHOD_SERVICE);
							imm.hideSoftInputFromWindow(
									searchBox.getWindowToken(), 0);
							return true;
						}
						return false;
					}
				});

		// filter list view
		// searchBox.addTextChangedListener(new TextWatcher() {
		//
		// @Override
		// public void onTextChanged(CharSequence searchString, int start,
		// int before, int count) {
		// String search = searchBox.getText().toString();
		// searchList.clear();
		// for (Song s : list) {
		// if (s.toString().length() >= search.length()) {
		// if (s.toString().toUpperCase(Locale.getDefault())
		// .contains(search.toUpperCase())) {
		// searchList.add(s);
		// }
		// }
		// }
		// adapter = new ArrayAdapter<Song>(getActivity(),
		// R.layout.list_view, R.id.label, searchList);
		// listView.setAdapter(adapter);
		// }
		//
		// @Override
		// public void beforeTextChanged(CharSequence arg0, int arg1,
		// int arg2, int arg3) {
		// // TODO Auto-generated method stub
		//
		// }
		//
		// @Override
		// public void afterTextChanged(Editable arg0) {
		// // TODO Auto-generated method stub
		//
		// }
		// });
	}
}
