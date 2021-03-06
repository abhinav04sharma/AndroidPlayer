package com.androidplayer.adapters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.androidplayer.R;

public class StringListAdapter extends ArrayAdapter<String> implements
		SectionIndexer {
	private Context context;
	private String[] strings;
	private int resource;

	HashMap<String, Integer> alphaIndexer;
	String[] sections;

	public StringListAdapter(Context context, int resource, String[] objects) {
		super(context, resource, objects);
		this.context = context;
		this.strings = objects;
		this.resource = resource;

		alphaIndexer = new HashMap<String, Integer>();
		int size = objects.length;

		for (int x = 0; x < size; x++) {
			String s = objects[x];
			// get the first letter of the store
			String ch = s.substring(0, 1);
			ch = ch.toUpperCase(Locale.getDefault());
			// HashMap will prevent duplicates
			alphaIndexer.put(ch, x);
		}

		Set<String> sectionLetters = alphaIndexer.keySet();
		ArrayList<String> sectionList = new ArrayList<String>(sectionLetters);

		Collections.sort(sectionList);
		sections = new String[sectionList.size()];
		sectionList.toArray(sections);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(resource, parent, false);

		TextView song = (TextView) rowView.findViewById(R.id.song);
		TextView artist = (TextView) rowView.findViewById(R.id.artist);

		song.setText(strings[position]);
		artist.setText("");

		return rowView;
	}

	public int getPositionForSection(int section) {
		return alphaIndexer.get(sections[section]);
	}

	public int getSectionForPosition(int position) {
		return 1;
	}

	public Object[] getSections() {
		return sections;
	}

}
