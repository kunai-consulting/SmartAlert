package com.slalomdigital.smartalert;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.widget.TextView;
import com.facebook.*;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphObjectList;
import com.facebook.model.GraphUser;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Created with IntelliJ IDEA.
 * User: aaronc
 * Date: 6/6/13
 * Time: 10:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class CheckForLikes extends Service {
    // Timestamp to check from...
    public static final String LAST_LIKE_PREFERENCE = "last_like";

    @Override
    public void onCreate() {
        // Check for new Likes...
        Session session = Session.getActiveSession();

        if (session != null && session.isOpened() && session.getPermissions().contains("user_likes")) {
            //The session is active so let's look for new likes...
            class LikesCallback implements Request.Callback {

                private CheckForLikes likesService = null;

                public LikesCallback(CheckForLikes checkForLikes) {
                    this.likesService = checkForLikes;
                }

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

                        SharedPreferences likesPrefs = getSharedPreferences(CheckForLikes.LAST_LIKE_PREFERENCE, 0);
                        long lastId = likesPrefs.getLong("id", 0);

                        //See if this is a new like...
                        if (id != 0 && lastId != 0 && id != lastId) {
                            // This is a new like, trigger a UA notification

                            // Save the id for next time...
                            SharedPreferences.Editor editor = likesPrefs.edit();
                            editor.putLong("id", id);
                            editor.commit();
                        }


                    }

                    likesService.stopSelf();
                }
            };
            LikesCallback callback = new LikesCallback(this);

            Request request = new Request(session, "me/likes", null, HttpMethod.GET, callback);
            RequestAsyncTask task = new RequestAsyncTask(request);
            task.execute();
        }
        else {
            stopSelf();
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }


    public IBinder onBind(Intent intent) {
        // This service is pretending to be the server for SmartAlert
        // so it doesn't interact with the application at all it just runs
        // while the application is running.
        return null;
    }
}
