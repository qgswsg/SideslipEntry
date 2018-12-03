package com.qgswsg.sideslipentry;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.NestedScrollingChild2;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

import static android.support.v4.view.ViewCompat.TYPE_TOUCH;

public class MyViewPager extends ViewPager implements NestedScrollingChild2 {

    private int mLastTouchX;
    private int mLastTouchY;
    private NestedScrollingChildHelper nestedScrollingChildHelper;
    private int mScrollPointerId;
    private float downX;
    private final int[] mScrollOffset = new int[2];
    private final int[] mScrollConsumed = new int[2];

    public MyViewPager(@NonNull Context context) {
        super(context);
        setNestedScrollingEnabled(true);
    }

    public MyViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setNestedScrollingEnabled(true);
    }

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        super.setNestedScrollingEnabled(enabled);
        this.getScrollingChildHelper().setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean startNestedScroll(int axes, int type) {
        return this.getScrollingChildHelper().startNestedScroll(axes, type);
    }

    @Override
    public void stopNestedScroll(int type) {
        getScrollingChildHelper().stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent(int type) {
        return getScrollingChildHelper().hasNestedScrollingParent(type);
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, @Nullable int[] offsetInWindow, int type) {
        return getScrollingChildHelper().dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed, @Nullable int[] offsetInWindow, int type) {
        return getScrollingChildHelper().dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type);
    }


    private NestedScrollingChildHelper getScrollingChildHelper() {
        if (nestedScrollingChildHelper == null) {
            nestedScrollingChildHelper = new NestedScrollingChildHelper(this);
        }
        return nestedScrollingChildHelper;
    }


    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final MotionEvent vtev = MotionEvent.obtain(ev);
        final int actionIndex = ev.getActionIndex();
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mLastTouchX = (int) ev.getX();
                mLastTouchY = (int) ev.getY();
                downX = ev.getX();
                mScrollPointerId = ev.getPointerId(actionIndex);
                this.startNestedScroll(1, TYPE_TOUCH);
                break;
            case MotionEvent.ACTION_UP:
                this.stopNestedScroll(TYPE_TOUCH);
                break;
            case MotionEvent.ACTION_MOVE:
                final int index = ev.findPointerIndex(mScrollPointerId);
                if (index < 0) {
                    return false;
                }
                final int x = (int) (ev.getX(index) + 0.5f);
                final int y = (int) (ev.getY(index) + 0.5f);
                if (getCurrentItem() == 0 && downX < ev.getX()) {
                    downX = Integer.MIN_VALUE;
                    int dx = mLastTouchX - x;
                    int dy = mLastTouchY - y;
                    if (dispatchNestedPreScroll(dx, dy, mScrollConsumed, mScrollOffset, TYPE_TOUCH)) {
                        vtev.offsetLocation(mScrollOffset[0], mScrollOffset[1]);
                    }
                    mLastTouchX = x - mScrollOffset[0];
                    mLastTouchY = y - mScrollOffset[1];
                    return false;
                } else {
                    return super.onTouchEvent(ev);
                }
        }
        return super.onTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return super.onInterceptTouchEvent(ev);
    }
}
