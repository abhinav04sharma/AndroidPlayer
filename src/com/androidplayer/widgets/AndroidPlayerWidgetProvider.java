package com.androidplayer.widgets;

import tags.Song;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.RemoteViews;

import com.androidplayer.MainActivity;
import com.androidplayer.MusicPlayer;
import com.androidplayer.R;

public class AndroidPlayerWidgetProvider extends AppWidgetProvider {

	private static MusicPlayer musicPlayer = null;
	public static final String ACTION_WIDGET_PLAY = "com.androidplayer.widgets.AndroidPlayerWidgetProvider.ActionWidgetPlay";
	public static final String ACTION_WIDGET_SKIP = "com.androidplayer.widgets.AndroidPlayerWidgetProvider.ActionWidgetSkip";
	public static final String ACTION_WIDGET_PREV = "com.androidplayer.widgets.AndroidPlayerWidgetProvider.ActionWidgetPrev";

	private BroadcastReceiver songChangedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			setSong((Song) intent
					.getSerializableExtra(MusicPlayer.CURRENT_SONG),
					context);
		}
	};

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {

		LocalBroadcastManager.getInstance(context)
				.registerReceiver(songChangedReceiver,
						new IntentFilter(MusicPlayer.META_CHANGED));

		musicPlayer = MusicPlayer.getInstance(context.getApplicationContext());
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
				R.layout.androidplayer_appwidget);
		ComponentName watchWidget = new ComponentName(context,
				AndroidPlayerWidgetProvider.class);

		remoteViews.setTextViewText(R.id.widget_current_song, musicPlayer
				.getCurrentSong().getTag().title);

		Intent active = new Intent(context, AndroidPlayerWidgetProvider.class);
		active.setAction(ACTION_WIDGET_PLAY);
		PendingIntent actionPendingIntent = PendingIntent.getBroadcast(context,
				0, active, PendingIntent.FLAG_UPDATE_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.widget_play,
				actionPendingIntent);

		active = new Intent(context, AndroidPlayerWidgetProvider.class);
		active.setAction(ACTION_WIDGET_SKIP);
		actionPendingIntent = PendingIntent.getBroadcast(context, 0, active,
				PendingIntent.FLAG_UPDATE_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.widget_skip,
				actionPendingIntent);

		active = new Intent(context, AndroidPlayerWidgetProvider.class);
		active.setAction(ACTION_WIDGET_PREV);
		actionPendingIntent = PendingIntent.getBroadcast(context, 0, active,
				PendingIntent.FLAG_UPDATE_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.widget_prev,
				actionPendingIntent);

		active = new Intent(context, MainActivity.class);
		actionPendingIntent = PendingIntent.getActivity(context, 0, active,
				PendingIntent.FLAG_UPDATE_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.widget_current_song,
				actionPendingIntent);

		appWidgetManager.updateAppWidget(watchWidget, remoteViews);
	}

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
		LocalBroadcastManager.getInstance(context)
				.registerReceiver(songChangedReceiver,
						new IntentFilter(MusicPlayer.META_CHANGED));
	}

	@Override
	public void onDisabled(Context context) {
		LocalBroadcastManager.getInstance(context).unregisterReceiver(
				songChangedReceiver);
		super.onDisabled(context);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		musicPlayer = MusicPlayer.getInstance(context.getApplicationContext());
		try {
			/*
			 * if (intent.getAction().equals(MusicPlayer.META_CHANGED)) {
			 * setSong((Song) intent
			 * .getSerializableExtra(MusicPlayer.CURRENT_SONG), context); } else
			 */if (intent.getAction().equals(ACTION_WIDGET_PLAY)) {
				if (musicPlayer.isPlaying()) {
					musicPlayer.pausePlayback();
				} else {
					musicPlayer.startPlayback();
				}
			} else if (intent.getAction().equals(ACTION_WIDGET_SKIP)) {
				musicPlayer.playSong(musicPlayer.getNext(),
						musicPlayer.isPlaying());
			} else if (intent.getAction().equals(ACTION_WIDGET_PREV)) {
				musicPlayer.playSong(musicPlayer.getPrev(),
						musicPlayer.isPlaying());
			} else {
				super.onReceive(context, intent);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setSong(Song song, Context context) {
		RemoteViews views = new RemoteViews(context.getPackageName(),
				R.layout.androidplayer_appwidget);
		ComponentName watchWidget = new ComponentName(context,
				AndroidPlayerWidgetProvider.class);
		views.setTextViewText(R.id.widget_current_song, song.getTag().title);

		if (musicPlayer.isPlaying()) {
			views.setImageViewResource(R.id.widget_play,
					R.drawable.ic_action_pause);
		} else {
			views.setImageViewResource(R.id.widget_play,
					R.drawable.ic_action_play);
		}
		(AppWidgetManager.getInstance(context)).updateAppWidget(watchWidget,
				views);
	}

}