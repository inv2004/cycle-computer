package com.example.unknoqn.cc.calc;

import android.content.SharedPreferences;
import android.location.Location;
import android.widget.Toast;

import com.example.unknoqn.cc.CCDataServiceSync;
import com.example.unknoqn.cc.CCStrava;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by unknown on 7/27/2017.
 */

public class CCCalcStrava {
    CCDataServiceSync service;

    List<List<LatLng>> segments;

    float near = 15.0f;

    public CCCalcStrava(CCDataServiceSync _service) {
        service = _service;
    }

    public void reload() {
        CCStrava strava = CCStrava.getInstance();
        segments = strava.getSegments();
    }


    public void calc(int code, long tm, double[] d_arr) {
        if(code != CCDataServiceSync.LATLNG) { return; }

        Location current_loc = new Location("A");
        current_loc.setLatitude(d_arr[0]);  // la
        current_loc.setLongitude(d_arr[1]); // ln

        Iterator<List<LatLng>> it = segments.iterator();
        while(it.hasNext()) {
            List<LatLng> seg = it.next();
            LatLng ll = seg.get(0);
            Location l = new Location("B");
            l.setLatitude(ll.latitude);
            l.setLongitude(ll.longitude);
            float meters = l.distanceTo(current_loc);
            if(meters <= 500) {
//                service.sendMsg(CCDataServiceSync.STRAVA_NEAR, (int) meters);
            }
            if(meters < near) {
                near = meters;
            } else {
//                    cc.stravaStart(meters);
            }
        }
    }
}
