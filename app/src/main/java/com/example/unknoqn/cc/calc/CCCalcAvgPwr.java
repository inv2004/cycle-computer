 package com.example.unknoqn.cc.calc;

import android.util.Log;
import android.util.Pair;

import com.example.unknoqn.cc.CCDataServiceSync;

/**
 * Created by unknoqn on 7/6/2017.
 */

// @TODO avg is not correct because interval starts in 10s

public class CCCalcAvgPwr {
    CCDataServiceSync service;

    private boolean started = false;
    long int_time = 0;
    long prev_time = 0;
    double avg = 0;
    double init_10s_avg;

    public CCCalcAvgPwr(CCDataServiceSync g_service) {
        service = g_service;
    }

    public void start(long time, float f_val) {
        int_time = time;
        prev_time = 0;
        init_10s_avg = f_val;
    }

    public void stop() {
        int_time = 0;
    }

    public void calc(long code, long tm, int i, float f_val) {
        if(CCDataServiceSync.LAP == code) {
            if(1 == i || 2 == i) {
                start(tm, f_val);
            } else {
                stop();
            }
        }
        if(CCDataServiceSync.PWR != code) { return; }
        calc(tm, i);
    }

    public void calc(long tm, int val) {
        if(0 == int_time) { return; }
        service.sendData(CCDataServiceSync.AVGPWR, tm, calc0(tm, val));
    }

    public int calc0(long tm, int val) {
        if(0 == prev_time) {
            avg = Math.abs(init_10s_avg);
            if(init_10s_avg < 0) { // sorry, too lazy for additional parameter <0 if manual INT, >0 if auto INT
                prev_time = tm - 1;
            } else {
                prev_time = tm - 10*1000;
            }
        } else {
            double prev_vol = avg * (prev_time - int_time);
            double add_vol = val * (tm - prev_time);
            double cur_vol = prev_vol + add_vol;
            avg = cur_vol / (tm - int_time);
            prev_time = tm;
        }
        return (int) avg;
    }
}
