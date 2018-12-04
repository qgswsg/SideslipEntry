package com.qgswsg.sideslipentry;

import android.support.annotation.LayoutRes;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;

public abstract class BaseActivity extends AppCompatActivity {

    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) supportActionBar.setDisplayHomeAsUpEnabled(true);
        String title = getIntent().getStringExtra("NAME");
        if (title != null) setTitle(title);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
