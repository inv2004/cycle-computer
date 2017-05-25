package com.example.unknoqn.cc;

import android.util.Log;

/**
 * Created by unknoqn on 5/21/2017.
 */

class CCCalcWC {
    CCServiceAntSync service;

    private boolean started = false;
    private int fullSWC = 10000;
    private int fullAWC = 20000;
    private int CP = 200;
    private int GP = 300;
    private int swc;
    private int awc;

    public CCCalcWC(CCServiceAntSync g_service) {
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

    public void calc(long tm, int val) {
        if(!started) { return; }
        Log.d(this.toString(), "SWC / AWC : "+swc+" / "+awc);
        swc -= 100;
        awc -= 1000;
        service.sendData(CCServiceAntSync.SWC, tm, (100 * swc) / fullSWC);
        service.sendData(CCServiceAntSync.AWC, tm, (100 * awc) / fullAWC);
    }
}
