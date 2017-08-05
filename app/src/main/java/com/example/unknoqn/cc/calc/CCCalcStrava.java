package com.example.unknoqn.cc.calc;

import android.location.Location;
import android.util.Log;

import com.example.unknoqn.cc.CC;
import com.example.unknoqn.cc.CCDataServiceSync;
import com.example.unknoqn.cc.CCStrava;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by unknown on 7/27/2017.
 */

public class CCCalcStrava {
    CCDataServiceSync service;

    List<List<LatLng>> segments = new ArrayList<>();
    List<Float> segments_dst = new ArrayList<>();

    int catch_phase = 0;
    float near = 15.0f;
    long prev_tm = 0;
    float dst_to_go = 0f;
    float last_dst = 0f;
    float started_dst = 0f;
    List<LatLng> current_seq;

    public CCCalcStrava(CCDataServiceSync _service) {
        service = _service;
    }

    public void reload() {
        CCStrava strava = CCStrava.getInstance();
        segments = strava.getSegments();
        segments_dst = strava.getSegmentsDst();
    }


    public void calc(int code, long tm, float dst, double[] d_arr) {
        if(code == CCDataServiceSync.DST) { last_dst = dst; }
        if(1 >= catch_phase) {
            if(code != CCDataServiceSync.LATLNG) { return; }
            checkStart(tm, d_arr);
        }
        if(2 == catch_phase) {
            if(code != CCDataServiceSync.LATLNG) { return; }
            follow(tm, d_arr, last_dst);
        }
    }

    private void follow(long tm, double[] d_arr, float dst) {
        float left = started_dst + dst_to_go - dst;
        service.sendData(CCDataServiceSync.STRAVA_INT, prev_tm, 1, left);

        Location current_loc = new Location("A");
        current_loc.setLatitude(d_arr[0]);  // la
        current_loc.setLongitude(d_arr[1]); // ln

        boolean is_ok = false;
        float meters_to_last = 1000;

        Iterator<LatLng> it = current_seq.iterator();
        while(it.hasNext()) {
            LatLng ll = it.next();
            Location l = new Location("B");
            l.setLatitude(ll.latitude);
            l.setLongitude(ll.longitude);

            float meters = l.distanceTo(current_loc);
            if(meters <= 30.0f) {
                is_ok = true;
                if(! it.hasNext()) {
                    meters_to_last = meters;
                }
                return;
            }
        }

        if(!is_ok) {
            reset();
        } else {
            if(meters_to_last <= near) {
                near = meters_to_last;
            } else {
                service.sendData(CCDataServiceSync.STRAVA_INT, prev_tm, 0, 0f);
            }
        }

        prev_tm = tm;
    }

    public void checkStart(long tm, double[] d_arr) {
        Location current_loc = new Location("A");
        current_loc.setLatitude(d_arr[0]);  // la
        current_loc.setLongitude(d_arr[1]); // ln

        boolean found_near = false;

        Iterator<List<LatLng>> it = segments.iterator();
        Iterator<Float> it2 = segments_dst.iterator();
        while(it.hasNext() && it2.hasNext()) {
            List<LatLng> seg = it.next();
            float dst = it2.next();
            LatLng ll = seg.get(0);
            Location l = new Location("B");
            l.setLatitude(ll.latitude);
            l.setLongitude(ll.longitude);
            float meters = l.distanceTo(current_loc);

            Log.d("STRAVA", ""+meters);

            if(meters <= 500) {
                found_near = true;
                if(meters < near) {
                    Log.d("STRAVA", "in "+near);
                    catch_phase = 1;
                    near = meters;
                    service.sendMsg(CCDataServiceSync.STRAVA_NEAR, (int) meters);
                } else if(1 == catch_phase) {
                    catch_phase = 2;
                    dst_to_go = dst;
                    started_dst = last_dst;
                    current_seq = seg;
                    near = 15f;
                    Log.d("STRAVA", "START "+dst_to_go);
                    service.sendData(CCDataServiceSync.STRAVA_INT, prev_tm, 1, dst_to_go);
                    return;
                } else {
                    catch_phase = 0; // @TODO this code do not work for multiple segments
                    Log.d("STRAVA", "in 500");
                    service.sendMsg(CCDataServiceSync.STRAVA_NEAR, (int) meters);
                }
            }
        }
        prev_tm = tm;

        if(! found_near) {
            reset();
        }
    }

    private void reset() {
        catch_phase = 0;
        service.sendMsg(CCDataServiceSync.STRAVA_NEAR, CC.NA);
    }
}
