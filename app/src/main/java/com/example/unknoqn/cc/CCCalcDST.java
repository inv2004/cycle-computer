package com.example.unknoqn.cc;

import android.util.Log;

/**
 * Created by unknoqn on 5/21/2017.
 */

class CCCalcDST {
    CCServiceAntSync service;

    private boolean started = false;
    float dst = 0;

    public CCCalcDST(CCServiceAntSync g_service) {
        service = g_service;
    }

    public void start() {
        started = true;
        dst = 0f;
    }

    public void stop() {
        started = false;
    }

    public void calc(long tm, float val) {
        if(!started) { return; }
        Log.d(this.toString(), "DEBUG1");
        dst += val;
        service.sendData(CCServiceAntSync.DST, tm, 0, dst);
    }
}
