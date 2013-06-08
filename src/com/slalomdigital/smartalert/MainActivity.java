/*
 * Copyright 2012 Urban Airship and Contributors
 */

package com.slalomdigital.smartalert;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.ImageView;
import android.widget.TextView;
import com.facebook.*;
import com.facebook.model.*;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.facebook.widget.ProfilePictureView;
import com.urbanairship.UAirship;
import com.urbanairship.push.PushManager;
import com.urbanairship.richpush.RichPushManager;
import com.urbanairship.richpush.RichPushUser;
import com.urbanairship.util.UAStringUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Calendar;

@SuppressWarnings("unused")
public class MainActivity extends SherlockFragmentActivity implements
        ActionBar.OnNavigationListener {
    protected static final String TAG = "MainActivity";

    static final String ALIAS_KEY = "com.slalomdigital.smartalert.ALIAS";
    static final int aliasType = 1;

    PendingIntent checkForLikesPendingIntent;

    ArrayAdapter<String> navAdapter;
    RichPushUser user;

    private ProfilePictureView profilePictureView;

    private static MainActivity currentActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentActivity = this;
        this.setContentView(R.layout.main);
        this.user = RichPushManager.shared().getRichPushUser();

        //enable push by default...
        PushManager.enablePush();

        profilePictureView = (ProfilePictureView) findViewById(R.id.profilePicture);

        // start Facebook Login
        Session.openActiveSession(this, true, new Session.StatusCallback() {

            // callback when session changes state
            @Override
            public void call(Session session, SessionState state, Exception exception) {
                if (session.isOpened()) {

                    // Make sure we have permission to get likes so that the CheckForLikes service can do it's job...
                    if (session.getPermissions().contains("user_likes")) {

                        // If we have permission, check and see if we set the last like...
                        SharedPreferences likesPrefs = getSharedPreferences(CheckForLikes.LAST_LIKE_PREFERENCE, 0);
                        if (!likesPrefs.contains("id")) {
                            // Get the last like and store it in the preferences...
                            Request.Callback callback = new Request.Callback() {

                                @Override
                                public void onCompleted(Response response) {
                                    // response should have the likes
                                    GraphObject graphObject = response.getGraphObject();
                                    if (graphObject != null) {
                                        JSONObject likes;
                                        long id;
                                        try {
                                            likes = graphObject.getInnerJSONObject().getJSONArray("data").getJSONObject(0);
                                            id = likes.getLong("id");
                                        } catch (JSONException e) {
                                            id = 0;
                                        }

                                        if (id != 0) {
                                            SharedPreferences likesPrefs = getSharedPreferences(CheckForLikes.LAST_LIKE_PREFERENCE, 0);
                                            SharedPreferences.Editor editor = likesPrefs.edit();
                                            editor.putLong("id", id);
                                            editor.commit();
                                        }
                                    }
                                }
                            };

                            Request request = new Request(session, "me/likes", null, HttpMethod.GET, callback);
                            RequestAsyncTask task = new RequestAsyncTask(request);
                            task.execute();
                        }
                    } else {
                        session.requestNewReadPermissions(new Session.NewPermissionsRequest(MainActivity.currentActivity, Arrays.asList("user_likes")));
                    }

                    // make request to the /me API
                    Request.executeMeRequestAsync(session, new Request.GraphUserCallback() {

                        // callback after Graph API response with user object
                        @Override
                        public void onCompleted(GraphUser user, Response response) {
                            if (user != null) {
                                TextView userName = (TextView) findViewById(R.id.userName);
                                userName.setText(user.getName());
                                TextView userLocation = (TextView) findViewById(R.id.userLocation);
                                try {
                                    userLocation.setText(((JSONObject) user.getProperty("location")).getString("name"));
                                } catch (JSONException e) {
                                    userLocation.setText("Location Unknown");
                                }
                                TextView userDemographic = (TextView) findViewById(R.id.userDemographic);
                                userDemographic.setText(user.getProperty("gender").toString());
                                profilePictureView.setProfileId(user.getId());
                            }
                        }
                    });

                }
            }
        });

        // Set up the check for likes service...

        Calendar cal = Calendar.getInstance();

        Intent intent = new Intent(this, CheckForLikes.class);
        checkForLikesPendingIntent = PendingIntent.getService(this, 0, intent, 0);

        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        // Start every 15 seconds
        alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), 15 * 1000, checkForLikesPendingIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove the check for likes service
        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(checkForLikesPendingIntent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        UAirship.shared().getAnalytics().activityStarted(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.configureActionBar();
        this.displayMessageIfNecessary();
    }

    @Override
    protected void onStop() {
        super.onStart();
        UAirship.shared().getAnalytics().activityStopped(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.getSupportMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.preferences:
                this.startActivity(new Intent(this, PushPreferencesActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        String navName = this.navAdapter.getItem(itemPosition);
        if (SmartAlertApplication.HOME_ACTIVITY.equals(navName)) {
            // do nothing, we're here
        } else if (SmartAlertApplication.INBOX_ACTIVITY.equals(navName)) {
            Intent intent = new Intent(this, InboxActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            this.startActivity(intent);
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
    }

    // helpers

    private void displayMessageIfNecessary() {
        String messageId = this.getIntent().getStringExtra(SmartAlertApplication.MESSAGE_ID_RECEIVED_KEY);
        if (!UAStringUtil.isEmpty(messageId)) {
            MessageFragment message = MessageFragment.newInstance(messageId);
            message.show(this.getSupportFragmentManager(), R.id.floating_message_pane, "message");
            this.findViewById(R.id.floating_message_pane).setVisibility(View.VISIBLE);
        }
    }

    private void dismissMessageIfNecessary() {
        MessageFragment message = (MessageFragment) this.getSupportFragmentManager()
                .findFragmentByTag("message");
        if (message != null) {
            message.dismiss();
            this.findViewById(R.id.floating_message_pane).setVisibility(View.INVISIBLE);
        }
    }

    private void configureActionBar() {
        ActionBar actionBar = this.getSupportActionBar();
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        this.navAdapter = new ArrayAdapter<String>(this, R.layout.sherlock_spinner_dropdown_item,
                SmartAlertApplication.navList);
        actionBar.setListNavigationCallbacks(this.navAdapter, this);
        actionBar.setSelectedNavigationItem(this.navAdapter.getPosition("Home"));
    }
}
