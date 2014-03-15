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

public class NowPlayingFragment extends Fragment {

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

	private static boolean initialized = false;

	private View rootView;

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		this.rootView = inflater.inflate(R.layout.now_playing_fragment,
				container, false);
		createView();
		setRetainInstance(true);
		return rootView;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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

	private void createView() {

		if (!initialized) {

			musicPlayer = MusicPlayer.getInstance(getActivity()
					.getApplicationContext());

			constructControls();
			registerListeners();

			try {
				playSong(musicPlayer.getCurrentSong(), false);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
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

	private void seekUpdation() {
		seekBar.setProgress(musicPlayer.getMediaPlayer().getCurrentPosition());
		seekHandler.postDelayed(run, 500);
	}

	private void constructControls() {
		play = (Button) rootView.findViewById(R.id.play);
		skip = (Button) rootView.findViewById(R.id.skip);
		prev = (Button) rootView.findViewById(R.id.prev);

		seekBar = (SeekBar) rootView.findViewById(R.id.seekBar);

		currentSong = (TextView) rootView.findViewById(R.id.currentSong);
		currentArtist = (TextView) rootView.findViewById(R.id.currentArtist);
		currentGenre = (TextView) rootView.findViewById(R.id.currentGenre);

		currentSong.setSelected(true);
		currentArtist.setSelected(true);
		currentGenre.setSelected(true);
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
					play.setText("Play ");
				} else {
					musicPlayer.startPlayback();
					play.setText("Pause");
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
}
