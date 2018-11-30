package com.qgswsg.sideslipentry;

import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.qgswsg.sideslipentrybehaviorlib.SideslipEntryBehavior;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ViewPager viewPager;
    private LinearLayout myLl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final List<View> viewList = new ArrayList<>();
        viewList.add(View.inflate(this, R.layout.viewpager_item, null));
        viewList.add(View.inflate(this, R.layout.viewpager_item, null));
        viewPager = findViewById(R.id.myVp);
        myLl = findViewById(R.id.myLl);
        myLl.post(new Runnable() {
            @Override
            public void run() {
                int width = findViewById(R.id.myCv).getWidth();
                Display display = getWindowManager().getDefaultDisplay();
                Point point = new Point();
                display.getSize(point);
                ViewGroup.LayoutParams lp1 = myLl.getLayoutParams();
                lp1.width = point.x + width;
                myLl.setLayoutParams(lp1);
                ViewGroup.LayoutParams lp2 = viewPager.getLayoutParams();
                lp2.width = point.x;
                viewPager.setLayoutParams(lp2);
                SideslipEntryBehavior<LinearLayout> from = SideslipEntryBehavior.from(myLl);
                from.setSmallTailWidth(width);
            }
        });
        viewPager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return viewList.size();
            }

            @Override
            public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
                return view == o;
            }

            @NonNull
            @Override
            public Object instantiateItem(@NonNull ViewGroup container, int position) {
                container.addView(viewList.get(position));
                return viewList.get(position);
            }

            @Override
            public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
                container.removeView(viewList.get(position));
            }
        });
    }
}
