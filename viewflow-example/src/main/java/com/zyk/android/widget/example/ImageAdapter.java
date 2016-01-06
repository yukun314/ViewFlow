package com.zyk.android.widget.example;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

public class ImageAdapter extends BaseAdapter {

	private LayoutInflater mInflater;
	private static final int[] ids = {R.drawable.gingerbread, R.drawable.honeycomb, R.drawable.icecream };
	public ImageAdapter(Context context) {
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {
		return ids.length;
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.image_item, null);
		}
		System.out.println("ImageAdapter position:"+position);
		((ImageView) convertView.findViewById(R.id.imgView)).setImageResource(ids[position]);
		return convertView;
	}

}
