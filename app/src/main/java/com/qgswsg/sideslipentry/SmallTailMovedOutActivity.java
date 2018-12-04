package com.qgswsg.sideslipentry;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.LinearLayout;

import com.qgswsg.side_slip_entry.SideSlipEntryBehavior;

public class SmallTailMovedOutActivity extends BaseActivity {

    private WebView myWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_small_tail_moved_out);
        LinearLayout bindSideSlipEntryBehaviorView = findViewById(R.id.bindSideSlipEntryBehaviorView);
        SideSlipEntryBehavior.from(bindSideSlipEntryBehaviorView).setSideslipEntryCallback(new SideSlipEntryBehavior.SideslipEntryCallback() {
            @Override
            public void onSlide(View view, float offset) {

            }

            @Override
            public void onStateChanged(View view, int state) {
                if (state == SideSlipEntryBehavior.STATE_EXPANDED) {
                    if (myWebView == null) {
                        myWebView = findViewById(R.id.myWebView);
                        myWebView.loadUrl("https://github.com/qgswsg/SideslipEntry");
                    }
                }
            }
        });
    }
}
