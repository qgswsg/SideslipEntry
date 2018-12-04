# SideSlipEntry

侧滑进入 控件如果想实现从右侧滑入的效果，可以绑定此Behavior 并添加到CoordinatorLayout布局中，方可实现从右侧滑入的效果，并支持嵌套滑动。 
       示例：
       <ViewGroup
           android:layout_width="match_parent"
           android:layout_height="wrap_content"
           android:layout_gravity="bottom"
           android:layout_marginBottom="16dp"
           app:fitToContents="false"
           app:halfable="true"
           app:hideable="true"
           app:layout_behavior="com.qgswsg.side_slip_entry.SideSlipEntryBehavior"
           app:smallTailMovedOut="true"
           app:smallTailWidth="50dp"
           app:smallTailView="@id/smallTailView"
           tools:ignore="MissingPrefix">
                    ...
           </ViewGroup>
           最后记得在根标签中添加 xmlns:app="http://schemas.android.com/apk/res-auto"
   
另外：
ViewPager和HorizontalScrollView也算是可以水平滑动的控件，但它们并没有实现NestedScrollingChild相关接口。 因此本Behavior是不支持与ViewPager和HorizontalScrollView嵌套滑动的，如果想实现它们的嵌套滑动， 可继承ViewPager或HorizontalScrollView实现NestedScrollingChild相关接口，并重写它们的onTouchEvent和onInterceptTouchEvent相关方法， 使之将滑动事件分享出来。
