package com.qgswsg.sideslipentry;

import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.view.View;
import android.widget.ImageView;

import com.qgswsg.side_slip_entry.SideSlipEntryBehavior;

public class SlidingIsViewActivity extends BaseActivity {

    private ImageView iv2;
    private ImageView iv3;
    private ImageView smallTailView;
    private SideSlipEntryBehavior<ConstraintLayout> sideSlipEntryBehavior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sliding_is_view);
        ConstraintLayout bindSideSlipEntryBehaviorView = findViewById(R.id.bindSideSlipEntryBehaviorView);
        sideSlipEntryBehavior = SideSlipEntryBehavior.from(bindSideSlipEntryBehaviorView);
        smallTailView = findViewById(R.id.smallTailView);
        iv2 = findViewById(R.id.iv2);
        iv3 = findViewById(R.id.iv3);
        ImageView iv4 = findViewById(R.id.iv4);
        sideSlipEntryBehavior.setSideslipEntryCallback(new SideSlipEntryBehavior.SideslipEntryCallback() {
            @Override
            public void onSlide(View view, float offset) {
                float rotation = offset * 360;
                smallTailView.setRotation(-rotation);
                iv2.setRotation(-rotation);
                iv3.setRotationX(rotation);
            }

            @Override
            public void onStateChanged(View view, int state) {
            }
        });
        iv4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sideSlipEntryBehavior.setState(SideSlipEntryBehavior.STATE_COLLAPSED);
            }
        });
    }
}
