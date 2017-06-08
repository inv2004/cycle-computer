package com.example.unknoqn.cc.calc;

import android.util.Log;

import com.example.unknoqn.cc.CCDataServiceSync;

/**
 * Created by unknoqn on 6/7/2017.
 */

public class CCCalcAutoInt {
    CCDataServiceSync service;

    long start_tm = 0;
    long prev_tm = 0;
    long lap = 1;

    public CCCalcAutoInt(CCDataServiceSync _service) {
        service = _service;
    }

    public void start(long _tm) {
        start_tm = _tm;
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
        if(0 == prev_tm) { prev_tm = tm; return; }
 //       Log.d("INT", "calc "+tm + " - "+prev_tm);
        if(1 == lap && 3*60*1000 < tm - prev_tm) {
            Log.d("INT", "OK");
            service.sendData(CCDataServiceSync.LAP, tm, 1);
            lap += 1;
        }
    }
}
