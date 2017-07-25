package com.example.unknoqn.cc;

import android.graphics.Color;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

/**
 * Created by unknown on 7/20/2017.
 */

public class CCMap implements OnMapReadyCallback {
    GoogleMap map;
    CC cc;
    LatLng prev;
    Polyline pl;
    long updateCounter = 0;
    boolean current_pos = true;
    boolean moving = false;

    CCMap(CC _cc) {
        cc = _cc;
        SupportMapFragment mf = (SupportMapFragment) cc.getSupportFragmentManager().findFragmentById(R.id.map);
        mf.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap _map) {
        map = _map;
        map.setMyLocationEnabled(true);

        map.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                current_pos = true;
                moving = true;
                return false;
            }
        });

        map.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() { // @TODO its very unstable
                Log.d("MOVING", ""+moving);
                if(moving) {
                    moving = false;
                } else {
                    current_pos = false;
                }
            }
        });

        pl = map.addPolyline(new PolylineOptions().width(3).color(Color.BLUE));
    }

    public void setLatLng(double la, double ln) {
        if(map == null) { return; }

        updateCounter++;
        LatLng ll = new LatLng(la, ln);

        if(0 == updateCounter % 10) {
            List<LatLng> l = pl.getPoints();
            l.add(ll);
            pl.setPoints(l);
        }

        Log.d("CURRENT_POS", ""+current_pos);
        if(current_pos) {
            moving = true;
            Log.d("MOVING", "to true");
            if (null == prev) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(ll, 15));
            } else {
                map.moveCamera(CameraUpdateFactory.newLatLng(ll));
            }
        }
        prev = ll;
    }

    public void disable() {
        SupportMapFragment mf = (SupportMapFragment) cc.getSupportFragmentManager().findFragmentById(R.id.map);
        mf.getView().setVisibility(View.GONE); // @TODO how to disable?
    }

    public void enable() {
        SupportMapFragment mf = (SupportMapFragment) cc.getSupportFragmentManager().findFragmentById(R.id.map);
        mf.getView().setVisibility(View.VISIBLE);
    }
}
