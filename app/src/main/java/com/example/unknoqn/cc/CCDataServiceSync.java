package com.example.unknoqn.cc;

import android.Manifest;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;
import com.example.unknoqn.cc.calc.CCCalcDST;
import com.example.unknoqn.cc.calc.CCCalcWC;
import com.garmin.fit.Decode;
import com.garmin.fit.MesgBroadcaster;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.RecordMesgListener;
import com.github.mikephil.charting.data.Entry;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class CCDataServiceSync extends Service {

    public static int TXT = 0;
    public static int PWR = 2;
    public static int HR = 3;
    public static int CAD = 4;
    public static int SPD = 5;
    public static int DST = 7;
    public static int TIME = 10;
    public static int SWC = 11;
    public static int AWC = 12;
    public static int DELTA_DST = 13;

    private boolean test = false;
    private PendingIntent intent2;
    private long hrCounter;

    private Timer timer = new Timer();
    private CCAntFit fit = new CCAntFit(this);
    private CCCalcWC calcWC = new CCCalcWC(this);
    private CCCalcDST calcDST = new CCCalcDST(this);

    LocationManager locationManager;
    Location prev_location;
    long startTime;

    public CCDataServiceSync() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(this.toString(), "onStart, Thread: " + Thread.currentThread().toString());
        if (null != intent) {
            intent2 = intent.getParcelableExtra("pendingIntent");
        }

        if ("init".equals(intent.getAction())) {
            searchAll();
        } else if ("start".equals(intent.getAction())) {
            startTimer();
        } else if ("stop".equals(intent.getAction())) {
            stopTimer();
        } else if ("test".equals(intent.getAction())) {
            test = true;
        }

        return super.onStartCommand(intent, flags, startId);
    }

    public void sendToUI(String res) {
        if (null == intent2) {
            return;
        }
        Intent result = new Intent();
        result.putExtra("txt", res);
        try {
            intent2.send(this, TXT, result);
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    public void sendTime(long time) {
        if (null == intent2) {
            return;
        }
        Intent result = new Intent();
        result.putExtra("time", time);
        try {
            intent2.send(this, TIME, result);
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(int code, int i) {
        if (null == intent2) {
            return;
        }
        Intent result = new Intent();
        result.putExtra("val", i);
        try {
            intent2.send(this, code, result);
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    public void sendData(int code, long time, int i) {
        sendData(code, time, i, 0f);
    }

    public void sendData(int code, long time, int i, float f) {
        if (null == intent2) { return; }

        fit.log(code, time, i, f);

        calcWC.calc(code, time, i);
        calcDST.calc(code, time, f);

        Intent result = new Intent();
        result.putExtra("time", time);
        result.putExtra("val", i);
        result.putExtra("float_val", f);
        try {
            intent2.send(this, code, result);
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    protected void searchAll() {
        searchPower();
        searchHR();
        searchGPS();
    }

    protected void searchPower() {
        sendMsg(PWR, -2);
        sendMsg(CAD, -2);

        if (test) { return; }

        AntPluginPcc.IPluginAccessResultReceiver<AntPlusBikePowerPcc> mResultReceiver = new AntPluginPcc.IPluginAccessResultReceiver<AntPlusBikePowerPcc>() {
            @Override
            public void onResultReceived(AntPlusBikePowerPcc result, RequestAccessResult resultCode, DeviceState initialDeviceState) {
                if (resultCode == RequestAccessResult.SUCCESS) {
                    sendToUI("FOUND " + result.getDeviceName() + "\n" + result.getAntDeviceNumber());
                    subscribePower(result);
                } else if (resultCode == RequestAccessResult.SEARCH_TIMEOUT) {
                    sendToUI("Timeout: Power");
                    searchPower();
                } else {
                    sendToUI("Error: Power: " + resultCode.toString());
                }
            }
        };
        AntPluginPcc.IDeviceStateChangeReceiver mStateReceiver = new AntPluginPcc.IDeviceStateChangeReceiver() {
            @Override
            public void onDeviceStateChange(DeviceState newDeviceState) {
                Log.d(this.toString(), "New state:" + newDeviceState.toString());
                if (newDeviceState == DeviceState.DEAD || newDeviceState == DeviceState.CLOSED) {
                    sendToUI("State: " + newDeviceState.toString());
                }
            }
        };

        AntPlusBikePowerPcc.requestAccess(this, 0, 0, mResultReceiver, mStateReceiver);

    }

    protected void searchHR() {
        sendMsg(HR, -2);

        if (test) { return; }

        AntPluginPcc.IPluginAccessResultReceiver<AntPlusHeartRatePcc> mResultReceiver = new AntPluginPcc.IPluginAccessResultReceiver<AntPlusHeartRatePcc>() {
            @Override
            public void onResultReceived(AntPlusHeartRatePcc result, RequestAccessResult resultCode, DeviceState initialDeviceState) {
                if (resultCode == RequestAccessResult.SUCCESS) {
                    sendToUI("FOUND " + result.getDeviceName() + "\n" + result.getAntDeviceNumber());
                    subscribeHR(result);
                } else if (resultCode == RequestAccessResult.SEARCH_TIMEOUT) {
                    sendToUI("Timeout: HR");
                    searchHR();
                } else {
                    sendToUI("Error: HR: " + resultCode.toString());
                }
            }
        };
        AntPluginPcc.IDeviceStateChangeReceiver mStateReceiver = new AntPluginPcc.IDeviceStateChangeReceiver() {
            @Override
            public void onDeviceStateChange(DeviceState newDeviceState) {
                Log.d(this.toString(), "New state:" + newDeviceState.toString());
                if (newDeviceState == DeviceState.DEAD || newDeviceState == DeviceState.CLOSED) {
                    sendToUI("State: " + newDeviceState.toString());
                }
            }
        };

        AntPlusHeartRatePcc.requestAccess(this, 0, 0, mResultReceiver, mStateReceiver);

    }

    public void searchGPS() {
        if (null == locationManager) {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        }

        sendMsg(SPD, -2);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            sendToUI("GPS: Disabled by permissions");
            sendMsg(SPD, -1);
            return;
        }

        LocationListener ll = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.d(this.toString(), "Location: " + location);

                Iterator<String> it = location.getExtras().keySet().iterator();
                if(location.hasSpeed()) {
                    sendData(SPD, System.currentTimeMillis(), 0, location.getSpeed());
                } else {
                    sendMsg(SPD, -1);
                }
//                Location prev = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if(null != prev_location) {
                    float d = location.distanceTo(prev_location);
                    Log.d("DEBUG2", String.valueOf(d));
                    sendData(DELTA_DST, System.currentTimeMillis(), 0, d);
                }
                prev_location = location;
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.d(this.toString(), "STATUS: "+status);
                if(LocationProvider.AVAILABLE == status) {
                    sendMsg(SPD, -1);
                } else if(LocationProvider.OUT_OF_SERVICE == status) {
                    sendMsg(SPD, -2);
                } else if(LocationProvider.TEMPORARILY_UNAVAILABLE == status) {
                    sendMsg(SPD, -2);
                }
            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.d(this.toString(), "GPS enabled");
                sendMsg(SPD, -1);
            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.d(this.toString(), "GPS disabled");
                sendMsg(SPD, -2);
            }
        };

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1 * 1000, 10, ll);
    }


    protected void subscribePower(AntPlusBikePowerPcc pcc) {
        sendMsg(PWR, -1);
        sendMsg(CAD, -1);
        pcc.subscribeCalculatedPowerEvent(new AntPlusBikePowerPcc.ICalculatedPowerReceiver() {
            @Override
            public void onNewCalculatedPower(long l, EnumSet<EventFlag> enumSet, AntPlusBikePowerPcc.DataSource dataSource, BigDecimal bigDecimal) {
                Log.d("PWR: ", l + " / " + dataSource.toString() + " / " + bigDecimal.toString());
                sendData(PWR, l, bigDecimal.intValue());
            }
        });
        pcc.subscribeCalculatedCrankCadenceEvent(new AntPlusBikePowerPcc.ICalculatedCrankCadenceReceiver() {
            @Override
            public void onNewCalculatedCrankCadence(long l, EnumSet<EventFlag> enumSet, AntPlusBikePowerPcc.DataSource dataSource, BigDecimal bigDecimal) {
                Log.d("CAD: ", l + " / " + dataSource.toString() + " / " + bigDecimal.toString());
                sendData(CAD, l, bigDecimal.intValue());
            }
        });
    }

    protected  void subscribePowerTest() {
        sendMsg(PWR, -1);
        sendMsg(CAD, -1);
        final int[] tmp = {100};
        Timer timer2 = new Timer();
        timer2.schedule(new TimerTask() {
            @Override
            public void run() {
                tmp[0] += (new Random()).nextInt(30)-15;
                sendData(PWR, System.currentTimeMillis(), 210+(new Random()).nextInt(80));
                sendData(CAD, System.currentTimeMillis(), 90+(new Random()).nextInt(30)-15);
            }
        }, 0, 1000);
    }

    protected void subscribeHR(AntPlusHeartRatePcc pcc) {
        sendMsg(HR, -1);

        hrCounter = -1;
        pcc.subscribeHeartRateDataEvent(new AntPlusHeartRatePcc.IHeartRateDataReceiver() {
            @Override
            public void onNewHeartRateData(long l, EnumSet<EventFlag> enumSet, int i, long l1, BigDecimal bigDecimal, AntPlusHeartRatePcc.DataState dataState) {
                if(hrCounter+3 < l1) {
                    Log.d("HR: ", i + " / " + l1 + " / " + l + " / " + bigDecimal + " / " + dataState.toString());
                    sendData(HR, l, i);
                    hrCounter = l1;
                }
            }
        });
    }

    protected void subscribeHRTest() {
        sendMsg(HR, -1);
        Timer timer2 = new Timer();
        timer2.schedule(new TimerTask() {
            @Override
            public void run() {
                sendData(HR, System.currentTimeMillis(), 130+(new Random().nextInt(30)));
            }
        }, 0, 1000);
    }

    protected void startTimer() {
        fit.start();
        calcWC.start(System.currentTimeMillis());
        calcDST.start();

        if(test) {
            Play();
        } else {
            Log.d(toString(), "START");
            if(null != timer) { timer.cancel(); }
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    sendTime(System.currentTimeMillis());
                }
            }, 0, 1000);
        }
    }

    protected void stopTimer() {
        fit.stop();
        calcWC.stop();
        calcDST.stop();
        Log.d(toString(), "STOP");
        timer.cancel();
        timer = null;
    }

    public void Play() {
        final LinkedList<RecordMesg> fit = new LinkedList<>();

        Decode decode = new Decode();
        MesgBroadcaster broadcaster = new MesgBroadcaster(decode);
        broadcaster.addListener(new RecordMesgListener() {
            @Override
            public void onMesg(RecordMesg rm) {
                fit.add(rm);
            }
        });

        try {
            FileInputStream fin = new FileInputStream(this.getFilesDir().getCanonicalFile()+"/import/13.fit");
            broadcaster.run(fin);

            final Handler h = new Handler();
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    RecordMesg rm = fit.poll();
                    if(null != rm) {
                        if (rm.hasField(RecordMesg.TimestampFieldNum)) {
                            long tm = 1000*rm.getTimestamp().getTimestamp().longValue();
                            if (rm.hasField(RecordMesg.PowerFieldNum)) {
                                int val = rm.getPower();
                                sendData(PWR, tm, val);
                            }
                            if (rm.hasField(RecordMesg.HeartRateFieldNum)) {
                                int val = rm.getHeartRate();
                                sendData(HR, tm, val);
                            }
                        }
                        h.postDelayed(this, 0);
                    }
                }
            }, 0);
        } catch (IOException e) {
            e.printStackTrace();
            sendToUI("CCChart: "+e.toString());
        }
    }

}
