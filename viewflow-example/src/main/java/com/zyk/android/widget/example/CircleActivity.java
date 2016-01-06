package com.zyk.android.widget.example;

import android.app.Activity;
import android.os.Bundle;

import com.zyk.android.widget.CircleFlowIndicator;
import com.zyk.android.widget.ViewFlow;

public class CircleActivity extends Activity{
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_test2);
		ViewFlow viewFlow = (ViewFlow) findViewById(R.id.activity_main_viewflow);
		viewFlow.setAdapter(new ImageAdapter(this), 0);
		viewFlow.setOrientation(true);
		CircleFlowIndicator indic = (CircleFlowIndicator) findViewById(R.id.activity_main_verticalIndicator);
		float scale = getResources().getDisplayMetrics().density;
		indic.setRadius(4*scale);
		viewFlow.setFlowIndicator(indic);
	}
}
