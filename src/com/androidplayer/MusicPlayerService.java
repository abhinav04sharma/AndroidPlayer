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
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
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
import android.os.Binder;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.androidplayer.widgets.AndroidPlayerWidgetProvider;

public class MusicPlayerService extends Service {

	private static final String TAG = "MusicPlayer";
	private static final int NOTIFICATION_ID = 1001;

	public static final String META_CHANGED = "com.androidplayer.MusicPlayer.META_CHANGED";
	public static final String CURRENT_SONG = "com.androidplayer.MusicPlayer.META.CURRENT_SONG";
	public static final String IS_PLAYING = "com.androidplayer.MusicPlayer.META.IS_PLAYING";

	private static boolean wasPlaying;

	private final List<Song> songs = new ArrayList<Song>();
	private final List<String> artists = new ArrayList<String>();
	private final List<String> genres = new ArrayList<String>();

	public final SongFactory songFactory = new SongFactory();
	public final MediaPlayer player = new MediaPlayer();

	private ComponentName mediaButtonReceiverComponent;
	private RemoteControlClientCompat remoteControlClientCompat;

	private RemoteViews notificationView;
	private boolean isNotificationCreated;

	private final NoisyAudioStreamReceiver noisyAudioStreamReceiver = new NoisyAudioStreamReceiver();

