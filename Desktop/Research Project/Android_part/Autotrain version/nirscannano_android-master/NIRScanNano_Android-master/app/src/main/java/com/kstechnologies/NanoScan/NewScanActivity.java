
package com.kstechnologies.NanoScan;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.kstechnologies.nirscannanolibrary.KSTNanoSDK;
import com.kstechnologies.nirscannanolibrary.SettingsManager;
import com.opencsv.CSVWriter;

import org.apache.commons.net.ftp.FTPClient;
import android.os.StrictMode;

import com.github.mikephil.charting.charts.LineChart;

import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity controlling the Nano once it is connected
 * This activity allows a user to initiate a scan, as well as access other "connection-only"
 * settings. When first launched, the app will scan for a preferred device
 * for {@link NanoBLEService#SCAN_PERIOD}, if it is not found, then it will start another "open"
 * scan for any Nano.
 *
 * If a preferred Nano has not been set, it will start a single scan. If at the end of scanning, a
 * Nano has not been found, a message will be presented to the user indicating and error, and the
 * activity will finish
 *
 * WARNING: This activity uses JNI function calls for communicating with the Spectrum C library, It
 * is important that the name and file structure of this activity remain unchanged, or the functions
 * will NOT work
 *
 * @author collinmast
 */
public class NewScanActivity extends Activity {

    private static Context mContext;

    private ProgressDialog barProgressDialog;

    private ViewPager mViewPager;
    private String fileName;

    private final BroadcastReceiver scanDataReadyReceiver = new scanDataReadyReceiver();
    private final BroadcastReceiver refReadyReceiver = new refReadyReceiver();
    private final BroadcastReceiver notifyCompleteReceiver = new notifyCompleteReceiver();
    private final BroadcastReceiver requestCalCoeffReceiver = new requestCalCoeffReceiver();
    private final BroadcastReceiver requestCalMatrixReceiver = new requestCalMatrixReceiver();
    private final BroadcastReceiver disconnReceiver = new DisconnReceiver();

    private final IntentFilter scanDataReadyFilter = new IntentFilter(KSTNanoSDK.SCAN_DATA);
    private final IntentFilter refReadyFilter = new IntentFilter(KSTNanoSDK.REF_CONF_DATA);
    private final IntentFilter notifyCompleteFilter = new IntentFilter(KSTNanoSDK.ACTION_NOTIFY_DONE);
    private final IntentFilter requestCalCoeffFilter = new IntentFilter(KSTNanoSDK.ACTION_REQ_CAL_COEFF);
    private final IntentFilter requestCalMatrixFilter = new IntentFilter(KSTNanoSDK.ACTION_REQ_CAL_MATRIX);
    private final IntentFilter disconnFilter = new IntentFilter(KSTNanoSDK.ACTION_GATT_DISCONNECTED);

    private final BroadcastReceiver scanConfReceiver = new ScanConfReceiver();
    private final IntentFilter scanConfFilter = new IntentFilter(KSTNanoSDK.SCAN_CONF_DATA);

    private ProgressBar calProgress;
    private TextView textProgress;
    private KSTNanoSDK.ScanResults results;
    private Button btn_scan;
    private Button btn_report;
    private Button btn_blacklist;
    private Button btn_emptylist;
    private Button btn_model;
    private Button yes_button;
    private Button no_button;

    private NanoBLEService mNanoBLEService;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private Handler mHandler;
    private static final String DEVICE_NAME = "NIRScanNano";
    private boolean connected;
    private AlertDialog alertDialog;
    private String preferredDevice;
    private KSTNanoSDK.ScanConfiguration activeConf;

    private Menu mMenu;

    private FTPClient transferClient;
    private Socket clientSocket;
    private String serverAddress = "nirs.cis.unimelb.edu.au";
    private String localAddress = "localhost";
    private String nameCSVToSend;
    String stringFromServer;
    String nameFromServer;
    String effectFromServer;
    ViewPager resultShow;
    TextToSpeech tts;
    TextView luxText;
    SensorManager sensorManager;
    Sensor lightSensor;
    float x;
    public Toast toast;
    public CountDownTimer toastCountDown;
    private String sCurrentLine;
    String scanType;
    long start;
    long readTime;
    SimpleDateFormat sdf;

    long elapsedSensorEvent;
    long elapsedDisconn;
    long elapsedScanConf;
    long elapsedrequestCalMatrix;
    long elapsedrequestCalCoeff;
    long elapsednotifyComplete;
    long elapsedrefReady;
    long elapsedscanDataReady;
    long elapsedWriteCSV;
    Date resultdate;
    Date startdate;
    Date refReadydate;
    Date notifyComplete;
    Date requestCalCoeff;
    Date requestCalMatrix;
    Date scanConf;
    Date Disconn;
    //Tag for logging events in the BluetoothLeService
    private static final String TAG = "__NewScanActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_scan);

        //ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);



        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        mContext = this;


        calProgress = (ProgressBar) findViewById(R.id.calProgress);
        textProgress = (TextView) findViewById(R.id.textView);

        calProgress.setVisibility(View.VISIBLE);
        connected = false;
        resultShow = (ViewPager) findViewById(R.id.view_pager);
        ImageAdapter adapter = new ImageAdapter(this); //Here we are defining the Imageadapter object

        resultShow.setAdapter(adapter); // Here we are passing and setting the adapter for the images

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        tts = new TextToSpeech(NewScanActivity.this, new TextToSpeech.OnInitListener() {

            @Override
            public void onInit(int status) {
                // TODO Auto-generated method stub
                if(status == TextToSpeech.SUCCESS){
                    tts.setLanguage(Locale.UK);
                    tts.setSpeechRate(0.9f);
                }
                else
                    Log.e("error", "Initilization Failed!");
            }
        });


        if(!SettingsManager.getBooleanPref(mContext, SettingsManager.SharedPreferencesKeys.tempUnits,false)) {
            //resultShow.getAdapter();
        }
        else {
            //resultShow.setImageResource(R.mipmap.instructionsgluten);
        }

        //Set the filename from the intent
        Intent intent = getIntent();
        fileName = intent.getStringExtra("file_name");

        //Set up action bar enable tab navigation
        ActionBar ab = getActionBar();
        if (ab != null) {

            //ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle("Scan");

            btn_emptylist = (Button) findViewById(R.id.btn_emptylist);
            btn_emptylist.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new Thread(){
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    Log.d(TAG, "overwrite the black list");
                                    writeBlacklist(true);
                                }
                            });

                        }
                    }.start();
                    Log.d(TAG, "Clicked  Emptylist button");
                }
            });

            btn_blacklist = (Button) findViewById(R.id.btn_blacklist);
            btn_blacklist.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new Thread(){
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    try{

                                        Log.d(TAG, "add a new pill to black list!");

                                        AlertDialog.Builder builder1 = new AlertDialog.Builder(mContext);
                                        builder1.setTitle("Please input the name of dangerous or allergic pills.");

                                        // Set up the input
                                        final EditText input = new EditText(mContext);
                                        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                                        input.setInputType(InputType.TYPE_CLASS_TEXT);
                                        builder1.setView(input);

                                        // Set up the buttons
                                        builder1.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                try{
                                                    addBlacklist(input.getText().toString(), true);

                                                } catch (Exception e) {
                                                    System.out.println("S: Error");
                                                    Log.d(TAG, "S: Error");
                                                    e.printStackTrace();
                                                }
                                            }
                                        });
                                        builder1.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.cancel();
                                            }
                                        });

                                        builder1.show();



                                    }catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });

                        }
                    }.start();

                    Log.d(TAG, "Clicked Addlist button");

                }
            });


            btn_report = (Button) findViewById(R.id.btn_report);
            btn_report.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    new Thread(){
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                public void run() {
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which){
                                case DialogInterface.BUTTON_POSITIVE:
                                    //Yes button clicked
                                    try{
                                        // Connect to server and open readers.
                                        clientSocket = new Socket(serverAddress, 10000);

                                        Log.d(TAG, "Clicked Yes");
                                        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
                                        outToServer.writeBytes("Yes" + '\n');
                                        Log.d(TAG, "Send yes success");
                                        DataOutputStream put = new DataOutputStream(
                                                clientSocket.getOutputStream());
                                        File dataToSend = new File("/storage/emulated/0/" + nameCSVToSend);
                                        if (dataToSend.isFile()) {
                                            FileInputStream fis = new FileInputStream(dataToSend);

                                            byte[] buf = new byte[1024];
                                            int read;
                                            while ((read = fis.read(buf, 0, 1024)) != -1) {
                                                put.write(buf, 0, read);
                                                put.flush();

                                            }
                                            clientSocket.close();

                                            //System.out.println("File transfered");
                                            Log.d(TAG, "File transfered");
                                            //client.close();
                                            fis.close();
                                        }
                                    }catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    try{
                                        // Connect to server and open readers.
                                        clientSocket = new Socket(serverAddress, 10000);
                                        Log.d(TAG, "Clicked No");
                                        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
                                        outToServer.writeBytes("No" + '\n');


                                        AlertDialog.Builder builder1 = new AlertDialog.Builder(mContext);
                                        builder1.setTitle("Please input the correct name of the pill.");

                                        // Set up the input
                                        final EditText input = new EditText(mContext);
                                        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                                        input.setInputType(InputType.TYPE_CLASS_TEXT);
                                        builder1.setView(input);

                                        // Set up the buttons
                                        builder1.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                try{
                                                    DataOutputStream outToServer = new DataOutputStream(
                                                            clientSocket.getOutputStream());
                                                    outToServer.writeBytes(input.getText().toString()+'\n');

                                                }catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                                try {

                                                    DataOutputStream put = new DataOutputStream(
                                                            clientSocket.getOutputStream());
                                                    File dataToSend = new File("/storage/emulated/0/" + nameCSVToSend);
                                                    if (dataToSend.isFile()) {
                                                        FileInputStream fis = new FileInputStream(dataToSend);

                                                        byte[] buf = new byte[1024];
                                                        int read;
                                                        while ((read = fis.read(buf, 0, 1024)) != -1) {
                                                            put.write(buf, 0, read);
                                                            put.flush();

                                                        }
                                                        clientSocket.close();

                                                        //System.out.println("File transfered");
                                                        Log.d(TAG, "File transfered");
                                                        //client.close();
                                                        fis.close();
                                                    }

                                                } catch (Exception e) {
                                                    System.out.println("S: Error");
                                                    Log.d(TAG, "S: Error");
                                                    e.printStackTrace();
                                                }
                                            }
                                        });
                                        builder1.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.cancel();
                                            }
                                        });

                                        builder1.show();



                                    }catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setMessage("Is this correct?").setPositiveButton("Yes", dialogClickListener)
                            .setNegativeButton("No", dialogClickListener).show();
                                }
                            });

                        }
                    }.start();

                    Log.d(TAG, "Clicked Report button");

                }
            });

            /*btn_model = (Button) findViewById(R.id.btn_model);

            btn_model.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popup = new PopupMenu(NewScanActivity.this, btn_model);
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem item) {
                            Toast.makeText(NewScanActivity.this,"You Clicked : " + item.getTitle(), Toast.LENGTH_SHORT).show();
                            // Connect to server and open readers.
                            try {
                                clientSocket = new Socket(serverAddress, 10000);

                                DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
                                //DataInputStream inFromServer = new DataInputStream(clientSocket.getInputStream());
                                BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                                switch (item.getItemId()) {
                                    case R.id.SVM:
                                        // do your code
                                        outToServer.writeBytes("SVM"+ '\n');
                                        Log.d(TAG, "Sending to server");

                                        stringFromServer = inFromServer.readLine();
                                        Log.d(TAG, "Received from server");
                                        Log.d(TAG, stringFromServer);
                                        return true;
                                    case R.id.RF:
                                        // do your code
                                        outToServer.writeBytes("RF"+ '\n');
                                        Log.d(TAG, "Sending to server");
                                        return true;
                                    case R.id.XGBoost:
                                        // do your code
                                        outToServer.writeBytes("XGBoost"+ '\n');
                                        Log.d(TAG, "Sending to server");
                                        return true;
                                    default:
                                        return false;
                                }
                            }catch (IOException e) {
                                //You'll need to add proper error handling here
                                return false;
                            }
                        }
                    });
                    //popup.inflate(R.menu.menu_example);
                    popup.getMenuInflater().inflate(R.menu.menu_example, popup.getMenu());
                    popup.show();
                }
            });*/
            btn_scan = (Button) findViewById(R.id.btn_scan);

            btn_scan.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (x > 1200) {
                        //resultShow.setImageResource(android.R.color.transparent);
                        //resultShow.removeAllViews();
                        resultShow.setVisibility(View.GONE);
                        btn_scan.setClickable(false);
                        btn_scan.setBackgroundColor(ContextCompat.getColor(mContext, R.color.btn_unavailable));
                        createToast("muchlight");
                        Handler handlerTimer4 = new Handler();
                        handlerTimer4.postDelayed(new Runnable() {
                            public void run() {
                                btn_scan.setClickable(true);
                                btn_scan.setBackgroundColor(ContextCompat.getColor(mContext, R.color.kst_red));

                                if(!SettingsManager.getBooleanPref(mContext, SettingsManager.SharedPreferencesKeys.tempUnits,false)) {
                                    //resultShow.setImageResource(R.mipmap.instructions);
                                }
                                else {
                                    //resultShow.setImageResource(R.mipmap.instructionsgluten);
                                }
                            }
                        }, 10500);

                    }
                    else {
                        //resultShow.setImageResource(android.R.color.transparent);
                        resultShow.setVisibility(View.GONE);
                        textProgress.setText("Waiting for the result...\nPlease do not move!");
                        //SettingsManager.storeStringPref(mContext, SettingsManager.SharedPreferencesKeys.prefix, filePrefix.getText().toString());
                        LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(KSTNanoSDK.START_SCAN));
                        calProgress.setVisibility(View.VISIBLE);
                        btn_scan.setClickable(false);
                        btn_scan.setBackgroundColor(ContextCompat.getColor(mContext, R.color.btn_unavailable));


                        if (x > 800) {
                            createToast("light");
                        }
                        start = System.currentTimeMillis();
                        Log.d(TAG, "Clicked Scan button");
                    }
                }
            });


            //Bind to the service. This will start it, and call the start command function
            Intent gattServiceIntent = new Intent(this, NanoBLEService.class);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
            Log.d(TAG, "Bind to the gattService");

            //Register all needed broadcast receivers
            LocalBroadcastManager.getInstance(mContext).registerReceiver(scanDataReadyReceiver, scanDataReadyFilter);
            LocalBroadcastManager.getInstance(mContext).registerReceiver(refReadyReceiver, refReadyFilter);
            LocalBroadcastManager.getInstance(mContext).registerReceiver(notifyCompleteReceiver, notifyCompleteFilter);
            LocalBroadcastManager.getInstance(mContext).registerReceiver(requestCalCoeffReceiver, requestCalCoeffFilter);
            LocalBroadcastManager.getInstance(mContext).registerReceiver(requestCalMatrixReceiver, requestCalMatrixFilter);
            LocalBroadcastManager.getInstance(mContext).registerReceiver(disconnReceiver, disconnFilter);
            LocalBroadcastManager.getInstance(mContext).registerReceiver(scanConfReceiver, scanConfFilter);


        }


        sensorManager.registerListener(lightListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);

        tts = new TextToSpeech(NewScanActivity.this, new TextToSpeech.OnInitListener() {

            @Override
            public void onInit(int status) {
                // TODO Auto-generated method stub
                if(status == TextToSpeech.SUCCESS){
                    tts.setLanguage(Locale.UK);
                    tts.setSpeechRate(0.9f);
                }
                else
                    Log.e("error", "Initilization Failed!");
            }
        });
    }
    /*
     * When the activity is destroyed, unregister all broadcast receivers, remove handler callbacks,
     * and store all user preferences
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (toast != null) {
            toast.cancel();
        }
        if (toastCountDown != null) {
            toastCountDown.cancel();
        }

        //resultShow.setImageResource(android.R.color.transparent);
        resultShow.setVisibility(View.GONE);
        unbindService(mServiceConnection);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(scanDataReadyReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(refReadyReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(notifyCompleteReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(requestCalCoeffReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(requestCalMatrixReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(disconnReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(scanConfReceiver);

        mHandler.removeCallbacksAndMessages(null);

        //SettingsManager.storeBooleanPref(mContext, SettingsManager.SharedPreferencesKeys.saveOS, btn_os.isChecked());
        //SettingsManager.storeBooleanPref(mContext, SettingsManager.SharedPreferencesKeys.saveSD, btn_sd.isChecked());
        //SettingsManager.storeBooleanPref(mContext, SettingsManager.SharedPreferencesKeys.continuousScan, btn_continuous.isChecked());
    }

    /*
     * Inflate the options menu so that user actions are present
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_new_scan, menu);
        mMenu = menu;

        if(btn_scan.isClickable()) {mMenu.findItem(R.id.action_settings).setEnabled(true);}
        else {mMenu.findItem(R.id.action_settings).setEnabled(false);}

        final MenuItem menuItem = mMenu.findItem(R.id.action_lux);
        luxText = (TextView) menuItem.getActionView();
        luxText.setText("Lux: " + Float.toString(x));
        luxText.setTextSize(20f);

        if (x < 1500f) {
            luxText.setTextColor(Color.GREEN);
        }
        else if(x > 1500f && x < 5000f) {
            luxText.setTextColor(Color.YELLOW);
        }
        else {
            luxText.setTextColor(Color.RED);
        }

        return true;
    }

    /*
     * Handle the selection of a menu item.
     * In this case, the user has the ability to access settings while the Nano is connected
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent configureIntent = new Intent(mContext, ConfigureActivity.class);
            startActivity(configureIntent);
        }

        if (id == android.R.id.home) {
            this.finish();
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Custom receiver for handling scan data and setting up the graphs properly
     */
    public class scanDataReadyReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            long tStart = System.currentTimeMillis();
            long tDelta = tStart - start;
            elapsedscanDataReady = tDelta;
            //calProgress.setVisibility(View.GONE);
            //btn_scan.setText(getString(R.string.scan));
            byte[] scanData = intent.getByteArrayExtra(KSTNanoSDK.EXTRA_DATA);
            //scanType = intent.getStringExtra(KSTNanoSDK.EXTRA_SCAN_TYPE);
            //String scanDate = intent.getStringExtra(KSTNanoSDK.EXTRA_SCAN_DATE);

            KSTNanoSDK.ReferenceCalibration ref = KSTNanoSDK.ReferenceCalibration.currentCalibration.get(0);
            results = KSTNanoSDK.KSTNanoSDK_dlpSpecScanInterpReference(scanData, ref.getRefCalCoefficients(), ref.getRefCalMatrix());


            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("ddMMyyhhmmss", Locale.getDefault());
            String ts = simpleDateFormat.format(new Date());

            long tWrite = System.currentTimeMillis();
            Log.d(TAG, "Prepare for writingCSV");


            writeCSV(ts, results, true);
            long tEnd =  System.currentTimeMillis();
            elapsedWriteCSV = tEnd - tWrite;
            //writeCSVDict(ts, scanType, scanDate, String.valueOf(minWavelength), String.valueOf(maxWavelength), String.valueOf(results.getLength()), String.valueOf(results.getLength()), "1", "2.00", saveOS);
            sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss");// prepare for simple date format

            Thread thread = new Thread() {
                @Override
                public void run() {

                    //transferClient = new FTPClient();

                    try {
                        // Connect to server.
                        /*
                        transferClient.connect(serverAddress);
                        transferClient.login("Test", "Test");
                        transferClient.setFileType(FTP.BINARY_FILE_TYPE);
                        transferClient.enterLocalPassiveMode();*/

                        // Retrieve file and send.
                        if(!SettingsManager.getBooleanPref(mContext, SettingsManager.SharedPreferencesKeys.tempUnits,false)) {
                            nameCSVToSend = "Pharmaceuticals.csv";
                        }
                        else {
                            nameCSVToSend = "Gluten.csv";
                        }

                        File dataToSend = new File("/storage/emulated/0/" + nameCSVToSend);
                        FileInputStream metaData = new FileInputStream(dataToSend);

                        //*Don't* hardcode "/sdcard"
                        File sdcard = Environment.getExternalStorageDirectory();

                        //Get the file
                        File file = new File(sdcard, nameCSVToSend);

                        //Read text from file
                        StringBuilder text = new StringBuilder();
                        long readstart = System.currentTimeMillis();
                        try {
                            BufferedReader br = new BufferedReader(new FileReader(file));
                            String line;

                            //skip the first line
                            br.readLine();
                            while ((line = br.readLine()) != null) {
                                String[] features =line.split(",");
                                float reflect = Float.parseFloat(features[2]);
                                text.append(reflect);
                                text.append(',');
                            }
                            br.close();
                        }
                        catch (IOException e) {
                            //You'll need to add proper error handling here
                        }


                        //transferClient.storeFile(nameCSVToSend, metaData);
                        //transferClient.disconnect();

                        // Connect to server and open readers.
                        clientSocket = new Socket(serverAddress, 10000);

                        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
                        //DataInputStream inFromServer = new DataInputStream(clientSocket.getInputStream());
                        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));


                        // Send start message and get result.

                        //outToServer.writeUTF(nameCSVToSend + '\n');
                        outToServer.writeBytes(text.toString().substring(0, text.length() - 1) + '\n');
                        //Log.d(text.toString().substring(0, text.length() - 1), "Absorbance");
                        //outToServer.writeBytes("-0.097657,-0.068578,-0.065569,-0.071737,-0.077938,-0.08244,-0.081585,-0.071182,-0.056991,-0.041888,-0.033788,-0.025694,-0.020879,-0.017964,-0.017337,-0.015771,-0.014087,-0.010784,-0.011806,-0.008835,-0.00842,-0.005162,-0.004218,-0.002731,-0.001307,0.001381,0.002795,0.005116,0.00608,0.007526,0.00785,0.00963,0.009461,0.010489,0.011447,0.011534,0.010616,0.011248,0.00987,0.0089,0.008878,0.008562,0.006618,0.005957,0.006645,0.004936,0.003616,0.002573,0.000978,0.001189,0.00053,-0.000871,-0.000171,-0.000536,-0.001192,-0.00149,-0.001872,-0.003204,-0.003866,-0.002586,-0.002182,-0.001138,0.00037,0.001909,0.002338,0.004292,0.004389,0.00514,0.005821,0.004804,0.005547,0.007176,0.007902,0.008155,0.010711,0.012909,0.013234,0.015413,0.016986,0.017247,0.018692,0.018822,0.016636,0.014509,0.012981,0.011102,0.007442,0.004917,0.003922,0.000569,-0.000935,-0.003789,-0.006086,-0.007349,-0.00645,-0.007272,-0.006588,-0.007189,-0.006711,-0.007559,-0.00738,-0.007608,-0.008372,-0.00745,-0.006614,-0.006875,-0.006601,-0.005999,-0.004771,-0.004492,-0.003465,-0.002354,-0.001148,0.000461,0.002015,0.003499,0.006105,0.007674,0.010189,0.011423,0.012906,0.014335,0.015763,0.017506,0.020352,0.023843,0.024843,0.027593,0.028819,0.03221,0.035916,0.040718,0.046646,0.053451,0.059492,0.067458,0.07552,0.083848,0.089829,0.096491,0.105092,0.108913,0.113693,0.117733,0.118138,0.120274,0.1224,0.124786,0.127113,0.127722,0.130313,0.131118,0.130962,0.131566,0.130793,0.130574,0.128423,0.128353,0.126997,0.124077,0.123458,0.122395,0.11905,0.11621,0.115377,0.114021,0.113551,0.11294,0.111525,0.112517,0.113,0.114696,0.115312,0.117795,0.11862,0.121176,0.122472,0.122953,0.125062,0.126158,0.126875,0.129065,0.129026,0.130325,0.130069,0.130907,0.12957,0.130118,0.128781,0.130206,0.12918,0.129423,0.128561,0.128041,0.127329,0.130681,0.128765,0.128203,0.133424,0.130272,0.133343,0.131148,0.13065,0.13047,0.133656,0.131657,0.132822,0.130121,0.135063,0.135115,0.13978,0.143531,0.148926,0.157011,0.167923,0.190163,0.220659,0.241351,0.260513,0.265153,0.272636,0.280147,0.302518,0.291088,0.287712,0.306441,0.289525,0.275613"+ '\n');
                        Log.d(TAG, "Sending to server");

                        stringFromServer = inFromServer.readLine();
                        Log.d(TAG, "Received from server");
                        Log.d(TAG, stringFromServer);

                        Log.d(TAG, "Read pills from black list");
                        //Get the file
                        File blacklist = new File(sdcard, "Blacklist.txt");
                        final StringBuilder textInList = new StringBuilder();
                        if(blacklist.exists()){
                            //Read text from file
                            Log.d(TAG, "there is a black list.");

                            try {
                                BufferedReader br = new BufferedReader(new FileReader(blacklist));

                                String line = br.readLine();
                                //StringBuilder sb = new StringBuilder();
                                while (line != null) {
                                    textInList.append(line);
                                    textInList.append("\n");
                                    line = br.readLine();
                                }
                                br.close();
                            }
                            catch (IOException e) {
                                //You'll need to add proper error handling here
                            }
                        }else{
                            Log.d(TAG, "there is no black list.");
                        }
                        readTime = System.currentTimeMillis() - readstart;





                        runOnUiThread(new Runnable() {
                                public void run() {

                                calProgress.setVisibility(View.GONE);
                                //textProgress.setVisibility(View.GONE);

                                if (stringFromServer.contains("scatter")) {
                                    //String[] separated = stringFromServer.split(" ");
                                    //stringFromServer = separated[0];
                                    createToast("scattering");

                                    Handler handlerTimer3 = new Handler();
                                    handlerTimer3.postDelayed(new Runnable(){
                                        public void run() {

                                            int resourceID = getResources().getIdentifier(stringFromServer, "mipmap", getPackageName());

                                            if (stringFromServer.equals("gluten_")) {
                                                //resultShow.setImageResource(resourceID);
                                                tts.speak("Warning! This item contains traces of gluten.", TextToSpeech.QUEUE_FLUSH, null);
                                            }
                                            else if (stringFromServer.equals("glutenfree_")) {
                                                //resultShow.setImageResource(resourceID);
                                                tts.speak("Completed! This item contains no traces of gluten.", TextToSpeech.QUEUE_FLUSH, null);
                                            }
                                            else {

                                                tts.speak(stringFromServer, TextToSpeech.QUEUE_FLUSH, null);
                                                resourceID = getResources().getIdentifier(stringFromServer, "mipmap", getPackageName());
                                                //resultShow.setImageResource(resourceID);

                                                textProgress.setText(stringFromServer);
                                                Log.d(TAG, "Display the result");
                                            }

                                            btn_scan.setClickable(true);
                                            btn_scan.setBackgroundColor(ContextCompat.getColor(mContext, R.color.kst_red));

                                        }}, 3500);


                                }
                                else if (stringFromServer.contains("position")) {
                                    textProgress.setText("");
                                    createToast("position");
                                    Handler handlerTimer3 = new Handler();
                                    handlerTimer3.postDelayed(new Runnable(){
                                        public void run() {

                                            if(!SettingsManager.getBooleanPref(mContext, SettingsManager.SharedPreferencesKeys.tempUnits,false)) {
                                                //resultShow.setImageResource(R.mipmap.instructions);
                                            }
                                            else {
                                                //resultShow.setImageResource(R.mipmap.instructionsgluten);
                                            }

                                            btn_scan.setClickable(true);
                                            btn_scan.setBackgroundColor(ContextCompat.getColor(mContext, R.color.kst_red));

                                        }}, 3500);
                                }

                                else {
                                    String[] separated = stringFromServer.split(";");
                                    nameFromServer = separated[0]; //
                                    effectFromServer = separated[1];

                                    String[] separated_nameFromServer = nameFromServer.split(",");
                                    String name = separated_nameFromServer[0];
                                    Log.d(TAG, name);
                                    if(textInList.toString().contains(name)){
                                        textProgress.setText("Dangerous!");
                                        createToast("Dangerous");
                                        tts.speak("Warning! The result might be not safe.", TextToSpeech.QUEUE_FLUSH, null);
                                    }else{
                                        if(nameFromServer.contains("low")) {
                                            textProgress.setText(nameFromServer);
                                            createToast("low confident");
                                            Log.d(TAG, "low confident");
                                            tts.speak("Warning! The confident rate is relatively low.", TextToSpeech.QUEUE_FLUSH, null);
                                        }else{
                                            int resourceID = getResources().getIdentifier(stringFromServer, "mipmap", getPackageName());

                                            if (stringFromServer.equals("gluten_")) {
                                                //resultShow.setImageResource(resourceID);
                                                tts.speak("Warning! This item contains traces of gluten.", TextToSpeech.QUEUE_FLUSH, null);
                                            }
                                            else if (stringFromServer.equals("glutenfree_")) {
                                                //resultShow.setImageResource(resourceID);
                                                tts.speak("Completed! This item contains no traces of gluten.", TextToSpeech.QUEUE_FLUSH, null);
                                            }
                                            else {

                                                resourceID = getResources().getIdentifier(stringFromServer, "mipmap", getPackageName());
                                                //resultShow.setImageResource(resourceID);
                                                tts.speak(nameFromServer, TextToSpeech.QUEUE_FLUSH, null);
                                                long runTime = System.currentTimeMillis() - start;
                                                textProgress.setText("Result: "+nameFromServer+'\n'+'\n'
                                                        //stringFromServer+'\n'
                                                        +'\n'+"Attribute: "+effectFromServer/*+'\n'
                                                +'\n'+"scanning time: "+elapsedscanDataReady+"ms"
                                                +'\n'+"wrtingCSV time: "+elapsedWriteCSV + "ms"
                                                +'\n'+"readingCSV time: "+readTime+"ms"*/
                                                );
                                                Log.d(TAG, "Display the result");
                                            }
                                        }
                                    }



                                    btn_scan.setClickable(true);
                                    btn_scan.setBackgroundColor(ContextCompat.getColor(mContext, R.color.kst_red));

                                }
                                textProgress.setVisibility(View.VISIBLE);
                            }
                        });
                    clientSocket.close();}
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            thread.start();


            //SettingsManager.storeStringPref(mContext, SettingsManager.SharedPreferencesKeys.prefix, filePrefix.getText().toString());
           /*
           if (continuous) {
               calProgress.setVisibility(View.VISIBLE);
               btn_scan.setText(getString(R.string.scanning));
               LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(KSTNanoSDK.SEND_DATA));
           }*/
        }
    }

    /**
     * Custom receiver for returning the event that reference calibrations have been read
     */
    public class refReadyReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {

            byte[] refCoeff = intent.getByteArrayExtra(KSTNanoSDK.EXTRA_REF_COEF_DATA);
            byte[] refMatrix = intent.getByteArrayExtra(KSTNanoSDK.EXTRA_REF_MATRIX_DATA);
            ArrayList<KSTNanoSDK.ReferenceCalibration> refCal = new ArrayList<>();
            refCal.add(new KSTNanoSDK.ReferenceCalibration(refCoeff, refMatrix));
            KSTNanoSDK.ReferenceCalibration.writeRefCalFile(mContext, refCal);
            calProgress.setVisibility(View.GONE);
        }
    }

    /**
     * Custom receiver that will request the time once all of the GATT notifications have been
     * subscribed to
     */
    public class notifyCompleteReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {

            LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(KSTNanoSDK.SET_TIME));
        }
    }

    /**
     * Write scan data to CSV file
     * @param currentTime the current time to save
     * @param scanResults the {@link KSTNanoSDK.ScanResults} structure to save
     * @param saveOS boolean indicating if the CSV file should be saved to the OS
     */
    private void writeCSV(String currentTime, KSTNanoSDK.ScanResults scanResults, boolean saveOS) {

        //String prefix = filePrefix.getText().toString();
        //if (prefix.equals("")) {prefix = "Nano";}
        String prefix = "";

        if(!SettingsManager.getBooleanPref(mContext, SettingsManager.SharedPreferencesKeys.tempUnits,false)) {
            prefix = "Pharmaceuticals";
        }
        else {
            prefix = "Gluten";
        }


        if (saveOS) {
            String csvOS = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + prefix + /*currentTime +*/ ".csv";

            CSVWriter writer;
            try {
                writer = new CSVWriter(new FileWriter(csvOS), ',', CSVWriter.NO_QUOTE_CHARACTER);
                List<String[]> data = new ArrayList<String[]>();
                data.add(new String[]{"Wavelength,Intensity,Absorbance,Reflectance"});

                int csvIndex;
                for (csvIndex = 0; csvIndex < scanResults.getLength(); csvIndex++) {
                    double waves = scanResults.getWavelength()[csvIndex];
                    int intens = scanResults.getUncalibratedIntensity()[csvIndex];
                    float absorb = (-1) * (float) Math.log10((double) scanResults.getUncalibratedIntensity()[csvIndex] / (double) scanResults.getIntensity()[csvIndex]);
                    float reflect = (float) results.getUncalibratedIntensity()[csvIndex] / results.getIntensity()[csvIndex];
                    data.add(new String[]{String.valueOf(waves), String.valueOf(intens), String.valueOf(absorb), String.valueOf(reflect)});
                }
                writer.writeAll(data);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Overwrite the black list
     * @param saveOS boolean indicating if the CSV file should be saved to the OS
     */
    private void writeBlacklist(boolean saveOS) {
        String prefix = "Blacklist";

        if (saveOS) {
            String fileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + prefix + /*currentTime +*/ ".txt";
            String msg = "Hello";
            //FileOutputStream outputStream;

            try {
                BufferedWriter bfw = new BufferedWriter(new FileWriter(fileName, false));
                bfw.write(msg);
                bfw.newLine();
                bfw.flush();
                bfw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * Add to the black list
     * @param pill the information to save
     * @param saveOS boolean indicating if the CSV file should be saved to the OS
     */
    private void addBlacklist(String pill, boolean saveOS) {
        //使用FileWriter类往文本文件中追加信息
        String prefix = "Blacklist";

        if (saveOS) {
            String fileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + prefix + /*currentTime +*/ ".txt";
            try {
                BufferedWriter bfw = new BufferedWriter(new FileWriter(fileName, true));
                bfw.write(pill);
                bfw.newLine();
                bfw.flush();
                bfw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {

            //Get a reference to the service from the service connection
            mNanoBLEService = ((NanoBLEService.LocalBinder) service).getService();

            //initialize bluetooth, if BLE is not available, then finish
            if (!mNanoBLEService.initialize()) {
                finish();
            }

            //Start scanning for devices that match DEVICE_NAME
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            if(mBluetoothLeScanner == null){
                finish();
                Toast.makeText(NewScanActivity.this, "Please ensure Bluetooth is enabled and try again", Toast.LENGTH_SHORT).show();
            }
            mHandler = new Handler();
            if (SettingsManager.getStringPref(mContext, SettingsManager.SharedPreferencesKeys.preferredDevice, null) != null) {
                preferredDevice = SettingsManager.getStringPref(mContext, SettingsManager.SharedPreferencesKeys.preferredDevice, null);
                scanPreferredLeDevice(true);
            } else {
                scanLeDevice(true);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mNanoBLEService = null;
        }
    };

    /**
     * Callback function for Bluetooth scanning. This function provides the instance of the
     * Bluetooth device {@link BluetoothDevice} that was found, it's rssi, and advertisement
     * data (scanRecord).
     * <p>
     * When a Bluetooth device with the advertised name matching the
     * string DEVICE_NAME {@link NewScanActivity#DEVICE_NAME} is found, a call is made to connect
     * to the device. Also, the Bluetooth should stop scanning, even if
     * the {@link NanoBLEService#SCAN_PERIOD} has not expired
     */
    private final ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            String name = device.getName();
            if (name != null) {
                if (device.getName().equals(DEVICE_NAME)) {
                    mNanoBLEService.connect(device.getAddress());
                    connected = true;
                    scanLeDevice(false);
                }
            }
        }
    };

    /**
     * Callback function for preferred Nano scanning. This function provides the instance of the
     * Bluetooth device {@link BluetoothDevice} that was found, it's rssi, and advertisement
     * data (scanRecord).
     * <p>
     * When a Bluetooth device with the advertised name matching the
     * string DEVICE_NAME {@link NewScanActivity#DEVICE_NAME} is found, a call is made to connect
     * to the device. Also, the Bluetooth should stop scanning, even if
     * the {@link NanoBLEService#SCAN_PERIOD} has not expired
     */
    private final ScanCallback mPreferredLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            String name = device.getName();
            if (name != null) {
                if (device.getName().equals(DEVICE_NAME)) {
                    if (device.getAddress().equals(preferredDevice)) {
                        mNanoBLEService.connect(device.getAddress());
                        connected = true;
                        scanPreferredLeDevice(false);
                    }
                }
            }
        }
    };

    /**
     * Scans for Bluetooth devices on the specified interval {@link NanoBLEService#SCAN_PERIOD}.
     * This function uses the handler {@link NewScanActivity#mHandler} to delay call to stop
     * scanning until after the interval has expired. The start and stop functions take an
     * LeScanCallback parameter that specifies the callback function when a Bluetooth device
     * has been found {@link NewScanActivity#mLeScanCallback}
     *
     * @param enable Tells the Bluetooth adapter {@link KSTNanoSDK#mBluetoothAdapter} if
     *               it should start or stop scanning
     */
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            Log.d("scanLeDevice", "NSA 1434");
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(mBluetoothLeScanner != null) {
                        mBluetoothLeScanner.stopScan(mLeScanCallback);
                        if (!connected) {
                            notConnectedDialog();
                        }
                    }
                }
            }, NanoBLEService.SCAN_PERIOD);
            if(mBluetoothLeScanner != null) {
                mBluetoothLeScanner.startScan(mLeScanCallback);
            }else{
                finish();
                Toast.makeText(NewScanActivity.this, "Please ensure Bluetooth is enabled and try again", Toast.LENGTH_SHORT).show();
            }
        } else {
            mBluetoothLeScanner.stopScan(mLeScanCallback);
        }
    }

    /**
     * Scans for preferred Nano devices on the specified interval {@link NanoBLEService#SCAN_PERIOD}.
     * This function uses the handler {@link NewScanActivity#mHandler} to delay call to stop
     * scanning until after the interval has expired. The start and stop functions take an
     * LeScanCallback parameter that specifies the callback function when a Bluetooth device
     * has been found {@link NewScanActivity#mPreferredLeScanCallback}
     *
     * @param enable Tells the Bluetooth adapter {@link KSTNanoSDK#mBluetoothAdapter} if
     *               it should start or stop scanning
     */
    private void scanPreferredLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            Log.d("scanPreferredLeDevice", "NSA 1470");
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothLeScanner.stopScan(mPreferredLeScanCallback);
                    if (!connected) {

                        scanLeDevice(true);
                    }
                }
            }, NanoBLEService.SCAN_PERIOD);
            mBluetoothLeScanner.startScan(mPreferredLeScanCallback);
        } else {
            mBluetoothLeScanner.stopScan(mPreferredLeScanCallback);
        }
    }

    /**
     * Dialog that tells the user that a Nano is not connected. The activity will finish when the
     * user selects ok
     */
    private void notConnectedDialog() {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(mContext.getResources().getString(R.string.not_connected_title));
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage(mContext.getResources().getString(R.string.not_connected_message));

        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
                finish();
            }
        });

        alertDialog = alertDialogBuilder.create();
        alertDialog.show();

    }

    /**
     * Custom receiver for receiving calibration coefficient data.
     */
    public class requestCalCoeffReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            intent.getIntExtra(KSTNanoSDK.EXTRA_REF_CAL_COEFF_SIZE, 0);
            Boolean size = intent.getBooleanExtra(KSTNanoSDK.EXTRA_REF_CAL_COEFF_SIZE_PACKET, false);
            if (size) {
                calProgress.setVisibility(View.INVISIBLE);
                barProgressDialog = new ProgressDialog(NewScanActivity.this);

                barProgressDialog.setTitle(getString(R.string.dl_ref_cal));
                barProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                barProgressDialog.setProgress(0);
                barProgressDialog.setMax(intent.getIntExtra(KSTNanoSDK.EXTRA_REF_CAL_COEFF_SIZE, 0));
                barProgressDialog.setCancelable(false);
                barProgressDialog.show();
            } else {
                barProgressDialog.setProgress(barProgressDialog.getProgress() + intent.getIntExtra(KSTNanoSDK.EXTRA_REF_CAL_COEFF_SIZE, 0));
            }
        }
    }

    /**
     * Custom receiver for receiving calibration matrix data. When this receiver action complete, it
     * will request the active configuration so that it can be displayed in the listview
     */
    public class requestCalMatrixReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            intent.getIntExtra(KSTNanoSDK.EXTRA_REF_CAL_MATRIX_SIZE, 0);
            Boolean size = intent.getBooleanExtra(KSTNanoSDK.EXTRA_REF_CAL_MATRIX_SIZE_PACKET, false);
            if (size) {
                barProgressDialog.dismiss();
                barProgressDialog = new ProgressDialog(NewScanActivity.this);

                barProgressDialog.setTitle(getString(R.string.dl_cal_matrix));
                barProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                barProgressDialog.setProgress(0);
                barProgressDialog.setMax(intent.getIntExtra(KSTNanoSDK.EXTRA_REF_CAL_MATRIX_SIZE, 0));
                barProgressDialog.setCancelable(false);
                barProgressDialog.show();
            } else {
                barProgressDialog.setProgress(barProgressDialog.getProgress() + intent.getIntExtra(KSTNanoSDK.EXTRA_REF_CAL_MATRIX_SIZE, 0));
            }
            if (barProgressDialog.getProgress() == barProgressDialog.getMax()) {

                LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(KSTNanoSDK.REQUEST_ACTIVE_CONF));
            }
        }
    }

    /**
     * Custom receiver for handling scan configurations
     */
    private class ScanConfReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Scan configuration received");

            byte[] smallArray = intent.getByteArrayExtra(KSTNanoSDK.EXTRA_DATA);
            byte[] addArray = new byte[smallArray.length * 3];
            byte[] largeArray = new byte[smallArray.length + addArray.length];

            System.arraycopy(smallArray, 0, largeArray, 0, smallArray.length);
            System.arraycopy(addArray, 0, largeArray, smallArray.length, addArray.length);

            Log.w("_JNI","largeArray Size: "+ largeArray.length);
            KSTNanoSDK.ScanConfiguration scanConf = KSTNanoSDK.KSTNanoSDK_dlpSpecScanReadConfiguration(intent.getByteArrayExtra(KSTNanoSDK.EXTRA_DATA));

            //activeConf = scanConf;
            //activeConf.setNumRepeats(1);

            barProgressDialog.dismiss();
            btn_scan.setClickable(true);
            btn_scan.setBackgroundColor(ContextCompat.getColor(mContext, R.color.kst_red));
            mMenu.findItem(R.id.action_settings).setEnabled(true);

            SettingsManager.storeStringPref(mContext, SettingsManager.SharedPreferencesKeys.scanConfiguration, scanConf.getConfigName());
            //tv_scan_conf.setText(scanConf.getConfigName());


        }
    }
    /**
     * Broadcast Receiver handling the disconnect event. If the Nano disconnects,
     * this activity should finish so that the user is taken back to the {@link ScanListActivity}
     * and display a toast message
     */
    public class DisconnReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(mContext, R.string.nano_disconnected, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    protected void onPause() {
        if(tts != null){

            tts.stop();
            tts.shutdown();
            sensorManager.unregisterListener(lightListener);
        }
        super.onPause();
    }

    public SensorEventListener lightListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int acc) { }

        public void onSensorChanged(SensorEvent event) {

            x = event.values[0];
            invalidateOptionsMenu();

            //luxText.setText((int)x + " lux");
            //final MenuItem menuItem = mMenu.findItem(R.id.action_lux);
            //luxText = (TextView) menuItem.getActionView();
            //luxText.setText("TJU");
        }
    };

    public void createToast(String warningType) {

        Log.d(TAG, "CreateToast");

        LinearLayout layout = new LinearLayout(this);
        layout.setBackgroundResource(R.color.white);
        TextView  tv = new TextView(this);
        ImageView img = new ImageView(this);

        // set the TextView properties like color, size etc
        tv.setTextColor(Color.BLACK);
        tv.setTextSize(20);
        tv.setGravity(Gravity.CENTER);
        layout.setOrientation(LinearLayout.VERTICAL);
        tv.setPadding(15, 0, 15, 60);

        if (warningType.equals("position")) {
            tv.setText("\n" + "Scan unsuccessful. Please make sure that the object is placed in the correct position!");
            tts.speak("Scan unsuccessful. Please make sure that the object is placed in the correct position!", TextToSpeech.QUEUE_FLUSH, null);
            img.setImageResource(R.mipmap.position);
            img.setScaleX(0.9f);
            img.setScaleY(0.9f);
            img.setPadding(0, 30, 0, 80);
        }
        else if (warningType.equals("scattering")) {
            tv.setText("\n"+"If the object is not transparent, please position the most even surface flat down!");
            tts.speak("If the object is not transparent, please position the most even surface flat down!", TextToSpeech.QUEUE_FLUSH, null);
            img.setImageResource(R.mipmap.uneven);
            img.setScaleX(0.9f);
            img.setScaleY(0.9f);
            img.setPadding(0, 30, 0, 80);
        }
        else if (warningType.equals("low confident")) {
            tv.setText("\n"+"The result might be inaccurate because the confident rate is relatively low!");
            img.setImageResource(R.mipmap.uncertain);
            img.setScaleX(0.9f);
            img.setScaleY(0.9f);
            img.setPadding(0, 30, 0, 80);
        }
        else if (warningType.equals("Dangerous")) {
            tv.setText("\n"+"The result might be not safe because it is in the black list!");
            img.setImageResource(R.mipmap.alert);
            img.setScaleX(0.9f);
            img.setScaleY(0.9f);
        }
        else if (warningType.equals("light")) {
            tv.setText("\n" + "The result might be inaccurate because there is a lot of light in the area!");
            tts.speak("The result might be inaccurate because there is a lot of light in the area!", TextToSpeech.QUEUE_FLUSH, null);
            img.setImageResource(R.mipmap.light);
            img.setScaleX(0.9f);
            img.setScaleY(0.9f);
        }else if (warningType.equals("muchlight")) {
            tv.setText("\n" + "Scan unsuccessful. There is too much light in the area for accurate scanning. Please move to a dimmer location!");
            tts.speak("Scan unsuccessful. There is too much light in the area for accurate scanning. Please move to a dimmer location!", TextToSpeech.QUEUE_FLUSH, null);
            img.setImageResource(R.mipmap.light);
            img.setScaleX(0.9f);
            img.setScaleY(0.9f);
        }
        else {
            tv.setText("\n" + "Scan unsuccessful!");
            tts.speak("Scan unsuccessful. There is too much light in the area for accurate scanning. Please move to a dimmer location!", TextToSpeech.QUEUE_FLUSH, null);
            img.setImageResource(R.mipmap.light);
            img.setScaleX(0.9f);
            img.setScaleY(0.9f);
        }

        // add both the Views TextView and ImageView in layout
        layout.addView(img);
        layout.addView(tv);
        GradientDrawable border = new GradientDrawable();
        border.setColor(0xFFFFFFFF); //white background
        border.setStroke(3, 0xFF000000); //black border with full opacity
        layout.setBackground(border);

        toast = new Toast(this); //context is object of Context write "this" if you are an Activity

        // Set The layout as Toast View
        toast.setView(layout);

        // Position you toast here toast position is 50 dp from bottom you can give any integral value
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.setDuration(Toast.LENGTH_LONG);

        calProgress.setVisibility(View.INVISIBLE);
        textProgress.setVisibility(View.INVISIBLE);
        //toast.show();


        Handler handlerTimer = new Handler();
       /*handlerTimer.postDelayed(new Runnable(){
           public void run() {
               toast.show();
           }}, 3800);*/

        // Set the countdown to display the toast

        toastCountDown = new CountDownTimer(10000, 1000 /*Tick duration*/) {
            public void onTick(long millisUntilFinished) {
                toast.show();
            }
            public void onFinish() {
                toast.cancel();
            }
        };

        // Show the toast and starts the countdown
        toast.show();
        toastCountDown.start();

    }
}




