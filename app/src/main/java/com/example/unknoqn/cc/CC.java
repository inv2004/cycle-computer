package com.example.unknoqn.cc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedList;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class CC extends FragmentActivity {
    final static int NA = -1;
    final static int SEARCH = -2;

    boolean test = false;

    enum Mode {MAP, CHART, STRAVA}
    Mode mode = Mode.MAP;

    Intent serviceIntent;
    private Handler toastHandler = new Handler();
    private LinkedList<String> msgs = new LinkedList<>();
    CCSearchTextView searchHR = new CCSearchTextView(false);
    CCSearchTextView searchPWR = new CCSearchTextView(false);
    CCSearchTextView searchCAD = new CCSearchTextView(false);
    CCSearchTextView searchSPD = new CCSearchTextView(true);

    CCChart chart;
    CCMap map;
    CCStrava strava;

    boolean started = false;
    long int_start = NA;
    long strava_start = NA;
    boolean freeze_time = false;

    Handler h = new Handler();

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

        setContentView(R.layout.activity_cc);

        Button btn_lap = (Button) findViewById(R.id.btnLap);
        btn_lap.setEnabled(false);

        View top = findViewById(R.id.top);
        top.setLongClickable(true);
        registerForContextMenu(top);

        registerMiddleTouch();

        CCVBarView b = (CCVBarView) findViewById(R.id.swc);
        b.setValue(100);
        b.setEnabled(false);

        PendingIntent resultIntent = createPendingResult(0, new Intent(), 0);
        serviceIntent = new Intent(this, CCDataServiceSync.class);
        serviceIntent.putExtra("pendingIntent", resultIntent);

        if(test) {
            serviceIntent.setAction("test");
            startService(serviceIntent);
        }

        serviceIntent.setAction("init");
        startService(serviceIntent);

        chart = new CCChart(this);
        chart.setTest(test);

        map = CCMap.getInstance();
        map.init(this);

        strava = CCStrava.getInstance();
        strava.init(this);

        resetScreen();

        serviceIntent.setAction("start");
        startService(serviceIntent);
    }

    public void reloadService() {
        serviceIntent.setAction("reload");
        startService(serviceIntent);
    }

    private void resetScreen() {
        updateTime(NA);
        updatePower(0, NA);
        updateHR(NA);
        chart.setCP(300);
        setMode(0);

        switchMode();
    }

    private void switchMode() {
        Log.d("CC", "switchMode");
        if(mode == Mode.MAP) {
            map.disable();
            chart.enable();
            mode = Mode.CHART;
        } else if(mode == Mode.CHART) {
            chart.disable();
            map.enable();
            mode = Mode.MAP;
        }
    }

    public void registerMiddleTouch() {
        LinearLayout middle = (LinearLayout) findViewById(R.id.middle);
        middle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(MotionEvent.ACTION_DOWN != event.getAction()) { return false; }
                float x = event.getX();
                if(x >= v.getWidth() / 2) {
                    // @TODO on-chart click - change mode ?
                } else {
                    // ^^^
                }
                return true;
            }
        });
    }

    public void onClickMode(View v) {
        switchMode();
    }

    public void onClickLap(View v) {

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (0 != requestCode) {
            return;
        }

        long tm = data.getLongExtra("time", NA);
        if(started && NA != tm) {
            updateTime(tm);
        }

        if(CCDataServiceSync.TIME == resultCode) {
            if(!started) { started = true; }
            updateTime(tm);
        } if (CCDataServiceSync.TXT == resultCode) {
            pushMsg(data.getStringExtra("txt"));
        } else if (CCDataServiceSync.PWR == resultCode) {
            updatePower(tm, data.getIntExtra("val", NA));
        } else if (CCDataServiceSync.HR == resultCode) {
            updateHR(data.getIntExtra("val", NA));
        } else if (CCDataServiceSync.CAD == resultCode) {
            updateCad(data.getIntExtra("val", NA));
        } else if (CCDataServiceSync.SWC == resultCode) {
            updateSWC(data.getIntExtra("val", NA));
        } else if (CCDataServiceSync.AWC == resultCode) {
            updateAWC(tm, data.getIntExtra("val", NA));
        } else if (CCDataServiceSync.LAP == resultCode) {
            updateInt(tm, data.getIntExtra("val", NA));
            updateLap(tm, data.getIntExtra("val", NA));
        } else if (CCDataServiceSync.STRAVA_NEAR == resultCode) {
            stravaMsg(data.getIntExtra("val", NA));
        } else if (CCDataServiceSync.STRAVA_INT == resultCode) {
            updateStrava(tm, data.getFloatExtra("float_val", NA));
        } else if (CCDataServiceSync.SPD == resultCode) {
            updateSPD(data.getIntExtra("val", NA), data.getFloatExtra("float_val", NA));
        } else if (CCDataServiceSync.DST == resultCode) {
            updateDST(data.getIntExtra("val", NA), data.getFloatExtra("float_val", NA));
        } else if (CCDataServiceSync.AVGPWR == resultCode) {
            updateAVG(data.getIntExtra("val", NA));
        } else if (CCDataServiceSync.LATLNG == resultCode) {
            updateLoc(data.getDoubleArrayExtra("double_arr"));
        } else if (CCDataServiceSync.TEST0 == resultCode) {
            if(started) {
                chart.setTEST0(tm, data.getIntExtra("val", NA));
            }
        } else if (CCDataServiceSync.TEST1 == resultCode) {
            if(started) {
                chart.setTEST1(tm, data.getIntExtra("val", NA));
            }
        }
    }

    private void updateStrava(long tm, float val) {
        Log.d("updateStrava", ""+val);
        if(NA == strava_start) {
            strava_start = tm;
            Log.d("updateStrava", "ss: "+strava_start);
            setMode(2);
        }
        TextView dst = (TextView) findViewById(R.id.dst_avg);
        dst.setText(String.format("%d m", (int) val));
    }

    protected void updateLap(long tm, int val) {
        chart.setLAP(tm, val);
    }

    protected void updateTime(long time) {
        if(freeze_time) { return; }
        TextView timeview = (TextView) findViewById(R.id.time);

        if(NA == time) { // @TODO not reachable
            if(NA == int_start) {
                timeview.setText("--:--:--");
            } else {
                timeview.setText("--:--");
            }
        } else {
            long mseconds = time;
            if(NA != int_start) {
                mseconds -= int_start;
            } else if(NA != strava_start) {
                mseconds -= strava_start;
            }
            long seconds = mseconds / 1000;
            long h = seconds / 3600;
            long m = (seconds / 60) - (h * 60);
            long s = seconds % 60;

            String str;
            if(NA == int_start && NA == strava_start) {
                str = String.format("%02d:%02d:%02d", h, m, s);
            } else {
                str = String.format("%02d:%02d", m, s);
            }
            timeview.setText(str);
        }
    }

    protected void updateHR(int val) {
        TextView hr = (TextView) findViewById(R.id.hr);
        if (SEARCH == val) {
            searchHR.start(hr);
        } else if (NA == val) {
            searchHR.stop();
            hr.setText("--");
        } else {
            searchHR.stop();
            hr.setText(String.valueOf(val));
        }
    }

    protected void updatePower(long tm, int val) {
        TextView power = (TextView) findViewById(R.id.power);
        if (SEARCH == val) {
            searchPWR.start(power);
        } else if (NA == val) {
            searchPWR.stop();
            power.setText("--");
        } else {
            searchPWR.stop();
            power.setText(String.valueOf(val));
            if(started) {
                chart.setPWR(tm, val);
            }
        }
    }

    protected void updateCad(int val) {
        TextView cad = (TextView) findViewById(R.id.cad);
        if (SEARCH == val) {
            searchCAD.start(cad);
        } else if (NA == val) {
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

    protected void updateAWC(long tm, int val) {
        CCVBarView awc = (CCVBarView) findViewById(R.id.awc);
        awc.setValue(val);
        chart.setAWC(tm, val);
    }

    protected void updateSPD(int val, float float_val) {
        TextView spd = (TextView) findViewById(R.id.spd);
        if (SEARCH == val) {
            searchSPD.start(spd);
        } else if (NA == val) {
            searchSPD.stop();
            spd.setText("--");
        } else {
            searchSPD.stop();
            spd.setText(String.format("%.2f", float_val * 18 / 5));
        }
    }

    protected void updateDST(int val, float float_val) {
        Log.d("updateDST", strava_start + " / " + String.valueOf(float_val));
        if(freeze_time || NA != strava_start || NA != int_start) { return; }
        TextView dst = (TextView) findViewById(R.id.dst_avg);
        dst.setText(String.format("%.1f km", float_val / 1000));
    }

    private void setMode(int x) {
        TextView msg1 = (TextView) findViewById(R.id.msg1);
        TextView msg2 = (TextView) findViewById(R.id.msg2);
        if(0 == x) { // main
            msg1.setText("");
            msg2.setText("");
        } else if(1 == x) { // interval
            msg1.setText("INT");
            msg2.setText("avg");
        } else { // strava
            msg1.setText("STRAVA");
            msg2.setText("LEFT");
        }
    }

    protected void updateInt(long tm, int val) {
        TextView msg1 = (TextView) findViewById(R.id.msg1);
        if(0 >= val) {
            int_start = NA;
            freeze_time = true;
            msg1.setText("INT OVER");
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    setMode(0);
                    freeze_time = false;
                }
            }, test ? 2000 : 10*1000);
        } else {
            int_start = tm;
            setMode(1);
        }
    }

    protected void updateAVG(int val) {
        if(NA == int_start) { return; }
        TextView avg = (TextView) findViewById(R.id.dst_avg);
        avg.setText(String.valueOf(val));
    }

    protected void updateLoc(double[] double_arr) {
        map.setLatLng(double_arr[0], double_arr[1], double_arr[2]);
    }

    public void stravaMsg(int x) {
        if(freeze_time || NA != int_start) { return; }
        TextView msg1 = (TextView) findViewById(R.id.msg1);
        msg1.setText(String.format("STRAVA IN %d m", x));
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if(started) {
            menu.add(0, 2, 0, "Stop");
        } else {
            menu.add(0, 1, 0, "Start");
        }
        menu.add(1, 3, 1, "Settings");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (1 == item.getItemId()) {
            serviceIntent.setAction("start");
            startService(serviceIntent);
            Button btn_lap = (Button) findViewById(R.id.btnLap);
            btn_lap.setEnabled(true);
            chart.reset();
        } else if (2 == item.getItemId()) {
            serviceIntent.setAction("stop");
            startService(serviceIntent);
            Button btn_lap = (Button) findViewById(R.id.btnLap);
            btn_lap.setEnabled(false);
        } else if (3 == item.getItemId()) {
            Intent intent = new Intent(this, CCSettingsActivity.class);
            startActivity(intent);
        }
        return super.onContextItemSelected(item);
    }
}
