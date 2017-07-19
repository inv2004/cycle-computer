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
    double avg = 0;
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

    public void calc(long code, long tm, int i) {
        if(CCDataServiceSync.LAP == code) {
            if(1 == i) {
                start(tm);
            } else {
                stop();
            }
        }
        if(CCDataServiceSync.PWR != code) { return; }
        calc(tm, i);
    }

    public void calc(long tm, long val) {
        if(0 == int_time) { return; }
        service.sendData(CCDataServiceSync.AVGPWR, tm, calc0(tm, val));
    }

    public int calc0(long tm, long val) {
        if(0 == prev_time) {
            avg = val;
        } else {
            double prev_vol = avg * (prev_time - int_time);
            double add_vol = val * (tm - prev_time);
            double cur_vol = prev_vol + add_vol;
            avg = cur_vol / (tm - int_time);
        }
        prev_time = tm;
        return (int) avg;
    }
}
