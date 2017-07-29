package com.example.unknoqn.cc.calc;

import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

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

    public CCCalcStrava(CCDataServiceSync _service) {
        service = _service;
    }

    public void reload() {
        CCStrava strava = CCStrava.getInstance();
        segments = strava.getSegments();
        segments_dst = strava.getSegmentsDst();
    }


    public void calc(int code, long tm, double[] d_arr) {
        if(code != CCDataServiceSync.LATLNG) { return; }
        if(1 >= catch_phase) {
            checkStart(tm, d_arr);
        }
    }

    public void checkStart(long tm, double[] d_arr) {
        Location current_loc = new Location("A");
        current_loc.setLatitude(d_arr[0]);  // la
        current_loc.setLongitude(d_arr[1]); // ln

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
                if(meters < near) {
                    Log.d("STRAVA", "in "+near);
                    catch_phase = 1;
                    near = meters;
                } else if(1 == catch_phase) {
                    catch_phase = 2;
                    dst_to_go = dst;
                    Log.d("STRAVA", "START "+dst_to_go);
                    service.sendData(CCDataServiceSync.STRAVA_INT, prev_tm, 0, dst_to_go);
                } else {
                    Log.d("STRAVA", "in 500");
                    service.sendMsg(CCDataServiceSync.STRAVA_NEAR, (int) meters);
                }
            }
        }
        prev_tm = tm;
    }
}
