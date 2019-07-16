# SideSlipEntry

侧滑进入 

![效果图](https://github.com/qgswsg/SideslipEntry/blob/master/%E7%A4%BA%E4%BE%8B.gif)

控件如果想实现从右侧滑入的效果，可以绑定此Behavior 并添加到CoordinatorLayout布局中，方可实现从右侧滑入的效果，并支持嵌套滑动。 

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

下边给出相关属性说明表

| 属性（attr） | 格式（format） | 说明（Description） |
| ------ | ------ | ------ |
| smallTailWidth | dimension | “小尾巴”的宽度，就是停靠在最右边时，露出来部分的宽度 |
| hideable | boolean | 是否可以被完全移出屏幕隐藏起来 |
| fitToContents | boolean | 为true时，控件所有内容都被显示了就不能再往左拖动了；否则将允许拖动到屏幕最左边 |
| smallTailMovedOut | boolean | 为true时，“小尾巴”部分将允许拖动到最左边的屏幕外；否则只能拖动到屏幕最左边 |
| halfable | boolean | 为true时，允许在屏幕水平方向中间停靠 |
| smallTailView | reference | 指定“小尾巴”控件的id，此控件只能是绑定此behavior控件的直接子控件；指定此属性后，smallTailWidth属性将无效，“小尾巴”的宽度会自动计算 |

依赖
<pre>implementation 'com.qgswsg.side_slip_entry:SideSlipEntry:1.5'</pre>
