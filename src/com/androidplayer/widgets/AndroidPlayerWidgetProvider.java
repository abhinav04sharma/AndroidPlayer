package com.androidplayer.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.androidplayer.MainActivity;
import com.androidplayer.MusicPlayer;
import com.androidplayer.R;

public class AndroidPlayerWidgetProvider extends AppWidgetProvider {

	private static MusicPlayer musicPlayer = null;
	public static final String ACTION_WIDGET_PLAY = "com.androidplayer.ActionWidgetPlay";
	public static final String ACTION_WIDGET_SKIP = "com.androidplayer.ActionWidgetSkip";
	public static final String ACTION_WIDGET_PREV = "com.androidplayer.ActionWidgetPrev";

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {

		musicPlayer = MusicPlayer.getInstance(context);
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
	public void onReceive(Context context, Intent intent) {
		musicPlayer = MusicPlayer.getInstance(context);
		RemoteViews views = new RemoteViews(context.getPackageName(),
				R.layout.androidplayer_appwidget);
		ComponentName watchWidget = new ComponentName(context,
				AndroidPlayerWidgetProvider.class);
		try {
			if (intent.getAction().equals(ACTION_WIDGET_PLAY)) {
				if (musicPlayer.isPlaying()) {
					musicPlayer.pausePlayback();
					views.setImageViewResource(R.id.widget_play,
							R.drawable.ic_action_play);
				} else {
					musicPlayer.startPlayback();
					views.setImageViewResource(R.id.widget_play,
							R.drawable.ic_action_pause);
				}
			} else if (intent.getAction().equals(ACTION_WIDGET_SKIP)) {
				musicPlayer.playSong(musicPlayer.getNext(), true);
			} else if (intent.getAction().equals(ACTION_WIDGET_PREV)) {
				musicPlayer.playSong(musicPlayer.getPrev(), true);
			} else {
				super.onReceive(context, intent);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		views.setTextViewText(R.id.widget_current_song, musicPlayer
				.getCurrentSong().getTag().title);
		(AppWidgetManager.getInstance(context)).updateAppWidget(watchWidget,
				views);
	}

}