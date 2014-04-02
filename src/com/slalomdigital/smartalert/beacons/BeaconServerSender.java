package com.slalomdigital.smartalert.beacons;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: aaronc
 * Date: 6/9/13
 * Time: 4:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class BeaconServerSender extends AsyncTask<JSONObject, Integer, HttpResponse> {

    private Context context;
    private String path;

    public BeaconServerSender(Context context, String path) {
        this.context = context;
        this.path = path;
    }

    @Override
    protected HttpResponse doInBackground(JSONObject... params) {
        // Get the server URL
        String server;
        try {
            InputStream inputStream = context.getResources().getAssets().open("beaconcms.properties");
            Properties properties = new Properties();
            properties.load(inputStream);

            server = properties.getProperty("server", "ibeaconcms-env2.elasticbeanstalk.com");
        } catch (IOException e) {
            System.err.println("Failed to open beaconcms.properties property file");
            e.printStackTrace();

            server = null;
        }

        // Create the message and send it...
        if (server != null) {
            HttpClient httpclient = new DefaultHttpClient();

            HttpPost httppost = new HttpPost("http://" + server + "/" + path);
            httppost.setHeader("Content-type", "application/json");

            try {
                // Add the JSON for the post...
                JSONObject postBody = params[0];

                StringEntity se = new StringEntity(postBody.toString());

                Log.d(this.getClass().toString(), "Sending post to beacon cms[" + server + "]:\n" + postBody.toString(1));
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
                    Log.e(this.getClass().toString(), "Beacon post returned " +
                            Integer.toString(httpResponse.getStatusLine().getStatusCode()) + ": \n" + responseString);

                }
                else {
                    Log.d(this.getClass().toString(), "Post was sent to beacon cms.");
                }

                return httpResponse;
            } catch (ClientProtocolException e) {
                Log.e(this.getClass().toString(),"ClientProtocolException while posting to the beacon cms: " + e.getMessage());
            } catch (IOException e) {
                Log.e(this.getClass().toString(),"IOException while posting to the beacon cms: " + e.getMessage());
            } catch (JSONException e) {
                Log.e(this.getClass().toString(),"JSONException while posting to the beacon cms: " + e.getMessage());
            }

        }

        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onPostExecute(HttpResponse httpResponse) {

    }

    /**
     * Create or Update a mobile_user
     */
    public static void updateMobileUser(String firstName, String lastName, String email, String uid, Context context) {
        JSONObject body = new JSONObject();
        try {
            body.put("first_name", firstName);
            body.put("last_name", lastName);
            body.put("email", email);
            body.put("uid", uid);
        } catch (JSONException e) {
            Log.e(BeaconServerSender.class.toString(), "JSONException while creating the body message: " + e.getMessage());
        }

        BeaconServerSender sender = new BeaconServerSender(context, "users/mobile_user");
        sender.execute(body);
    }

    /**
     * Create or Update a beacon
     */
    public static void updateBeacon() {
        //TODO: implement
    }
}
