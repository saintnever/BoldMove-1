/*
 * Copyright (C) 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.wearable.jumpingjack;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.viewpager.widget.ViewPager;
import androidx.wear.ambient.AmbientModeSupport;
import androidx.wear.widget.CircularProgressLayout;

import com.example.android.wearable.jumpingjack.fragments.FunctionOneFragment;
import com.example.android.wearable.jumpingjack.fragments.FunctionThreeFragment;
import com.example.android.wearable.jumpingjack.fragments.FunctionTwoFragment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static java.lang.Math.abs;
import static java.lang.Math.log;

/**
 * The main activity for the Jumping Jack application. This activity registers itself to receive
 * sensor values.
 *
 * This activity includes a {@link ViewPager} with two pages, one that
 * shows the current count and one that allows user to reset the counter. the current value of the
 * counter is persisted so that upon re-launch, the counter picks up from the last value. At any
 * stage, user can set this counter to 0.
 */
public class MainActivity extends FragmentActivity
        implements AmbientModeSupport.AmbientCallbackProvider{

    private static final String TAG = "MainActivity";

    private SensorManager mSensorManager;
    private Sensor mAcceleratorSensor;
    private Sensor mGyroSensor;
    private Sensor mMagnetSensor;
    private int SENSOR_RATE_NORMAL = 20000;//Sensor sample rate =50Hz
    private long mLastTime = 0;
    private int timer=0;
    private Timer scrollTimer;
    private TimerTask scrollTask;
    private Timer waitTimer;
    private TimerTask waitTask;
    private boolean isHold=false;
    private int isTop=0;
    private int isBottom=0;
    private TextView gestureText;
    private TextView counterText;
    private PagerAdapter adapter;
    private int previousTime=0;
    private float roll;
    private int i=0;
    private int session = 0;
    private int functionOrder=1;
    private int functionTime =1;
    private int context =1;
    private int trial = 0;
    private int block =0;
    private Integer[][] blocks_StudyOne;
    private List<List<Integer>> randomBlocks_StudyOne;

    private ViewPager mPager;
    private FunctionOneFragment mCounterPage;
    private FunctionThreeFragment mLeftSwipeCounterPage;
    private FunctionTwoFragment mSettingPage;
    private ImageView mSecondIndicator;
    private ImageView mFirstIndicator;
    private ImageView mThirdIndicator;

    /**Bluetooth setup*/
    // Initializes Bluetooth adapter.
    private BluetoothAdapter bluetoothAdapter;
    private final static int REQUEST_ENABLE_BT = 1;
    private boolean mScanning;
    private Handler handler;
    private BluetoothLeScanner lescanner;
    private ScanSettings settings;
    private List<ScanFilter> filters = new ArrayList<ScanFilter>();
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 300000;
    private byte[] manudata= new byte[4];
    private byte  BUTTON_LEFT = 0;
    private byte  BUTTON_RIGHT= 0;
    private byte  BUTTON_TRIGGER = 0;
    private byte  SLIDER_TOUCH = 0;
    private byte  SLIDER_VALUE = 0;
    private byte TOGGLE = 0;
    private byte PREVIOUS_BUTTON_LEFT=0;
    private byte PREVIOUS_BUTTON_RIGHT=0;
    private byte PREVIOUS_SLIDER_TOUCH=0;
    private byte PREVIOUS_SLIDER_VALUE=0;
    private byte PREVIOUS_TOGGLE=0;

    private float[] gravity= new float[3];
    private float[] linear_acceleration= new float[3];
    private float[] gyro=new float[3];
    private float[] accelerator=new float[3];
    private float[] magnet=new float[3];
    private double[] RA=new double[3]; //Relative Accelerator
    private float AO=0.0f; //Absolute Orientation
    private double[] acc=new double[3];
    private String mPosition = POSITION_UNKNOWN;

    public static final String POSITION_UNKNOWN = "Unkown";
    private final String POSITION_TOP="up";
    private final String POSITION_BOTTOM="down";
    private final String POSITION_LEFT="left";
    private final String POSITION_RIGHT="right";
    private final String POSITION_FORWARD="push";
    private final String STATION_DISCRETE_DETECTING="Detecting discrete gestures";
    private final String STATION_SELECTING="Selecting functions";
    private final String STATION_CONTINUOUS_SELECTING="Selecting continuous functions";
    private final String STATION_CONTINUOUS_DETECTING="Detecting continuous gestures";
    private String mStation=STATION_DISCRETE_DETECTING;

    private CircularProgressLayout circularProgress;
    private int layoutId;
    private function current_function;
    private boolean stopfunction = false;
    List<function> all_functions = new ArrayList<>();
    int[] device_states;

    // wifi
    static int PORT = 11121;
    Socket socket = null;
    BufferedReader reader;
    PrintWriter writer;
    boolean listening;
    String tmp_s;
    String ip = "192.168.43.224";
    log_data log_trial = new log_data();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AmbientModeSupport.attach(this);

        blocks_StudyOne = new Integer[][]{{1, 0}, {1, 2}, {1, 4},{2,0},{2,2},{2,4},{3,0},{3,2},{3,4}};
        randomBlocks_StudyOne = new ArrayList<>();
        for (Integer[] ints : blocks_StudyOne) {
            randomBlocks_StudyOne.add(Arrays.asList(ints));
        }

        try {
            all_functions = assembly_functions(1, 0, block, 0);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        device_states = new int[all_functions.size()];


        handler= new Handler();
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if(socket == null) {
            new NetworkAsyncTask().execute(ip);
        }
        send("New Experiment\n");
        setupstartview(block);

    }

    /**Register sensor listener*/
    @Override
    protected void onResume() {
        super.onResume();
        Log.i("resume", "resume");
        if (socket == null){
            new NetworkAsyncTask().execute(ip);
        }
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else{
            lescanner = bluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder()//
                           .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)//
                           .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)//
                           .build();
//            ScanFilter namefilter = new ScanFilter.Builder().setManufacturerData(0x0059, new byte[]{0x00, 0x59}, new byte[]{(byte) 0xFF, (byte) 0xFF}).build();
            ScanFilter namefilter = new ScanFilter.Builder().setDeviceName("BoldMove1").build();

            filters.add(namefilter);
            scanLeDevice(true);

        }
    }

    /**Unregister sensor listener*/
    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        disconnect();
    }

    @Override
    public void setContentView(int layoutResID) {
        this.layoutId = layoutResID;
        super.setContentView(layoutResID);
    }

    private void setupstartview(int block_num){
        setContentView(R.layout.session_start);
        TextView block_textview = findViewById(R.id.block_num);
        TextView session_textview = findViewById(R.id.session_num);


        // reinitialize states of all devices
        for (function f:all_functions
        ) {
            device_states[f.get_id()] = 0;
        }
        // display block number
        if (block_num < 4) {
            block_textview.setText("Block" + block_num);
        }
        else{
            block_textview.setText("Session "+session+"Finished!");
            block = 0;
            session = session + 1;
        }
        // display session number
        if (session < 2){
            session_textview.setText("Session "+session);
        }
        else{
            session_textview.setText("Experiment Finished");
            send("Experiment Finished!\n");
            disconnect();
        }

        Button start_button = findViewById(R.id.button_start);
        start_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int cnt = 10;
                if (socket == null){
                    new NetworkAsyncTask().execute(ip);
                }
//                while (socket == null && cnt > 0) {
//                    new NetworkAsyncTask().execute(ip);
//                    if (socket.isConnected()){
//                        break;
//                    }
//                    Log.e("wifi", "socket not connected!");
//                    cnt -= 1;
//                }

                setupTrialview(block, trial);
            }
        });

        Collections.shuffle(randomBlocks_StudyOne);
    }

    private void setupTrialview(int block_num, int trial_num){
        setContentView(R.layout.block_layout);
        TextView block_textview = findViewById(R.id.block);
        TextView task_textview = findViewById(R.id.task);
        String blocktext = "Block "+ block_num;
        String tasktext = "Trial "+ trial_num;

        block_textview.setText(blocktext);
        task_textview.setText(tasktext);

        Log.d("view", Integer.toString(layoutId));
    }

    private void setupfunctionview(int trial_num, int semantic, int pressed, int slider_value){
        if (pressed == 1 && layoutId == R.layout.block_layout) {
            log_trial.timestamp_pressed = System.currentTimeMillis();

            setContentView(R.layout.circular_timer);

            int functionOrder= randomBlocks_StudyOne.get(trial_num).get(1);
            int functionTime = randomBlocks_StudyOne.get(trial_num).get(0);

            log_trial.configure = new int[]{functionOrder, functionTime};
            int index = 0;
//            //写入log文件，当前参数
//            Log.d("currentSettings", functionOrder+" "+ functionTime +" "+ task +" "+session);

            final List<function> functions = functionList(semantic, trial_num, functionOrder);
            for (int j = 0; j < functions.size(); j++) {
                log_trial.func_id[j] = functions.get(j).get_id();
            }
            log_trial.funcid_target = log_trial.func_id[functionOrder];

            circularProgress = (CircularProgressLayout) findViewById(R.id.circular_progress);
            circularProgress.setTotalTime(functionTime * 1000);
            stopfunction = false;
            if (session == 1) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
            updatefunctionview(index, functions, circularProgress);

        }

        if (pressed == 0 && layoutId == R.layout.circular_timer){
            log_trial.timestamp_selected = System.currentTimeMillis();
            log_trial.funcid_selected = current_function.get_id();
            circularProgress.stopTimer();
            circularProgress.setVisibility(View.INVISIBLE);
            stopfunction = true;
            // deal with state display
            final int functionid = current_function.get_id();
            int temp_stateid = device_states[functionid];
            if (semantic == 0){
                temp_stateid -= 1;
                if (temp_stateid < 0){
                    temp_stateid = current_function.get_state().length - 1;
                }
            }

            if (semantic == 1 || semantic == 2){
                temp_stateid += 1;
                if (temp_stateid > current_function.get_state().length - 1){
                    temp_stateid = 0;
                }
            }

            TextView state = findViewById(R.id.state);
            state.setText(current_function.get_state()[temp_stateid]);
            // make buttons visible
            Button redo = findViewById(R.id.redo);
//            Button nextTrial = findViewById(R.id.nextTrial);
            redo.setVisibility(View.VISIBLE);
//            nextTrial.setVisibility(View.VISIBLE);

            View func_view = findViewById(R.id.func_select);
            final int finalTemp_stateid = temp_stateid;
            func_view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    device_states[functionid] = finalTemp_stateid;
                    log_trial.session = session;
                    log_trial.block = block;
                    log_trial.trial = trial;
                    if (socket == null){
                        new NetworkAsyncTask().execute(ip);
                    }
                    else{
                        Log.d("socket", String.valueOf(socket.isConnected()));
                    }
                    send(log_trial.assemby_send_string());
                    log_trial = new log_data();

                    trial = trial + 1;
                    if (trial == randomBlocks_StudyOne.size()){
                        block = block + 1;
                        trial = 0;
                        setupstartview(block);
                    }
                    else{
                        setupTrialview(block, trial);
                    }
                }
            });

            redo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    log_trial = new log_data();
                    setupTrialview(block, trial);
                }
            });

