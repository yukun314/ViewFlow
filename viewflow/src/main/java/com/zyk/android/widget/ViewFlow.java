
package com.zyk.android.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.Scroller;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import com.zyk.android.widget.adapter.ViewFlowAdapter;

/**
 * A horizontally scrollable {@link ViewGroup} with items populated from an
 * {@link Adapter}. The ViewFlow uses a buffer to store loaded {@link View}s in.
 * The default size of the buffer is 3 elements on both sides of the currently
 * visible {@link View}, making up a total buffer size of 3 * 2 + 1 = 7. The
 * buffer size can be changed using the {@code sidebuffer} xml attribute.
 * 
 */
public class ViewFlow extends AdapterView<Adapter> {

	private static final int SNAP_VELOCITY = 1000;
	private static final int INVALID_SCREEN = -1;
	private final static int TOUCH_STATE_REST = 0;
	private final static int TOUCH_STATE_SCROLLING = 1;

	private LinkedList<View> mLoadedViews;
	private LinkedList<View> mRecycledViews;
	private int mCurrentBufferIndex;
	private int mCurrentAdapterIndex;
	private int mSideBuffer = 2;
	private Scroller mScroller;
	//����VelocityTracker �ο� http://blog.csdn.net/hudashi/article/details/7352157
	private VelocityTracker mVelocityTracker;
	private int mTouchState = TOUCH_STATE_REST;
	private float mLastMotionX;
	private float mLastMotionY;
	private int mTouchSlop;
	private int mMaximumVelocity;
	private int mCurrentScreen;
	private int mNextScreen = INVALID_SCREEN;
	private boolean mFirstLayout = true;
	private ViewSwitchListener mViewSwitchListener;
	private ViewLazyInitializeListener mViewInitializeListener;
	private EnumSet<LazyInit> mLazyInit = EnumSet.allOf(LazyInit.class);
	private Adapter mAdapter;
	private int mLastScrollDirection;
	private AdapterDataSetObserver mDataSetObserver;
	private FlowIndicator mIndicator;
	private int mLastOrientation = -1;
	/** Extra return value from obtainView: tells you whether the item it returned on the last call was recycled rather than created by the adapter.
	 * This is a member because getting a second return value requires an allocation. */
	private boolean mLastObtainedViewWasRecycled = false;
	
	public static final boolean HORIZONTAL = true;
    public static final boolean VERTICAL = false;
    /**
     * �л��ķ��� Ĭ����ˮƽ��
     */
    private boolean mOrientation = HORIZONTAL;
    
	private OnGlobalLayoutListener orientationChangeListener = new OnGlobalLayoutListener() {

		@Override
		public void onGlobalLayout() {
			getViewTreeObserver().removeGlobalOnLayoutListener(
					orientationChangeListener);
//			getViewTreeObserver().removeOnGlobalLayoutListener(orientationChangeListener);
			setSelection(mCurrentAdapterIndex);
		}
	};

	/**
	 * Receives call backs when a new {@link View} has been scrolled to.
	 */
	public interface ViewSwitchListener {

		/**
		 * This method is called when a new View has been scrolled to.
		 * 
		 * @param view
		 *			  the {@link View} currently in focus.
		 * @param position
		 *			  The position in the adapter of the {@link View} currently in focus.
		 */
		void onSwitched(View view, int position);

	}

	public interface ViewLazyInitializeListener {
		void onViewLazyInitialize(View view, int position);
	}

	enum LazyInit {
		LEFT, RIGHT
	}

	public ViewFlow(Context context) {
		super(context);
		mSideBuffer = 3;
		init();
	}

	public ViewFlow(Context context, int sideBuffer) {
		super(context);
		mSideBuffer = sideBuffer;
		init();
	}

	public ViewFlow(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray styledAttrs = context.obtainStyledAttributes(attrs,
				R.styleable.ViewFlow);
		mSideBuffer = styledAttrs.getInt(R.styleable.ViewFlow_sidebuffer, 3);
		init();
		styledAttrs.recycle();
	}

