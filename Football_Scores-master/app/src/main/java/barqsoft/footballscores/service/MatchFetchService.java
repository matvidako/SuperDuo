package barqsoft.footballscores.service;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

import barqsoft.footballscores.DatabaseContract;
import barqsoft.footballscores.MainActivity;
import barqsoft.footballscores.R;
import barqsoft.footballscores.widget.LatestMatchWidgetProvider;

/**
 * Created by yehya khaled on 3/2/2015.
 */
public class MatchFetchService extends IntentService {

    public static final String LOG_TAG = "myFetchService";
    public static final int STATUS_FINISHED = 1;
    private ResultReceiver receiver;

    final String BASE_URL = "http://api.football-data.org/alpha/fixtures"; //Base URL

    public MatchFetchService() {
        super("myFetchService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        receiver = intent.getParcelableExtra("receiver");
        getData("n2");
        getData("p2");
        return;
    }

    private void getData(String timeFrame) {
        final String QUERY_TIME_FRAME = "timeFrame"; //Time Frame parameter to determine days
        Uri fetch_build = Uri.parse(BASE_URL).buildUpon().appendQueryParameter(QUERY_TIME_FRAME, timeFrame).build();
        HttpURLConnection m_connection = null;
        BufferedReader reader = null;
        String JSON_data = null;

        //Opening Connection
        try {
            URL fetch = new URL(fetch_build.toString());
            m_connection = (HttpURLConnection) fetch.openConnection();
            m_connection.setRequestMethod("GET");
            m_connection.addRequestProperty("X-Auth-Token", "e136b7858d424b9da07c88f28b61989a");
            m_connection.connect();

            // Read the input stream into a String
            InputStream inputStream = m_connection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return;
            }
            JSON_data = buffer.toString();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception here" + e.getMessage());
        } finally {
            if (m_connection != null) {
                m_connection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error Closing Stream");
                }
            }
        }
        if(JSON_data == null) {
            Log.d(LOG_TAG, "Could not connect to server.");
            return;
        }
        try {
            //This bit is to check if the data contains any matches. If not, we call processJson on the dummy data
            JSONArray matches = new JSONObject(JSON_data).getJSONArray("fixtures");
            //if there is no data, call the function on dummy data
            //this is expected behavior during the off season.
            boolean shouldUseDummyData = matches.length() == 0;
            if (shouldUseDummyData) {
                processJSONdata(getString(R.string.dummy_data), getApplicationContext(), false);
            } else {
                processJSONdata(JSON_data, getApplicationContext(), true);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }
    }

