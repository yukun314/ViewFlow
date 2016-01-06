package com.zyk.android.widget.example;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.zyk.android.widget.VerticalFlowIndicator;
import com.zyk.android.widget.ViewFlow;

public class VerticalActivity extends Activity{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_test1);
		ViewFlow vf = (ViewFlow) findViewById(R.id.activity_main_viewflow);
		List<View> list = new ArrayList<View>();
		LayoutInflater inflater = LayoutInflater.from(this);
		View view1 = inflater.inflate(R.layout.test1, null);
		View view3 = inflater.inflate(R.layout.test3, null);
		View view2 = inflater.inflate(R.layout.test2, null);
		Button button = (Button) view2.findViewById(R.id.test2_button);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(VerticalActivity.this,"再这里做相应的操作" , Toast.LENGTH_LONG).show();
			}
		});
		list.add(view1);
		list.add(view3);
		list.add(view2);
		vf.setData(list);
		VerticalFlowIndicator vfi = (VerticalFlowIndicator) findViewById(R.id.activity_main_verticalIndicator);
		vf.setFlowIndicator(vfi);
		vf.setOrientation(false);
	}

}
