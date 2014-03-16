package com.androidplayer.fragments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import tags.Song;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
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

public class SongListFragment extends Fragment implements FragmentInterface {

	private static EditText searchBox;
	private static ListView listView;

	private static ArrayAdapter<Song> adapter;
	private static ArrayList<Song> list;
	private static ArrayList<Song> searchList;

	private static MusicPlayer musicPlayer;

	private View rootView;

	private static boolean isListConstructed = false;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		this.rootView = inflater.inflate(R.layout.list_view_fragment,
				container, false);
		createView();
		setRetainInstance(true);
		return rootView;
	}

	public void createView() {
		musicPlayer = MusicPlayer.getInstance(getActivity()
				.getApplicationContext());
		constructControls();
		registerListeners();
	}

	private void constructControls() {
		searchBox = (EditText) rootView.findViewById(R.id.searchBox);
		searchBox.clearFocus();
		searchBox.setText("");
		listView = (ListView) rootView.findViewById(R.id.listView);
		if (!isListConstructed) {
			constructList();
			isListConstructed = true;
		}
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
				try {
					musicPlayer.playSong(itemValue, true);
				} catch (Exception e) {
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
		searchBox.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence searchString, int start,
					int before, int count) {
				String search = searchBox.getText().toString();
				searchList.clear();
				for (Song s : list) {
					if (s.toString().length() >= search.length()) {
						if (s.toString()
								.toUpperCase(Locale.getDefault())
								.contains(
										search.toUpperCase(Locale.getDefault()))) {
							searchList.add(s);
						}
					}
				}
				adapter = new ArrayAdapter<Song>(getActivity(),
						R.layout.list_view, R.id.label, searchList);
				listView.setAdapter(adapter);
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {
			}

			@Override
			public void afterTextChanged(Editable arg0) {
			}
		});
	}

	private void constructList() {
		list = new ArrayList<Song>(musicPlayer.getSongs().size());
		searchList = new ArrayList<Song>();

		list.addAll(musicPlayer.getSongs());
		Collections.sort(list, new Comparator<Song>() {
			@Override
			public int compare(Song arg0, Song arg1) {
				return arg0.toString().compareTo(arg1.toString());
			}
		});
	}

}
