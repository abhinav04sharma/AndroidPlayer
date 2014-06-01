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
import com.androidplayer.MusicPlayerService;
import com.androidplayer.MusicPlayerServiceProvider;
import com.androidplayer.R;

public class AndroidPlayerWidgetProvider extends AppWidgetProvider {

	private static MusicPlayerServiceProvider musicPlayerServiceProvider;
	private static MusicPlayerService musicPlayerService;

	public static final String ACTION_WIDGET_PLAY = "com.androidplayer.widgets.AndroidPlayerWidgetProvider.ActionWidgetPlay";
	public static final String ACTION_WIDGET_SKIP = "com.androidplayer.widgets.AndroidPlayerWidgetProvider.ActionWidgetSkip";
	public static final String ACTION_WIDGET_PREV = "com.androidplayer.widgets.AndroidPlayerWidgetProvider.ActionWidgetPrev";

	private BroadcastReceiver songChangedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			setSong((Song) intent
					.getSerializableExtra(MusicPlayerService.CURRENT_SONG),
					context);
		}
	};

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		LocalBroadcastManager.getInstance(context).registerReceiver(
				songChangedReceiver,
				new IntentFilter(MusicPlayerService.META_CHANGED));

		musicPlayerServiceProvider = new MusicPlayerServiceProvider(context);
		musicPlayerServiceProvider.doBindService();
		musicPlayerService = musicPlayerServiceProvider.getMusicPlayerService();

		RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
				R.layout.androidplayer_appwidget);
		ComponentName watchWidget = new ComponentName(context,
				AndroidPlayerWidgetProvider.class);

		remoteViews.setTextViewText(R.id.widget_current_song, musicPlayerService
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
		LocalBroadcastManager.getInstance(context).registerReceiver(
				songChangedReceiver,
				new IntentFilter(MusicPlayerService.META_CHANGED));
	}

	@Override
	public void onDisabled(Context context) {
		LocalBroadcastManager.getInstance(context).unregisterReceiver(
				songChangedReceiver);
		super.onDisabled(context);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		musicPlayerServiceProvider = new MusicPlayerServiceProvider(context);
		musicPlayerServiceProvider.doBindService();
		musicPlayerService = musicPlayerServiceProvider.getMusicPlayerService();

		try {
			if (intent.getAction().equals(ACTION_WIDGET_PLAY)) {
				if (musicPlayerService.isPlaying()) {
					musicPlayerService.pausePlayback();
				} else {
					musicPlayerService.startPlayback();
					musicPlayerService.createNotification();
				}
			} else if (intent.getAction().equals(ACTION_WIDGET_SKIP)) {
				musicPlayerService.playSong(musicPlayerService.getNext(),
						musicPlayerService.isPlaying());
			} else if (intent.getAction().equals(ACTION_WIDGET_PREV)) {
				musicPlayerService.playSong(musicPlayerService.getPrev(),
						musicPlayerService.isPlaying());
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

		if (musicPlayerService.isPlaying()) {
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