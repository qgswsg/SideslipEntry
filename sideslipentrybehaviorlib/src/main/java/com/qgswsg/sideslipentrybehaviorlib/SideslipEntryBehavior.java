package com.qgswsg.sideslipentrybehaviorlib;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.math.MathUtils;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;

/**
 * Create by qgswsg on 2018/11/30
 * Version 1.0
 * Description: 侧滑进入
 */
public class SideslipEntryBehavior<V extends View> extends CoordinatorLayout.Behavior<V> {

    public static final int STATE_DRAGGING = 1;
    public static final int STATE_SETTLING = 2;
    public static final int STATE_EXPANDED = 3;
    public static final int STATE_COLLAPSED = 4;
    public static final int STATE_HIDDEN = 5;
    public static final int STATE_HALF_EXPANDED = 6;
    boolean hideable;
    private int smallTailWidth;
    int parentWidth;
    int collapsedOffset;
    int halfExpandedOffset;
    int state = STATE_COLLAPSED;
    WeakReference<V> viewRef;
    WeakReference<View> nestedScrollingChildRef;
    boolean touchingScrollingChild;
    int activePointerId;
    ViewDragHelper viewDragHelper;
    //滑动到内容宽度就不能再滑了
    private boolean fitToContents = false;
    int fitToContentsOffset;
    //滑动完成是否还显示peek
    private boolean smallTailMovedOut = false;
    int smallTailMovedOutOffset;
    VelocityTracker velocityTracker;
    int initialX;
    private boolean ignoreEvents;
    private float maximumVelocity;


    private final ViewDragHelper.Callback dragCallback = new ViewDragHelper.Callback() {
        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            if (SideslipEntryBehavior.this.state == STATE_DRAGGING) {
                return false;
            } else if (SideslipEntryBehavior.this.touchingScrollingChild) {
                return false;
            } else {
                if (SideslipEntryBehavior.this.state == 3 && SideslipEntryBehavior.this.activePointerId == pointerId) {
                    View scroll = SideslipEntryBehavior.this.nestedScrollingChildRef.get();
                    if (scroll != null && scroll.canScrollHorizontally(-1)) {
                        return false;
                    }
                }
                return SideslipEntryBehavior.this.viewRef != null && SideslipEntryBehavior.this.viewRef.get() == child;
            }
        }

        public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
            int left;
            byte targetState;
            int currentLeft;
            if (xvel < 0.0F) {//向左滑动
                if (smallTailMovedOut) {
                    currentLeft = releasedChild.getLeft();
                    if (currentLeft > SideslipEntryBehavior.this.halfExpandedOffset) {
                        left = SideslipEntryBehavior.this.halfExpandedOffset;
                        targetState = STATE_HALF_EXPANDED;
                    } else {
                        left = -smallTailWidth;
                        targetState = STATE_EXPANDED;
                    }
                } else {
                    if (SideslipEntryBehavior.this.fitToContents) {
                        left = SideslipEntryBehavior.this.fitToContentsOffset;
                        targetState = STATE_EXPANDED;
                    } else {
                        currentLeft = releasedChild.getLeft();
                        if (currentLeft > SideslipEntryBehavior.this.halfExpandedOffset) {
                            left = SideslipEntryBehavior.this.halfExpandedOffset;
                            targetState = STATE_HALF_EXPANDED;
                        } else {
                            left = 0;
                            targetState = STATE_EXPANDED;
                        }
                    }
                }
            } else if (!SideslipEntryBehavior.this.hideable || /*!SideslipEntryBehavior.this.shouldHide(releasedChild, yvel) ||*/ releasedChild.getLeft() <= SideslipEntryBehavior.this.collapsedOffset && Math.abs(xvel) >= Math.abs(yvel)) {
                if (xvel != 0.0F && Math.abs(yvel) <= Math.abs(xvel)) {
                    left = SideslipEntryBehavior.this.collapsedOffset;
                    targetState = 4;
                } else {
                    currentLeft = releasedChild.getLeft();
                    if (smallTailMovedOut){
                        if (currentLeft < SideslipEntryBehavior.this.halfExpandedOffset) {
                            if (currentLeft < Math.abs(currentLeft - SideslipEntryBehavior.this.collapsedOffset)) {
                                left = smallTailMovedOut ? -smallTailWidth : 0;
                                targetState = 3;
                            } else {
                                left = SideslipEntryBehavior.this.halfExpandedOffset;
                                targetState = 6;
                            }
                        } else if (Math.abs(currentLeft - SideslipEntryBehavior.this.halfExpandedOffset) < Math.abs(currentLeft - SideslipEntryBehavior.this.collapsedOffset)) {
                            left = SideslipEntryBehavior.this.halfExpandedOffset;
                            targetState = 6;
                        } else {
                            left = SideslipEntryBehavior.this.collapsedOffset;
                            targetState = 4;
                        }
                    }else {
                        if (SideslipEntryBehavior.this.fitToContents) {
                            if (Math.abs(currentLeft - SideslipEntryBehavior.this.fitToContentsOffset) < Math.abs(currentLeft - SideslipEntryBehavior.this.collapsedOffset)) {
                                left = SideslipEntryBehavior.this.fitToContentsOffset;
                                targetState = 3;
                            } else {
                                left = SideslipEntryBehavior.this.collapsedOffset;
                                targetState = 4;
                            }
                        } else if (currentLeft < SideslipEntryBehavior.this.halfExpandedOffset) {
                            if (currentLeft < Math.abs(currentLeft - SideslipEntryBehavior.this.collapsedOffset)) {
                                left = smallTailMovedOut ? -smallTailWidth : 0;
                                targetState = 3;
                            } else {
                                left = SideslipEntryBehavior.this.halfExpandedOffset;
                                targetState = 6;
                            }
                        } else if (Math.abs(currentLeft - SideslipEntryBehavior.this.halfExpandedOffset) < Math.abs(currentLeft - SideslipEntryBehavior.this.collapsedOffset)) {
                            left = SideslipEntryBehavior.this.halfExpandedOffset;
                            targetState = 6;
                        } else {
                            left = SideslipEntryBehavior.this.collapsedOffset;
                            targetState = 4;
                        }
                    }

                }
            } else {
                left = SideslipEntryBehavior.this.parentWidth;
                targetState = 5;
            }