	/**
	 * ���÷���
	 * @param orientation true ˮƽ���� false ��ֱ����
	 */
	public void setOrientation(boolean orientation){
		this.mOrientation = orientation;
	}
	
	private void init() {
		mLoadedViews = new LinkedList<View>();
		mRecycledViews = new LinkedList<View>();
		mScroller = new Scroller(getContext());
		final ViewConfiguration configuration = ViewConfiguration
				.get(getContext());
		mTouchSlop = configuration.getScaledTouchSlop();
		mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
	}

	public void onConfigurationChanged(Configuration newConfig) {
		if (newConfig.orientation != mLastOrientation) {
			mLastOrientation = newConfig.orientation;
			getViewTreeObserver().addOnGlobalLayoutListener(orientationChangeListener);
		}
	}

	public int getViewsCount() {
		return mAdapter.getCount();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);
		final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

		int childWidth = 0;
		int childHeight = 0;
		int childState = 0;

		final int widthPadding = getWidthPadding();
		final int heightPadding = getHeightPadding();

		int count = mAdapter == null ? 0 : mAdapter.getCount();
		if (count > 0) {
			final View child = obtainView(0);
			measureChild(child, widthMeasureSpec, heightMeasureSpec);
			childWidth = child.getMeasuredWidth();
			childHeight = child.getMeasuredHeight();
			childState = child.getMeasuredState();
			mRecycledViews.add(child);
		}

		switch (widthMode) {
			case MeasureSpec.UNSPECIFIED:
				widthSize = childWidth + widthPadding;
				break;
			case MeasureSpec.AT_MOST:
				widthSize = (childWidth + widthPadding) | childState;
				break;
			case MeasureSpec.EXACTLY:
				if (widthSize < childWidth + widthPadding)
					widthSize |= MEASURED_STATE_TOO_SMALL;
				break;
		}
		switch (heightMode) {
			case MeasureSpec.UNSPECIFIED:
				heightSize = childHeight + heightPadding;
				break;
			case MeasureSpec.AT_MOST:
				heightSize = (childHeight + heightPadding) | (childState >> MEASURED_HEIGHT_STATE_SHIFT);
				break;
			case MeasureSpec.EXACTLY:
				if (heightSize < childHeight + heightPadding)
					heightSize |= MEASURED_STATE_TOO_SMALL;
				break;
		}

		if (heightMode == MeasureSpec.UNSPECIFIED) {
			heightSize = heightPadding + childHeight;
		} else {
			heightSize |= (childState&MEASURED_STATE_MASK);
		}

