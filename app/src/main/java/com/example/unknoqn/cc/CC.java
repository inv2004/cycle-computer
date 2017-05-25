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

    Intent serviceIntent;

    private Handler toastHandler = new Handler();
    private LinkedList<String> msgs = new LinkedList<>();
    CCSearchAntText searchHR = new CCSearchAntText(false);
    CCSearchAntText searchPWR = new CCSearchAntText(false);
    CCSearchAntText searchCAD = new CCSearchAntText(false);
    CCSearchAntText searchSPD = new CCSearchAntText(true);

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
        serviceIntent = new Intent(this, CCServiceAntSync.class);
        serviceIntent.putExtra("pendingIntent", resultIntent);
        serviceIntent.setAction("init");
        startService(serviceIntent);

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
        if (CCServiceAntSync.TXT == resultCode) {
            pushMsg(data.getStringExtra("txt"));
        } else if (CCServiceAntSync.TIME == resultCode) {
            updateTime(data.getLongExtra("time", -1));
        } else if (CCServiceAntSync.PWR == resultCode) {
            updatePower(data.getIntExtra("val", -1));
        } else if (CCServiceAntSync.HR == resultCode) {
            updateHR(data.getIntExtra("val", -1));
        } else if (CCServiceAntSync.CAD == resultCode) {
            updateCad(data.getIntExtra("val", -1));
        } else if (CCServiceAntSync.SWC == resultCode) {
            updateSWC(data.getIntExtra("val", -1));
        } else if (CCServiceAntSync.AWC == resultCode) {
            updateAWC(data.getIntExtra("val", -1));
        } else if(CCServiceAntSync.SPD == resultCode) {
            updateSPD(data.getIntExtra("val", -1), data.getFloatExtra("val2", -1f));
        } else if(CCServiceAntSync.DST == resultCode) {
            updateDST(data.getIntExtra("val", -1), data.getFloatExtra("val2", -1f));
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

    protected void updateSPD(int val, float val2) {
        TextView spd = (TextView) findViewById(R.id.spd);
        if (-2 == val) {
            searchSPD.start(spd);
        } else if (-1 == val) {
            searchSPD.stop();
            spd.setText("--");
        } else {
            searchSPD.stop();
            spd.setText(String.valueOf(val2));
        }
    }

    protected void updateDST(int val, float val2) {
        Log.d("updateDST", String.valueOf(val2));
        TextView dst = (TextView) findViewById(R.id.dst);
        dst.setText(String.format("%.1f km", val2 / 1000));
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
