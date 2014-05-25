package com.androidplayer.fragments;

import tags.Song;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.androidplayer.MusicPlayer;
import com.androidplayer.R;

public class NowPlayingFragment extends Fragment implements FragmentInterface {

	private static ImageButton play;
	private static ImageButton skip;
	private static ImageButton prev;

	private static TextView currentSong;
	private static TextView currentArtist;
	private static TextView currentGenre;

	private static SeekBar seekBar;
	private static Handler seekHandler = new Handler();
	private static Runnable run;

	private static MusicPlayer musicPlayer = null;

	private View rootView;
	private BroadcastReceiver broadCastReveiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			setSong((Song) intent
					.getSerializableExtra(MusicPlayer.CURRENT_SONG));
		}
	};

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		this.rootView = inflater.inflate(R.layout.now_playing_fragment,
				container, false);
		LocalBroadcastManager
				.getInstance(getActivity().getApplicationContext())
				.registerReceiver(broadCastReveiver,
						new IntentFilter(MusicPlayer.META_CHANGED));
		musicPlayer = MusicPlayer.getInstance(getActivity()
				.getApplicationContext());
		createView();

		return rootView;
	}

	@Override
	public void onDestroy() {
		LocalBroadcastManager
				.getInstance(getActivity().getApplicationContext())
				.unregisterReceiver(broadCastReveiver);
		super.onDestroy();
	}

	public void createView() {
		// receiver for when song changes
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

	private void setSong(Song song) {
		currentSong.setText(song.getTag().title);
		currentArtist.setText("{" + song.getTag().artist + "}");
		currentGenre.setText("[[" + song.getTag().genre + "]]");

		seekBar.setProgress(musicPlayer.getMediaPlayer().getCurrentPosition());
		seekBar.setMax(musicPlayer.getMediaPlayer().getDuration());

		if (!musicPlayer.isPlaying()) {
			play.setImageResource(R.drawable.ic_action_play);
		} else {
			play.setImageResource(R.drawable.ic_action_pause);
		}
	}

	private void constructControls() {
		play = (ImageButton) rootView.findViewById(R.id.play);
		skip = (ImageButton) rootView.findViewById(R.id.skip);
		prev = (ImageButton) rootView.findViewById(R.id.prev);

		seekBar = (SeekBar) rootView.findViewById(R.id.seekBar);

		currentSong = (TextView) rootView.findViewById(R.id.currentSong);
		currentArtist = (TextView) rootView.findViewById(R.id.currentArtist);
		currentGenre = (TextView) rootView.findViewById(R.id.currentGenre);
	}

	private void registerListeners() {
		skip.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				try {
					musicPlayer.playSong(musicPlayer.getNext(),
							musicPlayer.isPlaying());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		prev.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				Song prevSong = musicPlayer.getPrev();
				try {
					musicPlayer.playSong(prevSong, musicPlayer.isPlaying());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		play.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (musicPlayer.isPlaying()) {
					musicPlayer.pausePlayback();
					play.setImageResource(R.drawable.ic_action_play);
				} else {
					musicPlayer.startPlayback();
					play.setImageResource(R.drawable.ic_action_pause);
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
