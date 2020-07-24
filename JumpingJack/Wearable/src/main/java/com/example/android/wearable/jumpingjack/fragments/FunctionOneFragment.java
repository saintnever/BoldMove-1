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

package com.example.android.wearable.jumpingjack.fragments;

import com.example.android.wearable.jumpingjack.MainActivity;
import com.example.android.wearable.jumpingjack.R;
import com.example.android.wearable.jumpingjack.Utils;

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

/**
 * A simple fragment for showing the count
 */
public class FunctionOneFragment extends Fragment {

    private TextView mMotionText;
    private Drawable mUpDrawable;
    private String resultFunction;

    public FunctionOneFragment(String result)
    {
        resultFunction=result;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.counter_layout, container, false);
        mMotionText = view.findViewById(R.id.counter);
        mMotionText.setCompoundDrawablesWithIntrinsicBounds(mUpDrawable, null, null, null);
        setCounter("Function 1\n"+resultFunction);
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