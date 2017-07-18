package com.example.unknoqn.cc;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.widget.Toast;

import com.sweetzpot.stravazpot.activity.model.Activity;
import com.sweetzpot.stravazpot.athlete.api.AthleteAPI;
import com.sweetzpot.stravazpot.athlete.model.Athlete;
import com.sweetzpot.stravazpot.authenticaton.api.AuthenticationAPI;
import com.sweetzpot.stravazpot.authenticaton.model.AppCredentials;
import com.sweetzpot.stravazpot.authenticaton.model.LoginResult;
import com.sweetzpot.stravazpot.common.api.AuthenticationConfig;
import com.sweetzpot.stravazpot.common.api.StravaConfig;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by unknown on 7/18/2017.
 */

public class CCStravaASyncTask extends AsyncTask<String,Void,String> {

    CCSettingsActivity activity;

    CCStravaASyncTask(CCSettingsActivity _activity) {
        activity = _activity;
    }

    static String getToken(PreferenceActivity _activity) {
        SharedPreferences pref = _activity.getPreferences(MODE_PRIVATE);
        return pref.getString("strava_token", "");
    }

    @Override
    protected String doInBackground(String... params) {
        if("token".equals(params[0])) {
            String code = params[1];

            AuthenticationConfig config = AuthenticationConfig.create()
                    .debug()
                    .build();
            AuthenticationAPI api = new AuthenticationAPI(config);
            LoginResult result = api.getTokenForApp(AppCredentials.with(CCSettingsActivity.CLIENT_ID, "05e15bf725a7c4ee80fcd6683c8bebd5a5811cef"))
                    .withCode(code)
                    .execute();
            SharedPreferences pref = activity.getPreferences(MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("strava_token", result.getToken().toString().split("Bearer ")[1]);
            editor.commit();

            return "token:Strava Connected "+result.getAthlete().getFirstName();
        } else if("logout".equals(params[0])) {
            return "Not Yet Implemented";
        } else if("athlete".equals(params[0])) {
            StravaConfig config = StravaConfig.withToken(getToken(activity))
                    .debug()
                    .build();
            AthleteAPI athleteAPI = new AthleteAPI(config);
            Athlete athlete = athleteAPI.retrieveCurrentAthlete()
                    .execute();
            String name = athlete.getFirstName()+" "+athlete.getLastName();
            return "athlete:"+name;
        }
        return "OTHER";
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        activity.onAsyncTaskComplete(result);
    }
}
