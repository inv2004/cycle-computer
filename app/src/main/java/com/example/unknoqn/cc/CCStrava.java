package com.example.unknoqn.cc;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.ArraySet;
import android.widget.Toast;

import com.sweetzpot.stravazpot.athlete.api.AthleteAPI;
import com.sweetzpot.stravazpot.athlete.model.Athlete;
import com.sweetzpot.stravazpot.authenticaton.api.AccessScope;
import com.sweetzpot.stravazpot.authenticaton.api.ApprovalPrompt;
import com.sweetzpot.stravazpot.authenticaton.api.AuthenticationAPI;
import com.sweetzpot.stravazpot.authenticaton.api.StravaLogin;
import com.sweetzpot.stravazpot.authenticaton.model.AppCredentials;
import com.sweetzpot.stravazpot.authenticaton.model.LoginResult;
import com.sweetzpot.stravazpot.common.api.AuthenticationConfig;
import com.sweetzpot.stravazpot.common.api.StravaConfig;
import com.sweetzpot.stravazpot.segment.api.SegmentAPI;
import com.sweetzpot.stravazpot.segment.model.Segment;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by unknown on 7/21/2017.
 */

class CCStrava {
    final static int RQ_LOGIN = 1001;
    final static int CLIENT_ID = 18057;
    final static int PERPAGE = 200;

    String token = "";

    private static final CCStrava inst = new CCStrava();

    CC cc;

    private CCStrava() {
    }

    public static synchronized CCStrava getInstance() {
        return inst;
    }

    public void init(CC _cc) {
        cc = _cc;
    }

    public String getToken() {
        if(0 == token.length()) {
            SharedPreferences pref = cc.getPreferences(MODE_PRIVATE);
            token = pref.getString("strava_token", "");
        }
        return token;
    }

    public void setToken(String _token) {
        token = _token;
        SharedPreferences pref = cc.getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("strava_token", token);
        editor.commit();
    }

    public int countSegments() {
        SharedPreferences pref = cc.getPreferences(MODE_PRIVATE);
        Iterator<? extends Map.Entry<String, ?>> it = pref.getAll().entrySet().iterator();
        int counter = 0;
        while(it.hasNext()) {
            if(it.next().getKey().startsWith("strava_segment_se_")) {
                counter++;
            }
        }
        return counter;
    }

    public void setSegment(Segment s) {
        SharedPreferences pref = cc.getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        editor.remove("strava_segment_se_"+s.getID());
        editor.remove("strava_segment_poly_"+s.getID());
        String start_end = s.getStartCoordinates().getLatitude()+","
                + s.getStartCoordinates().getLongitude()+";"
                + s.getEndCoordinates().getLatitude()+","
                + s.getEndCoordinates().getLongitude();
        editor.putString("strava_segment_se_"+s.getID(), start_end);
        if(s.getMap() != null && s.getMap().getPolyline() != null) {
            editor.putString("strava_segment_poly_"+s.getID(), s.getMap().getPolyline());
        }
        editor.commit();
    }

    public void login(Activity _activity) {
        Intent intent = StravaLogin.withContext(_activity)
                .withClientID(CLIENT_ID)
                .withRedirectURI("http://localhost/token_exchange")
                .withApprovalPrompt(ApprovalPrompt.AUTO)
                .withAccessScope(AccessScope.WRITE)
                .makeIntent();
        _activity.startActivityForResult(intent, RQ_LOGIN);
    }

    public void logout(Activity _activity) { // @TODO implement
        Toast t = Toast.makeText(_activity, "Not implemented yet", Toast.LENGTH_LONG);
        t.show();
    }

    public void token(final CCStravaResult obj, String code) {
        AsyncTask<String, Void, String> task = new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String[] params) {
                String code = params[0];
                AuthenticationConfig config = AuthenticationConfig.create()
                        .debug()
                        .build();
                AuthenticationAPI api = new AuthenticationAPI(config);
                LoginResult result = api.getTokenForApp(AppCredentials.with(CLIENT_ID, "05e15bf725a7c4ee80fcd6683c8bebd5a5811cef"))
                        .withCode(code)
                        .execute();
                return result.getToken().toString();
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                setToken(result);
                obj.onStravaResult(new String[]{"token"});
            }
        };
        task.execute(code);
    }

    public void athlete(final CCStravaResult obj) {
        AsyncTask<String, Void, String> task = new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String[] params) {
                StravaConfig config = StravaConfig.withToken(getToken())
                        .debug()
                        .build();
                AthleteAPI athleteAPI = new AthleteAPI(config);
                Athlete athlete = athleteAPI.retrieveCurrentAthlete()
                        .execute();
                String name = athlete.getFirstName()+" "+athlete.getLastName();
                return name;
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                obj.onStravaResult(new String[]{"athlete", result});
            }
        };
        task.execute();
    }

    public void syncFavSegments(final CCStravaResult obj) {
        AsyncTask<String, Void, List<Segment>> task = new AsyncTask<String, Void, List<Segment>>() {
            @Override
            protected List<Segment> doInBackground(String[] params) {
                StravaConfig config = StravaConfig.withToken(getToken())
                        .debug()
                        .build();
                SegmentAPI segmentAPI = new SegmentAPI(config);
                List<Segment> segments = segmentAPI.listMyStarredSegments()
                        .inPage(1) // @TODO
                        .perPage(PERPAGE)
                        .execute();
                return segments;
            }

            @Override
            protected void onPostExecute(List<Segment> result) {
                super.onPostExecute(result);
                obj.onStravaResultSegmentList(result);
            }
        };
        task.execute();
    }

    public synchronized void getSegment(final CCStravaResult obj, int seg_id) {
        AsyncTask<Integer, Void, Segment> task = new AsyncTask<Integer, Void, Segment>() {
            @Override
            protected Segment doInBackground(Integer[] params) {
                StravaConfig config = StravaConfig.withToken(getToken())
                        .debug()
                        .build();
                SegmentAPI segmentAPI = new SegmentAPI(config);
                Segment segment = segmentAPI.getSegment(params[0])
                        .execute();
                return segment;
            }

            @Override
            protected void onPostExecute(Segment result) {
                super.onPostExecute(result);
                setSegment(result);
                obj.onStravaResultSegment(result);
            }
        };
        task.execute(seg_id);
    }
}