//            nextTrial.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    log_trial.session = session;
//                    log_trial.block = block;
//                    log_trial.trial = trial;
//                    if (socket == null){
//                        new NetworkAsyncTask().execute(ip);
//                    }
//                    else{
//                        Log.d("socket", String.valueOf(socket.isConnected()));
//                    }
//                    send(log_trial.assemby_send_string());
//                    log_trial = new log_data();
//
//                    trial = trial + 1;
//                    if (trial == randomBlocks_StudyOne.size()){
//                        block = block + 1;
//                        trial = 0;
//                        setupstartview(block);
//                    }
//                    else{
//                        setupTrialview(block, trial);
//                    }
//                }
//            });
        }

        // for slider selection
        if (pressed == 2 && layoutId == R.layout.circular_timer){
            log_trial.timestamp_selected = System.currentTimeMillis();
            circularProgress.stopTimer();
            circularProgress.setVisibility(View.INVISIBLE);
            stopfunction = true;
            // make buttons visible
            TextView svalue = findViewById(R.id.state);
            String[] scale = current_function.get_state();

            int min  = Integer.parseInt(scale[0], 10);
            int max = Integer.parseInt(scale[scale.length-1], 10);
            int scaled_value = min + (max-min) * (SLIDER_VALUE-0)/ 15;

            device_states[current_function.get_id()] = scaled_value - min;
            //Log.d("scale", scale[0]+"-"+scale[scale.length-1]+"-"+min+"-"+max+"-"+scaled_value);
            svalue.setText(Integer.toString(scaled_value));
        }
    }

    private void  updatefunctionview(final int index, final List<function> functions, CircularProgressLayout layout){
        if (!stopfunction) {
            current_function = functions.get(index);
            layout.stopTimer();
            ImageView imageDevice = findViewById(R.id.device);
            Log.d("funcupdate", Integer.toString(index));
            if (functions.get(index).get_imageid() != null) {
                imageDevice.setImageResource(functions.get(index).get_imageid());
            }

            TextView nameFunction = findViewById(R.id.function);
            nameFunction.setText(functions.get(index).get_name());

            TextView state = findViewById(R.id.state);
            state.setText(current_function.get_state()[device_states[current_function.get_id()]]);

            layout.setOnTimerFinishedListener(new CircularProgressLayout.OnTimerFinishedListener() {
                @Override
                public void onTimerFinished(CircularProgressLayout layout) {
                    if (index == functions.size() - 1) {
                        updatefunctionview(0, functions, layout);
                    } else {
                        updatefunctionview(index + 1, functions, layout);
                    }
                }
            });
            layout.startTimer();
            log_trial.timestamp_func_start[index] = System.currentTimeMillis();
        }
        else{
            layout.stopTimer();
        }
    };

    ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (callbackType != ScanSettings.CALLBACK_TYPE_ALL_MATCHES) {
                // Should not happen.
                Log.e(TAG, "LE Scan has already started");
                return;
            }
            ScanRecord scanRecord = result.getScanRecord();
            if (scanRecord == null) {
                return;
            }
