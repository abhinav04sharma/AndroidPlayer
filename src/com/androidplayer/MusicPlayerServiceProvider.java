package com.androidplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

public class MusicPlayerServiceProvider {

	private MusicPlayerService musicPlayerService;
	private boolean isBound;
	private boolean isConnected;
	private Context context;

	public MusicPlayerServiceProvider(Context context) {
		this.context = context;
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			musicPlayerService = ((MusicPlayerService.MusicPlayerBinder) service)
					.getService();
			isConnected = true;
		}

		public void onServiceDisconnected(ComponentName className) {
			musicPlayerService = null;
			isConnected = false;
		}
	};

	public MusicPlayerService getMusicPlayerService() {
		while (!isConnected())
			;
		return musicPlayerService;
	}

	public void doBindService() {
		isBound = context.bindService(new Intent(context,
				MusicPlayerService.class), mConnection,
				Context.BIND_AUTO_CREATE);
	}

	public void doUnbindService() {
		if (isBound) {
			context.unbindService(mConnection);
			isBound = false;
		}
	}

	public boolean isBound() {
		return isBound;
	}

	public boolean isConnected() {
		return isConnected;
	}

}
