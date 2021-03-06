/*
 * Copyright 2012 Urban Airship and Contributors
 */

package com.slalomdigital.smartalert;

import android.app.Application;

import android.content.Context;
import com.urbanairship.UAirship;
import com.urbanairship.push.PushManager;
import com.urbanairship.richpush.RichPushManager;
import com.urbanairship.richpush.RichPushMessageJavaScript;

public class SmartAlertApplication extends Application {

    public static final String MESSAGE_ID_RECEIVED_KEY = "com.slalomdigital.smartalert.MESSAGE_ID_RECEIVED";
    public static final String HOME_ACTIVITY = "Home";
    public static final String INBOX_ACTIVITY = "Inbox";
    public static final String[] navList = new String[]{
            HOME_ACTIVITY, INBOX_ACTIVITY
    };
    private static Context context;


    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        UAirship.takeOff(this);
        PushManager.shared().setIntentReceiver(PushReceiver.class);
        RichPushManager.setJavascriptInterface(RichPushMessageJavaScript.class, "urbanairship");
    }


    public static Context getAppContext() {
        return SmartAlertApplication.context;
    }
}
