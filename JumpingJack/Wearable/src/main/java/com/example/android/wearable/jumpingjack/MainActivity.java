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
import android.view.KeyEvent;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.ViewPager;
import androidx.wear.ambient.AmbientModeSupport;

import com.example.android.wearable.jumpingjack.fragments.CounterFragment;
import com.example.android.wearable.jumpingjack.fragments.SwipeDetectionFragment;
import com.example.android.wearable.jumpingjack.fragments.SettingsFragment;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

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

    public String resultPosition;

    private static final String TAG = "MainActivity";

    private SensorManager mSensorManager;
    private Sensor mAcceleratorSensor;
    private Sensor mGyroSensor;
    private Sensor mPPGSensor;
    private int SENSOR_RATE_NORMAL = 20000;//Sensor sample rate =50Hz
    private long mLastTime = 0;
    private int timer=0;
    private Timer scrollTimer;
    private TimerTask scrollTask;
    private int mJumpCounter = 0;
    private boolean mHandDown = true;
    private boolean isHold=false;
    private int isTop=0;
    private int isBottom=0;
    private TextView gestureText;
    private PagerAdapter adapter;
    private int previousTime=0;

    private ViewPager mPager;
    private CounterFragment mCounterPage;
    private SwipeDetectionFragment mLeftSwipeCounterPage;
    private SettingsFragment mSettingPage;
    private ImageView mSecondIndicator;
    private ImageView mFirstIndicator;
    private ImageView mThirdIndicator;

    private static final float POSITION_IN_BORDER = 10.0f;
    private float[] gravity= new float[3];
    private float[] linear_acceleration= new float[3];
    private float[] gyro=new float[3];
    private float[] accelerator=new float[3];
    private String mPosition = POSITION_UNKNOWN;

    public static final String POSITION_UNKNOWN = "Relax...";
    private final String POSITION_BEGIN="Start!";
    private final String POSITION_END="End!";
    private final String POSITION_LEFT="Left";
    private final String POSITION_RIGHT="Right";
    private final String POSITION_TOP="Top";
    private final String POSITION_BOTTOM="Bottom";
    private final String POSITION_FORWARD="Push";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gesture_detection_layout);
        gestureText = findViewById(R.id.gesture);
        gestureText.setText(POSITION_BEGIN);

        AmbientModeSupport.attach(this);

        mJumpCounter = Utils.getCounterFromPreference(this);

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

        mPosition = POSITION_BEGIN;

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
            /**LINEAR_ACCELERATION is needed fo algorithm 2*/
            //mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            mAcceleratorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        if (mGyroSensor == null) {
            mGyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }
        if(mPPGSensor==null){
            mPPGSensor=mSensorManager.getDefaultSensor(65537);
        }

    }

    /**Scroll function list*/
    private void setupViews() {
        setContentView(R.layout.jumping_jack_layout);
        mPager = findViewById(R.id.pager);
        mFirstIndicator = findViewById(R.id.indicator_0);
        mSecondIndicator = findViewById(R.id.indicator_1);
        mThirdIndicator=findViewById(R.id.indicator_2);

        adapter = new PagerAdapter(getSupportFragmentManager());

        mCounterPage = new CounterFragment(resultPosition);
        mSettingPage = new SettingsFragment();
        mLeftSwipeCounterPage=new SwipeDetectionFragment();

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
        if (mSensorManager.registerListener(this, mPPGSensor,
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
                inAIrSwipe(accelerator,gyro,event.timestamp);
                break;
            case 65537:
                //Log.e(TAG, "PPG Data: "+(event.values[2]*1000000000000000000L*1000000000000000000L*10000L));
                break;
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
                        timer++;
                        /**200ms*10=2s hold 2 seconds*/
                        if(timer>=10){
                            if(mPosition!=POSITION_BEGIN&&mPosition!=POSITION_END){
                                resultPosition=mPosition;
                                Log.e(TAG, "Hold");
                                mPosition=POSITION_END;
                                setupViews();
                                timer=0;
                            }}
                    }else{
                        timer=0;
                    }
                   /* if(isTop>=2 && isBottom>=2){
                        mPosition=POSITION_FORWARD;
                        Log.e(TAG, "Push");
                        setText(mPosition);
                        isTop=0;
                        isBottom=0;
                    }else{
                        isTop=0;
                        isBottom=0;
                    }*/
                }});}
    };

    /**Algorithm 1: https://w3c.github.io/motion-sensors/#complementary-filter*/
    private void inAIrSwipe(float[] AcceleratorValues, float[] GyroValues, long timestamp){
        double alpha = 0;
        double beta = 0;
        double gamma=0;
        float bias=0.98f;
        long dt = (timestamp - mLastTime)/100000;
        mLastTime = timestamp;
        double norm = Math.sqrt(Math.pow(accelerator[0],2) + Math.pow(accelerator[1],2) + Math.pow(accelerator[2],2));
        double scale = Math.PI / 2;
        alpha = bias * (alpha + gyro[0] * dt) + (1.0 - bias) * (accelerator[0] * scale / norm);
        beta = bias * (beta + gyro[1] * dt) + (1.0 - bias) * (accelerator[1] * scale / norm);
        gamma = bias * (gamma + gyro[2] * dt) + (1.0 - bias) * (accelerator[2] * scale / norm);
        Log.e(TAG, "Mulitsensors:   "+Math.round(alpha)+"  "+Math.round(beta)+"   "+Math.round(gamma));

        if(Math.sqrt(Math.pow(alpha,2)+Math.pow(beta,2)+Math.pow(gamma,2))<20)
        {
            isHold=true;
        }else{
            isHold=false;
                /**Detect left/right/top/bottom swipe gestures*/
                if(Math.abs(gamma)>=Math.abs(alpha)&&Math.abs(gamma)>=Math.abs(beta))
                {
                    if(gamma>200)
                    {
                        if(mPosition!=POSITION_END){
                            Log.e(TAG, "Left");
                            mPosition=POSITION_LEFT;
                            setText(mPosition);
                        }
                    }
                    else if(gamma<-200)
                    {
                        if(mPosition!=POSITION_END){
                            Log.e(TAG, "Right");
                            mPosition=POSITION_RIGHT;
                            setText(mPosition);
                        }
                    }
                }else if(Math.abs(beta)>=Math.abs(alpha)&&Math.abs(beta)>=Math.abs(gamma))
                {
                    /**Test:Double click gesture****************************/
                    if((isTop==1||isBottom==1)&&previousTime!=0)
                    {
                        previousTime=0;
                    }
                    previousTime++;
                    if(previousTime==10)
                    {
                        if(isTop>=2&&isBottom>=2){
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
                            if(beta<-200)
                            {
                                if(mPosition!=POSITION_END){
                                    Log.e(TAG, "Top");
                                    mPosition=POSITION_TOP;
                                    setText(mPosition);
                                    isTop++;
                                }
                            }
                            else if(beta>200)
                            {
                                if(mPosition!=POSITION_END){
                                    Log.e(TAG, "Bottom");
                                    mPosition=POSITION_BOTTOM;
                                    setText(mPosition);
                                    isBottom++;
                                }else{
                                    /**Detect hand down gesture to restart swipe detection*/
                                    mPosition=POSITION_BEGIN;
                                    int currentPosition=mPager.getCurrentItem()+1;
                                    setContentView(R.layout.gesture_detection_layout);
                                    gestureText = findViewById(R.id.gesture);
                                    setText("Function"+currentPosition+"\n"+mPosition);
                                    Log.e(TAG, "Function"+currentPosition+"\n"+mPosition);
                                    if (scrollTimer != null) {
                                        scrollTimer.cancel();
                                        scrollTimer = null;
                                    }
                                    if (scrollTask != null) {
                                        scrollTask.cancel();
                                        scrollTask = null;
                                    }
                                }
                            }
                        }
                    }else{
                        /************************************************************/
                        if(beta<-200)
                        {
                            if(mPosition!=POSITION_END){
                                Log.e(TAG, "Top");
                                mPosition=POSITION_TOP;
                                setText(mPosition);
                                isTop++;
                            }
                        }
                        else if(beta>200)
                        {
                            if(mPosition!=POSITION_END){
                                Log.e(TAG, "Bottom");
                                mPosition=POSITION_BOTTOM;
                                setText(mPosition);
                                isBottom++;
                            }else{
                                /**Detect hand down gesture to restart swipe detection*/
                                mPosition=POSITION_BEGIN;
                                int currentPosition=mPager.getCurrentItem()+1;
                                setContentView(R.layout.gesture_detection_layout);
                                gestureText = findViewById(R.id.gesture);
                                setText("Function"+currentPosition+"\n"+mPosition);
                                Log.e(TAG, "Function"+currentPosition+"\n"+mPosition);
                                if (scrollTimer != null) {
                                    scrollTimer.cancel();
                                    scrollTimer = null;
                                }
                                if (scrollTask != null) {
                                    scrollTask.cancel();
                                    scrollTask = null;
                                }
                            }
                        }
                    }

                }
        }
    }

    /**Algorithm 2:http://josejuansanchez.org/android-sensors-overview/gravity_and_linear_acceleration/README.html*/
    private boolean swipeEvent(SensorEvent event, long timestamp) {
        final float alpha = 0.8f;

        // Isolate the force of gravity with the low-pass filter.
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        // Remove the gravity contribution with the high-pass filter.
        linear_acceleration[0] = event.values[0] - gravity[0];
        linear_acceleration[1] = event.values[1] - gravity[1];
        linear_acceleration[2] = event.values[2] - gravity[2];

        Log.e(TAG, "linear_acceleration:   "+linear_acceleration[0]+"  "+linear_acceleration[1]+"   "+linear_acceleration[2]);

        if(Math.abs(linear_acceleration[0])>=Math.abs(linear_acceleration[1])&&Math.abs(linear_acceleration[0])>=Math.abs(linear_acceleration[2]))
        {
            if(linear_acceleration[0]>POSITION_IN_BORDER)
            {
                Log.e(TAG, "Top");
                mPosition="Top";
            }
            else if(linear_acceleration[0]<-POSITION_IN_BORDER)
            {
                Log.e(TAG, "Bottom");
                mPosition="Bottom";
            }
        }else if(Math.abs(linear_acceleration[1])>=Math.abs(linear_acceleration[0])&&Math.abs(linear_acceleration[1])>=Math.abs(linear_acceleration[2]))
        {
            if(linear_acceleration[1]<-POSITION_IN_BORDER)
            {
                Log.e(TAG, "Left");
                mPosition="Left";
            }
            else if(linear_acceleration[1]>POSITION_IN_BORDER)
            {
                Log.e(TAG, "Right");
                mPosition="Right";
            }
        }
        setText(mPosition);

        return true;
    }

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
