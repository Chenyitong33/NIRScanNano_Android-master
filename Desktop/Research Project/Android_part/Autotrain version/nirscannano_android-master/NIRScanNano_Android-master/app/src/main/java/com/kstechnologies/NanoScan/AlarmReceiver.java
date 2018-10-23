package com.kstechnologies.NanoScan;

/**
 * Created by frankie on 2018/9/19.
 */

import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import java.util.Timer;
import java.util.TimerTask;

public class AlarmReceiver extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        MainActivity.getTextView2().setText("Time to eat pills!");
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        final Ringtone ringtone = RingtoneManager.getRingtone(context, uri);
        ringtone.play();
        final Timer t = new Timer();
        t.schedule(new TimerTask() {
            public void run() {
                ringtone.stop();
                t.cancel();
            }
        }, 10000);
    }
}