		setMeasuredDimension(widthSize, heightSize);
	}

	private int getWidthPadding() {
		return getPaddingLeft() + getPaddingRight() + getHorizontalFadingEdgeLength() * 2;
	}

	public int getChildWidth() {
		return getWidth() - getWidthPadding();
	}

	private int getHeightPadding() {
//		return getPaddingTop() + getPaddingBottom();
		return getPaddingTop() + getPaddingBottom() + getVerticalFadingEdgeLength() * 2;
	}

	public int getChildHeight() {
		return getHeight() - getHeightPadding();
	}

	@Override 
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);

		final int count = getChildCount();
		for (int i = 0; i < count ; ++i) {
			final View child = getChildAt(i);
			child.measure(MeasureSpec.makeMeasureSpec(getChildWidth(), MeasureSpec.EXACTLY),
					MeasureSpec.makeMeasureSpec(getChildHeight(), MeasureSpec.EXACTLY));
		}

		if (mFirstLayout) {
			if(mOrientation){
				mScroller.startScroll(0, 0, mCurrentScreen * getChildWidth(), 0, 0);
			}else{
				mScroller.startScroll(0, 0, 0, mCurrentScreen * getChildHeight(), 0);
			}
			mFirstLayout = false;
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if(mOrientation){
			int childLeft = getPaddingLeft() + getHorizontalFadingEdgeLength();
			final int count = getChildCount();
			for (int i = 0; i < count; i++) {
				final View child = getChildAt(i);
				if (child.getVisibility() != View.GONE) {
					final int childWidth = child.getMeasuredWidth();
					child.layout(childLeft, getPaddingTop(), childLeft + childWidth,
							getPaddingTop() + child.getMeasuredHeight());
					childLeft += childWidth;
				}
			}
		}else{
			int childTop = getPaddingTop() + getVerticalFadingEdgeLength();
			final int count = getChildCount();
			for (int i = 0; i < count; i++) {
				final View child = getChildAt(i);
				if (child.getVisibility() != View.GONE) {
					final int childHeight = child.getMeasuredHeight();
					child.layout(getPaddingLeft(),childTop, getPaddingLeft()+child.getMeasuredWidth(),
							childTop+childHeight);
					childTop += childHeight;
				}
			}
		}
	}

	@Override 
	protected float getTopFadingEdgeStrength() {
		return 0.0f;
	}

	@Override 
	protected float getBottomFadingEdgeStrength() {
		return 0.0f;
	}

	@Override 
	protected float getLeftFadingEdgeStrength() {
		// always do the fading edge
		return 1.0f;
	}

	@Override 
	protected float getRightFadingEdgeStrength() {
		// always do the fading edge
		return 1.0f;
	}

