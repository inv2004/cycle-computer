package com.example.unknoqn.cc;

import android.util.Log;
import android.util.Pair;

import com.garmin.fit.DateTime;
import com.garmin.fit.Decode;
import com.garmin.fit.FileCreatorMesg;
import com.garmin.fit.FileEncoder;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.Fit;
import com.garmin.fit.Manufacturer;
import com.garmin.fit.Mesg;
import com.garmin.fit.MesgBroadcaster;
import com.garmin.fit.MesgListener;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.RecordMesgListener;
import com.github.mikephil.charting.charts.LineChart;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by unknoqn on 5/21/2017.
 */

public class CCAntFit {
    CCDataServiceSync service;
    String ext = "";
    ArrayList<Integer> codes = new ArrayList();

    public final static double LL_CONV = Math.pow(2,31) / 180;

    File data_dir;
    private FileEncoder encoder;

    private File file;
    long start_time = 0;

    long prev_time = 0;
    short hr;
    short cad;
    int pwr;

    int[] ll = {-1, -1};

    public CCAntFit(CCDataServiceSync _service, String _ext, boolean _raw) {
        codes.add(CCDataServiceSync.HR);
        codes.add(CCDataServiceSync.SPD);
        codes.add(CCDataServiceSync.LATLNG);
        if(_raw) {
            codes.add(CCDataServiceSync.PWRRAW);
            codes.add(CCDataServiceSync.CADRAW);
        } else {
            codes.add(CCDataServiceSync.PWR);
            codes.add(CCDataServiceSync.CAD);
        }
        service = _service;
        ext = 0 < _ext.length() ? "."+_ext : "";
    }

    private void addFileId(FileEncoder encoder, long tm) {
        FileIdMesg fid = new FileIdMesg();
        fid.setType(com.garmin.fit.File.ACTIVITY);
        fid.setManufacturer(Manufacturer.INVALID);
        fid.setProduct(1);
        fid.setSerialNumber(new Long(001));
        fid.setTimeCreated(new DateTime(tm));
        encoder.write(fid);

    }

    private void addFileCreator(FileEncoder encoder) {
        FileCreatorMesg fc = new FileCreatorMesg();
        fc.setSoftwareVersion(1);
        encoder.write(fc);
    }

    public void log(int code, long time, int int_val, float float_val, double[] d_arr) {
        if(!codes.contains(code)) { return; }
        if(null == encoder) { return; }

//        Log.d(this.toString(), "FITLOG: "+code+" / "+int_val+" / "+float_val);

        long tm = (time + start_time) / 1000;

        if(prev_time == 0) { prev_time = tm; }

        if(tm != prev_time) {
            RecordMesg r = new RecordMesg();
            r.setTimestamp(new DateTime(prev_time));
            if(hr >= 0) { r.setHeartRate(hr); }
            if(cad >= 0) { r.setCadence(cad); }
            if(pwr >= 0) { r.setPower(pwr); }
            if(ll[0] != -1.0) {
                r.setPositionLat(ll[0]);
                r.setPositionLong(ll[1]);
            }
            encoder.write(r);
            hr = -1; pwr = -1; cad = -1; ll[0] = -1;
        }

        if(CCDataServiceSync.HR == code) {
            hr = (short) int_val;
        } else if(CCDataServiceSync.PWR == code) {
            pwr = int_val;
        } else if(CCDataServiceSync.CAD == code) {
            cad = (short) int_val;
        } else if(CCDataServiceSync.LATLNG == code) {
            ll[0] = (int) (d_arr[0] * LL_CONV);
            ll[1] = (int) (d_arr[1] * LL_CONV);
        }

        prev_time = tm;

        /*} else if(CCDataServiceSync.SPD == code) {
            r.setSpeed(float_val);
        } else if(CCDataServiceSync.DST == code) {
            r.setDistance(float_val);
        } else if(CCDataServiceSync.PWRRAW == code) {
            r.setPower(int_val);
        } else if(CCDataServiceSync.CADRAW == code) {
            r.setCadence((short) int_val);
        }*/
    }

    public void start(long _start_time) {
        start_time = _start_time;
        long tm = System.currentTimeMillis();
        String fileName = getDTStr(tm)+"_"+ext+".fit";
        data_dir = service.getFilesDir(); // !!!
        service.sendToUI(data_dir.toString());
        file = new File(data_dir, fileName);
        encoder = new FileEncoder(file, Fit.ProtocolVersion.V2_0);

        addFileId(encoder, tm);
        addFileCreator(encoder);
    }

    private String getDTStr(long tm) {
        return android.text.format.DateFormat.format("yyyyMMdd_hhmmss", tm).toString();
    }

    public void stop() {
        encoder.close();
        File newFile = null;
        try {
            newFile = new File(file.getCanonicalPath().replaceAll("_"+ext+".fit$", "-"+getDTStr(System.currentTimeMillis())+ext+".fit"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(file.renameTo(newFile)) {
            service.sendToUI("Saved: " + newFile.getName());
            service.sendToUI("Size: " + newFile.length());
        } else {
            service.sendToUI("Failed to save");
        }

        encoder = null;
    }

}
