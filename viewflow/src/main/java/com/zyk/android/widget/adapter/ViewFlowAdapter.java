package com.zyk.android.widget.adapter;

import java.util.ArrayList;
import java.util.List;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class ViewFlowAdapter extends BaseAdapter{

	private List<View> mList;
	public ViewFlowAdapter(List<View> list){
		if(list == null){
			mList = new ArrayList<View>();
		}else{
			this.mList = list;
		}
	}
	@Override
	public int getCount() {
		return mList.size();
	}

	@Override
	public Object getItem(int position) {
		return mList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if(position < mList.size()){
			return mList.get(position);
		}
		return null;
	}
	
}
