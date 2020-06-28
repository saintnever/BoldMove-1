package com.example.android.wearable.jumpingjack.fragments;

import com.example.android.wearable.jumpingjack.R;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.util.Timer;
import java.util.TimerTask;


public class SwipeDetectionFragment extends Fragment {

    private static final long ANIMATION_INTERVAL_MS = 500; // in milliseconds
    private TextView mMotionText;
    private Timer mAnimationTimer;
    private Handler mHandler;
    private TimerTask mAnimationTask;
    private boolean up = false;
    private Drawable mDownDrawable;
    private Drawable mUpDrawable;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.left_swipe_layout, container, false);
        mDownDrawable = getResources().getDrawable(R.drawable.jump_down_50);
        mUpDrawable = getResources().getDrawable(R.drawable.jump_up_50);
        mMotionText = view.findViewById(R.id.left_swipe_counter);
        mMotionText.setCompoundDrawablesWithIntrinsicBounds(mUpDrawable, null, null, null);
        setCounter("Start!");
        mHandler = new Handler();
        startAnimation();
        return view;
    }

    private void startAnimation() {
        mAnimationTask = new TimerTask() {
            @Override
            public void run() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mMotionText.setCompoundDrawablesWithIntrinsicBounds(
                                up ? mUpDrawable : mDownDrawable, null, null, null);
                        up = !up;
                    }
                });
            }
        };
        mAnimationTimer = new Timer();
        mAnimationTimer.scheduleAtFixedRate(mAnimationTask, ANIMATION_INTERVAL_MS,
                ANIMATION_INTERVAL_MS);
    }

    public void setCounter(String text) {
        if(mMotionText!=null)
        {
        mMotionText.setText(text);
        }
    }

    @Override
    public void onDetach() {
        mAnimationTimer.cancel();
        super.onDetach();
    }
}