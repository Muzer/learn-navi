package org.learnnavi.app;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

class customSorter implements java.util.Comparator {
	public int compare(Object a, Object b) {
		final int BEFORE = -1;
		final int EQUAL = 0;
		final int AFTER = 1;

		WordStruct x = (WordStruct)a;
		WordStruct y = (WordStruct)b;

		if (x.distance == y.distance) return EQUAL;

		if (x.distance < y.distance) return BEFORE;
		if (x.distance > y.distance) return AFTER;
		return EQUAL; //will never be reached
	
	}
}

public class SearchResultsAdapter extends BaseAdapter {
	private Context mContext;
	private ArrayList<WordStruct> mItems;

	public SearchResultsAdapter(Context context, ArrayList<WordStruct> items) {
		mContext = context;
		mItems = items;

		//sort
		Collections.sort(mItems, new customSorter());
	}

	public int getCount() {
		return mItems.size();
	}

	public Object getItem(int position) {
		return mItems.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
			LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(R.layout.entry_row, null);
		}

		WordStruct w = mItems.get(position);
		if (w != null) {
			TextView ew = (TextView)v.findViewById(R.id.EntryWord);
			TextView ed = (TextView)v.findViewById(R.id.EntryDefinition);
			ew.setText(w.word);
			ed.setText(w.definition);
		}

		return v;
	}
} 
