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
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.RemoteViews;

import com.androidplayer.widgets.AndroidPlayerWidgetProvider;

public class MusicPlayer {

	private static MusicPlayer musicPlayer = null;

	private Context context;
	private final String GENRES_META_FILE_NAME = "GENRES_META_FILE.txt";

	private List<Song> songs = new ArrayList<Song>();
	private List<String> artists = new ArrayList<String>();
	private List<String> genres = new ArrayList<String>();

	public static SongFactory songFactory = new SongFactory();
	public static MediaPlayer player;

	public static synchronized MusicPlayer getInstance(Context context) {
		if (musicPlayer == null) {
			musicPlayer = new MusicPlayer();
			musicPlayer.context = context;
			musicPlayer.initialize();

			return musicPlayer;
		}
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
		updateWidgets();
	}

	public Song getPrev() {
		Song ret = songFactory.prev(player.getCurrentPosition() / 1000,
				player.getDuration() / 1000);
		updateWidgets();
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
	}

	public void pausePlayback() {
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
			player.start();
		}
		updateWidgets();
	}

	public MediaPlayer getMediaPlayer() {
		return player;
	}

	private MusicPlayer() {
	}

	private void updateWidgets() {
		RemoteViews views = new RemoteViews(context.getPackageName(),
				R.layout.androidplayer_appwidget);
		ComponentName watchWidget = new ComponentName(context,
				AndroidPlayerWidgetProvider.class);
		views.setTextViewText(R.id.widget_current_song, musicPlayer
				.getCurrentSong().getTag().title);
		(AppWidgetManager.getInstance(context)).updateAppWidget(watchWidget,
				views);
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

	private void initialize() {
		try {
			constructLists();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		player = new MediaPlayer();
		songFactory.initialize(songs, artists, genres);
		updateWidgets();
	}
}
