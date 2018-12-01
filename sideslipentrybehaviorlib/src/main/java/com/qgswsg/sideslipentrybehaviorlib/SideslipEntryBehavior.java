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
    boolean halfable = true;
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
    private boolean fitToContents = true;
    int fitToContentsOffset;
    //滑动完成是否还显示peek
    private boolean smallTailMovedOut = true;
    int smallTailMovedOutOffset;
    VelocityTracker velocityTracker;
    int initialX;
    private boolean ignoreEvents;
    private float maximumVelocity;
    private int lastNestedScrollDx;
    private boolean nestedScrolled;


    private final ViewDragHelper.Callback dragCallback = new ViewDragHelper.Callback() {
        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            if (state == STATE_DRAGGING) {
                return false;
            } else if (touchingScrollingChild) {
                return false;
            } else {
                if (state == 3 && activePointerId == pointerId) {
                    View scroll = nestedScrollingChildRef.get();
                    if (scroll != null && scroll.canScrollHorizontally(-1)) {
                        return false;
                    }
                }
                return viewRef != null && viewRef.get() == child;
            }
        }

        public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
            int left;
            byte targetState;
            int currentLeft = releasedChild.getLeft();
            if (xvel < 0.0F) {//向左滑动
                int leftEdge = getExpandedOffset();
                int halfX = halfable ? halfExpandedOffset : leftEdge;
                left = currentLeft > halfX ? halfX : leftEdge;
                targetState = (byte) (left == halfExpandedOffset ? STATE_HALF_EXPANDED : STATE_EXPANDED);
            } else {//向右滑动
                int rightEdge = hideable ? parentWidth : collapsedOffset;
                int halfX = halfable ? halfExpandedOffset : rightEdge;
                int halfXAndCollapsedXMin = Math.min(halfX, collapsedOffset);
                int halfXAndCollapsedXMax = Math.max(halfX, collapsedOffset);
                left = currentLeft < halfXAndCollapsedXMin ? halfXAndCollapsedXMin : currentLeft > halfXAndCollapsedXMax ? rightEdge : halfXAndCollapsedXMax;
                targetState = (byte) (left == halfExpandedOffset ? STATE_HALF_EXPANDED : STATE_HIDDEN);
            }
            if (viewDragHelper.settleCapturedViewAt(left, releasedChild.getTop())) {
                setStateInternal(STATE_SETTLING);
                ViewCompat.postOnAnimation(releasedChild, new SettleRunnable(releasedChild, targetState));
            } else {
                setStateInternal(targetState);
            }

        }

        public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