//            Log.d("blescan", scanRecord.toString());
            manudata = scanRecord.getManufacturerSpecificData(0x0059);
            //Log.d("manudata", Arrays.toString(manudata));
//            if (manudata != null) {
//                send("ble scan results" + manudata[0] + manudata[1] + manudata[2] + manudata[3]);
//            }
            int[] inputs = getTouchInput(manudata);
            if (inputs[0] > -1 && inputs[1] > -1) {
                Log.d("manudata",Integer.toString(inputs[0])+Integer.toString(inputs[1]));
                setupfunctionview(trial, inputs[0], inputs[1], SLIDER_VALUE);
            }
//            try {
//                ubiTouchStatus();
//            } catch (IOException | JSONException e) {
//                e.printStackTrace();
//            }
//            callback.onLeScan(result.getDevice(), result.getRssi(),
//                    scanRecord.getBytes());
        }
    };

    private int[] getTouchInput(byte[] advdata){
        int semantic = -1;
        int pressed = -1; //1 pressed, 0 released, 2 dragged
        // Previous
        if (BUTTON_LEFT != advdata[0]){
            semantic = 0;
            BUTTON_LEFT = advdata[0];
            if (advdata[0] == 0){
                // button released
                pressed = 0;
            }
            else{
                // button pressed
                pressed = 1;
            }
        };
        // Next
        if (BUTTON_RIGHT != advdata[1]){
            semantic = 1;
            BUTTON_RIGHT = advdata[1];
            if (advdata[1] == 0){
                // button released
                pressed = 0;
            }
            else{
                // button pressed
                pressed = 1;
            }
        };
        // Tigger
        if (BUTTON_TRIGGER != advdata[2]){
            semantic = 2;
            BUTTON_TRIGGER = advdata[2];
            if (advdata[2] == 0){
                // button released
                pressed = 0;
            }
            else{
                // button pressed
                pressed = 1;
            }
        };
        // Slider
        if (SLIDER_TOUCH != getByteValues(advdata[3])[0]){
            semantic = 3;
            SLIDER_TOUCH = getByteValues(advdata[3])[0];//获得前四位值
            SLIDER_VALUE = getByteValues(advdata[3])[1];//获得后四位值
            if (SLIDER_TOUCH == 0){
                // slider released
                pressed = 0;
            }
            else{
                // slider pressed
                pressed = 1;
            }
        };

        if (abs(SLIDER_VALUE-getByteValues(advdata[3])[1])>5){  // deal with noises
            semantic = 3;
            SLIDER_VALUE = getByteValues(advdata[3])[1];//获得后四位值
            // slider pressed
            pressed = 2;
        };

        return new int[] {semantic, pressed};
    }
    /**Get first four bits and last four bits values*/
    public static byte[] getByteValues(byte b) {
        byte[] array = new byte[2];
        for (int i = 1; i >= 0; i--) {
            array[i] = (byte)(b & 15);
            b = (byte) (b >> 4);
        }
        return array;
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mScanning = true;
            lescanner.startScan(filters, settings, scanCallback);
            //lescanner.startScan(scanCallback);

        } else {
            mScanning = false;
            lescanner.stopScan(scanCallback);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    /**Redefined function list*/
    private List<function> assembly_functions(int study, int session, int block, int semantic) throws IOException, JSONException {
        InputStream jsonStream = getAssets().open("functions_study.json");
        JSONObject jsonObject = new JSONObject(Utils.convertStreamToString(jsonStream));
        JSONArray json_scenarios = new JSONArray();
        Context context = getApplicationContext();
        if (study == 1) {
             json_scenarios = jsonObject.getJSONArray("functions_study" + Integer.toString(study));
        }
        else if (study == 2){
            json_scenarios = jsonObject.getJSONArray("functions_study" + Integer.toString(study) + "_scenario"+Integer.toString(block));
        }
        List<function> functions = new ArrayList<function>();
        for (int i = 0; i < json_scenarios.length(); i++) {
            functions.add(new function(json_scenarios.getJSONObject(i), context));
        }
        return functions;
    }

    private List<function> extract_semantic_functions (List<function> block_functions, int semantic){
        List<function> mapping_functions = new ArrayList<>();
        for (function item:
             block_functions) {
            for (int i:item.get_semantic()
                 ) {
                if (i == semantic){
                    mapping_functions.add(item);
                    break;
                }
            }
        }
        return mapping_functions;
    }


    /**Predefined function list*/
    private List<function> functionList(int semantic,int block, int functionOrder){
        List<function> semantic_functions = extract_semantic_functions(all_functions, semantic);
        Collections.shuffle(semantic_functions);
        function target_function = new function();
        for (function item:semantic_functions) {
            if (item.get_id() < 4){
                target_function = item;
                semantic_functions.remove(item);
                break;
            }
        }
        semantic_functions.add(functionOrder, target_function);
        return semantic_functions;
    }

//    /**Real-time gesture display*/
//    private void function_display(List<function> functions){
//        setContentView(R.layout.circular_timer);
//
//        gestureText = findViewById(R.id.gesture);
//        gestureText.setText(initialText);
//
//        counterText=  findViewById(R.id.counter);
//        counterText.setText("第"+(block==9?1:(block+1))+"次");
//
//        if (scrollTimer != null) {
//            scrollTimer.cancel();
//            scrollTimer = null;
//        }
//        if (scrollTask != null) {
//            scrollTask.cancel();
//            scrollTask = null;
//        }
//
//        Button remove = (Button) this.findViewById(R.id.removeButton);
//        remove.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                //TODO Auto-generated method stub
//                if(block >0)
//                    block--;
//                counterText.setText("第"+(block==9?1:(block+1))+"次");
//                Log.i("buttonEvent", "removeButton被用户点击了。");
//            }
//        });
//    }
//
//    /**Scroll function list*/
//    private void setupScrollViews(String function1, String function2, String function3) {
//        setContentView(R.layout.jumping_jack_layout);
//        mPager = findViewById(R.id.pager);
//        mFirstIndicator = findViewById(R.id.indicator_0);
//        mSecondIndicator = findViewById(R.id.indicator_1);
//        mThirdIndicator=findViewById(R.id.indicator_2);
//
//        adapter = new PagerAdapter(getSupportFragmentManager());
//
//        mCounterPage = new FunctionOneFragment(function1);
//        mSettingPage = new FunctionTwoFragment(function2);
//        mLeftSwipeCounterPage=new FunctionThreeFragment(function3);
//
//        adapter.addFragment(mCounterPage);
//        adapter.addFragment(mSettingPage);
//        adapter.addFragment(mLeftSwipeCounterPage);
//
//        setIndicator(0);
//        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
//            @Override
//            public void onPageScrolled(int i, float v, int i2) {
//                // No-op.
//                Log.d(TAG, String.valueOf(i));
//            }
//
//            @Override
//            public void onPageSelected(int i) {
//
//                setIndicator(i);
//            }
//
//            @Override
//            public void onPageScrollStateChanged(int i) {
//                // No-op.
//            }
//        });
//
//        circularProgress = (CircularProgressLayout) findViewById(R.id.circular_progress);
////        circularProgress.setwidt(50);
//        circularProgress.setOnTimerFinishedListener(this);
//
//        circularProgress.setTotalTime(functionTime *1000);
//        // Start the timer
//        circularProgress.startTimer();
//
//        scrollTimer = new Timer();
//
//        /**Timer: page scroll every 2s*/
//        scrollTask = new TimerTask() {
//            @Override
//            public void run() {
//                // TODO Auto-generated method stub
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        circularProgress.stopTimer();
//                        mPager.setCurrentItem(mPager.getCurrentItem()==2?0:mPager.getCurrentItem()+1);
//                        //写入Log文件，每个选项出现时间
//                        Log.d("everyFunctionTime",String.valueOf(System.currentTimeMillis()));
//                        // Start the timer
//                        circularProgress.startTimer();
//                    }});}
//        };
//
//        scrollTimer.schedule(scrollTask, functionTime *1000, functionTime *1000);//every 2 seconds
//
//        mPager.setAdapter(adapter);
//    }
//
//    private void setText(String text)
//    {
//        if(gestureText!=null)
//        {
//            gestureText.setText(text);
//        }
//    }
//
//    private void stopTimer(){
//        if (waitTimer != null) {
//            waitTimer.cancel();
//            waitTimer = null;
//        }
//        if (waitTask != null) {
//            waitTask.cancel();
//            waitTask = null;
//        }
//    }
//
//    /**Sets the page indicator for the ViewPager.*/
//    private void setIndicator(int i) {
//        switch (i) {
//            case 0:
//                mFirstIndicator.setImageResource(R.drawable.full_10);
//                mSecondIndicator.setImageResource(R.drawable.empty_10);
//                mThirdIndicator.setImageResource(R.drawable.empty_10);
//                break;
//            case 1:
//                mFirstIndicator.setImageResource(R.drawable.empty_10);
//                mSecondIndicator.setImageResource(R.drawable.full_10);
//                mThirdIndicator.setImageResource(R.drawable.empty_10);
//                break;
//            case 2:
//                mFirstIndicator.setImageResource(R.drawable.empty_10);
//                mSecondIndicator.setImageResource(R.drawable.empty_10);
//                mThirdIndicator.setImageResource(R.drawable.full_10);
//                break;
//        }
//    }

    @Override
    public AmbientModeSupport.AmbientCallback getAmbientCallback() {
        return new MyAmbientCallback();
    }


    /** Customizes appearance for Ambient mode. (We don't do anything minus default.) */
    private static class MyAmbientCallback extends AmbientModeSupport.AmbientCallback {
        /** Prepares the UI for ambient mode. */
        @Override
        public void onEnterAmbient(Bundle ambientDetails) {
            super.onEnterAmbient(ambientDetails);
        }

        /**
         * Updates the display in ambient mode on the standard interval. Since we're using a custom
         * refresh cycle, this method does NOT update the data in the display. Rather, this method
         * simply updates the positioning of the data in the screen to avoid burn-in, if the display
         * requires it.
         */
        @Override
        public void onUpdateAmbient() {
            super.onUpdateAmbient();
        }

        /** Restores the UI to active (non-ambient) mode. */
        @Override
        public void onExitAmbient() {
            super.onExitAmbient();
        }
    }

    void disconnect() {
        try {
            //if (reader != null) reader.close();
            //if (writer != null) writer.close();
            socket.close();
            socket = null;
            //text_connect_info.setText("disconnected");
        } catch (Exception e) {
            //text_connect_info.setText(e.toString());
        }
    }

    void send(String s) {
        tmp_s = s;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (socket != null) {
                    writer.write(tmp_s);
                    writer.flush();
                }
            }
        }).start();
    }
