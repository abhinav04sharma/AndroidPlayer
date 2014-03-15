package com.androidplayer.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.SparseArray;

import com.androidplayer.R;
import com.androidplayer.fragments.NowPlayingFragment;
import com.androidplayer.fragments.SongListFragment;

public class TabsPagerAdapter extends FragmentPagerAdapter {

	// TODO: very hacky!
	private static SparseArray<Fragment> map = new SparseArray<Fragment>();

	public TabsPagerAdapter(FragmentManager fm) {
		super(fm);
	}

	@Override
	public Fragment getItem(int index) {
		Fragment f = null;
		switch (index) {
		case 0:
			f = new NowPlayingFragment();
			map.put(R.id.now_playing_fragment, f);
			break;
		case 1:
			f = new SongListFragment();
			map.put(R.id.list_view_fragment, f);
			break;
		}

		return f;
	}

	@Override
	public int getCount() {
		return 2;
	}

	public static Fragment getFragment(int id) {
		return map.get(id);
	}

}
