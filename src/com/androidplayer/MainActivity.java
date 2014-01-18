package com.androidplayer;

import java.io.IOException;

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
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static Button play;
	private static Button skip;

	private static TextView currentSong;
	private static TextView currentArtist;
	private static TextView currentGenre;

	private static SeekBar seekBar;
	private static Handler seekHandler = new Handler();
	private static Runnable run;

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

	private void playSong(Song song) throws IllegalArgumentException,
			SecurityException, IllegalStateException, IOException {
		if (player != null) {
			player.stop();
		}

		player = new MediaPlayer();
		player.setDataSource(getURLFileName(song.getFileName()));
		player.prepare();
		player.start();
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

	// private void getSongList() {
	// //your database elect statement
	// String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
	// //your projection statement
	// String[] projection = {
	// MediaStore.Audio.Media._ID,
	// MediaStore.Audio.Media.ARTIST,
	// MediaStore.Audio.Media.TITLE,
	// MediaStore.Audio.Media.DATA,
	// MediaStore.Audio.Media.DISPLAY_NAME,
	// MediaStore.Audio.Media.DURATION,
	// MediaStore.Audio.Media.ALBUM
	// };
	// //query
	// Cursor cursor = this.managedQuery(
	// MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
	// projection,
	// selection,
	// null,
	// null);
	//
	// cursor.moveToNext();
	// Toast.makeText(context, cursor.getString(5),Toast.LENGTH_LONG);
	// while(cursor.moveToNext()){
	// songs.add(cursor.getString(0));
	// songs.add(cursor.getString(1));
	// songs.add(cursor.getString(2));
	// songs.add(cursor.getString(3));
	// songs.add(cursor.getString(4));
	// songs.add(cursor.getString(5));
	// album_id.add((long) cursor.getFloat(6));
	// }
	// }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (!initialized) {
			constructControls();
			registerListeners();

			Song firstSong = songFactory.initialize(
					Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath(), Environment
							.getExternalStorageDirectory().getAbsolutePath()
							+ "/AndroidPlayer");
			Toast.makeText(context, "Done Scanning Songs!!", Toast.LENGTH_LONG)
					.show();
			try {
				playSong(firstSong);
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
		seekBar = (SeekBar) findViewById(R.id.seekBar);
		currentSong = (TextView) findViewById(R.id.currentSong);
		currentArtist = (TextView) findViewById(R.id.currentArtist);
		currentGenre = (TextView) findViewById(R.id.currentGenre);

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
					playSong(nextSong);
				} catch (Exception e) {
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
	}
}