//
//    void recv(String s) {
//        Log.d("b2wdebug", "receive: " + s);
//        tmp_s = s;
//        activity_uithread.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                //text_0.setText(tmp_s);
//            }
//        });
//    }

    public static String bytesToHex(byte[] in) {
        final StringBuilder builder = new StringBuilder();
        for(byte b : in) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }


    @SuppressLint("StaticFieldLeak")
    class NetworkAsyncTask extends AsyncTask<String, Integer, String> {

        protected String doInBackground(String... params) {
            try {
                socket = new Socket(params[0], PORT);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
                Thread.sleep(300);
                writer.print("Client Send!");
                writer.flush();
                listening = false;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("b2wdebug", "listening");
                        while (listening) {
                            try {
                                String s = reader.readLine();
                                if (s == null) listening = false;
//                                recv(s);
                            } catch (Exception e) {
                                Log.d("b2wdebug", "listen thread error: " + e.toString());
                                listening = false;
                                break;
                            }
                        }
//                        activity_uithread.runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                disconnect();
//                            }
//                        }
//                        );
                    }
                }).start();
                return socket.toString();
            } catch (Exception e) {
                socket = null;
                return e.toString();
            }
        }

        protected void onPostExecute(String string) {
            Log.d("b2wdebug", "connect info: " + string);
            //text_connect_info.setText(string);
        }
    }

}
