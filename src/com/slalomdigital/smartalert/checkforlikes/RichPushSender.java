package com.slalomdigital.smartalert.checkforlikes;

import android.app.Service;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
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
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: aaronc
 * Date: 6/9/13
 * Time: 4:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class RichPushSender extends AsyncTask<String, Integer, HttpResponse> {

    Service service;

    public RichPushSender(Service service) {
        this.service = service;
    }

    @Override
    protected HttpResponse doInBackground(String... params) {
        // This is a new like, trigger a UA notification...
        RichPushUser user = RichPushManager.shared().getRichPushUser();

        // Get the UA properties we need to send a push...
        String appKey;
        String appSecret;
        try {
            InputStream inputStream = service.getResources().getAssets().open("airshipconfig.properties");
            Properties properties = new Properties();
            properties.load(inputStream);

            if (properties.getProperty("inProduction").trim().equalsIgnoreCase("false")) {
                appKey = properties.getProperty("developmentAppKey", null);
                appSecret = properties.getProperty("developmentAppSecret", null);
            }
            else {
                appKey = properties.getProperty("productionAppKey", null);
                appSecret = properties.getProperty("productionAppSecret", null);
            }
        } catch (IOException e) {
            System.err.println("Failed to open airshipconfig.properties property file");
            e.printStackTrace();

            appKey = null;
            appSecret = null;
        }

        // Create the message and send it...
        if (appKey != null && appSecret!= null) {
            HttpClient httpclient = new DefaultHttpClient();

            HttpPost httppost = new HttpPost("https://go.urbanairship.com/api/airmail/send/");
            String authorizationString = "Basic " + Base64.encodeToString(
                    (appKey + ":" + "ElUXZUI7RAq4_i498x0rjw").getBytes(),
                    Base64.NO_WRAP);
            httppost.setHeader("Authorization", authorizationString);
            httppost.setHeader("Content-type", "application/json");

            try {
                // Add the JSON for the push message...
                JSONObject postBody = new JSONObject();
                ArrayList<String> userList = new ArrayList<String>();
                userList.add(user.getId());
                postBody.put("users", new JSONArray(userList));
                JSONObject androidAlert = new JSONObject("{\"android\": {\"alert\": \"" + params[0] + "\"}}");
                postBody.put("push", androidAlert);
                postBody.put("title", params[0]);
                postBody.put("message", params[1]);
                postBody.put("content-type", "text/html");

                StringEntity se = new StringEntity(postBody.toString());

                Log.d(this.getClass().toString(), "Sending Push message:\n" + postBody.toString(1));
                se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                httppost.setEntity(se);

                // Execute HTTP Post Request

                HttpResponse httpResponse = httpclient.execute(httppost);
                if (httpResponse.getStatusLine().getStatusCode() != 200) {
                    String responseString;
                    try {
                        responseString = EntityUtils.toString(httpResponse.getEntity());
                    } catch (IOException e) {
                        responseString = "[IOException while converting response entity to string]";
                    }
                    Log.e(this.getClass().toString(), "Push message returned " +
                            Integer.toString(httpResponse.getStatusLine().getStatusCode()) + ": \n" + responseString);

                }
                else {
                    Log.d(this.getClass().toString(), "Push was sent to UA");
                }

                return httpResponse;
            } catch (ClientProtocolException e) {
                Log.e(this.getClass().toString(),"ClientProtocolException while sending a push message: " + e.getMessage());
            } catch (IOException e) {
                Log.e(this.getClass().toString(),"IOException while sending a push message: " + e.getMessage());
            } catch (JSONException e) {
                Log.e(this.getClass().toString(),"JSONException while sending a push message: " + e.getMessage());
            }

        }

        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onPostExecute(HttpResponse httpResponse) {
        service.stopSelf();
    }
}
