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

    boolean test = true;

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

        chart = new CCChart(this);
        chart.setTest(test);

        map = new CCMap(this);

        strava = CCStrava.getInstance();
        strava.init(this);

        resetScreen();

        CCVBarView b = (CCVBarView) findViewById(R.id.swc);
        b.setValue(100);
        b.setEnabled(false);

        PendingIntent resultIntent = createPendingResult(0, new Intent(), 0);
        serviceIntent = new Intent(this, CCDataServiceSync.class);
        serviceIntent.putExtra("pendingIntent", resultIntent);
        serviceIntent.setAction("init");
        startService(serviceIntent);

        if(test) {
            serviceIntent.setAction("test");
            startService(serviceIntent);
        }
    }

    private void resetScreen() {
        updateTime(NA);
        updatePower(0, NA);
        updateHR(NA);
        chart.setCP(300);
        updateInt(0, NA);
    }

    public void registerMiddleTouch() {
        LinearLayout middle = (LinearLayout) findViewById(R.id.middle);
        middle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(MotionEvent.ACTION_DOWN != event.getAction()) { return false; }
                float x = event.getX();
                if(x >= v.getWidth() / 2) {
                } else {
                }
                return true;
            }
        });
    }

    public void onClickStartStop(View v) {
        Button btn = (Button) findViewById(R.id.btnStartStop);
        Button btn_lap = (Button) findViewById(R.id.btnLap);
        if ("START".equals(btn.getText())) {
            serviceIntent.setAction("start");
            startService(serviceIntent);
            btn.setText("STOP");
            btn_lap.setEnabled(true);
            chart.reset();
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
        } else if(CCDataServiceSync.SPD == resultCode) {
            updateSPD(data.getIntExtra("val", NA), data.getFloatExtra("float_val", NA));
        } else if(CCDataServiceSync.DST == resultCode) {
            updateDST(data.getIntExtra("val", NA), data.getFloatExtra("float_val", NA));
        } else if(CCDataServiceSync.AVGPWR == resultCode) {
            updateAVG(data.getIntExtra("val", NA));
        } else if(CCDataServiceSync.LATLNG == resultCode) {
            updateMap(data.getDoubleArrayExtra("double_arr"));
        } else if(CCDataServiceSync.TEST0 == resultCode) {
            if(started) {
                chart.setTEST0(tm, data.getIntExtra("val", NA));
            }
        } else if(CCDataServiceSync.TEST1 == resultCode) {
            if(started) {
                chart.setTEST1(tm, data.getIntExtra("val", NA));
            }
        }
    }

    protected void updateLap(long tm, int val) {
        chart.setLAP(tm, val);
    }

    protected void updateTime(long time) {
        TextView timeview = (TextView) findViewById(NA == int_start ? R.id.time : R.id.int_time);

        if(NA == time) {
            timeview.setText("--:--:--");
        } else {

            long seconds = (NA == int_start ? time : time-int_start) / 1000;
            long h = seconds / 3600;
            long m = (seconds / 60) - (h * 60);
            long s = seconds % 60;

            String str = String.format("%02d:%02d:%02d", h, m, s);
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
//        Log.d("updateDST", String.valueOf(float_val));
        TextView dst = (TextView) findViewById(R.id.dst);
        dst.setText(String.format("%.1f km", float_val / 1000));
    }

    private void setIntMode(boolean x) {
        LinearLayout int_layout = (LinearLayout) findViewById(R.id.int_layout);
        TextView time = (TextView) findViewById(R.id.time);
        TextView dst = (TextView) findViewById(R.id.dst);
        LinearLayout avg_layout = (LinearLayout) findViewById(R.id.avg_layout);

        int yes = x ? View.VISIBLE : View.GONE;
        int no = x ? View.GONE : View.VISIBLE;

        time.setVisibility(no);
        int_layout.setVisibility(yes);
        dst.setVisibility(no);
        avg_layout.setVisibility(yes);
    }

    protected void updateInt(long tm, int val) {
        TextView intText = (TextView) findViewById(R.id.int_text);
        if(0 >= val) {
            int_start = NA;
            intText.setText("INT OVER");
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    setIntMode(false);
                }
            }, test ? 2000 : 10*1000);
        } else {
            int_start = tm;
            intText.setText("INT");
            setIntMode(true);
        }
    }

    protected void updateAVG(int val) {
        TextView avg = (TextView) findViewById(R.id.avg);
        avg.setText(String.valueOf(val));
    }

    protected void updateMap(double[] double_arr) {
        map.setLatLng(double_arr[0], double_arr[1]);
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
}
