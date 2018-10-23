package com.kstechnologies.NanoScan;

/**
 * Created by frankie on 2018/9/19.
 */

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.widget.TimePicker;
import android.util.Log;

import javax.net.ssl.HandshakeCompletedEvent;

public class MyDialogFragment extends DialogFragment {
    private int timeHour;
    private int timeMinute;
    private Handler handler;

    public MyDialogFragment(){
        this.handler = handler;
    }
    public void setHandler(Handler handler){
        this.handler = handler;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        timeHour = bundle.getInt(MyConstants.HOUR);
        timeMinute = bundle.getInt(MyConstants.MINUTE);
        Log.d("dddd","test");

        //bundle.getBundle()
        TimePickerDialog.OnTimeSetListener listener = new TimePickerDialog.OnTimeSetListener() {
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                timeHour = hourOfDay;
                timeMinute = minute;
                Bundle b = new Bundle();
                b.putInt(MyConstants.HOUR, timeHour);
                b.putInt(MyConstants.MINUTE, timeMinute);
                Message msg = new Message();
                msg.setData(b);
                handler.sendMessage(msg);
            }
        };
        return new TimePickerDialog(getActivity(), listener, timeHour, timeMinute, false);
    }
}