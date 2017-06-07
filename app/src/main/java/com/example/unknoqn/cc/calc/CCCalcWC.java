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
        long tm_delta_ms = (tm - prev_tm);
        int pwr_delta = val - CP;
        if(0 < pwr_delta) {
            awc_exp += tm_delta_ms*pwr_delta/1000;
        } else {
//            int year = 2015;
//            if(2015 == year) {
            double a = tm_delta_ms*(pwr_delta/fullAWC)/1000;
            Log.d("exp0", String.valueOf(a));
            double b = Math.exp(a);
            Log.d("exp", String.valueOf(b));
                awc_exp *= a;
//            }
        }
        Log.d("DEBUG1", String.valueOf(awc_exp));
        prev_tm = tm;
        service.sendData(CCDataServiceSync.AWC, tm, (100 * (fullAWC-awc_exp)) / fullAWC);
    }
}
