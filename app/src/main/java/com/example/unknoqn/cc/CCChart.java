package com.example.unknoqn.cc;

import android.graphics.Color;
import android.util.Log;

import com.garmin.fit.Decode;
import com.garmin.fit.MesgBroadcaster;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.RecordMesgListener;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by unknoqn on 5/26/2017.
 */

public class CCChart {
    CC cc;
    long start_time = 0;
    LineChart chart;
    LineData ld = new LineData();
    LineDataSet lds_pwr = new LineDataSet(new ArrayList<Entry>(), "pwr");
    LineDataSet lds_awc = new LineDataSet(new ArrayList<Entry>(), "awc");
//    LineDataSet lds_hr = new LineDataSet(new ArrayList<Entry>(), "hr");

    public CCChart(CC g_cc) {
        cc = g_cc;
        chart = (LineChart) cc.findViewById(R.id.chart);

        lds_pwr.setColor(Color.YELLOW);
        lds_pwr.setDrawCircles(false);
        lds_pwr.setLineWidth(0.5f);
        lds_pwr.setAxisDependency(YAxis.AxisDependency.LEFT);
        //lds_pwr.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        lds_pwr.addEntry(new Entry(0, 0));
        ld.addDataSet(lds_pwr);

        lds_awc.setColor(Color.RED);
        lds_awc.setDrawCircles(false);
        lds_awc.setLineWidth(0.5f);
        lds_awc.setAxisDependency(YAxis.AxisDependency.RIGHT);
        //lds_awc.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        lds_awc.addEntry(new Entry(0, 0));
        ld.addDataSet(lds_awc);

        chart.setViewPortOffsets(0,0,0,0);
        chart.setData(ld);

/*        for(int i = 0; i<100; i++) {
                lds_awc.addEntry(new Entry(i, 0 == i % 5 ? 0 : i));
            ld.notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.invalidate();
                lds_pwr.addEntry(new Entry(i, 200 - (0 == i % 3 ? 0 : i)));
            ld.notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.invalidate();
        }
        //lds_awc.notifyDataSetChanged();
*/
    }

    public void start(long tm) {
        if(0 == start_time) {
            start_time = tm;
            lds_pwr.removeFirst();
            lds_awc.removeFirst();
        }
    }

    public void setPWR(long tm, int val) {
        if(0 == start_time) { return; }
        long t = (tm-start_time) / 1000;
        Log.d("PWR", t + "/" + val);
        lds_pwr.addEntry(new Entry(t, val));
        ld.notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    public void setAWC(long tm, int val) {
        if(0 == start_time) { return; }
        long t = (tm-start_time) / 1000;
        Log.d("AWC", t + "/" + val);
        lds_awc.addEntry(new Entry(t, val));
        /*ld.notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.invalidate();*/
    }

}
