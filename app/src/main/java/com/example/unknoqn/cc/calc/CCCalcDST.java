package com.example.unknoqn.cc.calc;

import android.util.Log;

import com.example.unknoqn.cc.CCDataServiceSync;

/**
 * Created by unknoqn on 5/21/2017.
 */

public class CCCalcDST {
    CCDataServiceSync service;

    private boolean started = false;
    float dst = 0;

    public CCCalcDST(CCDataServiceSync g_service) {
        service = g_service;
    }

    public void start() {
        started = true;
        dst = 0f;
    }

    public void stop() {
        started = false;
    }

    public void calc(long code, long tm, float float_val) {
        if(CCDataServiceSync.DELTA_DST != code) { return; }
        calc(tm, float_val);
    }

    public void calc(long tm, float float_val) {
        if(!started) { return; }
        dst += float_val;
        service.sendData(CCDataServiceSync.DST, tm, 0, dst);
    }
}
