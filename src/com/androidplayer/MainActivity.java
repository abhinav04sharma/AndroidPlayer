package com.androidplayer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;

import shuffle.SongFactory;
import tags.Song;
import tags.Tag;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class MainActivity extends Activity {

	private final String GENRES_META_FILE_NAME = "GENRES_META_FILE.txt";

	private final String GENRES_META_FILE = Environment
			.getExternalStorageDirectory().getAbsolutePath()
			+ "/AndroidPlayer/" + GENRES_META_FILE_NAME;

	private List<Song> songs = new ArrayList<Song>();
	private List<String> artists = new ArrayList<String>();
	private List<String> genres = new ArrayList<String>();

	private static Button play;
	private static Button skip;
	private static Button prev;
	private static Button listButton;

	private static TextView currentSong;
	private static TextView currentArtist;
	private static TextView currentGenre;

	private static EditText searchBox;

	private static SeekBar seekBar;
	private static Handler seekHandler = new Handler();
	private static Runnable run;

	private static ListView listView;
	private static ArrayAdapter<Song> adapter;
	private static ArrayList<Song> list;
	private static ArrayList<Song> searchList;
	private static boolean showingList;

	public static SongFactory songFactory = new SongFactory();
	public static MediaPlayer player;

	private static boolean initialized = false;
	final Context context = this;

	private String getURLFileName(String filename) {
		try {
			return URIUtil.encodeQuery("file:///" + filename);
		} catch (URIException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void playSong(Song song, boolean start)
			throws IllegalArgumentException, SecurityException,
			IllegalStateException, IOException {
		if (player != null) {
			player.stop();
		}

		player = new MediaPlayer();
		player.setAudioStreamType(AudioManager.STREAM_MUSIC);
		player.setDataSource(getURLFileName(song.getFileName()));
		player.prepare();
		if (start) {
			player.start();
		}
		player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer player) {
				skip.performClick();
			}
		});

		currentSong.setText(song.getTag().title);
		currentArtist.setText("{" + song.getTag().artist + "}");
		currentGenre.setText("[[" + song.getTag().genre + "]]");

		seekBar.setProgress(0);
		seekBar.setMax(player.getDuration());
	}

	private void seekUpdation() {
		seekBar.setProgress(player.getCurrentPosition());
		seekHandler.postDelayed(run, 500);
	}

	private void readGenresFromFile() throws IOException {
		BufferedReader br;
		File fsGenreFile = new File(
				new File(GENRES_META_FILE).getAbsolutePath());
		// case: get from file-system
		if (fsGenreFile.exists()) {
			br = new BufferedReader(new FileReader(fsGenreFile));
			// case: not present in file-system, get from within jar
		} else {
			br = new BufferedReader(new InputStreamReader(getClass()
					.getResourceAsStream("/" + GENRES_META_FILE_NAME)));
		}
		String line;
		while ((line = br.readLine()) != null) {
			genres.add(line);
		}
		br.close();
	}

	private void constructLists() throws IOException {

		readGenresFromFile();

		Cursor mediaCursor;
		Cursor genresCursor;

		String[] mediaProjection = { MediaStore.Audio.Media._ID,
				MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
				MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA };

		String[] genresProjection = { MediaStore.Audio.Genres.NAME,
				MediaStore.Audio.Genres._ID };

		mediaCursor = context.getContentResolver().query(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaProjection,
				null, null, null);

		if (mediaCursor.moveToFirst()) {
			do {
				Song song = null;
				Tag tag = new Tag();
				String genre = "";

				String title = mediaCursor.getString(mediaCursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
				String album = mediaCursor.getString(mediaCursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
				String artist = mediaCursor.getString(mediaCursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
				String file = mediaCursor.getString(mediaCursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
				int musicId = Integer
						.parseInt(mediaCursor.getString(mediaCursor
								.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)));
				Uri uri = MediaStore.Audio.Genres.getContentUriForAudioId(
						"external", musicId);

				genresCursor = context.getContentResolver().query(uri,
						genresProjection, null, null, null);

				if (genresCursor.moveToFirst()) {
					genre = genresCursor
							.getString(genresCursor
									.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME));
				}

				genresCursor.close();

				if (!genres.contains(genre)) {
					genres.add(genre);
				}

				tag.title = title;
				tag.album = album;
				tag.genre = genre;
				tag.artist = artist;

				song = new Song(tag, file);
				songs.add(song);

			} while (mediaCursor.moveToNext());
		}

		mediaCursor.close();

		Map<String, HashSet<String>> artistMap = new HashMap<String, HashSet<String>>(
				genres.size());

		for (Song song : songs) {
			String g = song.getTag().genre;
			String a = song.getTag().artist;
			if (!artistMap.containsKey(g)) {
				HashSet<String> set = new HashSet<String>();
				set.add(a);
				artistMap.put(g, set);
			} else {
				artistMap.get(g).add(a);
			}
		}

		for (HashSet<String> artistSet : artistMap.values()) {
			artists.addAll(artistSet);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (!initialized) {

			try {
				constructLists();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			Song firstSong = songFactory.initialize(songs, artists, genres);

			constructControls();
			registerListeners();

			Toast.makeText(context, "Done Scanning Songs!!", Toast.LENGTH_LONG)
					.show();
			try {
				playSong(firstSong, false);
			} catch (Exception e) {
				e.printStackTrace();
			}

			run = new Runnable() {
				@Override
				public void run() {
					seekUpdation();
				}
			};

			run.run();
			initialized = true;
		}
	}

	@Override
	public void onBackPressed() {
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private void constructControls() {
		play = (Button) findViewById(R.id.play);
		skip = (Button) findViewById(R.id.skip);
		prev = (Button) findViewById(R.id.prev);
		listButton = (Button) findViewById(R.id.listButton);

		seekBar = (SeekBar) findViewById(R.id.seekBar);

		currentSong = (TextView) findViewById(R.id.currentSong);
		currentArtist = (TextView) findViewById(R.id.currentArtist);
		currentGenre = (TextView) findViewById(R.id.currentGenre);

		searchBox = (EditText) findViewById(R.id.searchBox);
		searchBox.setVisibility(View.INVISIBLE);

		listView = (ListView) findViewById(R.id.listView);
		list = new ArrayList<Song>(songFactory.getSongs().size());
		searchList = new ArrayList<Song>();
		for (Song s : songFactory.getSongs()) {
			list.add(s);
		}
		Collections.sort(list, new Comparator<Song>() {
			@Override
			public int compare(Song arg0, Song arg1) {
				return arg0.toString().compareTo(arg1.toString());
			}
		});

		adapter = new ArrayAdapter<Song>(this, R.layout.list_view, R.id.label,
				list);
		listView.setAdapter(adapter);
		listView.setVisibility(View.INVISIBLE);

		currentSong.setSelected(true);
		currentArtist.setSelected(true);
		currentGenre.setSelected(true);
	}

	private void registerListeners() {
		skip.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				Song nextSong = songFactory.next(
						player.getCurrentPosition() / 1000,
						player.getDuration() / 1000);
				try {
					playSong(nextSong, true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		prev.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				Song prevSong = songFactory.prev(
						player.getCurrentPosition() / 1000,
						player.getDuration() / 1000);
				if (prevSong != null) {
					try {
						playSong(prevSong, true);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});

		play.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (player.isPlaying()) {
					player.pause();
					play.setText("Play ");
				} else {
					player.start();
					play.setText("Pause");
				}
			}
		});

		listButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (showingList) {
					currentArtist.setVisibility(View.VISIBLE);
					currentGenre.setVisibility(View.VISIBLE);
					currentSong.setVisibility(View.VISIBLE);
					play.setVisibility(View.VISIBLE);
					prev.setVisibility(View.VISIBLE);
					skip.setVisibility(View.VISIBLE);
					seekBar.setVisibility(View.VISIBLE);
					listView.setVisibility(View.INVISIBLE);
					searchBox.setVisibility(View.INVISIBLE);
					showingList = false;
				} else {
					currentArtist.setVisibility(View.INVISIBLE);
					currentGenre.setVisibility(View.INVISIBLE);
					currentSong.setVisibility(View.INVISIBLE);
					play.setVisibility(View.INVISIBLE);
					prev.setVisibility(View.INVISIBLE);
					skip.setVisibility(View.INVISIBLE);
					seekBar.setVisibility(View.INVISIBLE);
					listView.setVisibility(View.VISIBLE);
					searchBox.setVisibility(View.VISIBLE);
					showingList = true;
				}
			}
		});

		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				// ListView Clicked item value
				Song itemValue = (Song) listView.getItemAtPosition(position);
				double duration = player.getCurrentPosition() / 1000;
				double maxDuration = player.getDuration() / 1000;
				songFactory.setCurrent(duration, maxDuration, itemValue);

				try {
					playSong(itemValue, true);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		});

		// remove play, prev, skip and seekbar when searching
		searchBox.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				int visible = hasFocus ? View.GONE : View.VISIBLE;
				seekBar.setVisibility(visible);
				prev.setVisibility(visible);
				play.setVisibility(visible);
				skip.setVisibility(visible);
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
						if (s.toString().toUpperCase(Locale.getDefault())
								.contains(search.toUpperCase())) {
							searchList.add(s);
						}
					}
				}
				adapter = new ArrayAdapter<Song>(MainActivity.this,
						R.layout.list_view, R.id.label, searchList);
				listView.setAdapter(adapter);
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {
				// TODO Auto-generated method stub

			}

			@Override
			public void afterTextChanged(Editable arg0) {
				// TODO Auto-generated method stub

			}
		});

		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar sb) {
				player.seekTo(sb.getProgress());
			}

			@Override
			public void onStartTrackingTouch(SeekBar sb) {
			}

			@Override
			public void onProgressChanged(SeekBar sb, int progress,
					boolean fromUser) {
				if (fromUser) {
					player.seekTo(sb.getProgress());
				}
			}
		});
	}
}
