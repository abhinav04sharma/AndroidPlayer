package com.androidplayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;

import shuffle.SongFactory;
import tags.Song;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static Button play;
	private static Button skip;
	private static Button prev;
	private static Button listButton;

	private static TextView currentSong;
	private static TextView currentArtist;
	private static TextView currentGenre;

	private static SeekBar seekBar;
	private static Handler seekHandler = new Handler();
	private static Runnable run;

	private static ListView listView;
	private static ArrayAdapter<Song> adapter;
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

		currentSong.setText(song.getTag().getTitle());
		currentArtist.setText("{" + song.getTag().getArtist() + "}");
		currentGenre.setText("[[" + song.getTag().getGenreDescription() + "]]");

		seekBar.setProgress(0);
		seekBar.setMax(player.getDuration());
	}

	private void seekUpdation() {
		seekBar.setProgress(player.getCurrentPosition());
		seekHandler.postDelayed(run, 500);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (!initialized) {
			Song firstSong = songFactory.initialize(
					Environment.getExternalStoragePublicDirectory(
							Environment.DIRECTORY_MUSIC).getAbsolutePath(),
					Environment.getExternalStorageDirectory().getAbsolutePath()
							+ "/AndroidPlayer");

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
		System.exit(0);
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

		listView = (ListView) findViewById(R.id.listView);
		ArrayList<Song> list = new ArrayList<Song>(songFactory.getSongs()
				.size());
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
					listView.setVisibility(View.INVISIBLE);
					showingList = false;
				} else {
					currentArtist.setVisibility(View.INVISIBLE);
					currentGenre.setVisibility(View.INVISIBLE);
					currentSong.setVisibility(View.INVISIBLE);
					listView.setVisibility(View.VISIBLE);
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
