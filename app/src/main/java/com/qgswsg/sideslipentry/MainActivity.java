package com.qgswsg.sideslipentry;

import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.qgswsg.sideslipentrybehaviorlib.SideslipEntryBehavior;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private MyViewPager viewPager;
    private LinearLayout myLl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final List<View> viewList = new ArrayList<>();
        View inflate = View.inflate(this, R.layout.viewpager_item, null);
        CardView icv = inflate.findViewById(R.id.icv);
        icv.setBackgroundColor(Color.BLUE);
        viewList.add(inflate);
        View inflate1 = View.inflate(this, R.layout.viewpager_item, null);
        CardView icv1 = inflate1.findViewById(R.id.icv);
        icv1.setBackgroundColor(Color.YELLOW);
        viewList.add(inflate1);
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

//        RecyclerView recyclerView = findViewById(R.id.myRv);
//        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
//        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
//        recyclerView.setLayoutManager(linearLayoutManager);
//        List<String> items = new ArrayList<>();
//        StringBuilder builder = new StringBuilder();
//        for (int i = 0; i < 100; i++) {
//            builder.delete(0, builder.length());
//            builder.append("这是第").append(i).append("项");
//            items.add(builder.toString());
//        }
//        recyclerView.setAdapter(new Adapter(items));

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

    class Adapter extends RecyclerView.Adapter<MyViewHolder> {

        private List<String> items;

        public Adapter(List<String> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new MyViewHolder(View.inflate(MainActivity.this, android.R.layout.simple_list_item_1, null));
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder myViewHolder, int i) {
            myViewHolder.textView.setText(items.get(i));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}
