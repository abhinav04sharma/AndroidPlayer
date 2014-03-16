package com.androidplayer.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.androidplayer.fragments.NowPlayingFragment;
import com.androidplayer.fragments.SongListFragment;

public class TabsPagerAdapter extends FragmentPagerAdapter {

	public TabsPagerAdapter(FragmentManager fm) {
		super(fm);
	}

	@Override
	public Fragment getItem(int index) {
		Fragment f = null;
		switch (index) {
		case 0:
			f = new NowPlayingFragment();
			break;
		case 1:
			f = new SongListFragment();
			break;
		}

		return f;
	}

	@Override
	public int getCount() {
		return 2;
	}

}
