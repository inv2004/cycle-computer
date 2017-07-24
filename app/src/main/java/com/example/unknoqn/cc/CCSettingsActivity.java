package com.example.unknoqn.cc;


import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.sweetzpot.stravazpot.authenticaton.ui.StravaLoginActivity;
import com.sweetzpot.stravazpot.route.model.Map;
import com.sweetzpot.stravazpot.segment.model.Segment;

import java.util.Iterator;
import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class CCSettingsActivity extends PreferenceActivity implements CCStravaResult {

    CCStrava strava;

    ListView lv;
    Button strava_login_button;
    Button strava_sync_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceScreen ps = getPreferenceManager().createPreferenceScreen(this);
        setPreferenceScreen(ps);

        lv = getListView();

        Button back = new Button(this);
        back.setText("<<< BACK");
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        lv.addHeaderView(back);

        strava = CCStrava.getInstance();

        strava_login_button = new Button(this);
        lv.addFooterView(strava_login_button);

        strava_sync_button = new Button(this);
        strava_sync_button.setText("Sync Fav Segments");
        setButton();

        final CCSettingsActivity obj = this;
        strava_sync_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                strava.syncFavSegments(obj);
            }
        });
        lv.addFooterView(strava_sync_button);

    }

    void setButton() {
        final CCSettingsActivity obj = this;

        if(0 < strava.getToken().length()) {
            strava_login_button.setText("Disconnect from Strava");
            strava_login_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    strava.logout(obj);
                }
            });
            strava_sync_button.setEnabled(true);
            strava.athlete(obj);
        } else {
            strava_login_button.setText("Connect to Strava");
            strava_login_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    strava.login(obj);
                }
            });
            strava_sync_button.setEnabled(false);
    }


//        StravaLoginButton slb = (StravaLoginButton) getLayoutInflater().inflate(R.layout.strava_button, lv, false);
//        lv.addFooterView(slb);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == CCStrava.RQ_LOGIN && resultCode == RESULT_OK && data != null) {
            String code = data.getStringExtra(StravaLoginActivity.RESULT_CODE);
            strava.token(this, code);
        }
    }

    public void onStravaResult(String[] msg) {
        String code = msg[0];
        if("token".equals(code)) {
            setButton();
        } else if("athlete".equals(code)) {
            TextView tv = new TextView(this);
            tv.setText("Athlete: "+msg[1]);
            lv.addFooterView(tv);
        }
    }

    public void onStravaResultSegmentList(List<Segment> segments) {
        if(segments.size() >= CCStrava.PERPAGE) {
            Toast.makeText(this, "Only first 200 segments will be loaded", Toast.LENGTH_SHORT).show();
        }
        Iterator<Segment> it = segments.iterator();
        while(it.hasNext()) {
            Segment s = it.next();
            strava.getSegment(this, s.getID());
        }
    }

    public void onStravaResultSegment(Segment segment) {
        Map map = segment.getMap();
        if(map == null) {
            Toast.makeText(this, "Map is not defined for segment", Toast.LENGTH_SHORT).show();
            return;
        }
    }
}
