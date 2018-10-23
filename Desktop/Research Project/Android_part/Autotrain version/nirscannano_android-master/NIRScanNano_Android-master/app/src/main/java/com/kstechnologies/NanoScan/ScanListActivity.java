package com.kstechnologies.NanoScan;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.kstechnologies.nirscannanolibrary.SettingsManager;


import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import android.widget.Toast;

/**
 * This activity controls the main launcher view
 * This activity is responsible for generating the splash screen and the main
 * file list view
 *
 * From this activity, the user can begin the scan process {@link NewScanActivity},
 * Go to view old scan data {@link GraphActivity}
 *
 * @author collinmast
 */
public class ScanListActivity extends Activity {

    private ArrayList<String> csvFiles = new ArrayList<>();
    private ArrayAdapter<String> mAdapter;
    private static Context mContext;
    private SwipeMenuListView lv_csv_files;
    ImageView instructionsShow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;
        setContentView(R.layout.activity_scan_list);

        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setTitle("Home");
        }

        instructionsShow = (ImageView) findViewById(R.id.imageView);
        instructionsShow.setImageResource(R.mipmap.startscreen2);
        Toast.makeText(getApplicationContext(),"Welcome to smart medication lab!",Toast.LENGTH_LONG).show();// Set your own toast  message
    }

    /*
     * When the activity is destroyed, make a call to the super class
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /* On resume, check for crashes and updates with HockeyApp,
     * and set up the file list,swipe menu, and event listeners
     */
    @Override
    public void onResume() {
        super.onResume();
    }

    /*
     * Inflate the options menu so that the info, settings, and connect icons are visible
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scan_list, menu);
        return true;
    }

    /*
     * Handle the selection of a menu item.
     * In this case, there are three options. The user can go to the info activity,
     * the settings activity, or connect to a Nano
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }

        else if (id == R.id.action_scan) {
            Intent graphIntent = new Intent(mContext, NewScanActivity.class);
            graphIntent.putExtra("file_name", getString(R.string.newScan));

            startActivity(graphIntent);
        }

        else if (id == R.id.action_video) {
            Intent videoIntent = new Intent(this, VideoActivity.class);
            startActivity(videoIntent);
            return true;
        }
        else if (id ==  R.id.action_alarm) {
            Intent videoIntent = new Intent(this, MainActivity.class);
            startActivity(videoIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Function to convert dip to pixels
     *
     * @param dp the number of dip to convert
     * @return the dip units converted to pixels
     */
    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }
}