            if (SideslipEntryBehavior.this.viewDragHelper.settleCapturedViewAt(left, releasedChild.getTop())) {
                SideslipEntryBehavior.this.setStateInternal(2);
                ViewCompat.postOnAnimation(releasedChild, SideslipEntryBehavior.this.new SettleRunnable(releasedChild, targetState));
            } else {
                SideslipEntryBehavior.this.setStateInternal(targetState);
            }

        }

        public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
//            SideslipEntryBehavior.this.dispatchOnSlide(left);
        }

        public void onViewDragStateChanged(int state) {
            if (state == 1) {
//                SideslipEntryBehavior.this.setStateInternal(1);
            }

        }

        public int clampViewPositionVertical(@NonNull View child, int top, int dy) {
            return child.getTop();
        }

        //
        public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
            return MathUtils.clamp(left, smallTailMovedOut ? -smallTailWidth : SideslipEntryBehavior.this.getExpandedOffset(), SideslipEntryBehavior.this.parentWidth);
        }

        //
        @Override
        public int getViewHorizontalDragRange(@NonNull View child) {
            return SideslipEntryBehavior.this.hideable ? SideslipEntryBehavior.this.parentWidth : SideslipEntryBehavior.this.collapsedOffset;
        }
    };

    private class SettleRunnable implements Runnable {
        private final View view;
        private final int targetState;

        SettleRunnable(View view, int targetState) {
            this.view = view;
            this.targetState = targetState;
        }

        public void run() {
            if (SideslipEntryBehavior.this.viewDragHelper != null && SideslipEntryBehavior.this.viewDragHelper.continueSettling(true)) {
                ViewCompat.postOnAnimation(this.view, this);
            } else {
                SideslipEntryBehavior.this.setStateInternal(this.targetState);
            }

        }
    }

    @Override
    public boolean onLayoutChild(@NonNull CoordinatorLayout parent, @NonNull V child, int layoutDirection) {
        parent.onLayoutChild(child, layoutDirection);
        parentWidth = parent.getWidth();
        smallTailWidth = this.parentWidth - parent.getWidth() * 9 / 16;
        this.fitToContentsOffset = Math.max(0, this.parentWidth - child.getWidth());
        halfExpandedOffset = parentWidth / 2;
        calculateCollapsedOffset();
        Log.i("qgswsg", "parentWidth: " + parentWidth);
        Log.i("qgswsg", "smallTailWidth: " + smallTailWidth);
        Log.i("qgswsg", "collapsedOffset: " + collapsedOffset);
        ViewCompat.offsetLeftAndRight(child, this.collapsedOffset);
        if (viewDragHelper == null) {
            viewDragHelper = ViewDragHelper.create(parent, dragCallback);
        }
        this.viewRef = new WeakReference(child);
        this.nestedScrollingChildRef = new WeakReference(this.findScrollingChild(child));
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        if (!child.isShown()) {
            this.ignoreEvents = true;
            return false;
        } else {
            int action = event.getActionMasked();
            if (action == 0) {
                this.reset();
            }

            if (this.velocityTracker == null) {
                this.velocityTracker = VelocityTracker.obtain();
            }

            this.velocityTracker.addMovement(event);
            switch (action) {
                case 0:
                    this.initialX = (int) event.getX();
                    int initialY = (int) event.getY();
                    View scroll = this.nestedScrollingChildRef != null ? (View) this.nestedScrollingChildRef.get() : null;
                    if (scroll != null && parent.isPointInChildBounds(scroll, this.initialX, initialY)) {
                        this.activePointerId = event.getPointerId(event.getActionIndex());
                        this.touchingScrollingChild = true;
                    }

                    this.ignoreEvents = this.activePointerId == -1 && !parent.isPointInChildBounds(child, this.initialX, initialY);
                    break;
                case 1:
                case 3:
                    this.touchingScrollingChild = false;
                    this.activePointerId = -1;
                    if (this.ignoreEvents) {
                        this.ignoreEvents = false;
                        return false;
                    }
                case 2:
            }

            if (!this.ignoreEvents && this.viewDragHelper != null && this.viewDragHelper.shouldInterceptTouchEvent(event)) {
                return true;
            } else {
                View scroll = this.nestedScrollingChildRef != null ? (View) this.nestedScrollingChildRef.get() : null;
                return action == 2 && scroll != null && !this.ignoreEvents && this.state != 1 && !parent.isPointInChildBounds(scroll, (int) event.getX(), (int) event.getY()) && this.viewDragHelper != null && Math.abs((float) this.initialX - event.getX()) > (float) this.viewDragHelper.getTouchSlop();
            }
        }
    }

    public boolean onTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        if (!child.isShown()) {
            return false;
        } else {
            int action = event.getActionMasked();
            if (this.state == 1 && action == 0) {
                return true;
            } else {
            if (this.viewDragHelper != null) {
                this.viewDragHelper.processTouchEvent(event);
            }

                if (action == 0) {
                    this.reset();
                }

                if (this.velocityTracker == null) {
                    this.velocityTracker = VelocityTracker.obtain();
                }

                this.velocityTracker.addMovement(event);
                if (action == 2 && !this.ignoreEvents && Math.abs((float) this.initialX - event.getX()) > (float) this.viewDragHelper.getTouchSlop()) {
                    this.viewDragHelper.captureChildView(child, event.getPointerId(event.getActionIndex()));
                }
            return !this.ignoreEvents;
            }
        }
    }

    private void reset() {
        this.activePointerId = -1;
        if (this.velocityTracker != null) {
            this.velocityTracker.recycle();
            this.velocityTracker = null;
        }

    }

    void setStateInternal(int state) {
        if (this.state != state) {
            this.state = state;
        }
    }

    @VisibleForTesting
    View findScrollingChild(View view) {
        if (ViewCompat.isNestedScrollingEnabled(view)) {
            return view;
        } else if (view instanceof ViewPager) {
            return view;
        } else {
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                int i = 0;

                for (int count = group.getChildCount(); i < count; ++i) {
                    View scrollingChild = this.findScrollingChild(group.getChildAt(i));
                    if (scrollingChild != null) {
                        return scrollingChild;
                    }
                }
            }

            return null;
        }
    }

    private int getExpandedOffset() {
        return this.fitToContents ? this.fitToContentsOffset : 0;
    }

    private void calculateCollapsedOffset() {
        if (this.smallTailMovedOut) {
            this.collapsedOffset = Math.max(this.parentWidth - this.smallTailWidth, -this.smallTailWidth);
        } else if (this.fitToContents) {
            this.collapsedOffset = Math.max(this.parentWidth - this.smallTailWidth, this.fitToContentsOffset);
        } else {
            this.collapsedOffset = this.parentWidth - this.smallTailWidth;
        }

    }

    public SideslipEntryBehavior() {
    }

    public SideslipEntryBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
