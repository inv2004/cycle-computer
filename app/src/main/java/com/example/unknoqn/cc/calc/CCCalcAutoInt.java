package com.example.unknoqn.cc.calc;

import android.util.Log;

import com.example.unknoqn.cc.CC;
import com.example.unknoqn.cc.CCDataServiceSync;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by unknoqn on 6/7/2017.
 */

public class CCCalcAutoInt {
    CCDataServiceSync service;

    boolean interval = false;
    int multiple_tm = 1;
    double avg_pwr = 0;
    int lap = 0;
    long last_tm = 0;
    int last_pwr;

    LinkedList<Long> tt = new LinkedList();
    LinkedList<Integer> vv = new LinkedList();
    LinkedList<Double> ma = new LinkedList();
    LinkedList<Double> _a = new LinkedList();

    double mavg_prev_10 = 0;
    double avg_prev_10 = 0;
    long time_prev_10 = 0;
    double mavg;

    boolean manual = false;

    public CCCalcAutoInt(CCDataServiceSync _service) {
        service = _service;
    }

    public void start() {
    }

    public void stop() {
    }

    public void updateLastTM(long tm) {
        last_tm = tm;
    }

    public void calc(long code, long tm, int val) {
        last_tm = tm;

        if (CCDataServiceSync.STRAVA_NEAR == code) {
            if(val == CC.NA) {
                manual = false;
                interval = false;
            } else if(val <= 100) {
                manual = true;
            }
        }
        if (CCDataServiceSync.STRAVA_INT == code) {
            if(1 == val) {
                manual = true;
            } else {
                manual = false;
                interval = false;
            }
        }
        if (CCDataServiceSync.LAP == code) {
            if(2 == val) {
                manual = true;
            } else if(0 == val){
                manual = false;
                interval = false;
            }
        }
        if (CCDataServiceSync.AVGPWR == code) {
            avg_pwr = val;
        }
        if(manual) {
            return;
        }
        if (CCDataServiceSync.PWR != code) {
            return;
        } else {
            last_pwr = val;
        }
        calc0(tm, val);
    }

    public int calc0_arr(int mul, long[] tm_arr, int[] val_arr) {
        int acc = 0;
        for(int i = 0; i < tm_arr.length; i++) {
            acc += calc0(tm_arr[i]*mul, val_arr[i]);
        }
        return acc;
    }

    public int calc0(long tm, int val) {
        if(interval) {
            return checkStop(tm, val);
        } else {
            return checkStart(tm, val);
        }
    }

    private double add(long tm, int val) {
        tt.add(tm);
        vv.add(val);
        _a.add(avg_pwr); // straight change of one mutable structure with all calculations in it would work better. @TODO: rewrite

        boolean cond = true;
        while(cond) {
            Long t = tt.peek();
            if(null != t && t <= tm-10000) {
                mavg_prev_10 = ma.getFirst();
                avg_prev_10 = _a.getFirst();
                time_prev_10 = tt.getFirst();
                tt.remove();
                vv.remove();
                ma.remove();
                _a.remove();
            } else {
                cond = false;
            }
        }

        long sum_avg = 0;
        long delta_tm_sum = 0;
        long prev_time = 0;
        if(0 == prev_time) {
            if(0 == time_prev_10) {
                prev_time = tm;
                // ???
                // if initial delta time = 0 then initial volume = 0
                // if initial delta time = 1 then initial volume = power
            } else {
                prev_time = time_prev_10;
            }
        }
        Iterator<Long> it = tt.iterator();
        Iterator<Integer> it2 = vv.iterator();
        while(it.hasNext() && it2.hasNext()) {
            long tm_of_it = it.next();
            long delta_tm = tm_of_it - prev_time;
            prev_time = tm_of_it;
            sum_avg += delta_tm * it2.next();
            delta_tm_sum += delta_tm;
        }
        double mavg = 0 == delta_tm_sum ? sum_avg : sum_avg / delta_tm_sum;
        ma.add(mavg);
        return mavg;
    }

    public boolean checkSpikes(double mavg_limit, boolean more) { // more: check (>) or (<)
        boolean stable = true;
        Iterator<Integer> it2 = vv.iterator();
        while(it2.hasNext()) {
            if(more) {
                if(mavg_limit > it2.next()) {
                    stable = false;
                }
            } else {
                if(mavg_limit < it2.next()) {
//                    Log.d("DEBUG2", "stable fail");
                    stable = false;
                }
            }
        }
        return stable;
    }

    public int checkStop(long tm, int val) {
        mavg = add(tm, val); // side-effect: time_prev_10, mavg_prev_10
//        service.sendData(CCDataServiceSync.TEST0, tm, mavg);

        if(0 != mavg_prev_10 && avg_pwr * 0.8 >= mavg) {
            if(checkSpikes(0.8*mavg_prev_10, false)) {
                Log.d("INT", "END");
                if(service != null) {
                    service.sendData(CCDataServiceSync.LAP, time_prev_10, 0, (float) avg_prev_10);
                }
                interval = false;
                return 0;
            }
        }
        return -1;

    }

    public int checkStart(long tm, int val) {
        double mavg = add(tm, val); // side-effect: time_prev_10, mavg_prev_10

//        service.sendData(CCDataServiceSync.TEST0, tm, mavg);
        if(0 != mavg_prev_10 && mavg_prev_10 * 1.3 <= mavg && mavg >= 1.0*300) { // CP dep
            if(checkSpikes(mavg_prev_10, true)) {
                Log.d("INT", "START");
                if(service != null) {
                    service.sendData(CCDataServiceSync.LAP, time_prev_10, 1, (float) mavg);
                }
                lap += 1;
                interval = true;
                return 1;
            }
        }
        return -1;
    }

    public void manualClick() {
        Log.d("CLICK", "LAP: "+manual);
        if(manual) {
            service.sendData(CCDataServiceSync.LAP, last_tm, 0, (float) avg_pwr);
        } else if(interval) {
            service.sendData(CCDataServiceSync.LAP, last_tm, 0, (float) mavg);
        } else {
            service.sendData(CCDataServiceSync.LAP, last_tm, 2, (float) -last_pwr); // negative value - mark of manual INT
        }
    }
}