//	@Override
//	public boolean onInterceptTouchEvent(MotionEvent ev) {
//		System.out.println("onInterceptTouchEcent");
//		if (getChildCount() == 0)
//			return false;
//
//		if (mVelocityTracker == null) {
//			mVelocityTracker = VelocityTracker.obtain();
//		}
//		mVelocityTracker.addMovement(ev);
//
//		final int action = ev.getAction();
//		final float x = ev.getX();
//		final float y = ev.getY();
//		
//		switch (action) {
//		case MotionEvent.ACTION_DOWN:
//			actionDown(x,y);
//			break;
//
//		case MotionEvent.ACTION_MOVE:
//			final int deltaX = (int) (mLastMotionX - x);
//			final int deltaY = (int) (mLastMotionY - y);
//
////			boolean xMoved = Math.abs(deltaX) > mTouchSlop;
////
////			if (xMoved) {
////				// Scroll if the user moved far enough along the X axis
////				mTouchState = TOUCH_STATE_SCROLLING;
////
////				if (mViewInitializeListener != null)
////					initializeView(deltaX);
////			}
//			
//			boolean Moved ;
//			int delta = 0;
//			if(mOrientation){
//				delta = deltaX;
//				Moved = Math.abs(deltaX) > mTouchSlop;
//			}else{
//				delta = deltaY;
//				Moved = Math.abs(deltaY) > mTouchSlop;
//			}
//			if (Moved) {
//				// Scroll if the user moved far enough along the X axis
//				mTouchState = TOUCH_STATE_SCROLLING;
////FIXME initializeView ��ʱ����
//				if (mViewInitializeListener != null)
//					initializeView(delta);
//			}
//
//			if (mTouchState == TOUCH_STATE_SCROLLING) {
//				// Scroll to follow the motion event
//
//				mLastMotionX = x;
//				onScrolling(deltaX,deltaY,getWidth(),getHeight());
////				final int scrollX = getScrollX();
////				if (deltaX < 0) {
////					if (scrollX > 0) {
////						scrollBy(Math.max(-scrollX, deltaX), 0);
////					}
////				} else if (deltaX > 0) {
////					final int availableToScroll = getChildAt(
////							getChildCount() - 1).getRight()
////							- getPaddingRight() - getHorizontalFadingEdgeLength()
////							- scrollX - getWidth();
////					if (availableToScroll > 0) {
////						scrollBy(Math.min(availableToScroll, deltaX), 0);
////					}
////				}
//				return true;
//			}
//			break;
//
//		case MotionEvent.ACTION_UP:
//			actionUp();
//			break;
//		case MotionEvent.ACTION_CANCEL:
//			mTouchState = TOUCH_STATE_REST;
//		}
//		return false;
//	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (getChildCount() == 0)
			return false;

		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(ev);

		final int action = ev.getAction();
		final float x = ev.getX();
		final float y = ev.getY();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			actionDown(x,y);
			break;

		case MotionEvent.ACTION_MOVE:
			final int deltaX = (int) (mLastMotionX - x);
			final int deltaY = (int) (mLastMotionY - y);

			boolean Moved ; 
			int delta = 0;
			if(mOrientation){
				delta = deltaX;
				Moved = Math.abs(deltaX) > mTouchSlop;
			}else{
				delta = deltaY;
				Moved = Math.abs(deltaY) > mTouchSlop;
			}
			if (Moved) {
				// Scroll if the user moved far enough along the X axis
				mTouchState = TOUCH_STATE_SCROLLING;
				if (mViewInitializeListener != null)
					initializeView(delta);
			}

			if (mTouchState == TOUCH_STATE_SCROLLING) {
				// Scroll to follow the motion event
				mLastMotionX = x;
				mLastMotionY = y;
				onScrolling(deltaX,deltaY,getChildWidth(),getChildHeight());
//				final int scrollX = getScrollX();
//				if (deltaX < 0) {
//					if (scrollX > 0) {
//						scrollBy(Math.max(-scrollX, deltaX), 0);
//					}
//				} else if (deltaX > 0) {
//					final int availableToScroll = getChildAt(
//							getChildCount() - 1).getRight()
//							- getPaddingRight() - getHorizontalFadingEdgeLength()
//							- scrollX - getChildWidth();
//					if (availableToScroll > 0) {
//						scrollBy(Math.min(availableToScroll, deltaX), 0);
//					}
//				}
				return true;
			}
			break;

		case MotionEvent.ACTION_UP:
			actionUp();
			break;
		case MotionEvent.ACTION_CANCEL:
			snapToDestination();
			mTouchState = TOUCH_STATE_REST;
		}
		return true;
	}
	
	private void actionDown(float x,float y){
		/*
		 * If being flinged and user touches, stop the fling. isFinished
		 * will be false if being flinged.
		 */
		if (!mScroller.isFinished()) {
			mScroller.abortAnimation();
		}

		// Remember where the motion event started
		mLastMotionX = x;
		mLastMotionY = y;

		mTouchState = mScroller.isFinished() ? TOUCH_STATE_REST
				: TOUCH_STATE_SCROLLING;
	}
	
	private void onScrolling(int deltaX,int deltaY,int width,int height){
		if(mOrientation){
			final int scrollX = getScrollX();
			if (deltaX < 0) {
				//��֤�������View��һ������߽粻���ƽ���Ļ
				if (scrollX > 0) {
					scrollBy(Math.max(-scrollX, deltaX), 0);
				}
			} else if (deltaX > 0) {
				final int availableToScroll = getChildAt(
						getChildCount() - 1).getRight()
						- getPaddingRight() - getHorizontalFadingEdgeLength()
						- scrollX - width;
				//��֤�����ұ�Viewһ�����ұ߽粻�ܽ�����Ļ
				if (availableToScroll > 0) {
					scrollBy(Math.min(availableToScroll, deltaX), 0);
				}
			}
		}else{
			final int scrollY = getScrollY();
			if (deltaY < 0) {
				//��֤���������һ��View���ϱ߽粻�ܽ�����Ļ
				if (scrollY >= 0) {
					scrollBy(0,Math.max(-scrollY, deltaY));
				}
			} else if (deltaY > 0) {
				final int availableToScroll = getChildAt(
						getChildCount() - 1).getBottom()
						- getPaddingBottom() - getVerticalFadingEdgeLength()//getHorizontalFadingEdgeLength()
						- scrollY - height + 10;
				//��֤��������һ��View���±߽粻�ܽ�����Ļ
				if (availableToScroll > 0) {
					scrollBy(0,Math.min(availableToScroll, deltaY));
				}
			}
		}
	}
	
	private void actionUp(){
		if (mTouchState == TOUCH_STATE_SCROLLING) {
			final VelocityTracker velocityTracker = mVelocityTracker;
			velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
			if(mOrientation){
				int velocityX = (int) velocityTracker.getXVelocity();

				if (velocityX > SNAP_VELOCITY && mCurrentScreen > 0) {
					// Fling hard enough to move left
					snapToScreen(mCurrentScreen - 1);
				} else if (velocityX < -SNAP_VELOCITY
						&& mCurrentScreen < getChildCount() - 1) {
					// Fling hard enough to move right
					snapToScreen(mCurrentScreen + 1);
				} else {
					snapToDestination();
				}
			}else{
				int velocityY = (int) velocityTracker.getYVelocity();

				if (velocityY > SNAP_VELOCITY && mCurrentScreen > 0) {
					// Fling hard enough to move left
					snapToScreen(mCurrentScreen - 1);
				} else if (velocityY < -SNAP_VELOCITY
						&& mCurrentScreen < getChildCount() - 1) {
					// Fling hard enough to move right
					snapToScreen(mCurrentScreen + 1);
				} else {
					snapToDestination();
				}
			}

			if (mVelocityTracker != null) {
				mVelocityTracker.recycle();
				mVelocityTracker = null;
			}
		}
		mTouchState = TOUCH_STATE_REST;
	}

	private void initializeView(final float direction) {
		if (direction > 0) {
			if (mLazyInit.contains(LazyInit.RIGHT)) {
				mLazyInit.remove(LazyInit.RIGHT);
				if (mCurrentBufferIndex+1 < mLoadedViews.size())
					mViewInitializeListener.onViewLazyInitialize(mLoadedViews.get(mCurrentBufferIndex + 1), mCurrentAdapterIndex + 1);
			}
		} else {
			if (mLazyInit.contains(LazyInit.LEFT)) {
				mLazyInit.remove(LazyInit.LEFT);
				if (mCurrentBufferIndex > 0)
					mViewInitializeListener.onViewLazyInitialize(mLoadedViews.get(mCurrentBufferIndex - 1), mCurrentAdapterIndex - 1);
			}
		}
	}

	@Override
	protected void onScrollChanged(int h, int v, int oldh, int oldv) {
		super.onScrollChanged(h, v, oldh, oldv);
		if (mIndicator != null) {
			if(mOrientation){
				/*
				 * The actual horizontal scroll origin does typically not match the
				 * perceived one. Therefore, we need to calculate the perceived
				 * horizontal scroll origin here, since we use a view buffer.
				 */
				int hPerceived = h + (mCurrentAdapterIndex - mCurrentBufferIndex)
						* getChildWidth();
				mIndicator.onScrolled(hPerceived, v, oldh, oldv);
			}else{
				/*
				 * �����¹���ʱ ΪʹCircleFlowIndicatorָʾ����ʾ������Ҫ��������
				 * ������ľ��뻻��ɺ���ľ���
				 * ����˵������ֱ������ô����ˮƽ��ָʾ����
				 */
//				int hPerceived = v*getChildWidth()/getChildHeight()+(mCurrentAdapterIndex - mCurrentBufferIndex) * getChildWidth();
//				mIndicator.onScrolled(hPerceived, h, oldh,oldv);
			}
		}
	}

	private void snapToDestination() {
		if(mOrientation){
			final int screenWidth = getChildWidth();
			final int whichScreen = (getScrollX() + (screenWidth / 2))
					/ screenWidth;

			snapToScreen(whichScreen);
		}else{
			final int screenHeight = getChildHeight();
			final int whichScreen = (getScrollY() + (screenHeight / 2))
					/ screenHeight;

			snapToScreen(whichScreen);
		}
	}

	private void snapToScreen(int whichScreen) {
		mLastScrollDirection = whichScreen - mCurrentScreen;
		if (!mScroller.isFinished())
			return;

		whichScreen = Math.max(0, Math.min(whichScreen, getChildCount() - 1));
		mNextScreen = whichScreen;
		
		if(mOrientation){
			final int newX = whichScreen * getChildWidth();
			final int delta = newX - getScrollX();
			mScroller.startScroll(getScrollX(), 0, delta, 0, Math.abs(delta) * 2);
		}else{
			final int newY = whichScreen * getChildHeight();
			final int delta = newY - getScrollY();
			mScroller.startScroll(0,getScrollY(), 0,delta, Math.abs(delta) * 2);
		}
		invalidate();
	}

	@Override
	public void computeScroll() {
		if (mScroller.computeScrollOffset()) {
			scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
			postInvalidate();
		} else if (mNextScreen != INVALID_SCREEN) {
			mCurrentScreen = Math.max(0,
					Math.min(mNextScreen, getChildCount() - 1));
			mNextScreen = INVALID_SCREEN;
			post(new Runnable() {
				@Override public void run() {
					postViewSwitched(mLastScrollDirection);
				}
			});
		}
	}

	/**
	 * Scroll to the {@link View} in the view buffer specified by the index.
	 * 
	 * @param indexInBuffer
	 *			  Index of the view in the view buffer.
	 */
	private void setVisibleView(int indexInBuffer, boolean uiThread) {
		mCurrentScreen = Math.max(0,
				Math.min(indexInBuffer, getChildCount() - 1));
		if(mOrientation){
			int dx = (mCurrentScreen * getChildWidth()) - mScroller.getCurrX();
			mScroller.startScroll(mScroller.getCurrX(), mScroller.getCurrY(), dx,
					0, 0);
			//FIXME �����׸�ʲô�ã�
			if(dx == 0){
				onScrollChanged(mScroller.getCurrX() + dx, mScroller.getCurrY(), mScroller.getCurrX() + dx, mScroller.getCurrY());
			}
		}else{
			int dy = (mCurrentScreen * getChildHeight()) - mScroller.getCurrY();
			mScroller.startScroll(mScroller.getCurrX(), mScroller.getCurrY(), 0, dy, 0);
			//FIXME �����׸�ʲô�ã�
			if(dy == 0){
				onScrollChanged(mScroller.getCurrX(), mScroller.getCurrY() + dy, mScroller.getCurrX(), mScroller.getCurrY() + dy);
			}
		}
			
		if (uiThread){
			invalidate();
		}else{
			postInvalidate();
		}
	}

	/**
	 * Set the listener that will receive notifications every time the {code
	 * ViewFlow} scrolls.
	 * 
	 * @param l
	 *			  the scroll listener
	 */
	public void setOnViewSwitchListener(ViewSwitchListener l) {
		mViewSwitchListener = l;
	}

	public void setOnViewLazyInitializeListener(ViewLazyInitializeListener l) {
		mViewInitializeListener = l;
	}

	@Override
	public Adapter getAdapter() {
		return mAdapter;
	}

	@Override
	public void setAdapter(Adapter adapter) {
		setAdapter(adapter, 0);
	}
	
	public void setAdapter(Adapter adapter, int initialPosition) {
		if (mAdapter != null) {
			mAdapter.unregisterDataSetObserver(mDataSetObserver);
		}

		mAdapter = adapter;

		if (mAdapter != null) {
			mDataSetObserver = new AdapterDataSetObserver();
			mAdapter.registerDataSetObserver(mDataSetObserver);

		}
		if (mAdapter == null || mAdapter.getCount() == 0)
			return;
		
		setSelection(initialPosition);		
	}
	
	public void setData(List<View> list){
		setData(list,0);
	}
	
	public void setData(List<View> list,int initialPosition){
		ViewFlowAdapter vfa = new ViewFlowAdapter(list);
		setAdapter(vfa,initialPosition);
	}
	
	@Override
	public View getSelectedView() {
		return (mCurrentBufferIndex < mLoadedViews.size() ? mLoadedViews
				.get(mCurrentBufferIndex) : null);
	}

	@Override
	public int getSelectedItemPosition() {
		return mCurrentAdapterIndex;
	}

	/**
	 * Set the FlowIndicator
	 * 
	 * @param flowIndicator
	 */
	public void setFlowIndicator(FlowIndicator flowIndicator) {
		mIndicator = flowIndicator;
		mIndicator.setViewFlow(this);
	}

	protected void recycleViews() {
		while (!mLoadedViews.isEmpty())
			recycleView(mLoadedViews.remove());
	}

	protected void recycleView(View v) {
		if (v == null)
			return;
		mRecycledViews.addFirst(v);
		detachViewFromParent(v);
	}

	protected View getRecycledView() {
		return (mRecycledViews.isEmpty() ? null : mRecycledViews.remove());
	}

	@Override
	public void setSelection(int position) {
		mNextScreen = INVALID_SCREEN;
		mScroller.forceFinished(true);
		if (mAdapter == null)
			return;
		
		position = Math.max(position, 0);
		position = Math.min(position, mAdapter.getCount()-1);

		recycleViews();
		View currentView = makeAndAddView(position, true);
		mLoadedViews.addLast(currentView);

		if (mViewInitializeListener != null)
			mViewInitializeListener.onViewLazyInitialize(currentView, position);

		for(int offset = 1; mSideBuffer - offset >= 0; offset++) {
			int leftIndex = position - offset;
			int rightIndex = position + offset;
			
			if(leftIndex >= 0){
				mLoadedViews.addFirst(makeAndAddView(leftIndex, false));
			}
			if(rightIndex < mAdapter.getCount()){
				mLoadedViews.addLast(makeAndAddView(rightIndex, true));
			}
		}

		mCurrentBufferIndex = mLoadedViews.indexOf(currentView);
		mCurrentAdapterIndex = position;
		
		requestLayout();
		setVisibleView(mCurrentBufferIndex, false);
		if (mIndicator != null) {
				mIndicator.onSwitched(currentView, mCurrentAdapterIndex);
		}
		if (mViewSwitchListener != null) {
			mViewSwitchListener.onSwitched(currentView, mCurrentAdapterIndex);
		}
	}

	private void resetFocus() {
		logBuffer();
		recycleViews();
		removeAllViewsInLayout();
		mLazyInit.addAll(EnumSet.allOf(LazyInit.class));

		for (int i = Math.max(0, mCurrentAdapterIndex - mSideBuffer); i < Math
				.min(mAdapter.getCount(), mCurrentAdapterIndex + mSideBuffer
						+ 1); i++) {
			mLoadedViews.addLast(makeAndAddView(i, true));
			if (i == mCurrentAdapterIndex) {
				mCurrentBufferIndex = mLoadedViews.size() - 1;
				if (mViewInitializeListener != null)
					mViewInitializeListener.onViewLazyInitialize(mLoadedViews.getLast(), mCurrentAdapterIndex);
			}
		}
		logBuffer();
		requestLayout();
	}

	private void postViewSwitched(int direction) {
		if (direction == 0)
			return;
		if (direction > 0) { // to the right
			mCurrentAdapterIndex++;
			mCurrentBufferIndex++;
			mLazyInit.remove(LazyInit.LEFT);
			mLazyInit.add(LazyInit.RIGHT);

			// Recycle view outside buffer range
			if (mCurrentAdapterIndex > mSideBuffer) {
				recycleView(mLoadedViews.removeFirst());
				mCurrentBufferIndex--;
			}
			
			

			// Add new view to buffer
			int newBufferIndex = mCurrentAdapterIndex + mSideBuffer;
			if (newBufferIndex < mAdapter.getCount()){
				mLoadedViews.addLast(makeAndAddView(newBufferIndex, true));
			}

		} else { // to the left
			mCurrentAdapterIndex--;
			mCurrentBufferIndex--;
			mLazyInit.add(LazyInit.LEFT);
			mLazyInit.remove(LazyInit.RIGHT);

			// Recycle view outside buffer range
			if (mAdapter.getCount() - 1 - mCurrentAdapterIndex > mSideBuffer) {
				recycleView(mLoadedViews.removeLast());
			}

			// Add new view to buffer
			int newBufferIndex = mCurrentAdapterIndex - mSideBuffer;
			if (newBufferIndex > -1) {
				mLoadedViews.addFirst(makeAndAddView(newBufferIndex, false));
				mCurrentBufferIndex++;
			}

		}

		requestLayout();
		setVisibleView(mCurrentBufferIndex, true);
		if (mIndicator != null) {
			if(mCurrentBufferIndex < mLoadedViews.size()){
			mIndicator.onSwitched(mLoadedViews.get(mCurrentBufferIndex),
						mCurrentAdapterIndex);
			}

		}
		if (mViewSwitchListener != null) {
			mViewSwitchListener
					.onSwitched(mLoadedViews.get(mCurrentBufferIndex),
							mCurrentAdapterIndex);
		}
		logBuffer();
	}

	@Override 
	protected void measureChild(View child, int parentWidthMeasureSpec, int parentHeightMeasureSpec) {
		LayoutParams lp = child.getLayoutParams();
		final int childWidthSpec = getChildMeasureSpec(parentWidthMeasureSpec, getWidthPadding(), lp.width);
		final int childHeightSpec = getChildMeasureSpec(parentHeightMeasureSpec, getHeightPadding(), lp.height);
		child.measure(childWidthSpec, childHeightSpec);
	}

	private View setupChild(View child, boolean addToEnd, boolean recycle) {
		final LayoutParams lp = child.getLayoutParams();
		child.measure(MeasureSpec.makeMeasureSpec(getChildWidth(), MeasureSpec.EXACTLY),
				MeasureSpec.makeMeasureSpec(getChildHeight(), MeasureSpec.EXACTLY));
		if (recycle){
			attachViewToParent(child, (addToEnd ? -1 : 0), lp);
		}else{
			addViewInLayout(child, (addToEnd ? -1 : 0), lp, true);
		}
			return child;
	}

	private View makeAndAddView(int position, boolean addToEnd) {
		View view = obtainView(position);
		return setupChild(view, addToEnd, mLastObtainedViewWasRecycled);
	}

	private View obtainView(int position) {
		View convertView = getRecycledView();
		View view = mAdapter.getView(position, convertView, this);
		if(view != convertView && convertView != null)
			mRecycledViews.add(convertView);
		mLastObtainedViewWasRecycled = (view == convertView);
		LayoutParams p = view.getLayoutParams();
		if (p == null) {
			p = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			view.setLayoutParams(p);
		}
		return view;
	}

	class AdapterDataSetObserver extends DataSetObserver {

		@Override
		public void onChanged() {
			View v = getChildAt(mCurrentBufferIndex);
			if (v != null) {
				for (int index = 0; index < mAdapter.getCount(); index++) {
					if (v.equals(mAdapter.getItem(index))) {
						mCurrentAdapterIndex = index;
						break;
					}
				}
			}
			resetFocus();
		}

		@Override
		public void onInvalidated() {
			// Not yet implemented!
		}

	}

	private void logBuffer() {

		Log.d("viewflow", "Size of mLoadedViews: " + mLoadedViews.size() +
				", Size of mRecycledViews: " + mRecycledViews.size() +
				", X: " + mScroller.getCurrX() + ", Y: " + mScroller.getCurrY());
		Log.d("viewflow", "IndexInAdapter: " + mCurrentAdapterIndex
				+ ", IndexInBuffer: " + mCurrentBufferIndex);
	}
}
