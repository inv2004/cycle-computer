package com.example.unknoqn.cc.calc;

import android.util.Log;

import com.example.unknoqn.cc.CCDataServiceSync;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by unknoqn on 6/7/2017.
 */

public class CCCalcAutoInt {
    CCDataServiceSync service;

    boolean interval = false;
    int avg_pwr = 0;
    int lap = 0;

    LinkedList<Long> tt = new LinkedList();
    LinkedList<Integer> vv = new LinkedList();
    LinkedList<Integer> ma = new LinkedList();

    public CCCalcAutoInt(CCDataServiceSync _service) {
        service = _service;
    }

    public void start() {
    }

    public void stop() {
    }

    public void calc(long code, long tm, int val) {
        if(CCDataServiceSync.AVGPWR == code) { avg_pwr = val; }
        if(CCDataServiceSync.PWR != code) { return; }
        calc(tm, val);
    }

    public void calc(long tm, int val) {
        if(interval) {
            checkStop(tm, val);
        } else {
            checkStart(tm, val);
        }
    }

    public void checkStop(long tm, int val) {
        int sum_avg = 0;
        Iterator<Integer> it = vv.iterator();
        while(it.hasNext()) {
            sum_avg += it.next();
        }
        int mavg = sum_avg / 10;

        tt.add(tm);
        vv.add(val);
        ma.add(mavg);
        Log.d("DEBUG2", tm+": "+val+" / "+mavg);

        int mavg_prev_10 = 0;
        long time_prev_10 = 0;

        boolean cond = true;
        while(cond) {
            Long t = tt.peek();
            if(null != t && t <= tm-10000) {
                mavg_prev_10 = ma.getFirst();
                time_prev_10 = tt.getFirst();
                tt.remove();
                vv.remove();
                ma.remove();
            } else {
                cond = false;
            }
        }

        service.sendData(CCDataServiceSync.TEST0, tm, mavg);
        if(0 != mavg_prev_10 && mavg_prev_10 * 0.7 >= mavg && mavg <= 1.0*300) {

            boolean stable = true;
            Iterator<Integer> it2 = vv.iterator();
            while(it2.hasNext()) {
                if(0.8*mavg_prev_10 < it2.next()) {
//                    Log.d("DEBUG2", "stable fail");
                    stable = false;
                }
            }

            if(stable) {
                Log.d("INT", "END");
                service.sendData(CCDataServiceSync.LAP, time_prev_10, 0);
                interval = false;
            }
        }

    }

    public void checkStart(long tm, int val) {
        int sum_avg = 0;
        Iterator<Integer> it = vv.iterator();
        while(it.hasNext()) {
            sum_avg += it.next();
        }
        int mavg = sum_avg / 10;

        tt.add(tm);
        vv.add(val);
        ma.add(mavg);
//        Log.d("DEBUG1", tm+": "+val+" / "+mavg);

        int mavg_prev_10 = 0;
        long time_prev_10 = 0;

        boolean cond = true;
        while(cond) {
            Long t = tt.peek();
            if(null != t && t <= tm-10000) {
                mavg_prev_10 = ma.getFirst();
                time_prev_10 = tt.getFirst();
                tt.remove();
                vv.remove();
                ma.remove();
            } else {
                cond = false;
            }
        }

        service.sendData(CCDataServiceSync.TEST0, tm, mavg);
        if(0 != mavg_prev_10 && mavg_prev_10 * 1.8 <= mavg && mavg >= 1.0*300) {

            boolean stable = true;
            Iterator<Integer> it2 = vv.iterator();
            while(it2.hasNext()) {
                if(mavg_prev_10 > it2.next()) {
//                    Log.d("DEBUG2", "stable fail");
                    stable = false;
                }
            }

            if(stable) {
                Log.d("INT", "START");
                service.sendData(CCDataServiceSync.LAP, time_prev_10, 1);
                lap += 1;
                interval = true;
            }
        }
    }
}
