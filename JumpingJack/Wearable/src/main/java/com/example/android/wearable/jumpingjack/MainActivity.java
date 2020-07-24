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

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.ViewPager;
import androidx.wear.ambient.AmbientModeSupport;

import com.example.android.wearable.jumpingjack.fragments.FunctionOneFragment;
import com.example.android.wearable.jumpingjack.fragments.FunctionThreeFragment;
import com.example.android.wearable.jumpingjack.fragments.FunctionTwoFragment;

import java.util.Timer;
import java.util.TimerTask;

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
        implements AmbientModeSupport.AmbientCallbackProvider, SensorEventListener {

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
    private PagerAdapter adapter;
    private int previousTime=0;
    private float roll;
    private int i=0;

    private ViewPager mPager;
    private FunctionOneFragment mCounterPage;
    private FunctionThreeFragment mLeftSwipeCounterPage;
    private FunctionTwoFragment mSettingPage;
    private ImageView mSecondIndicator;
    private ImageView mFirstIndicator;
    private ImageView mThirdIndicator;

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
    private final String POSITION_LEFT="Left";
    private final String POSITION_RIGHT="Right";
    private final String POSITION_TOP="Top";
    private final String POSITION_BOTTOM="Bottom";
    private final String POSITION_FORWARD="Push";
    private final String STATION_DISCRETE_DETECTING="Detecting discrete gestures";
    private final String STATION_SELECTING="Selecting functions";
    private final String STATION_CONTINUOUS_SELECTING="Selecting continuous functions";
    private final String STATION_CONTINUOUS_DETECTING="Detecting continuous gestures";
    private String mStation=STATION_DISCRETE_DETECTING;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupGestureViews(STATION_DISCRETE_DETECTING);

        AmbientModeSupport.attach(this);

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
        timer.scheduleAtFixedRate(task, 0,200);//Sample rate =5Hz
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

        if (scrollTimer != null) {
            scrollTimer.cancel();
            scrollTimer = null;
        }
        if (scrollTask != null) {
            scrollTask.cancel();
            scrollTask = null;
        }
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

    /**Register sensor listener*/
    @Override
    protected void onResume() {
        super.onResume();
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
        }
    }

    /**Unregister sensor listener*/
    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Unregistered for sensor events");
        }
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
                RA=RelativeAccelerator(accelerator,gyro,event.timestamp);
                inAirGesture();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                //Log.e(TAG, "Magnet:   "+event.values[0]+"  "+event.values[1]+"   "+event.values[2]);
                AO=AbsoluteOrientation(accelerator,magnet);
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
    TimerTask task = new TimerTask() {
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

}