//            dispatchOnSlide(left);
        }

        public void onViewDragStateChanged(int state) {
            if (state == 1) {
//                setStateInternal(1);
            }

        }

        public int clampViewPositionVertical(@NonNull View child, int top, int dy) {
            return child.getTop();
        }

        //
        public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
            return MathUtils.clamp(left, smallTailMovedOut ? -smallTailWidth : getExpandedOffset(), parentWidth);
        }

        //
        @Override
        public int getViewHorizontalDragRange(@NonNull View child) {
            return hideable ? parentWidth : collapsedOffset;
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
            if (viewDragHelper != null && viewDragHelper.continueSettling(true)) {
                ViewCompat.postOnAnimation(view, this);
            } else {
                setStateInternal(targetState);
            }

        }
    }

    public void setSmallTailWidth(int smallTailWidth) {
        this.smallTailWidth = smallTailWidth;
    }

    @Override
    public boolean onLayoutChild(@NonNull CoordinatorLayout parent, @NonNull V child, int layoutDirection) {
        parent.onLayoutChild(child, layoutDirection);
        parentWidth = parent.getWidth();
//        smallTailWidth = parentWidth - parent.getWidth() * 9 / 16;
        fitToContentsOffset = Math.max(0, parentWidth - child.getWidth());
        halfExpandedOffset = parentWidth / 2;
        calculateCollapsedOffset();
        ViewCompat.offsetLeftAndRight(child, collapsedOffset);
        if (viewDragHelper == null) {
            viewDragHelper = ViewDragHelper.create(parent, dragCallback);
        }
        viewRef = new WeakReference(child);
        nestedScrollingChildRef = new WeakReference(findScrollingChild(child));
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        if (!child.isShown()) {
            ignoreEvents = true;
            return false;
        } else {
            int action = event.getActionMasked();
            if (action == 0) {
                reset();
            }

            if (velocityTracker == null) {
                velocityTracker = VelocityTracker.obtain();
            }

            velocityTracker.addMovement(event);
            switch (action) {
                case 0:
                    initialX = (int) event.getX();
                    int initialY = (int) event.getY();
                    View scroll = nestedScrollingChildRef != null ? nestedScrollingChildRef.get() : null;
                    if (scroll != null && parent.isPointInChildBounds(scroll, initialX, initialY)) {
                        activePointerId = event.getPointerId(event.getActionIndex());
                        touchingScrollingChild = true;
                    }

                    ignoreEvents = activePointerId == -1 && !parent.isPointInChildBounds(child, initialX, initialY);
                    break;
                case 1:
                case 3:
                    touchingScrollingChild = false;
                    activePointerId = -1;
                    if (ignoreEvents) {
                        ignoreEvents = false;
                        return false;
                    }
                case 2:
            }

            if (!ignoreEvents && viewDragHelper != null && viewDragHelper.shouldInterceptTouchEvent(event)) {
                return true;
            } else {
                View scroll = nestedScrollingChildRef != null ? (View) nestedScrollingChildRef.get() : null;
                return action == 2 && scroll != null && !ignoreEvents && state != 1 && !parent.isPointInChildBounds(scroll, (int) event.getX(), (int) event.getY()) && viewDragHelper != null && Math.abs((float) initialX - event.getX()) > (float) viewDragHelper.getTouchSlop();
            }
        }
    }

    public boolean onTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        if (!child.isShown()) {
            return false;
        } else {
            int action = event.getActionMasked();
            if (state == 1 && action == 0) {
                return true;
            } else {
                if (viewDragHelper != null) {
                    viewDragHelper.processTouchEvent(event);
                }

                if (action == 0) {
                    reset();
                }

                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain();
                }

                velocityTracker.addMovement(event);
                if (action == 2 && !ignoreEvents && Math.abs((float) initialX - event.getX()) > (float) viewDragHelper.getTouchSlop()) {
                    viewDragHelper.captureChildView(child, event.getPointerId(event.getActionIndex()));
                }
                return !ignoreEvents;
            }
        }
    }

    public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V child, @NonNull View directTargetChild, @NonNull View target, int axes, int type) {
        lastNestedScrollDx = 0;
        nestedScrolled = false;
        return (axes & ViewCompat.SCROLL_AXIS_HORIZONTAL) != 0;
    }

    public void onNestedPreScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V child, @NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
        if (type != 1) {
            View scrollingChild = nestedScrollingChildRef.get();
            if (target == scrollingChild) {
                int currentLeft = child.getLeft();
                int newLeft = currentLeft - dx;
                if (dx > 0) {//向左滑
                    if (newLeft < getExpandedOffset()) {
                        consumed[0] = currentLeft - getExpandedOffset();
                        ViewCompat.offsetLeftAndRight(child, -consumed[0]);
                        setStateInternal(STATE_EXPANDED);
                    } else {
                        consumed[0] = dx;
                        ViewCompat.offsetLeftAndRight(child, -dx);
                        setStateInternal(1);
                    }
                } else if (dx < 0) {//向右滑
                    if (newLeft > collapsedOffset && !hideable) {
                        consumed[0] = currentLeft - collapsedOffset;
                        ViewCompat.offsetLeftAndRight(child, -consumed[0]);
                        setStateInternal(4);
                    } else {
                        consumed[0] = dx;
                        ViewCompat.offsetLeftAndRight(child, -dx);
                        setStateInternal(1);
                    }
                }

//                dispatchOnSlide(child.getTop());
                lastNestedScrollDx = dx;
                nestedScrolled = true;
            }
        }
    }

    public void onStopNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V child, @NonNull View target, int type) {
        if (child.getLeft() == getExpandedOffset()) {
            setStateInternal(3);
        } else if (target == nestedScrollingChildRef.get() && nestedScrolled) {
            int left;
            byte targetState;
            if (lastNestedScrollDx > 0) {
                left = getExpandedOffset();
                targetState = 3;
            } else if (hideable/* && shouldHide(child, getYVelocity())*/) {
                left = parentWidth;
                targetState = 5;
            } else if (lastNestedScrollDx == 0) {
                int currentLeft = child.getLeft();
                if (fitToContents) {
                    if (Math.abs(currentLeft - fitToContentsOffset) < Math.abs(currentLeft - collapsedOffset)) {
                        left = fitToContentsOffset;
                        targetState = 3;
                    } else {
                        left = collapsedOffset;
                        targetState = 4;
                    }
                } else if (currentLeft < halfExpandedOffset) {
                    if (currentLeft < Math.abs(currentLeft - collapsedOffset)) {
                        left = smallTailMovedOut ? -smallTailWidth : 0;
                        targetState = 3;
                    } else {
                        left = halfExpandedOffset;
                        targetState = 6;
                    }
                } else if (Math.abs(currentLeft - halfExpandedOffset) < Math.abs(currentLeft - collapsedOffset)) {
                    left = halfExpandedOffset;
                    targetState = 6;
                } else {
                    left = collapsedOffset;
                    targetState = 4;
                }
            } else {
                left = collapsedOffset;
                targetState = 4;
            }

            if (viewDragHelper.smoothSlideViewTo(child, left, child.getTop())) {
                setStateInternal(2);
                ViewCompat.postOnAnimation(child, new SideslipEntryBehavior.SettleRunnable(child, targetState));
            } else {
                setStateInternal(targetState);
            }

            nestedScrolled = false;
        }
    }

    public boolean onNestedPreFling(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V child, @NonNull View target, float velocityX, float velocityY) {
        return target == nestedScrollingChildRef.get() && (state != 3 || super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY));
    }

    private void reset() {
        activePointerId = -1;
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }

    }

    void setStateInternal(int state) {
        if (state != state) {
            state = state;
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
                    View scrollingChild = findScrollingChild(group.getChildAt(i));
                    if (scrollingChild != null) {
                        return scrollingChild;
                    }
                }
            }

            return null;
        }
    }

    private int getExpandedOffset() {
        return smallTailMovedOut ? -smallTailWidth : fitToContents ? fitToContentsOffset : 0;
    }

    private void calculateCollapsedOffset() {
        if (smallTailMovedOut) {
            collapsedOffset = Math.max(parentWidth - smallTailWidth, -smallTailWidth);
        } else if (fitToContents) {
            collapsedOffset = Math.max(parentWidth - smallTailWidth, fitToContentsOffset);
        } else {
            collapsedOffset = parentWidth - smallTailWidth;
        }

    }

    public SideslipEntryBehavior() {
    }

    public SideslipEntryBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
