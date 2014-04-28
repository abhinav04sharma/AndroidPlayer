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

		MusicPlayer musicPlayer = MusicPlayer.getInstance(context);

		switch (key.getKeyCode()) {
		case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
			if (musicPlayer.isPlaying()) {
				musicPlayer.pausePlayback();
			} else {
				musicPlayer.startPlayback();
			}
			break;
		case KeyEvent.KEYCODE_MEDIA_PLAY:
			musicPlayer.startPlayback();
			break;
		case KeyEvent.KEYCODE_MEDIA_PAUSE:
			musicPlayer.pausePlayback();
			break;
		case KeyEvent.KEYCODE_MEDIA_NEXT:
			try {
				musicPlayer.playSong(musicPlayer.getNext(),
						musicPlayer.isPlaying());
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
			try {
				musicPlayer.playSong(musicPlayer.getPrev(),
						musicPlayer.isPlaying());
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		default:
			return;
		}
	}
}