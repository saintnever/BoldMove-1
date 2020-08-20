package com.example.android.wearable.jumpingjack;

import android.graphics.drawable.Drawable;

import java.lang.reflect.Field;
import com.example.android.wearable.jumpingjack.R;
import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class function {
    private int id;
    private String name;
    private Integer[] semantics;
    private String[] devices;
    private String[] state;
    private Integer image;
//
//    public function(String id, String name, String[] semantics, String[] devices, String[] state,
//                    String image, Context context){
//        this.id = Integer.parseInt(id);
//        this.name = name;
//        this.semantics = extract_semantics(semantics);
//        this.devices = devices;
//        this.state = extract_state(state);
//        this.image = extract_image(image, context);
//    }

    public function(JSONObject func, Context context) throws JSONException {
        this.id = func.getInt("id");
        this.name = func.getString("function");
        this.semantics = extract_semantics(func.getJSONArray("semantic"));
        this.devices = extract_devices(func.getJSONArray("device"));
        this.state = extract_state(func.getJSONArray("state"));
        this.image = extract_image(func.getString("image"), context);
    }

    private Integer[] extract_semantics(JSONArray semantics) throws JSONException {
        Integer[] semanticsid = new Integer[semantics.length()] ;
        for (int i = 0; i < semanticsid.length; i++) {
            switch (semantics.getString(0)){
                case "previous":
                    semanticsid[i] = 0;
                    break;
                case "next":
                    semanticsid[i] = 1;
                    break;
                case "toggle":
                    semanticsid[i] = 2;
                    break;
                case "adjust":
                    semanticsid[i] = 3;
                default:
                    break;
            }
        }
        return semanticsid;
    }

    private String[] extract_devices(JSONArray json_device) throws JSONException {
        String[] devices = new String[json_device.length()];
        for (int i = 0; i < devices.length; i++) {
            devices[i] = json_device.getString(i);
        }
        return devices;
    }

    private String[] extract_state(JSONArray state) throws JSONException {
        String[] stateid = new String[state.length()];
        if (state.length() == 1  && state.getString(0).contains("-")){
            String[] strs = state.getString(0).split("-");
            int start = Integer.parseInt(strs[0], 10);
            int stop = Integer.parseInt(strs[1], 10);
            stateid = new String[stop-start+1];
            for (int i = 0; i < stateid.length; i++) {
                stateid[i] = Integer.toString(i+start);
            }
        }
        else{
            for (int i = 0; i < state.length(); i++) {
                stateid[i] = state.getString(i);
            }
        }
        return stateid;
    }

    private Integer extract_image(String image, Context context){
        int drawable = -1;
        try {
            drawable = context.getResources().getIdentifier(image, "drawable",context.getPackageName());
        } catch (Exception e) {
            // report exception
        }
        return drawable;
    }
}