    private void processJSONdata(String JSONdata, Context mContext, boolean isReal) {
        //JSON data
        final String SERIE_A = "357";
        final String PREMIER_LEGAUE = "354";
        final String CHAMPIONS_LEAGUE = "362";
        final String PRIMERA_DIVISION = "358";
        final String BUNDESLIGA = "351";
        final String SEASON_LINK = "http://api.football-data.org/alpha/soccerseasons/";
        final String MATCH_LINK = "http://api.football-data.org/alpha/fixtures/";
        final String FIXTURES = "fixtures";
        final String LINKS = "_links";
        final String SOCCER_SEASON = "soccerseason";
        final String SELF = "self";
        final String MATCH_DATE = "date";
        final String HOME_TEAM = "homeTeamName";
        final String AWAY_TEAM = "awayTeamName";
        final String RESULT = "result";
        final String HOME_GOALS = "goalsHomeTeam";
        final String AWAY_GOALS = "goalsAwayTeam";
        final String MATCH_DAY = "matchday";

        //Match data
        String league = null;
        String mDate = null;
        String mTime = null;
        String home = null;
        String away = null;
        String homeGoals = null;
        String awayGoals = null;
        String match_id = null;
        String match_day = null;

        try {
            JSONArray matches = new JSONObject(JSONdata).getJSONArray(FIXTURES);

            //ContentValues to be inserted
            Vector<ContentValues> values = new Vector<ContentValues>(matches.length());
            for (int i = 0; i < matches.length(); i++) {
                JSONObject match_data = matches.getJSONObject(i);
                league = match_data.getJSONObject(LINKS).getJSONObject(SOCCER_SEASON).
                        getString("href");
                league = league.replace(SEASON_LINK, "");
                match_id = match_data.getJSONObject(LINKS).getJSONObject(SELF).
                        getString("href");
                match_id = match_id.replace(MATCH_LINK, "");
                if (!isReal) {
                    //This if statement changes the match ID of the dummy data so that it all goes into the database
                    match_id = match_id + Integer.toString(i);
                }

                mDate = match_data.getString(MATCH_DATE);
                mTime = mDate.substring(mDate.indexOf("T") + 1, mDate.indexOf("Z"));
                mDate = mDate.substring(0, mDate.indexOf("T"));
                SimpleDateFormat match_date = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss");
                match_date.setTimeZone(TimeZone.getTimeZone("UTC"));
                try {
                    Date parseddate = match_date.parse(mDate + mTime);
                    SimpleDateFormat new_date = new SimpleDateFormat("yyyy-MM-dd:HH:mm");
                    new_date.setTimeZone(TimeZone.getDefault());
                    mDate = new_date.format(parseddate);
                    mTime = mDate.substring(mDate.indexOf(":") + 1);
                    mDate = mDate.substring(0, mDate.indexOf(":"));

                    if (!isReal) {
                        //This if statement changes the dummy data's date to match our current date range.
                        Date fragmentdate = new Date(System.currentTimeMillis() + ((i - 2) * 86400000));
                        SimpleDateFormat mformat = new SimpleDateFormat("yyyy-MM-dd");
                        mDate = mformat.format(fragmentdate);
                    }
                } catch (Exception e) {
                    Log.d(LOG_TAG, "error here!");
                    Log.e(LOG_TAG, e.getMessage());
                }
                home = match_data.getString(HOME_TEAM);
                away = match_data.getString(AWAY_TEAM);
                homeGoals = match_data.getJSONObject(RESULT).getString(HOME_GOALS);
                awayGoals = match_data.getJSONObject(RESULT).getString(AWAY_GOALS);
                match_day = match_data.getString(MATCH_DAY);
                ContentValues match_values = new ContentValues();
                match_values.put(DatabaseContract.scores_table.MATCH_ID, match_id);
                match_values.put(DatabaseContract.scores_table.DATE_COL, mDate);
                match_values.put(DatabaseContract.scores_table.TIME_COL, mTime);
                match_values.put(DatabaseContract.scores_table.HOME_COL, home);
                match_values.put(DatabaseContract.scores_table.AWAY_COL, away);
                match_values.put(DatabaseContract.scores_table.HOME_GOALS_COL, homeGoals);
                match_values.put(DatabaseContract.scores_table.AWAY_GOALS_COL, awayGoals);
                match_values.put(DatabaseContract.scores_table.LEAGUE_COL, league);
                match_values.put(DatabaseContract.scores_table.MATCH_DAY, match_day);
                values.add(match_values);

                if(i == 0) {
                    updateWidget(home, away, homeGoals, awayGoals);
                }
            }

            int inserted_data = 0;
            ContentValues[] insert_data = new ContentValues[values.size()];
            values.toArray(insert_data);
            inserted_data = mContext.getContentResolver().bulkInsert(
                    DatabaseContract.BASE_CONTENT_URI, insert_data);

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    private void updateWidget(String home, String away, String homeGoals, String awayGoals) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, LatestMatchWidgetProvider.class));
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget);

            views.setTextViewText(R.id.team_home, home);
            views.setTextViewText(R.id.team_away, away);
            views.setTextViewText(R.id.score, homeGoals + " - " + awayGoals);

            Intent launchIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, 0);
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

}

