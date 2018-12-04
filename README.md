# SideSlipEntry

侧滑进入 控件如果想实现从右侧滑入的效果，可以绑定此Behavior 并添加到CoordinatorLayout布局中，方可实现从右侧滑入的效果，并支持嵌套滑动。 

示例：
<pre>
&lt;ViewGroup
      android:layout_width=&quot;match_parent&quot;
      android:layout_height=&quot;wrap_content&quot;
      android:layout_gravity=&quot;bottom&quot;
      android:layout_marginBottom=&quot;16dp&quot;
      app:fitToContents=&quot;false&quot;
      app:halfable=&quot;true&quot;
      app:hideable=&quot;true&quot;
      app:layout_behavior=&quot;com.qgswsg.side_slip_entry.SideSlipEntryBehavior&quot;
      app:smallTailMovedOut=&quot;true&quot;
      app:smallTailWidth=&quot;50dp&quot;
      app:smallTailView=&quot;@id/smallTailView&quot;
      tools:ignore=&quot;MissingPrefix&quot;&gt;
            ...
&lt;/ViewGroup&gt;
</pre>
最后记得在根标签中添加 xmlns:app="http://schemas.android.com/apk/res-auto"

另外：
ViewPager和HorizontalScrollView也算是可以水平滑动的控件，但它们并没有实现NestedScrollingChild相关接口。 因此本Behavior是不支持与ViewPager和HorizontalScrollView嵌套滑动的，如果想实现它们的嵌套滑动， 可继承ViewPager或HorizontalScrollView实现NestedScrollingChild相关接口，并重写它们的onTouchEvent和onInterceptTouchEvent相关方法， 使之将滑动事件分享出来。
