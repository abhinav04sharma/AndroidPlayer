package com.androidplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

public class RemoteControlBroadcastReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction() != Intent.ACTION_MEDIA_BUTTON)
			return;

		KeyEvent key = (KeyEvent) intent
				.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

		if (key.getAction() != KeyEvent.ACTION_DOWN)
			return;

		MusicPlayerServiceProvider musicPlayerServiceProvider = new MusicPlayerServiceProvider(
				context);
		musicPlayerServiceProvider.doBindService();
		MusicPlayerService musicPlayerService = musicPlayerServiceProvider
				.getMusicPlayerService();

		switch (key.getKeyCode()) {
		case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
			if (musicPlayerService.isPlaying()) {
				musicPlayerService.pausePlayback();
			} else {
				musicPlayerService.startPlayback();
			}
			break;
		case KeyEvent.KEYCODE_MEDIA_PLAY:
			musicPlayerService.startPlayback();
			break;
		case KeyEvent.KEYCODE_MEDIA_PAUSE:
			musicPlayerService.pausePlayback();
			break;
		case KeyEvent.KEYCODE_MEDIA_NEXT:
			try {
				musicPlayerService.playSong(musicPlayerService.getNext(),
						musicPlayerService.isPlaying());
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
			try {
				musicPlayerService.playSong(musicPlayerService.getPrev(),
						musicPlayerService.isPlaying());
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		default:
			try {
				throw new Exception("Unrecognized message");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		musicPlayerServiceProvider.doUnbindService();
	}
}