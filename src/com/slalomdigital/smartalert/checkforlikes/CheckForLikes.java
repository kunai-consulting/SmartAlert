package com.slalomdigital.smartalert.checkforlikes;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;
import com.facebook.*;
import com.facebook.model.GraphObject;
import com.urbanairship.richpush.RichPushManager;
import com.urbanairship.richpush.RichPushUser;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.*;

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
                        String name;
                        try {
                            likes = graphObject.getInnerJSONObject().getJSONArray("data").getJSONObject(0);
                            id = likes.getLong("id");
                            name = likes.getString("name");
                        } catch (JSONException e) {
                            id = 0;
                            name = "";
                        }

                        SharedPreferences likesPrefs = getSharedPreferences(CheckForLikes.LAST_LIKE_PREFERENCE, 0);
                        long lastId = likesPrefs.getLong("id", 0);

                        //See if this is a new like...
                        if (id != 0 && lastId != 0 && id != lastId) {
                            // Save the id for next time...
                            SharedPreferences.Editor editor = likesPrefs.edit();
                            editor.putLong("id", id);
                            editor.commit();

                            // Send a push notification...
                            RichPushSender pushSender = new RichPushSender(likesService);

                            String[] params = {"10% off on " + name, getCouponHtml(name, Long.toString(id), "http://www.sephora.com")};
                            pushSender.execute(params);

                            // Don't stop the service, the RichPushSender does that...
                            return;
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

    private String getCouponHtml(String name, String id, String webUrl) {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<body>\n" +
                "\n" +
                "<h2>We know you like " + name + "...</h2>\n" +
                "<img border=\"10px\" src=\"https://graph.facebook.com/" + id + "/picture?type=normal\" width=\"100\" height=\"100\">\n" +
                "<h2>...so shop for it at Sephora and get 10% off with this coupon.</h2>\n" +
                "<img align=\"middle\" src=\"http://barcode.tec-it.com/barcode.ashx?code=QRCode&modulewidth=fit&dpi=96&imagetype=gif&rotation=0&color=&bgcolor=&fontcolor=&quiet=0&qunit=mm&data="
                + URLEncoder.encode(webUrl) + "\">\n" +
                "\n" +
                "</body>\n" +
                "</html>";
    }
}