	private final OnAudioFocusChangeListener audioFocusListener = new OnAudioFocusChangeListener() {

		public void onAudioFocusChange(int focusChange) {
			AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

			if (focusChange != AudioManager.AUDIOFOCUS_GAIN)
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
				ComponentName component = new ComponentName(
						MusicPlayerService.this,
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
	}

	public Song getPrev() {
		Song ret = songFactory.prev(player.getCurrentPosition() / 1000,
				player.getDuration() / 1000);
		return ret;
	}

	public Song getNext() {
		Song ret = songFactory.next(player.getCurrentPosition() / 1000,
				player.getDuration() / 1000);
		return ret;
	}

	public boolean isPlaying() {
		return player.isPlaying();
	}

	public void startPlayback() {
		player.start();
		sendMetaChangedRequest();
	}

	public void pausePlayback() {
		player.pause();
		sendMetaChangedRequest();
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

		sendMetaChangedRequest();

		if (start)
			startPlayback();
	}

	public MediaPlayer getMediaPlayer() {
		return player;
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
				.getResourceAsStream("/" + "GENRES_META_FILE.txt")));
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

		mediaCursor = getContentResolver().query(
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

				genresCursor = getContentResolver().query(uri,
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

	private void initialize() {
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

		registerEquilizer();
		registerRemoteClient();
		registerAudioJackListener();
	}

	public void finalize() {
		unRegisterAudioJackListener();
		unRegisterRemoteClient();
		unRegisterEquilizer();
		closeNotification();
	}

	// TODO: Figure out when to call this
	private void sendMetaChangedRequest() {
		Intent intent = new Intent(META_CHANGED);
		intent.putExtra(CURRENT_SONG, getCurrentSong());
		intent.putExtra(IS_PLAYING, isPlaying());
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
		// update remote client
		updateRemoteClientMetaData();
		// update notification view
		updateNotificationView();
	}

	private void registerEquilizer() {
		final Intent audioEffectsIntent = new Intent(
				AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
		audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION,
				player.getAudioSessionId());
		audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME,
				getPackageName());
		sendBroadcast(audioEffectsIntent);
	}

	private void unRegisterEquilizer() {
		final Intent audioEffectsIntent = new Intent(
				AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
		audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION,
				player.getAudioSessionId());
		audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME,
				getPackageName());
		sendBroadcast(audioEffectsIntent);
	}

	private void initializeWidgets() {
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
		int[] appWidgetIds = appWidgetManager
				.getAppWidgetIds(new ComponentName(this,
						AndroidPlayerWidgetProvider.class));
		if (appWidgetIds.length > 0) {
			new AndroidPlayerWidgetProvider().onUpdate(this, appWidgetManager,
					appWidgetIds);
		}
	}

	private boolean getAudioFocus() {
		AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		int result = am.requestAudioFocus(audioFocusListener,
				AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
		if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
			Log.i(TAG, "Audio focus not granted");
			return false;
		}
		am.registerMediaButtonEventReceiver(mediaButtonReceiverComponent);
		return true;
	}

	private void registerRemoteClient() {
		AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		final Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
		mediaButtonIntent.setComponent(mediaButtonReceiverComponent);
		remoteControlClientCompat = new RemoteControlClientCompat(
				PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0));
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

	private void updateRemoteClientMetaData() {
		remoteControlClientCompat
				.editMetadata(true)
				.putString(MediaMetadataRetriever.METADATA_KEY_TITLE,
						getCurrentSong().getTag().title)
				.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST,
						getCurrentSong().getTag().artist)
				.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM,
						getCurrentSong().getTag().album).apply();
		if (getAudioFocus()) {
			if (isPlaying()) {
				remoteControlClientCompat
						.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
			} else {
				remoteControlClientCompat
						.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);

			}
		}
	}

	private void unRegisterRemoteClient() {
		try {
			AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
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
		registerReceiver(noisyAudioStreamReceiver, noiseFilter);
	}

	private void unRegisterAudioJackListener() {
		unregisterReceiver(noisyAudioStreamReceiver);
	}

	private static final String PLAY_PAUSE_INTENT = "com.androidplayer.MusicPlayer.INTENT.PlayPause";
	private static final String PREV_INTENT = "com.androidplayer.MusicPlayer.INTENT.Previous";
	private static final String NEXT_INTENT = "com.androidplayer.MusicPlayer.INTENT.Next";
	private static final String COLLAPSE_INTENT = "com.androidplayer.MusicPlayer.INTENT.Collapse";

	public void createNotification() {
		if (isNotificationCreated) {
			return;
		}

		isNotificationCreated = true;
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(NEXT_INTENT);
		intentFilter.addAction(PREV_INTENT);
		intentFilter.addAction(PLAY_PAUSE_INTENT);
		intentFilter.addAction(COLLAPSE_INTENT);

		registerReceiver(intentReciever, intentFilter);

		Intent action = new Intent(PLAY_PAUSE_INTENT);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
				action, PendingIntent.FLAG_CANCEL_CURRENT);
		notificationView.setOnClickPendingIntent(R.id.notification_base_play,
				pendingIntent);

		action = new Intent(PREV_INTENT);
		pendingIntent = PendingIntent.getBroadcast(this, 0, action,
				PendingIntent.FLAG_CANCEL_CURRENT);
		notificationView.setOnClickPendingIntent(
				R.id.notification_base_previous, pendingIntent);

		action = new Intent(NEXT_INTENT);
		pendingIntent = PendingIntent.getBroadcast(this, 0, action,
				PendingIntent.FLAG_CANCEL_CURRENT);
		notificationView.setOnClickPendingIntent(R.id.notification_base_next,
				pendingIntent);

		action = new Intent(COLLAPSE_INTENT);
		pendingIntent = PendingIntent.getBroadcast(this, 0, action,
				PendingIntent.FLAG_CANCEL_CURRENT);
		notificationView.setOnClickPendingIntent(
				R.id.notification_base_collapse, pendingIntent);

		updateNotificationView();
	}

	public void closeNotification() {
		if (!isNotificationCreated) {
			return;
		}
		unregisterReceiver(intentReciever);
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(NOTIFICATION_ID);
		isNotificationCreated = false;
	}

	private void updateNotificationView() {
		if (!isNotificationCreated) {
			return;
		}
		notificationView.setTextViewText(R.id.notification_base_line_one,
				getCurrentSong().getTag().title);
		notificationView.setTextViewText(R.id.notification_base_line_two,
				getCurrentSong().getTag().artist);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(
				this).setSmallIcon(R.drawable.ic_launcher).setContent(
				notificationView);
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = builder.build();
		notification.flags |= Notification.FLAG_NO_CLEAR;
		notificationManager.notify(NOTIFICATION_ID, notification);
	}

	BroadcastReceiver intentReciever = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (PLAY_PAUSE_INTENT.equals(intent.getAction())) {
				if (isPlaying())
					pausePlayback();
				else
					startPlayback();
			} else if (PREV_INTENT.equals(intent.getAction())) {
				try {
					playSong(getPrev(), isPlaying());
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (NEXT_INTENT.equals(intent.getAction())) {
				try {
					playSong(getNext(), isPlaying());
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (COLLAPSE_INTENT.equals(intent.getAction())) {
				pausePlayback();
				closeNotification();
			}
		}
	};

	// Service specific stuff (Local service)

	public class MusicPlayerBinder extends Binder {
		MusicPlayerService getService() {
			return MusicPlayerService.this;
		}
	}

	private final IBinder binder = new MusicPlayerBinder();

	@Override
	public void onCreate() {
		super.onCreate();
		this.mediaButtonReceiverComponent = new ComponentName(this,
				RemoteControlBroadcastReceiver.class);
		notificationView = new RemoteViews(getPackageName(),
				R.layout.notification_template);
		initialize();
		initializeWidgets();

		try {
			playSong(getCurrentSong(), false);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// send a song change request each time the music player is initialized
		sendMetaChangedRequest();
	}

	@Override
	public void onDestroy() {
		finalize();
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
}