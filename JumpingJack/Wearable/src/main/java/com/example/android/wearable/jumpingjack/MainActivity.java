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

import android.app.Activity;
import android.app.ListActivity;
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
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.ViewPager;
import androidx.wear.ambient.AmbientModeSupport;
import androidx.wear.widget.CircularProgressLayout;

import com.example.android.wearable.jumpingjack.fragments.FunctionOneFragment;
import com.example.android.wearable.jumpingjack.fragments.FunctionThreeFragment;
import com.example.android.wearable.jumpingjack.fragments.FunctionTwoFragment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import com.example.android.wearable.jumpingjack.function;
import com.google.android.gms.common.util.Strings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
        implements AmbientModeSupport.AmbientCallbackProvider, SensorEventListener, CircularProgressLayout.OnTimerFinishedListener {

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
    private boolean isHold=false;
    private int isTop=0;
    private int isBottom=0;
    private TextView gestureText;
    private TextView counterText;
    private PagerAdapter adapter;
    private int previousTime=0;
    private float roll;
    private int i=0;
    private int session =1;
    private int functionOrder=1;
    private int functionTime =1;
    private int context =1;
    private int task =1;
    private int finishedBlocks=0;
    private int finishedTasks=0;
    private Integer[][] blocks_StudyOne;
    private Integer[] tasks_StudyOne;
    private List<List<Integer>> randomBlocks_StudyOne;
    private List<Integer> randomTasks_StudyOne;

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
    private LeDeviceListAdapter mLeDeviceListAdapter;
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
    private byte  SLIDER_TOUCH = 0;
    private byte  SLIDER_VALUE = 0;
    private byte PREVIOUS_BUTTON_LEFT=0;
    private byte PREVIOUS_BUTTON_RIGHT=0;
    private byte PREVIOUS_SLIDER_TOUCH=0;
    private byte PREVIOUS_SLIDER_VALUE=0;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AmbientModeSupport.attach(this);
        setContentView(R.layout.circular_timer);
        setupGestureViews("实验开始!");
        /*setupGestureViews(STATION_DISCRETE_DETECTING);

        gravity[0] = 0.0f;
        gravity[1] = 0.0f;
        gravity[2] = 0.0f;

        linear_acceleration[0] = 0.0f;
        linear_acceleration[1] = 0.0f;
        linear_acceleration[2] = 0.0f;

        gyro[0] = 0.0f;
        gyro[1] = 0.0f;
        gyro[2] = 0.0f;

        accelerator[0] = 0.0f;
        accelerator[1] = 0.0f;
        accelerator[2] = 0.0f;

        mPosition = POSITION_UNKNOWN;

        startSensor();

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(holdTimer, 0,200);//Sample rate =5Hz*/

        blocks_StudyOne = new Integer[][]{{1, 1}, {1, 3}, {1, 5},{2,1},{2,3},{2,5},{3,1},{3,3},{3,5}};
        randomBlocks_StudyOne = new ArrayList<>();
        for (Integer[] ints : blocks_StudyOne) {
            randomBlocks_StudyOne.add(Arrays.asList(ints));
        }
        Collections.shuffle(randomBlocks_StudyOne);

        tasks_StudyOne =new Integer[]{1,2,3,4};
        randomTasks_StudyOne =Arrays.asList(tasks_StudyOne);
        Collections.shuffle(randomTasks_StudyOne);

        handler= new Handler();
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        for(int i=0;i<4;i++){
            manudata[i] = 0x00;
        }

        try {
            List<function> functions = assembly_functions(2,1,1,0);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    /**Register sensor listener*/
    @Override
    protected void onResume() {
        super.onResume();
        Log.i("resume", "resume");
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
            ScanFilter namefilter = new ScanFilter.Builder().setDeviceName("BoldMove").build();
            filters.add(namefilter);
            scanLeDevice(true);
            mLeDeviceListAdapter = new LeDeviceListAdapter();

        }

        // Initializes list view adapter.
        /*
        if (mSensorManager.registerListener(this, mAcceleratorSensor,
                SENSOR_RATE_NORMAL)) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Successfully registered for the accelerator sensor updates");
            }
        }
        if (mSensorManager.registerListener(this, mGyroSensor,
                SENSOR_RATE_NORMAL)) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Successfully registered for the gyro sensor updates");
            }
        }
        if (mSensorManager.registerListener(this, mMagnetSensor,
                SENSOR_RATE_NORMAL)) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Successfully registered for the gyro sensor updates");
            }
        }*/
    }

    /**Unregister sensor listener*/
    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        /*mSensorManager.unregisterListener(this);
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Unregistered for sensor events");
        }*/
    }

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
            Log.d("blescan", scanRecord.toString());
            manudata = scanRecord.getManufacturerSpecificData(0x0059);
            Log.d("manudata", Arrays.toString(manudata));
            BUTTON_LEFT = manudata[0];
            BUTTON_RIGHT = manudata[1];
            SLIDER_TOUCH = manudata[2];
            SLIDER_VALUE = manudata[3];
            ubiTouchStatus();