//        TypedArray a = context.obtainStyledAttributes(attrs, android.support.design.R.styleable.BottomSheetBehavior_Layout);
//        TypedValue value = a.peekValue(android.support.design.R.styleable.BottomSheetBehavior_Layout_behavior_peekHeight);
//        if (value != null && value.data == -1) {
//            setPeekHeight(value.data);
//        } else {
//            setPeekHeight(a.getDimensionPixelSize(android.support.design.R.styleable.BottomSheetBehavior_Layout_behavior_peekHeight, -1));
//        }

//        setHideable(a.getBoolean(android.support.design.R.styleable.BottomSheetBehavior_Layout_behavior_hideable, false));
//        setFitToContents(a.getBoolean(android.support.design.R.styleable.BottomSheetBehavior_Layout_behavior_fitToContents, true));
//        setSkipCollapsed(a.getBoolean(android.support.design.R.styleable.BottomSheetBehavior_Layout_behavior_skipCollapsed, false));
//        a.recycle();
        ViewConfiguration configuration = ViewConfiguration.get(context);
        maximumVelocity = (float) configuration.getScaledMaximumFlingVelocity();
    }

//    void dispatchOnSlide(int left) {
//        View view = viewRef.get();
//        if (view != null && callback != null) {
//            if (left > collapsedOffset) {
//                callback.onSlide(view, (float) (collapsedOffset - left) / (float) (parentWidth - collapsedOffset));
//            } else {
//                callback.onSlide(view, (float) (collapsedOffset - left) / (float) (collapsedOffset - getExpandedOffset()));
//            }
//        }
//
//    }

    public static <V extends View> SideslipEntryBehavior<V> from(V view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (!(params instanceof android.support.design.widget.CoordinatorLayout.LayoutParams)) {
            throw new IllegalArgumentException("The view is not a child of CoordinatorLayout");
        } else {
            CoordinatorLayout.Behavior behavior = ((android.support.design.widget.CoordinatorLayout.LayoutParams) params).getBehavior();
            if (!(behavior instanceof SideslipEntryBehavior)) {
                throw new IllegalArgumentException("The view is not associated with SideslipEntryBehavior");
            } else {
                return (SideslipEntryBehavior) behavior;
            }
        }
    }
}
