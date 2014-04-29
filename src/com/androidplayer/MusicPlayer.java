package com.androidplayer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;

import shuffle.SongFactory;
import tags.Song;
import tags.Tag;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.RemoteControlClient;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class MusicPlayer {

	private static final String TAG = "MusicPlayer";

	private static MusicPlayer musicPlayer = null;
	private Context context;

	private final String GENRES_META_FILE_NAME = "GENRES_META_FILE.txt";

	private final List<Song> songs = new ArrayList<Song>();
	private final List<String> artists = new ArrayList<String>();
	private final List<String> genres = new ArrayList<String>();

	public static final SongFactory songFactory = new SongFactory();
	public static final MediaPlayer player = new MediaPlayer();

	public static final String CURRENT_SONG = "com.androidplayer.CURRENT_SONG";
	public static final String SONG_CHANGED = "com.androidplayer.SONG_CHANGED";

	private ComponentName mediaButtonReceiverComponent;
	private RemoteControlClientCompat remoteControlClientCompat;

	private static boolean wasPlaying = false;
	private final OnAudioFocusChangeListener audioFocusListener = new OnAudioFocusChangeListener() {

		public void onAudioFocusChange(int focusChange) {
			AudioManager am = (AudioManager) context
					.getSystemService(Context.AUDIO_SERVICE);
			
			if(focusChange != AudioManager.AUDIOFOCUS_GAIN)
				wasPlaying = isPlaying();

			switch (focusChange) {
			case (AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK):
				player.setVolume(0.2f, 0.2f);
				break;

			case (AudioManager.AUDIOFOCUS_LOSS_TRANSIENT):
				pausePlayback();
				break;

			case (AudioManager.AUDIOFOCUS_LOSS):
				pausePlayback();
				ComponentName component = new ComponentName(context,
						RemoteControlBroadcastReceiver.class);
				am.unregisterMediaButtonEventReceiver(component);
				break;

			case (AudioManager.AUDIOFOCUS_GAIN):
				player.setVolume(1f, 1f);
				if (wasPlaying)
					startPlayback();
				break;

			default:
				break;
			}
		}
	};

	public static synchronized MusicPlayer getInstance(Context context) {
		if (musicPlayer == null) {
			musicPlayer = new MusicPlayer();
			musicPlayer.initialize(context);

			return musicPlayer;
		}
		// send a song change request each time the music player is initialized
		musicPlayer.sendSongChangedRequest();
		return musicPlayer;
	}

	public List<Song> getSongs() {
		return songFactory.getSongs();
	}

	public Song getCurrentSong() {
		return songFactory.getCurrent();
	}

	public void setCurrent(Song song) {
		double duration = player.getCurrentPosition() / 1000;
		double maxDuration = player.getDuration() / 1000;
		songFactory.setCurrent(duration, maxDuration, song);
		sendSongChangedRequest();
	}

	public Song getPrev() {
		Song ret = songFactory.prev(player.getCurrentPosition() / 1000,
				player.getDuration() / 1000);
		sendSongChangedRequest();
		return ret;
	}

	public Song getNext() {
		Song ret = songFactory.next(player.getCurrentPosition() / 1000,
				player.getDuration() / 1000);
		sendSongChangedRequest();
		return ret;
	}

	public boolean isPlaying() {
		return player.isPlaying();
	}

	public void startPlayback() {
		if (getAudioFocus()) {
			remoteControlClientCompat
					.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
		}
		player.start();
	}

	public void pausePlayback() {
		if (getAudioFocus()) {
			remoteControlClientCompat
					.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
		}
		player.pause();
	}

	public void seek(int msec) {
		player.seekTo(msec);
	}

	public void playSong(Song song, boolean start)
			throws IllegalArgumentException, SecurityException,
			IllegalStateException, IOException {

		if (player.isPlaying()) {
			player.stop();
		}
		player.reset();
		player.setAudioStreamType(AudioManager.STREAM_MUSIC);
		player.setDataSource(getURLFileName(song.getFileName()));
		player.prepare();

		if (start) {
			if (getAudioFocus()) {
				remoteControlClientCompat
						.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
			}
			player.start();
		}
	}

	public MediaPlayer getMediaPlayer() {
		return player;
	}

	private MusicPlayer() {
	}

	private String getURLFileName(String filename) {
		try {
			return URIUtil.encodeQuery("file:///" + filename);
		} catch (URIException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void readGenresFromFile() throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(getClass()
				.getResourceAsStream("/" + GENRES_META_FILE_NAME)));
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
				MediaStore.Audio.Media.IS_MUSIC + " != 0", null, null);

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

	private void initialize(Context context) {
		this.context = context;
		try {
			constructLists();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		songFactory.initialize(songs, artists, genres);
		player.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				try {
					playSong(getNext(), true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		try {
			playSong(getCurrentSong(), false);
		} catch (Exception e) {
			e.printStackTrace();
		}
		registerEquilizer();
		// registerRemoteClient();
		registerRemoteClient();
		registerAudioJackListener();
	}

	private void sendSongChangedRequest() {
		Intent intent = new Intent(SONG_CHANGED);
		intent.putExtra(CURRENT_SONG, getCurrentSong());
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
		// update remote client
		updateRemoteClientMetaData();
	}

	private void registerEquilizer() {
		final Intent audioEffectsIntent = new Intent(
				AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
		audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION,
				player.getAudioSessionId());
		audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME,
				context.getPackageName());
		context.sendBroadcast(audioEffectsIntent);
	}

	private void unRegisterEquilizer() {
		final Intent audioEffectsIntent = new Intent(
				AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
		audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION,
				player.getAudioSessionId());
		audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME,
				context.getPackageName());
		context.sendBroadcast(audioEffectsIntent);
	}

	private void registerRemoteClient() {
		AudioManager am = (AudioManager) context
				.getSystemService(Context.AUDIO_SERVICE);
		final Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
		mediaButtonReceiverComponent = new ComponentName(context,
				RemoteControlBroadcastReceiver.class);
		mediaButtonIntent.setComponent(mediaButtonReceiverComponent);
		remoteControlClientCompat = new RemoteControlClientCompat(
				PendingIntent.getBroadcast(context, 0, mediaButtonIntent, 0));
		RemoteControlHelper.registerRemoteControlClient(am,
				remoteControlClientCompat);
		final int flags = RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS
				| RemoteControlClient.FLAG_KEY_MEDIA_NEXT
				| RemoteControlClient.FLAG_KEY_MEDIA_PLAY
				| RemoteControlClient.FLAG_KEY_MEDIA_PAUSE
				| RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE
				| RemoteControlClient.FLAG_KEY_MEDIA_STOP;
		remoteControlClientCompat.setTransportControlFlags(flags);
	}

	// private void registerRemoteClient() {
	// AudioManager audioManager = (AudioManager) context
	// .getSystemService(Context.AUDIO_SERVICE);
	//
	// Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
	//
	// ComponentName remoteComponentName = new ComponentName(context,
	// RemoteControlBroadcastReceiver.class);
	// mediaButtonIntent.setComponent(remoteComponentName);
	//
	// PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(context,
	// 0, mediaButtonIntent, 0);
	//
	// remoteControlClient = new RemoteControlClient(mediaPendingIntent);
	// audioManager.registerRemoteControlClient(remoteControlClient);
	//
	// remoteControlClient
	// .setTransportControlFlags(RemoteControlClient.FLAG_KEY_MEDIA_PLAY
	// | RemoteControlClient.FLAG_KEY_MEDIA_PAUSE
	// | RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS
	// | RemoteControlClient.FLAG_KEY_MEDIA_NEXT);
	//
	// audioManager.registerMediaButtonEventReceiver(mediaPendingIntent);
	// }

	private void updateRemoteClientMetaData() {
		remoteControlClientCompat
				.editMetadata(true)
				.putString(MediaMetadataRetriever.METADATA_KEY_TITLE,
						getCurrentSong().getTag().title)
				.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST,
						getCurrentSong().getTag().artist)
				.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM,
						getCurrentSong().getTag().album).apply();
	}

	private boolean getAudioFocus() {
		AudioManager am = (AudioManager) context
				.getSystemService(Context.AUDIO_SERVICE);
		int result = am.requestAudioFocus(audioFocusListener,
				AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
		if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
			Log.i(TAG, "Audio focus not granted");
			return false;
		}
		am.registerMediaButtonEventReceiver(mediaButtonReceiverComponent);
		return true;
	}

	private void unregisterRemoteClient() {
		try {
			AudioManager audioManager = (AudioManager) context
					.getSystemService(Context.AUDIO_SERVICE);
			RemoteControlHelper.unregisterRemoteControlClient(audioManager,
					remoteControlClientCompat);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Pausing output when the headset is disconnected
	 */
	private class NoisyAudioStreamReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent
					.getAction())) {
				pausePlayback();
			}
		}
	}

	private void registerAudioJackListener() {
		IntentFilter noiseFilter = new IntentFilter(
				AudioManager.ACTION_AUDIO_BECOMING_NOISY);
		context.registerReceiver(new NoisyAudioStreamReceiver(), noiseFilter);
	}
}