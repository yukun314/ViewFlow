package com.zyk.android.widget;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

public class VerticalFlowIndicator extends View implements FlowIndicator{

	private int currentPosition = 0;
	private ViewFlow viewFlow;
	private float scale = 1.0f;
	private Context mContext;
	private Paint mPaint = new Paint();
	private float currentY = -1;
	private float currentX = -1;
	private Timer mTimer ;
	private MyTimerTask mTask;
	private int times=0;
	private int alpha[]={204,153,102,76,51,10};
	private int mNum = 0;
	private Bitmap bitmap;
	private Rect src;
	private RectF dst;
	private float width,height;

	public VerticalFlowIndicator(Context context) {
		super(context);
		this.mContext = context;
		init();
	}

	public VerticalFlowIndicator(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.mContext = context;
		init();
	}

	public VerticalFlowIndicator(Context context, AttributeSet attrs,
								 int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		this.mContext = context;
		init();
	}

	@Override
	public void onSwitched(View view, int position) {
		this.currentPosition = position;
		invalidate();
	}

	@Override
	public void setViewFlow(ViewFlow view) {
		this.viewFlow = view;
		invalidate();
	}

	@Override
	public void onScrolled(int h, int v, int oldh, int oldv) {

	}

//	@Override
//	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//		setMeasuredDimension(300,300);
//	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (mNum == 0) {
			mNum = viewFlow.getViewsCount();
		}

		if(currentX < 0 ){
			currentX = getWidth()/2.0f;
		}

		if(mTimer == null){
			mTimer = new Timer();
			mTask = new MyTimerTask();
			mTimer.schedule(mTask,150,150);
		}

		if(mNum == currentPosition +1){
			mTimer.cancel();
			mTimer = null;
			times = 0;
			mPaint.setAlpha(255);
			return;
		}

		if(times == 0){
//			currentY = getHeight() - 22*scale;
			currentY = getHeight();
		}else if(times < 6){
//			currentY -=2*scale;
			currentY -=3*scale;
		}else if(times < 12){
			currentY -= 0.7*scale;
			mPaint.setAlpha(alpha[times - 6]);
		}else{
			times = -1;
			mTask.cancel();
			mTask = new MyTimerTask();
			mTimer.schedule(mTask,800,150);
			mPaint.setAlpha(0);
		}
		dst.set(currentX - width, currentY - height, currentX + width, currentY);
		canvas.drawBitmap(bitmap, src, dst, mPaint);
//		String text = (currentPosition+2)+"";
//		float textw = mPaint.measureText(text);
//		canvas.drawText(text, currentX - textw/2, currentY, mPaint);

//		canvas.translate(currentX, currentY);
//		canvas.rotate(45);
//		float w = 4*scale;
//		float r = 2*scale;
//		float h = 21*scale;
//		
//		RectF oval1 = new RectF(0 ,0 ,h, w);// 设置个新的长方形  
//        canvas.drawRoundRect(oval1, r, r, mPaint);//第二个参数是x半径，第三个参数是y半径 
//        RectF oval2 = new RectF(0 , 0 ,w,h);// 设置个新的长方形
//        canvas.drawRoundRect(oval2, r, r, mPaint);//第二个参数是x半径，第三个参数是y半径 
//        canvas.rotate(-45);
//        canvas.translate(-currentX, -currentY);
//		canvas.save();
//		
//		canvas.translate(currentX, currentY - 10*scale);
//		canvas.rotate(45);
//        canvas.drawRoundRect(oval1, r, r, mPaint);//第二个参数是x半径，第三个参数是y半径 
//        canvas.drawRoundRect(oval2, r, r, mPaint);//第二个参数是x半径，第三个参数是y半径 
//        canvas.rotate(-45);
//        canvas.translate(-currentX, -currentY);
//		canvas.save();
//		
//        canvas.restore();
		mPaint.setAlpha(255);
		times++;
	}


	private void init(){
		scale = mContext.getResources().getDisplayMetrics().density;
		if(scale < 0.5f){
			scale = 1.0f;
		}
		mPaint.setStyle(Paint.Style.FILL);//充满
		mPaint.setAntiAlias(true);// 设置画笔的锯齿效果
//	     mPaint.setColor(Color.rgb(199, 218, 208));
//	     mPaint.setTextSize(14*scale);
//	     mPaint.setTypeface(Typeface.DEFAULT_BOLD);

		bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.up);
		src = new Rect();
		src.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
		dst = new RectF();
		width = 30*scale;
		height = 2*bitmap.getHeight()*width/bitmap.getWidth();
	}

//	public void setTextColor(int color){
//		mPaint.setColor(color);
//	}

	private class MyTimerTask extends TimerTask{
		@Override
		public void run() {
			postInvalidate();
		}

	}
}
