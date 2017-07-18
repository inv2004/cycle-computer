package com.example.unknoqn.cc;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.sweetzpot.stravazpot.authenticaton.api.AccessScope;
import com.sweetzpot.stravazpot.authenticaton.api.ApprovalPrompt;
import com.sweetzpot.stravazpot.authenticaton.api.AuthenticationAPI;
import com.sweetzpot.stravazpot.authenticaton.api.StravaLogin;
import com.sweetzpot.stravazpot.authenticaton.model.AppCredentials;
import com.sweetzpot.stravazpot.authenticaton.model.LoginResult;
import com.sweetzpot.stravazpot.authenticaton.ui.StravaLoginActivity;
import com.sweetzpot.stravazpot.authenticaton.ui.StravaLoginButton;
import com.sweetzpot.stravazpot.common.api.AuthenticationConfig;

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
public class CCSettingsActivity extends PreferenceActivity {
    final static int RQ_LOGIN = 1001;
    final static int CLIENT_ID = 18057;

    ListView lv;
    Button strava_login_button;

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

        final CCSettingsActivity obj = this;

        strava_login_button = new Button(this);

        if(0 < CCStravaASyncTask.getToken(this).length()) {
            strava_login_button.setText("Disconnect from Strava");
            strava_login_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CCStravaASyncTask task = new CCStravaASyncTask(obj);
                    task.execute("logout");
                }
            });
            new CCStravaASyncTask(this).execute("athlete");
        } else {
            strava_login_button.setText("Connect to Strava");
            strava_login_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = StravaLogin.withContext(obj)
                            .withClientID(CLIENT_ID)
                            .withRedirectURI("http://localhost/token_exchange")
                            .withApprovalPrompt(ApprovalPrompt.AUTO)
                            .withAccessScope(AccessScope.WRITE)
                            .makeIntent();
                    startActivityForResult(intent, RQ_LOGIN);
                }
            });
        }

        lv.addFooterView(strava_login_button);

//        StravaLoginButton slb = (StravaLoginButton) getLayoutInflater().inflate(R.layout.strava_button, lv, false);
//        lv.addFooterView(slb);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == RQ_LOGIN && resultCode == RESULT_OK && data != null) {
            String code = data.getStringExtra(StravaLoginActivity.RESULT_CODE);

            CCStravaASyncTask task = new CCStravaASyncTask(this);
            task.execute("token", String.valueOf(code));
        }
    }

    public void onAsyncTaskComplete(String _msg) {
        String[] strs = _msg.split(":", 2);
        String cmd = strs[0];
        String msg = strs[1];

        if(2 <= strs.length) {
            if ("athlete".equals(cmd)) {
                TextView tv = new TextView(this);
                tv.setText("Athlete: " + msg);
                lv.addFooterView(tv);
            } else if ("token".equals(cmd)) {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();

                // !!! COPY
                final CCSettingsActivity obj = this;
                strava_login_button.setText("Disconnect from Strava");
                strava_login_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        CCStravaASyncTask task = new CCStravaASyncTask(obj);
                        task.execute("logout");
                    }
                });
            }
        } else {
            Toast.makeText(this, _msg, Toast.LENGTH_LONG).show();
        }
    }
}
