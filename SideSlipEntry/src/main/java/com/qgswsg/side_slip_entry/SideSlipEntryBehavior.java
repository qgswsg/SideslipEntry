package com.qgswsg.side_slip_entry;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.math.MathUtils;
import android.support.v4.view.AbsSavedState;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;


/**
 * 侧滑进入 <br/>
 * 控件如果想实现从右侧滑入的效果，可以绑定此Behavior 并添加到CoordinatorLayout布局中，方可实现从右侧滑入的效果，并支持嵌套滑动。<br/>
 * <pre>
 *     示例：
 *     &lt;ViewGroup
 *         android:layout_width=&quot;match_parent&quot;
 *         android:layout_height=&quot;wrap_content&quot;
 *         android:layout_gravity=&quot;bottom&quot;
 *         android:layout_marginBottom=&quot;16dp&quot;
 *         app:fitToContents=&quot;false&quot;
 *         app:halfable=&quot;true&quot;
 *         app:hideable=&quot;true&quot;
 *         app:layout_behavior=&quot;com.qgswsg.side_slip_entry.SideSlipEntryBehavior&quot;
 *         app:smallTailMovedOut=&quot;true&quot;
 *         app:smallTailWidth=&quot;50dp&quot;
 *         app:smallTailWidthView=&quot;@id/smallTailView&quot;
 *         tools:ignore=&quot;MissingPrefix&quot;&gt;
 *                  ...
 *         &lt;/ViewGroup&gt;
 *         最后记得在根标签中添加 xmlns:app="http://schemas.android.com/apk/res-auto"
 * </pre>
 * <p>另外：</p>
 * ViewPager和HorizontalScrollView也算是可以水平滑动的控件，但它们并没有实现NestedScrollingChild相关接口。<br/>
 * 因此本Behavior是不支持与ViewPager和HorizontalScrollView嵌套滑动的，如果想实现它们的嵌套滑动，<br/>
 * 可继承ViewPager或HorizontalScrollView实现NestedScrollingChild相关接口，并重写它们的onTouchEvent和onInterceptTouchEvent相关方法，
 * 使之将滑动事件分享出来。<br/>
 */
public class SideSlipEntryBehavior<V extends View> extends CoordinatorLayout.Behavior<V> {

    //此控件的状态
    /**
     * 拖动状态
     */
    public static final int STATE_DRAGGING = 1;
    /**
     * 状态设置中
     */
    public static final int STATE_SETTLING = 2;
    /**
     * 完成滑出状态
     */
    public static final int STATE_EXPANDED = 3;
    /**
     * 仅显示“小尾巴”的状态
     */
    public static final int STATE_COLLAPSED = 4;
    /**
     * 隐藏状态
     */
    public static final int STATE_HIDDEN = 5;
    /**
     * 滑动到一半的状态
     */
    public static final int STATE_HALF_EXPANDED = 6;
    /**
     * 滑动到一半位置是否停住
     */
    private boolean halfable = true;
    /**
     * 可否隐藏
     */
    private boolean hideable;
    /**
     * 完全滑出时，是否将“小尾巴”滑动到不可见
     */
    private boolean smallTailMovedOut = true;
    /**
     * 控件的宽度完全滑出来后，是否还能继续向左滑动。
     * 注意，如果允许“小尾巴”滑动到不可见时,也就是{@link #smallTailMovedOut} = {@code true}，此属性将无效
     */
    private boolean fitToContents;
    private boolean ignoreEvents;
    private boolean touchingScrollingChild;
    private boolean nestedScrolled;
    /**
     * “小尾巴”的宽度
     */
    private int smallTailWidth;
    /**
     * 可滑动的可视宽度
     */
    private int parentWidth;
    /**
     * 只显示“小尾巴”状态时，相对于可滑动区域的左边界的偏移量
     */
    private int collapsedOffset;
    /**
     * 一半位置相对于可滑动区域的左边界的偏移量
     */
    private int halfExpandedOffset;
    /**
     * 控件完全滑出时，相对于可滑动区域的左边界的偏移量
     */
    private int fitToContentsOffset;
    /**
     * 当前状态
     */
    @State
    private int state = STATE_COLLAPSED;
    private int activePointerId;
    private int initialX;
    private int lastNestedScrollDx;
    /**
     * 可以在xml中指定“小尾巴”具体是哪个控件，程序将自动获取该控件的宽度
     */
    private int smallTailViewId;
    /**
     * 绑定此Behavior控件
     */
    private WeakReference<V> viewRef;
    /**
     * 可嵌套滑动的控件
     */
    private WeakReference<View> nestedScrollingChildRef;
    /**
     * 拖动相关的处理对象
     */
    private ViewDragHelper viewDragHelper;
    /**
     * 滑动过程中位置和状态的回调
     */
    private SideslipEntryCallback callback;


