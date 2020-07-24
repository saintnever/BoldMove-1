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


public class FunctionThreeFragment extends Fragment {

    private TextView mMotionText;
    private Drawable mUpDrawable;
    private String resultFunction;

    public FunctionThreeFragment(String result)
    {
        resultFunction=result;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.left_swipe_layout, container, false);
        mMotionText = view.findViewById(R.id.left_swipe_counter);
        mMotionText.setCompoundDrawablesWithIntrinsicBounds(mUpDrawable, null, null, null);
        setCounter("Function 3\n"+resultFunction);
        return view;
    }

    public void setCounter(String text) {
        if(mMotionText!=null)
        {
        mMotionText.setText(text);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}