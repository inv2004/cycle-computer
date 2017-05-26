package com.example.unknoqn.cc;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;

import java.util.concurrent.CountDownLatch;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class CCDataServiceAsync_disabled extends IntentService {
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS

    private PendingIntent intent2;
    private Handler time_handler = new Handler();
    private CountDownLatch cdl = new CountDownLatch(1);

    public static volatile boolean stop;
    public static void stop() {
        CCDataServiceAsync_disabled.stop = true;
    }

    public CCDataServiceAsync_disabled() {
        super("CCDataServiceAsync_disabled");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    /*public static void startActionFoo(Context context, String param1, String param2) {
        Intent intent = new Intent(context, CCDataServiceAsync_disabled.class);
        intent.setAction(ACTION_FOO);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }*/

    public void sendtoUI(int code, String res) {
        if(null != intent2) {
            Intent result = new Intent();
            result.putExtra("error", res);
            try {
                intent2.send(this, code, result);
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }
    }

    protected void searchHR() {
        Log.d(this.toString(), "search: BEGIN");
        AntPluginPcc.IPluginAccessResultReceiver<AntPlusHeartRatePcc> mResultReceiver = new AntPluginPcc.IPluginAccessResultReceiver<AntPlusHeartRatePcc>() {
            @Override
            public void onResultReceived(AntPlusHeartRatePcc result, RequestAccessResult resultCode, DeviceState initialDeviceState) {
                if(resultCode == RequestAccessResult.SUCCESS) {
                    Log.d(this.toString(), "SUCCESS");
                    sendtoUI(2, "OK");
                } else if(resultCode == RequestAccessResult.SEARCH_TIMEOUT) {
                    Log.d(this.toString(), "TIMEOUT");
                } else if(resultCode == RequestAccessResult.ADAPTER_NOT_DETECTED) {
                    Log.d(this.toString(), "ADAPTER");
                } else if(resultCode == RequestAccessResult.DEPENDENCY_NOT_INSTALLED) {
                    Log.d(this.toString(), "DEPENDENCY");
                    sendtoUI(1, "DEP");
                } else {
                    Log.d(this.toString(), "unknown");
                    sendtoUI(1, "UNKNOWN");
                }
                cdl.countDown();
            }
        };
        AntPluginPcc.IDeviceStateChangeReceiver mStateReceiver = new AntPluginPcc.IDeviceStateChangeReceiver() {
            @Override
            public void onDeviceStateChange(DeviceState newDeviceState) {
                Log.d(this.toString(), "New state:"+newDeviceState.toString());
            }
        };
        Log.d(this.toString(), "search: BEGIN2");

        AntPlusHeartRatePcc.requestAccess(this ,0, 0, mResultReceiver, mStateReceiver);
        try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.d(this.toString(), "search: END");

    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        if (intent != null) {
            intent2 = intent.getParcelableExtra("pendingIntent");
            if("search".equals(intent.getAction())) {
                searchHR();
            } else if ("start".equals(intent.getAction())) {
                CCDataServiceAsync_disabled.stop = false;
                final CCDataServiceAsync_disabled obj = this;
                time_handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent result = new Intent();
                        result.putExtra("time", System.currentTimeMillis());
                        try {
                            PendingIntent reply = intent.getParcelableExtra("pendingIntent");
                            reply.send(obj, 0, result);
                        } catch (PendingIntent.CanceledException e) {
                            e.printStackTrace();
                        }
                        if (!CCDataServiceAsync_disabled.stop) {
                            time_handler.postDelayed(this, 1000);
                        }
                    }
                }, 1000);
            }
        }
    }

 //   public void onDestroy() {
//        Log.d(this.toString(), "DESTR");
//    }
}

