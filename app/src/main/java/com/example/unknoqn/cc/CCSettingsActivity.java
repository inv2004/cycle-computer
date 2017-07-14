package com.example.unknoqn.cc;


import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import com.sweetzpot.stravazpot.authenticaton.ui.StravaLoginButton;

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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceScreen ps = getPreferenceManager().createPreferenceScreen(this);
        setPreferenceScreen(ps);

        CheckBoxPreference chb1 = new CheckBoxPreference(this);
        chb1.setKey("chb1");
        chb1.setTitle("test 1");


        Button b = new Button(this);
        b.setText("Connect to Strava");
        ListView lv = getListView();
        lv.addFooterView(b);

        ImageButton b2 = new StravaLoginButton(this);
        lv.addFooterView(b2);
        Button b3 = new Button(this);
        b3.setText("b3");
        lv.addFooterView(b3);

    }
}
