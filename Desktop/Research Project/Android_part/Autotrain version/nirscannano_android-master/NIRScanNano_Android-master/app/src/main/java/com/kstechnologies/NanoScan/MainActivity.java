package com.kstechnologies.NanoScan;

/**
 * Created by frankie on 2018/9/19.
 */

import java.util.Calendar;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
public class MainActivity extends FragmentActivity{
    private static int timeHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    private static int timeMinute = Calendar.getInstance().get(Calendar.MINUTE);
    TextView textView1;
    private static TextView textView2;
    public static TextView getTextView2() {
        return textView2;
    }
    AlarmManager alarmManager;
    private PendingIntent pendingIntent;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mylayout);
        textView1 = (TextView)findViewById(R.id.msg1);
        textView1.setText(timeHour + ":" + timeMinute);
        textView2 = (TextView)findViewById(R.id.msg2);

        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent myIntent = new Intent(MainActivity.this, AlarmReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 100, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        OnClickListener listener1 = new OnClickListener() {
            public void onClick(View view) {
                textView2.setText("");
                Bundle bundle = new Bundle();
                bundle.putInt(MyConstants.HOUR, timeHour);
                bundle.putInt(MyConstants.MINUTE, timeMinute);
                MyDialogFragment fragment = new MyDialogFragment();
                fragment.setHandler(new MyHandler());
                Log.d("dddd","main");
                fragment.setArguments(bundle);
                FragmentManager manager = getSupportFragmentManager();
                FragmentTransaction transaction = manager.beginTransaction();
                transaction.add(fragment, MyConstants.TIME_PICKER);
                transaction.commit();
                Log.d("dddd","commit");
            }
        };

        Button btn1 = (Button)findViewById(R.id.button1);
        btn1.setOnClickListener(listener1);


        /*OnClickListener listener2 = new OnClickListener() {
            public void onClick(View view) {
                textView2.setText("");
                cancelAlarm();
            }
        };
        Button btn2 = (Button)findViewById(R.id.button2);
        btn2.setOnClickListener(listener2);*/
    }
    class MyHandler extends Handler {
        @Override
        public void handleMessage (Message msg){
            Bundle bundle = msg.getData();
            timeHour = bundle.getInt(MyConstants.HOUR);
            timeMinute = bundle.getInt(MyConstants.MINUTE);
            textView1.setText(timeHour + ":" + timeMinute);
            setAlarm();
        }
    }
    private void setAlarm(){
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, timeHour);
        calendar.set(Calendar.MINUTE, timeMinute);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }
    private void cancelAlarm() {
        /*if (alarmManager!= null) {
            alarmManager.cancel(pendingIntent);
        }*/
        alarmManager.cancel(pendingIntent);
    }
}
