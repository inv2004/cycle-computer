package com.example.unknoqn.cc;

import android.graphics.Color;
import android.util.Log;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;

/**
 * Created by unknoqn on 5/26/2017.
 */

public class CCChart {
    CC cc;
    long start_time = 0;
    boolean stated = false;
    long prev_tm = 0;
    LineChart chart;
    LineData ld = new LineData();
    LineDataSet lds_pwr = new LineDataSet(new ArrayList<Entry>(), "pwr");
    LineDataSet lds_awc = new LineDataSet(new ArrayList<Entry>(), "awc");
    LineDataSet lds_t0 = new LineDataSet(new ArrayList<Entry>(), "t0");
    LineDataSet lds_t1 = new LineDataSet(new ArrayList<Entry>(), "t0");

    public CCChart(CC g_cc) {
        cc = g_cc;
        chart = (LineChart) cc.findViewById(R.id.chart);

        lds_pwr.setColor(Color.YELLOW);
        lds_pwr.setDrawCircles(false);
        lds_pwr.setLineWidth(0.5f);
        lds_pwr.setAxisDependency(YAxis.AxisDependency.LEFT);
        lds_pwr.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        lds_pwr.addEntry(new Entry(0, 0));
        ld.addDataSet(lds_pwr);

        lds_awc.setColor(Color.RED);
        lds_awc.setDrawCircles(false);
        lds_awc.setLineWidth(1f);
        lds_awc.setAxisDependency(YAxis.AxisDependency.RIGHT);
        lds_awc.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        lds_awc.addEntry(new Entry(0, 0));
        ld.addDataSet(lds_awc);

        lds_t0.setColor(Color.WHITE);
        lds_t0.setDrawCircles(false);
        lds_t0.setLineWidth(1f);
        lds_t0.setAxisDependency(YAxis.AxisDependency.LEFT);
        lds_t0.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        lds_t0.addEntry(new Entry(0, 0));
  //      ld.addDataSet(lds_t0);

        lds_t1.setColor(Color.BLUE);
        lds_t1.setDrawCircles(false);
        lds_t1.setLineWidth(1f);
        lds_t1.setAxisDependency(YAxis.AxisDependency.LEFT);
        lds_t1.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        lds_t1.addEntry(new Entry(0, 0));
//        ld.addDataSet(lds_t1);

        chart.setViewPortOffsets(0,0,0,0);
        chart.setData(ld);
        chart.getXAxis().setDrawAxisLine(true);
        chart.getXAxis().setDrawGridLines(false);
        chart.getAxisLeft().setDrawAxisLine(true);
        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisRight().setEnabled(false);
    }

    public void setCP(long cp) {
        LimitLine ll = new LimitLine(cp);
//        ll.setTextColor(Color.YELLOW);
        ll.setLabelPosition(LimitLine.LimitLabelPosition.LEFT_TOP);
        chart.getAxisLeft().addLimitLine(ll);
        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    public void reset() {
    }

    public void setPWR(long tm, int val) {
        long t = tm / 1000;
        lds_pwr.addEntry(new Entry(t, val));
        if(20*1000 < tm - prev_tm) {
            ld.notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.invalidate();
            prev_tm = tm;
        }
    }

    public void setAWC(long tm, int val) {
        long t = tm / 1000;
        lds_awc.addEntry(new Entry(t, val));
    }

    public void setTEST0(long tm, int val) {
        long t = tm / 1000;
        lds_t0.addEntry(new Entry(t, val));
    }

    public void setTEST1(long tm, int val) {
        long t = tm / 1000;
        lds_t1.addEntry(new Entry(t, val));
    }

    public void setLAP(long tm, int val) {
        Log.d("CHART", "LAP");
        long t = tm / 1000;
        chart.getXAxis().addLimitLine(new LimitLine(t, "LAP"));
    }

}
