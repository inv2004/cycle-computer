package com.example.unknoqn.cc;

import android.util.Log;

import com.garmin.fit.DateTime;
import com.garmin.fit.FileCreatorMesg;
import com.garmin.fit.FileEncoder;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.Fit;
import com.garmin.fit.Manufacturer;
import com.garmin.fit.RecordMesg;

import java.io.File;
import java.io.IOException;

/**
 * Created by unknoqn on 5/21/2017.
 */

public class CCAntFit {
    private CCServiceAntSync service;
    private FileEncoder encoder;
    private File file;

    public CCAntFit(CCServiceAntSync g_service) {
        service = g_service;
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

    public void log(int code, long time, int val, float val2) {
        if(null == encoder) { return; }
        Log.d(this.toString(), "FITLOG: "+time+" / "+code+" / "+val+" / "+val2);
        RecordMesg r = new RecordMesg();
        r.setTimestamp(new DateTime(time));
        if(CCServiceAntSync.HR == code) {
            r.setHeartRate((short) val);
        } else if(CCServiceAntSync.PWR == code) {
            r.setPower(val);
        } else if(CCServiceAntSync.SPD == code) {
            r.setSpeed(val2);
        } else if(CCServiceAntSync.DST == code) {
            r.setDistance(val2);
        }
        encoder.write(r);
    }

    public void start() {
        long tm = System.currentTimeMillis();
        String fileName = getDTStr(tm)+"_.fit";
        File dir = service.getFilesDir();
        file = new File(dir, fileName);
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
            newFile = new File(file.getCanonicalPath().replaceAll("_.fit$", "-"+getDTStr(System.currentTimeMillis())+".fit"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        file.renameTo(newFile);
        service.sendToUI("Saved: "+newFile.getName());
        service.sendToUI("Size: "+newFile.length());

        encoder = null;
    }
}
