package com.example.unknoqn.cc.calc;

import android.util.Log;

import com.example.unknoqn.cc.CCDataServiceSync;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by unknoqn on 6/7/2017.
 */

public class CCCalcAutoInt {
    CCDataServiceSync service;

    long start_tm = 0;
    long prev_tm = 0;
    long lap = 1;
    long emavg = 0;
    double ravg = 0;
    double alpha = 0.01;
    LinkedList<Long> tt = new LinkedList();
    LinkedList<Integer> vv = new LinkedList();

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
        emavg = emavg + (val-emavg) / 10;
        service.sendData(CCDataServiceSync.TEST0, tm, (int) emavg);

        tt.push(tm);
        vv.push(val);

        boolean cond = true;
        while(cond) {
            Long t = tt.peek();
            if(null != t && t <= tm-10000) {
                tt.remove();
                vv.remove();
            } else {
                cond = false;
            }
        }
        ravg = 0;
        Iterator<Integer> it = vv.iterator();
        while(it.hasNext()) {
            ravg += it.next();
        }

//        Log.d("EMAVG: ", String.valueOf(emavg));
        Log.d("MAVG10", String.valueOf(ravg));
        service.sendData(CCDataServiceSync.TEST0, tm, (int) ravg);
        if(false) {
            Log.d("INT", "OK");
            service.sendData(CCDataServiceSync.LAP, tm, 1);
            lap += 1;
        }
    }
}
