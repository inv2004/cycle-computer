package com.example.unknoqn.cc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.garmin.fit.DateTime;
import com.garmin.fit.Decode;
import com.garmin.fit.MesgBroadcaster;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.RecordMesgListener;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class CC extends Activity {

    Intent serviceIntent;

    private Handler toastHandler = new Handler();
    private LinkedList<String> msgs = new LinkedList<>();
    CCSearchTextView searchHR = new CCSearchTextView(false);
    CCSearchTextView searchPWR = new CCSearchTextView(false);
    CCSearchTextView searchCAD = new CCSearchTextView(false);
    CCSearchTextView searchSPD = new CCSearchTextView(true);

    protected void pushMsg(String msg) {
        Log.d("pushMSG:", msg);
        msgs.push(msg);
        showMsg();
    }

    protected void showMsg() {
        String m0 = msgs.poll();
        if (null != m0) {
            Toast.makeText(this, m0, Toast.LENGTH_LONG).show();
            toastHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showMsg();
                }
            }, 1000);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(this.getLocalClassName(), "BEGIN");

        setContentView(R.layout.activity_cc);

        Button btn_lap = (Button) findViewById(R.id.btnLap);
        btn_lap.setEnabled(false);

        View top = findViewById(R.id.top);
        top.setLongClickable(true);
        registerForContextMenu(top);

        resetScreen();

        CCVBarView b = (CCVBarView) findViewById(R.id.swc);
        b.setValue(70);

        PendingIntent resultIntent = createPendingResult(0, new Intent(), 0);
        serviceIntent = new Intent(this, CCDataServiceSync.class);
        serviceIntent.putExtra("pendingIntent", resultIntent);
        serviceIntent.setAction("init");
        startService(serviceIntent);

        ShowFit();
    }

    private void resetScreen() {
        updateTime(-1);
        updatePower(-1);
        updateHR(-1);
    }

    public void onClickStartStop(View v) {
        Button btn = (Button) findViewById(R.id.btnStartStop);
        Button btn_lap = (Button) findViewById(R.id.btnLap);
        if ("START".equals(btn.getText())) {
            serviceIntent.setAction("start");
            startService(serviceIntent);
            btn.setText("STOP");
            btn_lap.setEnabled(true);
        } else {
            serviceIntent.setAction("stop");
            startService(serviceIntent);
            btn.setText("START");
            btn_lap.setEnabled(false);
        }
    }

    public void onClickLap(View v) {

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (0 != requestCode) {
            return;
        }
        ;
        if (CCDataServiceSync.TXT == resultCode) {
            pushMsg(data.getStringExtra("txt"));
        } else if (CCDataServiceSync.TIME == resultCode) {
            updateTime(data.getLongExtra("time", -1));
        } else if (CCDataServiceSync.PWR == resultCode) {
            updatePower(data.getIntExtra("val", -1));
        } else if (CCDataServiceSync.HR == resultCode) {
            updateHR(data.getIntExtra("val", -1));
        } else if (CCDataServiceSync.CAD == resultCode) {
            updateCad(data.getIntExtra("val", -1));
        } else if (CCDataServiceSync.SWC == resultCode) {
            updateSWC(data.getIntExtra("val", -1));
        } else if (CCDataServiceSync.AWC == resultCode) {
            updateAWC(data.getIntExtra("val", -1));
        } else if(CCDataServiceSync.SPD == resultCode) {
            updateSPD(data.getIntExtra("val", -1), data.getFloatExtra("float_val", -1f));
        } else if(CCDataServiceSync.DST == resultCode) {
            updateDST(data.getIntExtra("val", -1), data.getFloatExtra("float_val", -1f));
        }
    }

    protected void updateTime(long time) {
        TextView timeview = (TextView) findViewById(R.id.time);

        long seconds = time / 1000;
        long h = seconds / 3600;
        long m = (seconds / 60) - (h*60);
        long s = seconds % 60;

        String str = String.format("%02d:%02d:%02d", h,m,s);

        timeview.setText(-1 == time ? "--:--:--" : str);
    }

    protected void updateHR(int val) {
        TextView hr = (TextView) findViewById(R.id.hr);
        if (-2 == val) {
            searchHR.start(hr);
        } else if (-1 == val) {
            searchHR.stop();
            hr.setText("--");
        } else {
            searchHR.stop();
            hr.setText(String.valueOf(val));
        }
    }

    protected void updatePower(int val) {
        TextView power = (TextView) findViewById(R.id.power);
        if (-2 == val) {
            searchPWR.start(power);
        } else if (-1 == val) {
            searchPWR.stop();
            power.setText("--");
        } else {
            searchPWR.stop();
            power.setText(String.valueOf(val));
        }
    }

    protected void updateCad(int val) {
        TextView cad = (TextView) findViewById(R.id.cad);
        if (-2 == val) {
            searchCAD.start(cad);
        } else if (-1 == val) {
            searchCAD.stop();
            cad.setText("--");
        } else {
            searchCAD.stop();
            cad.setText(String.valueOf(val));
        }
    }

    protected void updateSWC(int val) {
        CCVBarView swc = (CCVBarView) findViewById(R.id.swc);
        swc.setValue(val);
    }

    protected void updateAWC(int val) {
        CCVBarView awc = (CCVBarView) findViewById(R.id.awc);
        awc.setValue(val);
    }

    protected void updateSPD(int val, float float_val) {
        TextView spd = (TextView) findViewById(R.id.spd);
        if (-1 == val) {
            searchSPD.start(spd);
        } else if (-1 == val) {
            searchSPD.stop();
            spd.setText("--");
        } else {
            searchSPD.stop();
            spd.setText(String.valueOf(float_val));
        }
    }

    protected void updateDST(int val, float float_val) {
        Log.d("updateDST", String.valueOf(float_val));
        TextView dst = (TextView) findViewById(R.id.dst);
        dst.setText(String.format("%.1f km", float_val / 1000));
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.add(0, 1, 0, "Settings");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (1 == item.getItemId()) {
            Intent intent = new Intent(this, CCSettingsActivity.class);
            startActivity(intent);
        }
        return super.onContextItemSelected(item);
    }


    private int c = 0;

    public void ShowFit() {
        try {
            ArrayList[] a = Load();
            Draw(a);
        } catch (IOException e) {
            e.printStackTrace();
            pushMsg("Draw: Exception: "+e.toString());
        }
    }

    public ArrayList[] Load() throws IOException {

        final ArrayList times = new ArrayList();
        final ArrayList values = new ArrayList();

        Decode decode = new Decode();
        MesgBroadcaster broadcaster = new MesgBroadcaster(decode);
        broadcaster.addListener(new RecordMesgListener() {
            @Override
            public void onMesg(RecordMesg rm) {
                if(rm.hasField(RecordMesg.TimestampFieldNum)) {
                    if(rm.hasField(RecordMesg.PowerFieldNum)) {
                        times.add(rm.getTimestamp());
                        values.add(rm.getPower());
                    }
                }
            }
        });

        FileInputStream fin = new FileInputStream(getFilesDir().getCanonicalFile()+"/import/2-3.fit");
        broadcaster.run(fin);
        return new ArrayList[]{times, values};
    }

    public void Draw(ArrayList[] arr) {

        Iterator<DateTime> it1 = arr[0].iterator();
        Iterator<Integer> it2 = arr[1].iterator();

        ArrayList<Entry> e = new ArrayList<>();
        LineChart lc = (LineChart) findViewById(R.id.chart);

        while(it1.hasNext() && it2.hasNext()) {
            long tm = it1.next().getTimestamp();
            long val = it2.next();
            Log.d("RM", tm+" : "+val);
            e.add(new Entry(tm, val));
            LineDataSet lds = new LineDataSet(e, "pwr");
            LineData ld = new LineData(lds);
            lc.setData(ld);
        }

        lc.invalidate();
        pushMsg("COUNT: "+e.size());

    }


}
