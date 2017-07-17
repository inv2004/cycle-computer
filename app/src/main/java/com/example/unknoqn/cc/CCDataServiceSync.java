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
import com.example.unknoqn.cc.calc.CCCalcAutoInt;
import com.example.unknoqn.cc.calc.CCCalcAvgPwr;
import com.example.unknoqn.cc.calc.CCCalcDST;
import com.example.unknoqn.cc.calc.CCCalcWC;
import com.garmin.fit.Decode;
import com.garmin.fit.MesgBroadcaster;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.RecordMesgListener;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

public class CCDataServiceSync extends Service {

    final static int SEARCH = CC.SEARCH;
    final static int NA = CC.NA;

    public static int TXT = 0;
    public static int PWR = 2;
    public static int HR = 3;
    public static int CAD = 4;
    public static int SPD = 5;
    public static int DST = 7;
    public static int PWRRAW = 22;
    public static int CADRAW = 24;
    public static int TIME = 10;
    public static int SWC = 11;
    public static int AWC = 12;
    public static int LAP = 13;
    public static int AVGPWR = 14;
    public static int DELTA_DST = 15;
    public static int TEST0 = 21;
    public static int TEST1 = 22;

    private boolean test = false;
    private PendingIntent intent2;

    private Timer timer = new Timer();
    private CCAntFit fit = new CCAntFit(this, "", false);
    private CCAntFit fit_raw = new CCAntFit(this, "raw", true);
    private CCCalcWC calcWC = new CCCalcWC(this);
    private CCCalcDST calcDST = new CCCalcDST(this);
    private CCCalcAutoInt calcAutoInt = new CCCalcAutoInt(this);
    private CCCalcAvgPwr calcAvgPwr = new CCCalcAvgPwr(this);

    LocationManager locationManager;
    Location prev_location;
    long pwr_init_time;
    long hr_init_time;
    long last_sent_time;
    long hrCounter;
    float prev_dst = 0f;

    long start_time;
    boolean first = true;

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
        result.putExtra("time", time - start_time);
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
        fit_raw.log(code, time,i, f);

//        Log.d("ST", time+" / "+(time - start_time));
        if(0 < start_time) {
            calcWC.calc(code, time, i);
            calcDST.calc(code, time, f);
            calcAvgPwr.calc(code, time, i);
            calcAutoInt.calc(code, time, i);
        }

        Intent result = new Intent();
        if(0 < start_time) {
            result.putExtra("time", time - start_time);
        }
        result.putExtra("val", i);
        result.putExtra( "float_val", f);
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
        sendMsg(PWR, SEARCH);
        sendMsg(CAD, SEARCH);

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
        sendMsg(HR, SEARCH);

        if (test) { return; }