//            callback.onLeScan(result.getDevice(), result.getRssi(),
//                    scanRecord.getBytes());
        }
    };

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
//                    bluetoothAdapter.stopLeScan(leScanCallback);
                    lescanner.stopScan(scanCallback);
                }
            }, SCAN_PERIOD);

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

    /**ubiTouch feedback*/
    private void ubiTouchStatus(){
        // get current task
        // if semantic matches task semantic
        // start selection
        // if type 1 interface
        //    start function selection
        // if type 2 interface
        //    if slider, start function selection
        //    if trigger/prev/next buttons, wait for 2s, then start function selection
        // if not
        // no feedback
        //如果是实验一，执行以下代码
        if(finishedBlocks==9){
            finishedBlocks=0;
            Collections.shuffle(randomBlocks_StudyOne);
            if(finishedTasks<3) {
                finishedTasks++;
            }else{
                finishedTasks=0;
                Collections.shuffle(randomTasks_StudyOne);
                if(session<2){
                    session++;
                }else{
                    //实验完成
                    setText("实验结束！");
                }
            }
        }

        functionOrder= randomBlocks_StudyOne.get(finishedBlocks).get(1);
        functionTime = randomBlocks_StudyOne.get(finishedBlocks).get(0);
        task = randomTasks_StudyOne.get(finishedTasks);
        //写入log文件
        Log.d("currentSettings", functionOrder+" "+ functionTime +" "+ task +" "+session);

        //实验一结束进行实验二么？还是编译成两个程序？
        //如果是实验二，执行以下代码
        //。。。。。。

    }

    /**Predefined function list*/
    private String[] functionList(int location,int semantic,int study,int functionOrder){
        String[] functions=new String[]{"","",""};
        switch(location){
            case 1:
                if(semantic==1){
                    functions[0]="LEFT\nLOCATION1\nFUNCTION1";
                    functions[1]="LEFT\nLOCATION1\nFUNCTION2";
                    functions[2]="LEFT\nLOCATION1\nFUNCTION3";
                    return functions;
                }else if(semantic==2){
                    functions[0]="RIGHT\nLOCATION1\nFUNCTION1";
                    functions[1]="RIGHT\nLOCATION1\nFUNCTION2";
                    functions[2]="RIGHT\nLOCATION1\nFUNCTION3";
                    return functions;
                }else if(semantic==3){
                    functions[0]="SLIDER\nLOCATION1\nFUNCTION1";
                    functions[1]="SLIDER\nLOCATION1\nFUNCTION2";
                    functions[2]="SLIDER\nLOCATION1\nFUNCTION3";
                    return functions;
                }
                break;
            case 2:
                if(semantic==1){
                    functions[0]="LEFT\nLOCATION2\nFUNCTION1";
                    functions[1]="LEFT\nLOCATION2\nFUNCTION2";
                    functions[2]="LEFT\nLOCATION2\nFUNCTION3";
                    return functions;
                }else if(semantic==2){
                    functions[0]="RIGHT\nLOCATION2\nFUNCTION1";
                    functions[1]="RIGHT\nLOCATION2\nFUNCTION2";
                    functions[2]="RIGHT\nLOCATION2\nFUNCTION3";
                    return functions;
                }else if(semantic==3){
                    functions[0]="SLIDER\nLOCATION2\nFUNCTION1";
                    functions[1]="SLIDER\nLOCATION2\nFUNCTION2";
                    functions[2]="SLIDER\nLOCATION2\nFUNCTION3";
                    return functions;
                }
                break;
            case 3:
                if(semantic==1){
                    functions[0]="LEFT\nLOCATION3\nFUNCTION1";
                    functions[1]="LEFT\nLOCATION3\nFUNCTION2";
                    functions[2]="LEFT\nLOCATION3\nFUNCTION3";
                    return functions;
                }else if(semantic==2){
                    functions[0]="RIGHT\nLOCATION3\nFUNCTION1";
                    functions[1]="RIGHT\nLOCATION3\nFUNCTION2";
                    functions[2]="RIGHT\nLOCATION3\nFUNCTION3";
                    return functions;
                }else if(semantic==3){
                    functions[0]="SLIDER\nLOCATION3\nFUNCTION1";
                    functions[1]="SLIDER\nLOCATION3\nFUNCTION2";
                    functions[2]="SLIDER\nLOCATION3\nFUNCTION3";
                    return functions;
                }
                break;
        }
        return functions;
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

    /**Start sensors*/
    private void startSensor() {
        if (mSensorManager == null) {
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        }
        if (mAcceleratorSensor == null) {
            /**LINEAR_ACCELERATION is required for algorithm 2*/
            //mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            mAcceleratorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        if (mGyroSensor == null) {
            mGyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }
        if(mMagnetSensor==null) {
            mMagnetSensor=mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }

    }

    /**Real-time gesture display*/
    private void setupGestureViews(String initialText){
        setContentView(R.layout.gesture_detection_layout);

        gestureText = findViewById(R.id.gesture);
        gestureText.setText(initialText);

        counterText=  findViewById(R.id.counter);
        counterText.setText(Integer.toString(context));

        circularProgress = (CircularProgressLayout) findViewById(R.id.circular_progress);
//        circularProgress.setwidt(50);
        circularProgress.setOnTimerFinishedListener(this);

        if (scrollTimer != null) {
            scrollTimer.cancel();
            scrollTimer = null;
        }
        if (scrollTask != null) {
            scrollTask.cancel();
            scrollTask = null;
        }

        Button add = (Button) this.findViewById(R.id.addButton);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO Auto-generated method stub
                if(context <3)
                    context++;
                counterText.setText(Integer.toString(context));

                // Two seconds to cancel the action
                circularProgress.setTotalTime(2000);
                // Start the timer
                circularProgress.startTimer();
                Log.i("buttonEvent", "addButton被用户点击了。");
            }
        });

        Button remove = (Button) this.findViewById(R.id.removeButton);
        remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO Auto-generated method stub
                if(context >1)
                    context--;
                counterText.setText(Integer.toString(context));
                Log.i("buttonEvent", "removeButton被用户点击了。");
            }
        });
    }


    /**Scroll function list*/
    private void setupScrollViews(String function1, String function2, String function3) {
        setContentView(R.layout.jumping_jack_layout);
        mPager = findViewById(R.id.pager);
        mFirstIndicator = findViewById(R.id.indicator_0);
        mSecondIndicator = findViewById(R.id.indicator_1);
        mThirdIndicator=findViewById(R.id.indicator_2);

        adapter = new PagerAdapter(getSupportFragmentManager());

        mCounterPage = new FunctionOneFragment(function1);
        mSettingPage = new FunctionTwoFragment(function2);
        mLeftSwipeCounterPage=new FunctionThreeFragment(function3);

        adapter.addFragment(mCounterPage);
        adapter.addFragment(mSettingPage);
        adapter.addFragment(mLeftSwipeCounterPage);

        setIndicator(0);
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {
                // No-op.
                Log.d(TAG, String.valueOf(i));
            }

            @Override
            public void onPageSelected(int i) {

                setIndicator(i);
            }

            @Override
            public void onPageScrollStateChanged(int i) {
                // No-op.
            }
        });

        scrollTimer = new Timer();

        /**Timer: page scroll every 2s*/
        scrollTask = new TimerTask() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPager.setCurrentItem(mPager.getCurrentItem()==2?0:mPager.getCurrentItem()+1);
                    }});}
        };

        scrollTimer.schedule(scrollTask, 2000,2000);//every 2 seconds

        mPager.setAdapter(adapter);
    }



    /**Get sensor data when data changed*/
    @Override
    public void onSensorChanged(SensorEvent event) {
        switch(event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                //Log.e(TAG, "Accelerator:   "+event.values[0]+"  "+event.values[1]+"   "+event.values[2]);
                accelerator=event.values;
                break;
            case Sensor.TYPE_GYROSCOPE:
                //Log.e(TAG, "Gyroscope:   "+event.values[0]+"  "+event.values[1]+"   "+event.values[2]);
                gyro=event.values;
                //RA=RelativeAccelerator(accelerator,gyro,event.timestamp);
                //inAirGesture();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                //Log.e(TAG, "Magnet:   "+event.values[0]+"  "+event.values[1]+"   "+event.values[2]);
                //AO=AbsoluteOrientation(accelerator,magnet);
                magnet=event.values;
                break;
        }
    }

    /**AbsoluteOrientation Algorithm: https://blog.csdn.net/u014702999/article/details/51483361*/
    private float AbsoluteOrientation(float[] AcceleratorValues,float[] MagnetValues ){
        if (AcceleratorValues != null && MagnetValues != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, AcceleratorValues, MagnetValues);
            if (success) {
                float orientation[] = new float[3];// orientation contains: azimut, pitch and roll
                SensorManager.getOrientation(R, orientation);
                roll = -(float)Math.toDegrees(orientation[2]);
                Log.e(TAG, "roll:   "+(int)(roll/4));
            }
        }
        return (int)(roll/4);
    }

    /**RelativeAccelerator Algorithm: https://w3c.github.io/motion-sensors/#complementary-filter*/
    private double[] RelativeAccelerator(float[] AcceleratorValues, float[] GyroValues, long timestamp){
        acc[0]=0;
        acc[1]=0;
        acc[2]=0;
        float bias=0.98f;
        long dt = (timestamp - mLastTime)/100000;
        mLastTime = timestamp;
        double norm = Math.sqrt(Math.pow(accelerator[0],2) + Math.pow(accelerator[1],2) + Math.pow(accelerator[2],2));
        double scale = Math.PI / 2;
        acc[0] = bias * (accelerator[0] + gyro[0] * dt) + (1.0 - bias) * (accelerator[0] * scale / norm);
        acc[1] = bias * (accelerator[1] + gyro[1] * dt) + (1.0 - bias) * (accelerator[1] * scale / norm);
        acc[2] = bias * (accelerator[2] + gyro[2] * dt) + (1.0 - bias) * (accelerator[2] * scale / norm);
        //Log.e(TAG, "Mulitsensors:   "+Math.round(accelerator[0])+"  "+Math.round(accelerator[1])+"   "+Math.round(accelerator[2]));

        return acc;
    }

    /**Station controller*/
    private void inAirGesture() {
        switch(mStation){
            case STATION_DISCRETE_DETECTING:
                SwipeDetection();
                //Detect wrist rotation
                if(gyro[0]<-5){
                    setupScrollViews("Function 1","Function 2","Function 3");
                    i=0;
                    mStation=STATION_CONTINUOUS_SELECTING;
                }
                break;
            case STATION_CONTINUOUS_DETECTING:
                setText(Float.toString(Math.round(AO)));
                if(Math.sqrt(Math.pow(RA[0],2)+Math.pow(RA[1],2)+Math.pow(RA[2],2))<30)
                {
                    isHold=true;
                }else{
                    isHold=false;
                }
                break;
            case STATION_CONTINUOUS_SELECTING:
                //Detect wrist rotation
               if(i>=50){
               if(gyro[0]<-5){
                    setupGestureViews("Result:\n"+mPager.getCurrentItem()+1);
                    Log.e(TAG, "Function"+(mPager.getCurrentItem()+1)+"\n"+mPosition);
                    mStation=STATION_CONTINUOUS_DETECTING;
                    i=0;
                }}else{
                   i++;
               }
                break;
            case STATION_SELECTING:
                ConfirmAndRestart();
                break;
                
        }
    }

    /**Detect discrete swipe gestures*/
    private void SwipeDetection(){
        if(Math.sqrt(Math.pow(RA[0],2)+Math.pow(RA[1],2)+Math.pow(RA[2],2))<30)
        {
            isHold=true;
        }else{
            isHold=false;
            /**Detect left/right/top/bottom swipe gestures*/
            if(Math.abs(RA[2])>=Math.abs(RA[0])&&Math.abs(RA[2])>=Math.abs(RA[1]))
            {
                if(RA[2]>200)
                {
                        Log.e(TAG, "Left");
                        mPosition=POSITION_LEFT;
                        setText(mPosition);
                }
                else if(RA[2]<-200)
                {
                        Log.e(TAG, "Right");
                        mPosition=POSITION_RIGHT;
                        setText(mPosition);
                }
            }else if(Math.abs(RA[1])>=Math.abs(RA[0])&&Math.abs(RA[1])>=Math.abs(RA[2]))
            {
                /**Detect push (Double click) gesture****************************/
                if((isTop==1||isBottom==1)&&previousTime!=0)
                {
                    previousTime=0;
                }
                previousTime++;
                if(previousTime==10)
                {
                    if(isTop>=3&&isBottom>=3){
                        Log.e(TAG, "Forward");
                        mPosition=POSITION_FORWARD;
                        setText(mPosition);
                        isTop=0;
                        isBottom=0;
                    }else{
                        isTop=0;
                        isBottom=0;
                    }
                }
                if(mPosition==POSITION_FORWARD){
                    if(previousTime>=100){
                        if(RA[1]<-200)
                        {
                                Log.e(TAG, "Top");
                                mPosition=POSITION_TOP;
                                setText(mPosition);
                                isTop++;
                        }
                        else if(RA[1]>200)
                        {
                                Log.e(TAG, "Bottom");
                                mPosition=POSITION_BOTTOM;
                                setText(mPosition);
                                isBottom++;
                        }
                    }
                }else{
                    /************************************************************/
                    if(RA[1]<-200)
                    {
                            Log.e(TAG, "Top");
                            mPosition=POSITION_TOP;
                            setText(mPosition);
                            isTop++;
                    }
                    else if(RA[1]>200)
                    {
                            Log.e(TAG, "Bottom");
                            mPosition=POSITION_BOTTOM;
                            setText(mPosition);
                            isBottom++;

                    }
                }
            }
        }
    }

    /**Detect hand down gesture to restart*/
    private void ConfirmAndRestart(){
        if(Math.abs(RA[1])>=Math.abs(RA[0])&&Math.abs(RA[1])>=Math.abs(RA[2])){
            if(RA[1]>200){
                mStation=STATION_DISCRETE_DETECTING;
                int currentPosition=mPager.getCurrentItem()+1;
                setupGestureViews("Result:\n"+currentPosition);
                //setText("Function"+currentPosition+"\n"+mPosition);
                Log.e(TAG, "Function"+currentPosition+"\n"+mPosition);
            }
        }
    }

    /**Timer: Execute every 200ms for hold gesture detection*/
    TimerTask holdTimer = new TimerTask() {
        @Override
        public void run() {
            // TODO Auto-generated method stub
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    /**Detect hold gesture to end swipe detection*/
                    if(isHold==true){
                        /**200ms*10=2s hold 2 seconds*/
                        if(timer>=10){
                            timer=0;
                            if(mStation==STATION_DISCRETE_DETECTING){
                                mStation=STATION_SELECTING;
                                setupScrollViews(mPosition,mPosition,mPosition);
                            }else if(mStation==STATION_CONTINUOUS_DETECTING){
                                Log.e(TAG, "Result:"+AO);
                                mStation=STATION_DISCRETE_DETECTING;
                                setText(mPosition);
                            }
                        }else{timer++;}
                    }else{
                        timer=0;
                    }
                }});}
    };


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No op.
    }

    private void setText(String text)
    {
        if(gestureText!=null)
        {
            gestureText.setText(text);
        }
    }

    /**Sets the page indicator for the ViewPager.*/
    private void setIndicator(int i) {
        switch (i) {
            case 0:
                mFirstIndicator.setImageResource(R.drawable.full_10);
                mSecondIndicator.setImageResource(R.drawable.empty_10);
                mThirdIndicator.setImageResource(R.drawable.empty_10);
                break;
            case 1:
                mFirstIndicator.setImageResource(R.drawable.empty_10);
                mSecondIndicator.setImageResource(R.drawable.full_10);
                mThirdIndicator.setImageResource(R.drawable.empty_10);
                break;
            case 2:
                mFirstIndicator.setImageResource(R.drawable.empty_10);
                mSecondIndicator.setImageResource(R.drawable.empty_10);
                mThirdIndicator.setImageResource(R.drawable.full_10);
                break;
        }
    }

    @Override
    public AmbientModeSupport.AmbientCallback getAmbientCallback() {
        return new MyAmbientCallback();
    }


    @Override
    public void onTimerFinished(CircularProgressLayout layout) {

    }

    /** Customizes appearance for Ambient mode. (We don't do anything minus default.) */
    private class MyAmbientCallback extends AmbientModeSupport.AmbientCallback {
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



    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;
        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = MainActivity.this.getLayoutInflater();
        }
        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }
        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }
        public void clear() {
            mLeDevices.clear();
        }
        @Override
        public int getCount() {
            return mLeDevices.size();
        }
        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }
        @Override
        public long getItemId(int i) {
            return i;
        }
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
//            DeviceScanActivity.ViewHolder viewHolder;
//            // General ListView optimization code.
//            if (view == null) {
//                view = mInflator.inflate(R.layout.listitem_device, null);
//                viewHolder = new DeviceScanActivity.ViewHolder();
//                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
//                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
//                view.setTag(viewHolder);
//            } else {
//                viewHolder = (DeviceScanActivity.ViewHolder) view.getTag();
//            }
//            BluetoothDevice device = mLeDevices.get(i);
//            final String deviceName = device.getName();
//            if (deviceName != null && deviceName.length() > 0)
//                viewHolder.deviceName.setText(deviceName);
//            else
//                viewHolder.deviceName.setText(R.string.unknown_device);
//            viewHolder.deviceAddress.setText(device.getAddress());
            return view;
        }
    }

}
