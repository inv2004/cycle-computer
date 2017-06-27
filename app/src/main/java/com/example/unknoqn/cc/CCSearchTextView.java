package com.example.unknoqn.cc;

import android.os.Handler;
import android.widget.TextView;

/**
 * Created by unknoqn on 5/22/2017.
 */

public class CCSearchTextView implements Runnable {
    boolean active = false;
    boolean gps = false;
    int n = 0;
    Handler h;
    TextView tv;

    public CCSearchTextView(boolean g_gps) {
        gps = g_gps;
        h = new Handler();
    }

    public void start(TextView g_tv) {
        if(active) { return; }
        tv = g_tv;
        active = true;

        run();
    }

    public void stop() {
        if (false == active) {
            return;
        }
        active = false;
        h.removeCallbacksAndMessages(null);
    }

    @Override
    public void run() {
        if (gps) {
            String[] cs = {"\\  ", " | ", "  /", " | "};
            tv.setText(cs[n]);
            n++;
            if (cs.length < 1 + n) {
                n = 0;
            }
        } else {
            tv.setText(new String(new char[n]).replace("\0", "."));
            n++;
            if (3 < n) {
                n = 0;
            }
        }
        h.postDelayed(this, 1000);
    }
}
