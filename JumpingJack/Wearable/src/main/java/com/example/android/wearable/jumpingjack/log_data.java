package com.example.android.wearable.jumpingjack;

import java.util.StringJoiner;

public class log_data {
    public int session;
    public int block;
    public int trial;
    public int funcid_selected;
    public int funcid_target;
    public int[] configure;
    public long  timestamp_pressed;
    public long timestamp_selected;
    public int[]  func_id;
    public long[] timestamp_func_start;
    public String[] log;

    public log_data (){
        this.session = -1;
        this.block = -1;
        this.trial = -1;
        this.funcid_selected  = -1;
        this.funcid_target  = -1;
        this.configure = new int[]{0,0};
        this.timestamp_pressed = 0;
        this.timestamp_selected = 0;
        this.timestamp_func_start = new long[5];
        this.func_id = new int[5];
        this.log = new String[10];
    }

    public log_data (int session, int block, int trial, int id, int target, int[] configure, long ts_pressed, long ts_selected, int[] func_id, long[] ts_func_start){
        this.session = session;
        this.block = block;
        this.trial = trial;
        this.funcid_selected = id;
        this.funcid_target  = target;
        this.configure = configure;
        this.timestamp_pressed = ts_pressed;
        this.timestamp_selected = ts_selected;
        this.func_id = func_id;
        this.timestamp_func_start = ts_func_start;
    }

    public String assemby_send_string (){
        StringJoiner send_data = new StringJoiner(";");
        StringJoiner id_func = new StringJoiner(",", "[", "]");
        StringJoiner ts_func = new StringJoiner(",", "[", "]");
        this.log[0] = Integer.toString(this.session);
        this.log[1] = Integer.toString(this.block);
        this.log[2] = Integer.toString(this.trial);
        this.log[3] = Integer.toString(this.funcid_selected);
        this.log[4] = Integer.toString(this.funcid_target);
        this.log[5] = "["+Integer.toString(this.configure[0])+","+ this.configure[1] +"]";
        this.log[6] = Long.toString(timestamp_pressed);
        this.log[7] = Long.toString(timestamp_selected);

        for(int func:this.func_id){
            id_func.add(Integer.toString(func));
        }
        this.log[8] = id_func.toString();

        for(long ts:this.timestamp_func_start){
            ts_func.add(Long.toString(ts));
        }
        this.log[9] = ts_func.toString();


        for (String s : this.log) {
            send_data.add(s);
        }
        return send_data.toString();
    }
}
