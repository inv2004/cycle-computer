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

import java.util.LinkedList;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class CC extends Activity {
    boolean test = true;
    long start_time = 0;

    Intent serviceIntent;

    private Handler toastHandler = new Handler();
    private LinkedList<String> msgs = new LinkedList<>();
    CCSearchTextView searchHR = new CCSearchTextView(false);
    CCSearchTextView searchPWR = new CCSearchTextView(false);
    CCSearchTextView searchCAD = new CCSearchTextView(false);
    CCSearchTextView searchSPD = new CCSearchTextView(true);
    CCChart chart;

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

        if(null == chart) {
            Log.d(this.toString(), "DEBUG1");
            chart = new CCChart(this);
        }

    }

    private void resetScreen() {
        updateTime(-1);
        updatePower(0, -1);
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
            start_time = 0;
        }
    }

    public void onClickLap(View v) {

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (0 != requestCode) {
            return;
        }
        if (CCDataServiceSync.TXT == resultCode) {
            pushMsg(data.getStringExtra("txt"));
        } else if (CCDataServiceSync.TIME == resultCode) {
            updateTime(data.getLongExtra("time", -1));
        } else if (CCDataServiceSync.PWR == resultCode) {
            if(test) { updateTime(data.getLongExtra("time", -1)); }
            updatePower(data.getLongExtra("time", -1), data.getIntExtra("val", -1));
        } else if (CCDataServiceSync.HR == resultCode) {
            updateHR(data.getIntExtra("val", -1));
        } else if (CCDataServiceSync.CAD == resultCode) {
            updateCad(data.getIntExtra("val", -1));
        } else if (CCDataServiceSync.SWC == resultCode) {
            updateSWC(data.getIntExtra("val", -1));
        } else if (CCDataServiceSync.AWC == resultCode) {
            if(test) { updateTime(data.getLongExtra("time", -1)); }
            updateAWC(data.getLongExtra("time", -1), data.getIntExtra("val", -1));
        } else if (CCDataServiceSync.LAP == resultCode) {
            updateLap(data.getLongExtra("time", -1), data.getIntExtra("val", -1));
        } else if(CCDataServiceSync.SPD == resultCode) {
            updateSPD(data.getIntExtra("val", -1), data.getFloatExtra("float_val", -1f));
        } else if(CCDataServiceSync.DST == resultCode) {
            updateDST(data.getIntExtra("val", -1), data.getFloatExtra("float_val", -1f));
        }
    }

    protected void updateLap(long tm, int val) {
        chart.setLAP(tm, val);
    }

    protected void updateTime(long time) {
        TextView timeview = (TextView) findViewById(R.id.time);

        if(0 > time) {
            if(0 == start_time) {
                timeview.setText("--:--:--");
            }
        } else {
            if (0 == start_time) {
                start_time = time;
                chart.setCP(300); // !!!
                chart.start(start_time);
            }

            long seconds = (time - start_time) / 1000;
            long h = seconds / 3600;
            long m = (seconds / 60) - (h * 60);
            long s = seconds % 60;

            String str = String.format("%02d:%02d:%02d", h, m, s);
            timeview.setText(str);
        }
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

    protected void updatePower(long tm, int val) {
        TextView power = (TextView) findViewById(R.id.power);
        if (-2 == val) {
            searchPWR.start(power);
        } else if (-1 == val) {
            searchPWR.stop();
            power.setText("--");
        } else {
            searchPWR.stop();
            power.setText(String.valueOf(val));
            chart.setPWR(tm, val);
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

    protected void updateAWC(long tm, int val) {
        CCVBarView awc = (CCVBarView) findViewById(R.id.awc);
        awc.setValue(val);
        chart.setAWC(tm, val);
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
}
