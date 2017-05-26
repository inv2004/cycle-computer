package com.example.unknoqn.cc.calc;

import android.util.Log;

import com.example.unknoqn.cc.CCDataServiceSync;

/**
 * Created by unknoqn on 5/21/2017.
 */

public class CCCalcWC {
    CCDataServiceSync service;

    private boolean started = false;
    private int fullSWC = 10000;
    private int fullAWC = 20000;
    private int CP = 200;
    private int GP = 300;
    private int swc;
    private int awc;

    public CCCalcWC(CCDataServiceSync g_service) {
        service = g_service;
    }

    public void start() {
        started = true;
        swc = fullSWC;
        awc = fullAWC;
    }

    public void stop() {
        started = false;
    }

    public void calc(long code, long tm, int val) {
        if(CCDataServiceSync.PWR != code) { return; }
        calc(tm, val);
    }

    public void calc(long tm, int val) {
        if(!started) { return; }
        Log.d(this.toString(), "SWC / AWC : "+swc+" / "+awc);
        swc -= 100;
        awc -= 1000;
        service.sendData(CCDataServiceSync.SWC, tm, (100 * swc) / fullSWC);
        service.sendData(CCDataServiceSync.AWC, tm, (100 * awc) / fullAWC);
    }
}
