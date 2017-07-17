 package com.example.unknoqn.cc.calc;

import com.example.unknoqn.cc.CCDataServiceSync;

/**
 * Created by unknoqn on 7/6/2017.
 */

public class CCCalcAvgPwr {
    CCDataServiceSync service;

    private boolean started = false;
    long int_time = 0;
    long prev_time = 0;
    float avg = 0;
    float acc = 0;

    public CCCalcAvgPwr(CCDataServiceSync g_service) {
        service = g_service;
    }

    public void start(long time) {
        int_time = time;
        acc = 0;
        prev_time = 0;
    }

    public void stop() {
        int_time = 0;
    }

    public void calc(long code, long tm, float float_val) {
        if(CCDataServiceSync.PWR != code) { return; }
        calc(tm, float_val);
    }

    public void calc(long tm, float float_val) {
        if(0 == int_time) { return; }
        service.sendData(CCDataServiceSync.AVGPWR, tm, calc0(tm, float_val));
    }

    public int calc0(long tm, float float_val) {
        if(0 == prev_time) {
            avg = float_val;
        } else {
            long prev_vol = (long) (avg * (prev_time - int_time));
            long add_vol = (long) float_val * (tm - prev_time);
            long cur_vol = prev_vol + add_vol;
            avg = cur_vol / (tm - int_time);
        }
        prev_time = tm;
        return (int) avg;
    }
}