        AntPluginPcc.IPluginAccessResultReceiver<AntPlusHeartRatePcc> mResultReceiver = new AntPluginPcc.IPluginAccessResultReceiver<AntPlusHeartRatePcc>() {
            @Override
            public void onResultReceived(AntPlusHeartRatePcc result, RequestAccessResult resultCode, DeviceState initialDeviceState) {
                if (resultCode == RequestAccessResult.SUCCESS) {
                    sendToUI("FOUND " + result.getDeviceName() + "\n" + result.getAntDeviceNumber());
                    subscribeHR(result);
                } else if (resultCode == RequestAccessResult.SEARCH_TIMEOUT) {
//                    sendToUI("Timeout: HR");
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

        sendMsg(SPD, SEARCH);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            sendToUI("GPS: Disabled by permissions");
            sendMsg(SPD, NA);
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
                    sendMsg(SPD, NA);
                }
//                Location prev = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if(null != prev_location) {
                    float d = location.distanceTo(prev_location);
//                    Log.d("DEBUG2", String.valueOf(d));
                    sendData(DELTA_DST, System.currentTimeMillis(), 0, d);
                }
                prev_location = location;
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.d(this.toString(), "STATUS: "+status);
                if(LocationProvider.AVAILABLE == status) {
                    sendMsg(SPD, NA);
                } else if(LocationProvider.OUT_OF_SERVICE == status) {
                    sendMsg(SPD, SEARCH);
                } else if(LocationProvider.TEMPORARILY_UNAVAILABLE == status) {
                    sendMsg(SPD, SEARCH);
                }
            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.d(this.toString(), "GPS enabled");
                sendMsg(SPD, NA);
            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.d(this.toString(), "GPS disabled");
                sendMsg(SPD, SEARCH);
            }
        };

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1 * 1000, 10, ll);
    }


    protected void subscribePower(AntPlusBikePowerPcc pcc) {
        sendMsg(PWR, CC.NA);
        sendMsg(CAD, CC.NA);
        pwr_init_time = 0;

        pcc.subscribeCalculatedPowerEvent(new AntPlusBikePowerPcc.ICalculatedPowerReceiver() {
            @Override
            public void onNewCalculatedPower(long l, EnumSet<EventFlag> enumSet, AntPlusBikePowerPcc.DataSource dataSource, BigDecimal bigDecimal) {
//                Log.d("PWR: ", l + " / " + dataSource.toString() + " / " + bigDecimal.toString());
                if(0 == pwr_init_time) {
                    pwr_init_time = System.currentTimeMillis() - l;
                }
                sendData(PWR, pwr_init_time + l, bigDecimal.intValue());
            }
        });
        pcc.subscribeCalculatedCrankCadenceEvent(new AntPlusBikePowerPcc.ICalculatedCrankCadenceReceiver() {
            @Override
            public void onNewCalculatedCrankCadence(long l, EnumSet<EventFlag> enumSet, AntPlusBikePowerPcc.DataSource dataSource, BigDecimal bigDecimal) {
//                Log.d("CAD: ", l + " / " + dataSource.toString() + " / " + bigDecimal.toString());
                if(0 == pwr_init_time) {
                    pwr_init_time = System.currentTimeMillis() - l;
                }
                sendData(CAD, pwr_init_time + l, bigDecimal.intValue());
            }
        });

        pcc.subscribeRawPowerOnlyDataEvent(new AntPlusBikePowerPcc.IRawPowerOnlyDataReceiver() {
            @Override
            public void onNewRawPowerOnlyData(long l, EnumSet<EventFlag> enumSet, long l1, int i, long l2) {
                Log.d("RAWPWR: ", l + " / " + l1 + " / " + i + " / " + l2);
                if(0 == pwr_init_time) {
                    pwr_init_time = System.currentTimeMillis() - l;
                }
                sendData(PWRRAW, pwr_init_time + l, i);
            }
        });

        pcc.subscribeInstantaneousCadenceEvent(new AntPlusBikePowerPcc.IInstantaneousCadenceReceiver() {
            @Override
            public void onNewInstantaneousCadence(long l, EnumSet<EventFlag> enumSet, AntPlusBikePowerPcc.DataSource dataSource, int i) {
                if(0 == pwr_init_time) {
                    pwr_init_time = System.currentTimeMillis() - l;
                }
                sendData(CADRAW, pwr_init_time + l, i);
            }
        });

    }

    protected void subscribeHR(AntPlusHeartRatePcc pcc) {
        sendMsg(HR, -1);

        hrCounter = -1;
        pcc.subscribeHeartRateDataEvent(new AntPlusHeartRatePcc.IHeartRateDataReceiver() {
            @Override
            public void onNewHeartRateData(long l, EnumSet<EventFlag> enumSet, int i, long l1, BigDecimal bigDecimal, AntPlusHeartRatePcc.DataState dataState) {
                Log.d("HR: ", i + " / " + l1 + " / " + l + " / " + bigDecimal + " / " + dataState.toString());
                sendData(HR, l, i);
                hrCounter = l1;
            }
        });
    }

    protected void startTimer() {
        start_time = System.currentTimeMillis();
        fit.start(start_time);
        fit_raw.start(start_time);
        calcWC.start(start_time);
        calcDST.start();
        calcAutoInt.start();

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
        fit_raw.stop();
        calcWC.stop();
        calcDST.stop();
        calcAutoInt.stop();
        Log.d(toString(), "STOP");
        timer.cancel();
        timer = null;
    }

    public void Play() {
        final LinkedList<RecordMesg> fit_dat = new LinkedList<>();

        Decode decode = new Decode();
        MesgBroadcaster broadcaster = new MesgBroadcaster(decode);
        broadcaster.addListener(new RecordMesgListener() {
            @Override
            public void onMesg(RecordMesg rm) {
                fit_dat.add(rm);
            }
        });

        try {
            FileInputStream fin = new FileInputStream(this.getFilesDir().getCanonicalFile()+"/import/10_4x3.fit");
            broadcaster.run(fin);

            final Handler h = new Handler();
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    RecordMesg rm = fit_dat.poll();
                    if(null != rm) {
                        if (rm.hasField(RecordMesg.TimestampFieldNum)) {
                            long tm = 1000*rm.getTimestamp().getTimestamp().longValue();
                            if(first) {
                                first = false;
                                start_time = tm;
                                sendTime(tm); // 0
                            }
                            if (rm.hasField(RecordMesg.PowerFieldNum)) {
                                int val = rm.getPower();
                                sendData(PWR, tm, val);
                            }
                            if (rm.hasField(RecordMesg.HeartRateFieldNum)) {
                                int val = rm.getHeartRate();
                                sendData(HR, tm, val);
                            }
                            if(rm.hasField(RecordMesg.DistanceFieldNum)) {
                                float val = (0f == prev_dst) ? rm.getDistance() : rm.getDistance() - prev_dst;
                                prev_dst = rm.getDistance();
                                sendData(DELTA_DST, tm, 0, val);
                            }
                        }
                        h.postDelayed(this, 100);
                    }
                }
            }, 0);
        } catch (IOException e) {
            e.printStackTrace();
            sendToUI("CCChart: "+e.toString());
        }
    }

}
