package com.qgswsg.sideslipentry;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RecyclerView activityListRv = findViewById(R.id.activityListRv);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        activityListRv.setLayoutManager(layoutManager);
        List<ActivityItem> activityItems = new ArrayList<>();
        activityItems.add(new ActivityItem("普通控件,无嵌套滑动", SlidingIsViewActivity.class));
        activityItems.add(new ActivityItem("单独一个RecyclerView", SlidingIsRecyclerViewActivity.class));
        activityItems.add(new ActivityItem("“小尾巴”会移出屏幕", SmallTailMovedOutActivity.class));
        activityListRv.setAdapter(new ActivityListAdapter(activityItems));
    }

    class ActivityListAdapter extends RecyclerView.Adapter<ActivityViewHolder> {

        private List<ActivityItem> items;

        ActivityListAdapter(List<ActivityItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new ActivityViewHolder(LayoutInflater.from(MainActivity.this).inflate(R.layout.item_layout,viewGroup,false));
        }

        @Override
        public void onBindViewHolder(@NonNull ActivityViewHolder activityViewHolder, int i) {
            final ActivityItem activityItem = items.get(i);
            activityViewHolder.name.setText(activityItem.getName());
            activityViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, activityItem.getClazz());
                    intent.putExtra("NAME", activityItem.getName());
                    startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    class ActivityViewHolder extends RecyclerView.ViewHolder {
        TextView name;

        ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.itemContent);
        }
    }

    class ActivityItem {
        private String name;
        private Class<?> clazz;

        public ActivityItem(String name, Class clazz) {
            this.name = name;
            this.clazz = clazz;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Class<?> getClazz() {
            return clazz;
        }

        public void setClazz(Class<?> clazz) {
            this.clazz = clazz;
        }
    }
}