//        TypedArray a = context.obtainStyledAttributes(attrs, android.support.design.R.styleable.BottomSheetBehavior_Layout);
//        TypedValue value = a.peekValue(android.support.design.R.styleable.BottomSheetBehavior_Layout_behavior_peekHeight);
//        if (value != null && value.data == -1) {
//            this.setPeekHeight(value.data);
//        } else {
//            this.setPeekHeight(a.getDimensionPixelSize(android.support.design.R.styleable.BottomSheetBehavior_Layout_behavior_peekHeight, -1));
//        }

//        this.setHideable(a.getBoolean(android.support.design.R.styleable.BottomSheetBehavior_Layout_behavior_hideable, false));
//        this.setFitToContents(a.getBoolean(android.support.design.R.styleable.BottomSheetBehavior_Layout_behavior_fitToContents, true));
//        this.setSkipCollapsed(a.getBoolean(android.support.design.R.styleable.BottomSheetBehavior_Layout_behavior_skipCollapsed, false));
//        a.recycle();
        ViewConfiguration configuration = ViewConfiguration.get(context);
        this.maximumVelocity = (float) configuration.getScaledMaximumFlingVelocity();
    }

//    void dispatchOnSlide(int left) {
//        View view = this.viewRef.get();
//        if (view != null && this.callback != null) {
//            if (left > this.collapsedOffset) {
//                this.callback.onSlide(view, (float) (this.collapsedOffset - left) / (float) (this.parentWidth - this.collapsedOffset));
//            } else {
//                this.callback.onSlide(view, (float) (this.collapsedOffset - left) / (float) (this.collapsedOffset - this.getExpandedOffset()));
//            }
//        }
//
//    }
}
