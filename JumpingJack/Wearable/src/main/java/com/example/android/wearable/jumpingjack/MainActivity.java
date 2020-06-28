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

import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.ViewPager;
import androidx.wear.ambient.AmbientModeSupport;

import com.example.android.wearable.jumpingjack.fragments.CounterFragment;
import com.example.android.wearable.jumpingjack.fragments.SwipeDetectionFragment;
import com.example.android.wearable.jumpingjack.fragments.SettingsFragment;

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

    private static final String TAG = "MainActivity";

    // An up-down movement that takes more than 2 seconds will not be registered (in nanoseconds).
    private static final long TIME_THRESHOLD_NS = TimeUnit.SECONDS.toNanos(2);

    /**
     * Earth gravity is around 9.8 m/s^2 but user may not completely direct his/her hand vertical
     * during the exercise so we leave some room. Basically, if the x-component of gravity, as
     * measured by the Gravity sensor, changes with a variation delta > 0.03 from the hand down
     * and hand up threshold we define below, we consider that a successful count.
     *
     * This is a very rudimentary formula and is by no means production accurate. You will want to
     * take into account Y and Z gravity changes to get a truly accurate jumping jack.
     *
     * This sample is just meant to show how to easily get sensor values and use them.
     */
    private static final float HAND_DOWN_GRAVITY_X_THRESHOLD = -.040f;
    private static final float HAND_UP_GRAVITY_X_THRESHOLD = -.010f;

    private SensorManager mSensorManager;
    private Sensor mAcceleratorSensor;
    private Sensor mGyroSensor;
    private long mLastTime = 0;
    private int temp=0;
    private int timer=0;
    private int mJumpCounter = 0;
    private boolean mHandDown = true;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.jumping_jack_layout);

        AmbientModeSupport.attach(this);

        setupViews();

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
    }

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
    }

    private void stopSensor(){
        if (mSensorManager != null) {
            mSensorManager = null;
        }
    }

    private void setupViews() {
        mPager = findViewById(R.id.pager);
        mFirstIndicator = findViewById(R.id.indicator_0);
        mSecondIndicator = findViewById(R.id.indicator_1);
        mThirdIndicator=findViewById(R.id.indicator_2);

        final PagerAdapter adapter = new PagerAdapter(getSupportFragmentManager());

        mCounterPage = new CounterFragment();
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

        mPager.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSensorManager.registerListener(this, mAcceleratorSensor,
                SensorManager.SENSOR_DELAY_NORMAL)) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Successfully registered for the accelerator sensor updates");
            }
        }
        if (mSensorManager.registerListener(this, mGyroSensor,
                SensorManager.SENSOR_DELAY_NORMAL)) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Successfully registered for the gyro sensor updates");
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Unregistered for sensor events");
        }
    }

    /** Wrist gesture: https://developer.android.com/training/wearables/ui/wrist-gestures */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.e(TAG, "Test");
        switch (keyCode) {
            case KeyEvent.KEYCODE_NAVIGATE_NEXT:
                // Do something that advances a user View to the next item in an ordered list.
                Log.e(TAG, "Next");
                return moveToNextItem();
            case KeyEvent.KEYCODE_NAVIGATE_PREVIOUS:
                // Do something that advances a user View to the previous item in an ordered list.
                Log.e(TAG, "Previous");
                return moveToPreviousItem();
        }
        // If you did not handle it, let it be handled by the next possible element as deemed by the Activity.
        return super.onKeyDown(keyCode, event);
    }

    /** Shows the next item in the custom list. */
    private boolean moveToNextItem() {
        boolean handled = false;
        Log.e(TAG, "Next");
        // Return true if handled successfully, otherwise return false.
        return handled;
    }

    /** Shows the previous item in the custom list. */
    private boolean moveToPreviousItem() {
        boolean handled = false;
        Log.e(TAG, "Previous");
        // Return true if handled successfully, otherwise return false.
        return handled;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch(event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                //Log.e(TAG, "Accelerator:   "+event.values[0]+"  "+event.values[1]+"   "+event.values[2]);
                //detectJump(event.values[0], event.timestamp);
                //swipeEvent(event,event.timestamp);
                accelerator=event.values;
                break;
            case Sensor.TYPE_GYROSCOPE:
                //Log.e(TAG, "Gyroscope:   "+event.values[0]+"  "+event.values[1]+"   "+event.values[2]);
                gyro=event.values;
                /**Distinguish begin from top*/
                if(mPosition==POSITION_BEGIN){
                    temp++;
                    if(temp>=10)
                    {
                        inAIrSwipe(accelerator,gyro,event.timestamp);
                    }
                }else{
                    temp=0;
                    inAIrSwipe(accelerator,gyro,event.timestamp);
                }

                break;
        }
    }

    /**Algorithm 1: https://w3c.github.io/motion-sensors/#complementary-filter*/
    private void inAIrSwipe(float[] AcceleratorValues, float[] GyroValues, long timestamp){
        double alpha = 0;
        double beta = 0;
        double gamma=0;
        float bias=0.98f;
        long dt = (timestamp - mLastTime)/1000000;
        //Log.e(TAG, "dt:   "+dt);
        mLastTime = timestamp;
        double norm = Math.sqrt(Math.pow(accelerator[0],2) + Math.pow(accelerator[1],2) + Math.pow(accelerator[2],2));
        double scale = Math.PI / 2;
        alpha = bias * (alpha + gyro[0] * dt) + (1.0 - bias) * (accelerator[0] * scale / norm);
        beta = bias * (beta + gyro[1] * dt) + (1.0 - bias) * (accelerator[1] * scale / norm);
        gamma = bias * (gamma + gyro[2] * dt) + (1.0 - bias) * (accelerator[2] * scale / norm);
        //Log.e(TAG, "Mulitsensors:   "+Math.round(alpha)+"  "+Math.round(beta)+"   "+Math.round(gamma));

        /**Detect hold gesture to end swipe detection*/
        if(Math.sqrt(Math.pow(alpha,2)+Math.pow(beta,2)+Math.pow(gamma,2))<20)
        {
            timer++;
            if(timer>=10){
            if(mPosition!=POSITION_BEGIN&&mPosition!=POSITION_END){
                setText("Result:"+mPosition);
                mPosition=POSITION_END;
                timer=0;
            }}
        }else{
            timer=0;
            /**Detect left/Right/Top/Bottom swipe gesture*/
            if(Math.abs(gamma)>=Math.abs(alpha)&&Math.abs(gamma)>=Math.abs(beta))
            {
                if(gamma>300)
                {
                    if(mPosition!=POSITION_END){
                        Log.e(TAG, "Left");
                        mPosition=POSITION_LEFT;
                        setText(mPosition);
                    }
                }
                else if(gamma<-300)
                {
                    if(mPosition!=POSITION_END){
                        Log.e(TAG, "Right");
                        mPosition=POSITION_RIGHT;
                        setText(mPosition);
                    }
                }
            }else if(Math.abs(beta)>=Math.abs(alpha)&&Math.abs(beta)>=Math.abs(gamma))
            {
                if(beta<-300)
                {
                    if(mPosition!=POSITION_END){
                        Log.e(TAG, "Top");
                        mPosition=POSITION_TOP;
                        setText(mPosition);
                    }
                }
                else if(beta>300)
                {
                    if(mPosition!=POSITION_END){
                        Log.e(TAG, "Bottom");
                        mPosition=POSITION_BOTTOM;
                        setText(mPosition);
                    }else{
                        /**Detect hand down gesture to restart swipe detection*/
                        mPosition=POSITION_BEGIN;
                        setText(mPosition);
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
        mLeftSwipeCounterPage.setCounter(text);
    }

    /**
     * A very simple algorithm to detect a successful up-down movement of hand(s). The algorithm
     * is based on a delta of the handing being up vs. down and taking less than TIME_THRESHOLD_NS
     * to happen.
     *
     *
     * This algorithm isn't intended to be used in production but just to show what's possible with
     * sensors. You will want to take into account other components (y and z) and other sensors to
     * get a more accurate reading.
     */
    private void detectJump(float xGravity, long timestamp) {

        if ((xGravity <= HAND_DOWN_GRAVITY_X_THRESHOLD)
                || (xGravity >= HAND_UP_GRAVITY_X_THRESHOLD)) {

            if (timestamp - mLastTime < TIME_THRESHOLD_NS) {
                // Hand is down when yValue is negative.
                onJumpDetected(xGravity <= HAND_DOWN_GRAVITY_X_THRESHOLD);
            }

            mLastTime = timestamp;
        }
    }

    /**
     * Called on detection of a successful down -> up or up -> down movement of hand.
     */
    private void onJumpDetected(boolean handDown) {
        if (mHandDown != handDown) {
            mHandDown = handDown;

            // Only count when the hand is down (means the hand has gone up, then down).
            if (mHandDown) {
                mJumpCounter++;
                setCounter(mJumpCounter);
            }
        }
    }

    /**
     * Updates the counter on UI, saves it to preferences and vibrates the watch when counter
     * reaches a multiple of 10.
     */
    private void setCounter(int i) {
        mJumpCounter = i;
        mCounterPage.setCounter(i);
        //mLeftSwipeCounterPage.setCounter(0);
        Utils.saveCounterToPreference(this, i);
        if (i > 0 && i % 10 == 0) {
            Utils.vibrate(this, 0);
        }
    }

    public void resetCounter() {
        setCounter(0);
    }

    /**
     * Sets the page indicator for the ViewPager.
     */
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
