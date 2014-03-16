package com.androidplayer.fragments;

import java.io.IOException;

import tags.Song;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.androidplayer.MusicPlayer;
import com.androidplayer.R;

public class NowPlayingFragment extends Fragment implements FragmentInterface {

	private static Button play;
	private static Button skip;
	private static Button prev;

	private static TextView currentSong;
	private static TextView currentArtist;
	private static TextView currentGenre;

	private static SeekBar seekBar;
	private static Handler seekHandler = new Handler();
	private static Runnable run;

	private static MusicPlayer musicPlayer = null;

	private static final String PLAY = "Play";
	private static final String PAUSE = "Pause";

	private View rootView;

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		this.rootView = inflater.inflate(R.layout.now_playing_fragment,
				container, false);
		createView();
		return rootView;
	}

	public void createView() {
		musicPlayer = MusicPlayer.getInstance(getActivity()
				.getApplicationContext());

		constructControls();
		registerListeners();

		run = new Runnable() {
			@Override
			public void run() {
				seekUpdation();
			}
		};

		run.run();
	}

	public void playSong(Song song, boolean start)
			throws IllegalArgumentException, SecurityException,
			IllegalStateException, IOException {

		musicPlayer.playSong(song, start);
		currentSong.setText(song.getTag().title);
		currentArtist.setText("{" + song.getTag().artist + "}");
		currentGenre.setText("[[" + song.getTag().genre + "]]");

		seekBar.setProgress(0);
		seekBar.setMax(musicPlayer.getMediaPlayer().getDuration());
	}

	private void constructControls() {
		play = (Button) rootView.findViewById(R.id.play);
		skip = (Button) rootView.findViewById(R.id.skip);
		prev = (Button) rootView.findViewById(R.id.prev);

		seekBar = (SeekBar) rootView.findViewById(R.id.seekBar);

		currentSong = (TextView) rootView.findViewById(R.id.currentSong);
		currentArtist = (TextView) rootView.findViewById(R.id.currentArtist);
		currentGenre = (TextView) rootView.findViewById(R.id.currentGenre);

		Song song = musicPlayer.getCurrentSong();
		currentSong.setText(song.getTag().title);
		currentArtist.setText("{" + song.getTag().artist + "}");
		currentGenre.setText("[[" + song.getTag().genre + "]]");

		if (!musicPlayer.isPlaying()) {
			play.setText(PLAY);
			try {
				musicPlayer.playSong(song, false);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			play.setText(PAUSE);
		}
		seekBar.setProgress(musicPlayer.getMediaPlayer().getCurrentPosition());
		seekBar.setMax(musicPlayer.getMediaPlayer().getDuration());
	}

	private void registerListeners() {
		skip.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				Song nextSong = musicPlayer.getNext();
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
				Song prevSong = musicPlayer.getPrev();
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
				if (musicPlayer.isPlaying()) {
					musicPlayer.pausePlayback();
					play.setText(PLAY);
				} else {
					musicPlayer.startPlayback();
					play.setText(PAUSE);
				}
			}
		});

		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar sb) {
				musicPlayer.seek(sb.getProgress());
			}

			@Override
			public void onStartTrackingTouch(SeekBar sb) {
			}

			@Override
			public void onProgressChanged(SeekBar sb, int progress,
					boolean fromUser) {
				if (fromUser) {
					musicPlayer.seek(sb.getProgress());
				}
			}
		});
	}

	private void seekUpdation() {
		seekBar.setProgress(musicPlayer.getMediaPlayer().getCurrentPosition());
		seekHandler.postDelayed(run, 500);
	}

}
