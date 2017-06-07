package com.example.unknoqn.cc.calc;

import android.util.Log;

import com.example.unknoqn.cc.CCDataServiceSync;

/**
 * Created by unknoqn on 5/21/2017.
 */

public class CCCalcWC {
    CCDataServiceSync service;

    long start_tm = 0;
    private int fullAWC = 18000;
    private int CP = 300;
    private int awc_exp;

    long prev_tm = 0;

    public CCCalcWC(CCDataServiceSync g_service) {
        service = g_service;
    }

    public void start(long _tm) {
        start_tm = _tm;
        awc_exp = 0;
    }

    public void stop() {
        start_tm = 0;
    }

    public void calc(long code, long tm, int val) {
        if(CCDataServiceSync.PWR != code) { return; }
        calc(tm, val);
    }

    public void calc(long tm, int val) {
        if(0 == start_tm) { return; }
        if(0 == prev_tm) { prev_tm = start_tm; }
        long tm_delta = (tm - prev_tm)/1000;
        int pwr_delta = val - CP;
        if(0 < pwr_delta) {
            awc_exp += pwr_delta*tm_delta;
        } else {
//            int year = 2015;
//            if(2015 == year) {
                awc_exp *= Math.exp(tm_delta*pwr_delta/fullAWC);
//            }
        }
        Log.d("DEBUG1", String.valueOf(awc_exp));
        prev_tm = tm;
        service.sendData(CCDataServiceSync.AWC, tm, (100 * (fullAWC-awc_exp)) / fullAWC);
    }
}