    private final ViewDragHelper.Callback dragCallback = new ViewDragHelper.Callback() {
        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            if (state == STATE_DRAGGING) {
                return false;//防止在拖动过程中捕获第二个以上的手指的触摸
            } else if (touchingScrollingChild) {
                return false;
            } else {
                if (state == 3 && activePointerId == pointerId) {
                    View scroll = nestedScrollingChildRef.get();
                    if (scroll != null && scroll.canScrollHorizontally(-1)) {
                        return false;
                    }
                }
                return viewRef != null && viewRef.get() == child;//只捕获绑定此Behavior的控件的触摸
            }
        }

        public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
            int left;
            byte targetState;
            int currentLeft = releasedChild.getLeft();
            if (xvel < 0.0F) {//向左滑动
                int leftEdge = getLeftEdge();//向左滑动的最左边
                int halfX = halfable ? halfExpandedOffset : leftEdge;//判断是否可以在一半的时候停住
                left = currentLeft > halfX ? halfX : leftEdge;//根据当前位置确定最终的目标位置
                targetState = (byte) (left == halfExpandedOffset ? STATE_HALF_EXPANDED : STATE_EXPANDED); //确定目标状态
            } else {//向右滑动
                int rightEdge = getRightEdge(); //向右滑动的最右边
                int halfX = halfable ? halfExpandedOffset : rightEdge;//判断可否在一半的时候停住
                int halfXAndCollapsedXMin = Math.min(halfX, collapsedOffset);//先到比较靠左的位置
                int halfXAndCollapsedXMax = Math.max(halfX, collapsedOffset);//用来判断是不是已经过了比较靠右的位置
                left = currentLeft < halfXAndCollapsedXMin ? halfXAndCollapsedXMin : currentLeft > halfXAndCollapsedXMax ? rightEdge : halfXAndCollapsedXMax;//根据当前位置确定最终的目标位置
                targetState = (byte) (left == halfExpandedOffset ? STATE_HALF_EXPANDED : STATE_HIDDEN);//确定目标状态
            }
            //自动补偿移动到刚才确定目标位置，并设置好目标状态
            if (viewDragHelper.settleCapturedViewAt(left, releasedChild.getTop())) {
                setStateInternal(STATE_SETTLING);
                ViewCompat.postOnAnimation(releasedChild, new SettleRunnable(releasedChild, targetState));
            } else {
                setStateInternal(targetState);
            }

        }

        public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
            dispatchOnSlide(left);
        }

        public void onViewDragStateChanged(int state) {
            if (state == ViewDragHelper.STATE_DRAGGING) {
                setStateInternal(STATE_DRAGGING);
            }
        }

        public int clampViewPositionVertical(@NonNull View child, int top, int dy) {
            return child.getTop();//不允许垂直方向滑动
        }


        public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
            //水平方向滑动的范围，根据相关设置确定左边缘和右边缘，如可以将“小尾巴”滑出屏幕时，左边缘为负的“小尾巴”宽度
            return MathUtils.clamp(left, smallTailMovedOut ? -smallTailWidth : getLeftEdge(), parentWidth);
        }

        @Override
        public int getViewHorizontalDragRange(@NonNull View child) {
            return getRightEdge();
        }
    };

    public SideSlipEntryBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
        //读取相关xml属性，并设置
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SideSlipEntryBehavior);
        setSmallTailViewId(a.getResourceId(R.styleable.SideSlipEntryBehavior_smallTailWidthView, -1));
        setSmallTailWidth(a.getDimensionPixelSize(R.styleable.SideSlipEntryBehavior_smallTailWidth, -1));
        setHideable(a.getBoolean(R.styleable.SideSlipEntryBehavior_hideable, false));
        setFitToContents(a.getBoolean(R.styleable.SideSlipEntryBehavior_fitToContents, false));
        setSmallTailMovedOut(a.getBoolean(R.styleable.SideSlipEntryBehavior_smallTailMovedOut, true));
        setHalfable(a.getBoolean(R.styleable.SideSlipEntryBehavior_halfable, false));
        a.recycle();
    }

    @Override
    public boolean onLayoutChild(@NonNull CoordinatorLayout parent, @NonNull V child, int layoutDirection) {
        if (ViewCompat.getFitsSystemWindows(parent) && !ViewCompat.getFitsSystemWindows(child)) {
            ViewCompat.setFitsSystemWindows(child, true);
        }
        int savedLeft = child.getTop();
        parent.onLayoutChild(child, layoutDirection);
        //可滑动的区域
        parentWidth = parent.getWidth();
        if (smallTailViewId != -1) {//如果用户指定了“小尾巴”控件，就找到这个控件并获取他的宽度
            View smallTailView = findSmallTailViewById(parent, smallTailViewId);
            if (smallTailView != null) smallTailWidth = smallTailView.getWidth();
        }
        //确定控件完全滑出时，相对于可滑动区域的左边界的偏移量
        fitToContentsOffset = Math.max(smallTailMovedOut ? -smallTailWidth : 0, parentWidth - child.getWidth());
        //确定一半位置相对于可滑动区域的左边界的偏移量
        halfExpandedOffset = parentWidth / 2;
        calculateCollapsedOffset();
        //根据状态，初始化位置
        switch (state) {
            case STATE_COLLAPSED:
                ViewCompat.offsetLeftAndRight(child, collapsedOffset);
                break;
            case STATE_EXPANDED:
                ViewCompat.offsetLeftAndRight(child, getLeftEdge());
                break;
            case STATE_HALF_EXPANDED:
                ViewCompat.offsetLeftAndRight(child, halfExpandedOffset);
                break;
            case STATE_HIDDEN:
                ViewCompat.offsetLeftAndRight(child, parentWidth);
                break;
            case STATE_DRAGGING:
            case STATE_SETTLING:
                ViewCompat.offsetLeftAndRight(child, savedLeft - child.getLeft());
                break;
        }
        if (viewDragHelper == null) {
            viewDragHelper = ViewDragHelper.create(parent, dragCallback);
        }
        viewRef = new WeakReference<>(child);
        nestedScrollingChildRef = new WeakReference<>(findScrollingChild(child));
        return true;
    }

    /**
     * 根据ID找到“小尾巴”控件
     *
     * @param parent          容器类控件
     * @param smallTailViewId “小尾巴”的ID
     * @return 返回对应ID的“小尾巴”控件，没找到返回null
     */
    private View findSmallTailViewById(ViewGroup parent, int smallTailViewId) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View view = parent.getChildAt(i);
            if (view.getId() == smallTailViewId) {
                return view;
            } else {
                if (view instanceof ViewGroup) {
                    return findSmallTailViewById((ViewGroup) view, smallTailViewId);
                }
            }
        }
        return null;
    }

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        if (!child.isShown()) {
            ignoreEvents = true;
            return false;
        } else {
            int action = event.getActionMasked();
            if (action == 0) {
                activePointerId = -1;
            }

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    initialX = (int) event.getX();
                    int initialY = (int) event.getY();
                    View scroll = nestedScrollingChildRef != null ? nestedScrollingChildRef.get() : null;
                    if (scroll != null && parent.isPointInChildBounds(scroll, initialX, initialY)) {
                        activePointerId = event.getPointerId(event.getActionIndex());
                        touchingScrollingChild = true;
                    }

                    ignoreEvents = activePointerId == -1 && !parent.isPointInChildBounds(child, initialX, initialY);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    touchingScrollingChild = false;
                    activePointerId = -1;
                    if (ignoreEvents) {
                        ignoreEvents = false;
                        return false;
                    }
                case MotionEvent.ACTION_MOVE:
            }

            if (!ignoreEvents && viewDragHelper != null && viewDragHelper.shouldInterceptTouchEvent(event)) {
                return true;
            } else {
                View scroll = nestedScrollingChildRef != null ? nestedScrollingChildRef.get() : null;
                return action == STATE_SETTLING && scroll != null && !ignoreEvents && state != 1 && !parent.isPointInChildBounds(scroll, (int) event.getX(), (int) event.getY()) && viewDragHelper != null && Math.abs((float) initialX - event.getX()) > (float) viewDragHelper.getTouchSlop();
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

                if (action == MotionEvent.ACTION_DOWN) {
                    activePointerId = -1;
                }

                if (action == MotionEvent.ACTION_MOVE && !ignoreEvents && Math.abs((float) initialX - event.getX()) > (float) viewDragHelper.getTouchSlop()) {
                    viewDragHelper.captureChildView(child, event.getPointerId(event.getActionIndex()));
                }
                return !ignoreEvents;
            }
        }
    }

    public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V child, @NonNull View directTargetChild, @NonNull View target, int axes, int type) {
        lastNestedScrollDx = 0;
        nestedScrolled = false;
        return (axes & ViewCompat.SCROLL_AXIS_HORIZONTAL) != 0;//只关心水平方向的滑动
    }

    public void onNestedPreScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V child, @NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
        if (type != 1) {
            View scrollingChild = nestedScrollingChildRef.get();
            if (target == scrollingChild) {
                int currentLeft = child.getLeft();
                int newLeft = currentLeft - dx;
                if (dx > 0) {//向左滑
                    if (newLeft < getLeftEdge()) {//如果滑动后已经超出最左边边界，就只滑动到最左边边界处
                        consumed[0] = currentLeft - getLeftEdge(); //告诉CoordinatorLayout在水平方向上消费了多少距离
                        ViewCompat.offsetLeftAndRight(child, -consumed[0]);//移动
                        setStateInternal(STATE_EXPANDED);
                    } else {
                        consumed[0] = dx;//如果滑动后没有越界，就如实告诉CoordinatorLayout滑动了多少距离
                        ViewCompat.offsetLeftAndRight(child, -dx);//移动
                        setStateInternal(STATE_DRAGGING);
                    }
                } else if (dx < 0 && !target.canScrollHorizontally(-1)) {//向右滑
                    int rightEdge = getRightEdge();
                    if (newLeft > rightEdge) {//此处和向左滑动一样，只是判断的边界变为了右边界
                        consumed[0] = currentLeft - rightEdge;
                        ViewCompat.offsetLeftAndRight(child, -consumed[0]);
                        setStateInternal(hideable ? STATE_HIDDEN : STATE_COLLAPSED);
                    } else {
                        consumed[0] = dx;
                        ViewCompat.offsetLeftAndRight(child, -dx);
                        setStateInternal(STATE_DRAGGING);
                    }
                }

                dispatchOnSlide(child.getLeft());
                lastNestedScrollDx = dx;
                nestedScrolled = true;
            }
        }
    }

    public void onStopNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V child, @NonNull View target, int type) {
        if (child.getLeft() == getLeftEdge()) {
            setStateInternal(STATE_EXPANDED);
        } else if (target == nestedScrollingChildRef.get() && nestedScrolled) {
            int left;
            byte targetState;
            int currentLeft = child.getLeft();
            //滑动结束后根据最后的滑动方向，确定并自动移动到目标位置
            if (lastNestedScrollDx > 0) {//向左
                int leftEdge = getLeftEdge();
                int halfX = halfable ? halfExpandedOffset : leftEdge;
                left = currentLeft > halfX ? halfX : leftEdge;
                targetState = (byte) (left == halfExpandedOffset ? STATE_HALF_EXPANDED : STATE_EXPANDED);
            } else if (lastNestedScrollDx == 0) {//如果方向无法确定，向左边最近的状态吸附
                int leftEdge = getLeftEdge();
                int halfX = halfable ? halfExpandedOffset : leftEdge;
                int rightEdge = getRightEdge();
                int halfXAndCollapsedXMin = Math.min(halfX, collapsedOffset);
                int halfXAndCollapsedXMax = Math.max(halfX, collapsedOffset);
                left = currentLeft < halfXAndCollapsedXMin ? leftEdge : currentLeft > halfXAndCollapsedXMax ? rightEdge : halfXAndCollapsedXMax;
                targetState = (byte) (left == halfExpandedOffset ? STATE_HALF_EXPANDED : left == collapsedOffset ? STATE_COLLAPSED : STATE_HIDDEN);
            } else {//向右
                int rightEdge = getRightEdge();
                int halfX = halfable ? halfExpandedOffset : rightEdge;
                int halfXAndCollapsedXMin = Math.min(halfX, collapsedOffset);
                int halfXAndCollapsedXMax = Math.max(halfX, collapsedOffset);
                left = currentLeft < halfXAndCollapsedXMin ? halfXAndCollapsedXMin : currentLeft > halfXAndCollapsedXMax ? rightEdge : halfXAndCollapsedXMax;
                targetState = (byte) (left == halfExpandedOffset ? STATE_HALF_EXPANDED :left == collapsedOffset ? STATE_COLLAPSED : STATE_HIDDEN);
            }
            if (viewDragHelper.smoothSlideViewTo(child, left, child.getTop())) {
                setStateInternal(STATE_SETTLING);
                ViewCompat.postOnAnimation(child, new SideSlipEntryBehavior.SettleRunnable(child, targetState));
            } else {
                setStateInternal(targetState);
            }

            nestedScrolled = false;
        }
    }

    public boolean onNestedPreFling(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V child, @NonNull View target, float velocityX, float velocityY) {
        return target == nestedScrollingChildRef.get() && (state != STATE_EXPANDED || super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY));
    }

    /**
     * 寻找可滑动的控件
     *
     * @param view 绑定Behavior的控件
     * @return 返回找到的可滑动的控件
     */
    private View findScrollingChild(View view) {
        if (ViewCompat.isNestedScrollingEnabled(view)) {
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

    /**
     * 根据相关设置确定左边界
     *
     * @return 左边界
     */
    private int getLeftEdge() {
        return smallTailMovedOut ? -smallTailWidth : fitToContents ? fitToContentsOffset : 0;
    }

    /**
     * 根据是否可隐藏确定右边界
     *
     * @return 右边界
     */
    private int getRightEdge() {
        return hideable ? parentWidth : collapsedOffset;
    }

    /**
     * 根据相关设置计算出只显示“小尾巴”状态时，相对于可滑动区域的左边界的偏移量
     */
    private void calculateCollapsedOffset() {
        if (smallTailMovedOut) {
            collapsedOffset = Math.max(parentWidth - smallTailWidth, -smallTailWidth);
        } else if (fitToContents) {
            collapsedOffset = Math.max(parentWidth - smallTailWidth, fitToContentsOffset);
        } else {
            collapsedOffset = parentWidth - smallTailWidth;
        }

    }

    /**
     * 获取当前状态
     *
     * @return 状态
     */
    @State
    public int getState() {
        return state;
    }

    /**
     * 指定状态，控件将自动滑动到具体状态对应的位置
     *
     * @param state 状态
     */
    public void setState(@State final int state) {
        if (this.state == state) {
            return;
        }
        if (viewRef == null) {
            if (state == STATE_COLLAPSED || state == STATE_EXPANDED || state == STATE_HIDDEN || state == STATE_HALF_EXPANDED) {
                this.state = state;
            }
            return;
        }
        final V child = viewRef.get();
        if (child == null) {
            return;
        }
        ViewParent parent = child.getParent();
        if (parent != null && parent.isLayoutRequested() && ViewCompat.isAttachedToWindow(child)) {
            child.post(new Runnable() {
                @Override
                public void run() {
                    startSettlingAnimation(child, state);
                }
            });
        } else {
            startSettlingAnimation(child, state);
        }
    }

    /**
     * 开始滑动到具体状态对应的位置
     *
     * @param child 绑定此Behavior的控件
     * @param state 状态
     */
    private void startSettlingAnimation(View child, @State int state) {
        int left;
        switch (state) {
            case STATE_COLLAPSED:
                left = collapsedOffset;
                break;
            case STATE_HALF_EXPANDED:
                left = halfExpandedOffset;
                break;
            case STATE_EXPANDED:
                left = getLeftEdge();
                break;
            case STATE_HIDDEN:
                left = parentWidth;
                break;
            case STATE_DRAGGING:
            case STATE_SETTLING:
            default:
                throw new IllegalArgumentException("Illegal state argument: " + state);
        }
        if (viewDragHelper.smoothSlideViewTo(child, left, child.getTop())) {
            setStateInternal(STATE_SETTLING);
            ViewCompat.postOnAnimation(child, new SettleRunnable(child, state));
        } else {
            setStateInternal(state);
        }
    }

    /**
     * 是否在滑动到一半时停住
     *
     * @return 为{@code true} 时在滑动到一半的位置停住，否则直接滑动到下一个状态
     */
    public boolean isHalfable() {
        return halfable;
    }

    /**
     * 设置在滑动到一半时是否停住
     *
     * @param halfable 为{@code true} 时在滑动到一半的位置停住，否则直接滑动到下一个状态
     */
    public void setHalfable(boolean halfable) {
        this.halfable = halfable;
    }

    /**
     * 是否可以滑动隐藏<br/>
     * 注意：此状态只限制滑动时可不可以隐藏，如果用户指定状态为隐藏，将会忽略些状态
     *
     * @return 为{@code true} 时可通过滑动操作隐藏，否则只能通过指定隐藏状态隐藏
     */
    public boolean isHideable() {
        return hideable;
    }

    /**
     * 设置可否被滑动隐藏<br/>
     * 注意：此状态只限制滑动时可不可以隐藏，如果用户指定状态为隐藏，将会忽略些状态
     *
     * @param hideable 为{@code true} 时可通过滑动操作隐藏，否则只能通过指定隐藏状态隐藏
     */
    public void setHideable(boolean hideable) {
        this.hideable = hideable;
    }

    /**
     * 得到露出来的“小尾巴”的宽度
     *
     * @return 露出来的“小尾巴”的宽度
     */
    public int getSmallTailWidth() {
        return smallTailWidth;
    }

    /**
     * 得到可滑动区域的宽度
     *
     * @return 可滑动区域的宽度
     */
    public int getParentWidth() {
        return parentWidth;
    }

    /**
     * 是否完全显示了，就不可再滑了<br/>
     * 注意：如果设置了{@link #smallTailMovedOut}为{@code true} 此属性将失效
     *
     * @return 为{@code true} 时一旦控件完全显示了，就不能再往左滑了，否则一直能滑动到左边界
     */
    public boolean isFitToContents() {
        return fitToContents;
    }

    /**
     * 是否完全显示了，就不可再滑了<br/>
     * 注意：如果设置了{@link #smallTailMovedOut}为{@code true} 此属性将失效
     *
     * @param fitToContents 为{@code true} 时一旦控件完全显示了，就不能再往左滑了，否则一直能滑动到左边界
     */
    public void setFitToContents(boolean fitToContents) {
        this.fitToContents = fitToContents;
    }

    /**
     * 是否在状态{@link #STATE_EXPANDED}时，将“小尾巴”滑动到不可见
     *
     * @return 为{@code true} 时状态{@link #STATE_EXPANDED}会将“小尾巴”滑动到不可见，否则“小尾巴”始终可见
     */
    public boolean isSmallTailMovedOut() {
        return smallTailMovedOut;
    }

    /**
     * 设置是否在状态{@link #STATE_EXPANDED}时，将“小尾巴”滑动到不可见
     *
     * @param smallTailMovedOut 为{@code true} 时状态{@link #STATE_EXPANDED}会将“小尾巴”滑动到不可见，否则“小尾巴”始终可见
     */
    public void setSmallTailMovedOut(boolean smallTailMovedOut) {
        this.smallTailMovedOut = smallTailMovedOut;
    }

    /**
     * 可以指定一半的宽度是多少，尽管这样做“一半”就有可能不是“一半”了
     *
     * @param halfWidth 指定的宽度
     */
    public void setHalfWidth(int halfWidth) {
        this.halfExpandedOffset = parentWidth - halfWidth;
    }

    /**
     * 得到一半时的宽度
     *
     * @return 一半时的宽度
     */
    public int getHalfWidth() {
        return parentWidth - halfExpandedOffset;
    }

    /**
     * 指定露出多少“小尾巴”
     *
     * @param smallTailWidth “小尾巴”的宽度
     */
    public void setSmallTailWidth(int smallTailWidth) {
        if (this.smallTailWidth != smallTailWidth) {
            this.smallTailWidth = smallTailWidth;
            if (viewRef != null) {
                V view = viewRef.get();
                if (view != null) {
                    view.requestLayout();
                }
            }
        }
    }

    /**
     * 指定“小尾巴”控件的ID
     *
     * @param resourceId 控件ID
     */
    private void setSmallTailViewId(int resourceId) {
        smallTailViewId = resourceId;
    }

    /**
     * 分发滑动位置
     *
     * @param left 当前位置
     */
    private void dispatchOnSlide(int left) {
        View view = viewRef.get();
        if (view != null && callback != null) {
            if (left > collapsedOffset) {
                callback.onSlide(view, (float) (collapsedOffset - left) / (float) (parentWidth - collapsedOffset));
            } else {
                callback.onSlide(view, (float) (collapsedOffset - left) / (float) (collapsedOffset - getLeftEdge()));
            }
        }

    }

    /**
     * 监听滑动时的状态变化和位置变化
     *
     * @param callback 状态信息和位置信息变化时的回调
     */
    public void setSideslipEntryCallback(SideslipEntryCallback callback) {
        this.callback = callback;
    }

    public interface SideslipEntryCallback {
        void onSlide(View view, float offset);

        void onStateChanged(View view, @State int state);
    }

    /**
     * 分发状态变化
     *
     * @param state 当前状态
     */
    private void setStateInternal(@State int state) {
        if (this.state == state) {
            return;
        }
        this.state = state;
        View view = this.viewRef.get();
        if (view != null && callback != null) {
            callback.onStateChanged(view, state);
        }
    }

    /**
     * 松手时，还没有达到目标位置时，自动吸附到目标位置
     */
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

    protected static class SavedState extends AbsSavedState {
        @State
        final int state;

        public SavedState(Parcel source) {
            this(source, null);
        }

        public SavedState(Parcel source, ClassLoader loader) {
            super(source, loader);
            //noinspection ResourceType
            state = source.readInt();
        }

        public SavedState(Parcelable superState, @State int state) {
            super(superState);
            this.state = state;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(state);
        }

        public static final Creator<SavedState> CREATOR = new ClassLoaderCreator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                return new SavedState(in, loader);
            }

            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in, null);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    public static <V extends View> SideSlipEntryBehavior<V> from(V view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (!(params instanceof android.support.design.widget.CoordinatorLayout.LayoutParams)) {
            throw new IllegalArgumentException("The view is not a child of CoordinatorLayout");
        } else {
            CoordinatorLayout.Behavior behavior = ((android.support.design.widget.CoordinatorLayout.LayoutParams) params).getBehavior();
            if (!(behavior instanceof SideSlipEntryBehavior)) {
                throw new IllegalArgumentException("The view is not associated with SideSlipEntryBehavior");
            } else {
                return (SideSlipEntryBehavior<V>) behavior;
            }
        }
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @IntDef({STATE_EXPANDED, STATE_COLLAPSED, STATE_DRAGGING, STATE_SETTLING, STATE_HIDDEN, STATE_HALF_EXPANDED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface State {
    }
}
