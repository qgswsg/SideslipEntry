package com.qgswsg.sideslipentry;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class SlidingIsRecyclerViewActivity extends BaseActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sliding_is_recycler_view);
        RecyclerView bindSideSlipEntryBehaviorRecyclerView = findViewById(R.id.bindSideSlipEntryBehaviorRecyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        bindSideSlipEntryBehaviorRecyclerView.setLayoutManager(layoutManager);
        List<String> items = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            stringBuilder.append("第").append(i).append("项");
            items.add(stringBuilder.toString());
            stringBuilder.delete(0, stringBuilder.length());
        }
        bindSideSlipEntryBehaviorRecyclerView.setAdapter(new MyRvAdapter(items));
    }

    class MyRvAdapter extends RecyclerView.Adapter<MyViewHolder> {

        private List<String> items;

        MyRvAdapter(List<String> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new MyViewHolder(View.inflate(SlidingIsRecyclerViewActivity.this, R.layout.item_layout, null));
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder myViewHolder, int i) {
            myViewHolder.itemContent.setText(items.get(i));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView itemContent;

        MyViewHolder(@NonNull View itemView) {
            super(itemView);
            itemContent = itemView.findViewById(R.id.itemContent);
        }
    }
}
