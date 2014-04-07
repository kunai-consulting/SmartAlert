package com.slalomdigital.smartalert.beacons;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import com.slalomdigital.smartalert.SmartAlertApplication;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
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
import java.util.List;
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

    private static final int BEACON_RESPONSE = 1;
    private int responseType = 0;

    public BeaconServerSender(Context context, String path) {
        this.context = context;
        this.path = path;
        this.responseType = 0;
    }

    public BeaconServerSender(Context context, String path, int responseType) {
        this.context = context;
        this.path = path;
        this.responseType = responseType;
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
            JSONObject postBody = params[0];
            HttpClient httpclient = new DefaultHttpClient();
            HttpUriRequest httpRequest;

            try {

                if (postBody != null) {
                    HttpPost httpPost = new HttpPost("http://" + server + "/" + path);
                    httpPost.setHeader("Content-type", "application/json");

                    // Add the JSON for the post...
                    StringEntity se = new StringEntity(postBody.toString());

                    Log.d(this.getClass().toString(), "Sending post to beacon cms[" + server + "]:\n" + postBody.toString(1));
                    se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                    httpPost.setEntity(se);
                    httpRequest = httpPost;
                } else {
                    httpRequest = new HttpGet("http://" + server + "/" + path);
                }

                // Execute HTTP Post Request
                HttpResponse httpResponse = httpclient.execute(httpRequest);
                if (httpResponse.getStatusLine().getStatusCode() != 200) {
                    String responseString;
                    try {
                        responseString = EntityUtils.toString(httpResponse.getEntity());
                    } catch (IOException e) {
                        responseString = "[IOException while converting response entity to string]";
                    }
                    Log.e(this.getClass().toString(), "Beacon post returned " +
                            Integer.toString(httpResponse.getStatusLine().getStatusCode()) + ": \n" + responseString);

                } else {
                    Log.d(this.getClass().toString(), "Request was sent to beacon cms [" + server + "].");
                }

                switch (responseType) {
                    case (BEACON_RESPONSE):
                        // Get the beacons and store them in preferences...
                        try {
                            JSONArray jsonArray = new JSONArray(EntityUtils.toString(httpResponse.getEntity()));
                            CMSBeacon.setBeacons(jsonArray);
                        } catch (JSONException e) {
                            Log.e(this.getClass().toString(), "JSONException while getting the beacon response: " + e.getMessage());
                        } catch (IOException e) {
                            Log.e(this.getClass().toString(), "IOException while getting the beacon response: " + e.getMessage());
                        }
                        break;
                    default:
                        break;

                }

                return httpResponse;
            } catch (ClientProtocolException e) {
                Log.e(this.getClass().toString(), "ClientProtocolException while posting to the beacon cms: " + e.getMessage());
            } catch (IOException e) {
                Log.e(this.getClass().toString(), "IOException while posting to the beacon cms: " + e.getMessage());
            } catch (JSONException e) {
                Log.e(this.getClass().toString(), "JSONException while posting to the beacon cms: " + e.getMessage());
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
    public static void updateMobileUser(String firstName, String lastName, String email, String uid, String device_id, String os) {
        JSONObject body = new JSONObject();
        try {
            body.put("first_name", firstName);
            body.put("last_name", lastName);
            body.put("email", email);
            body.put("uid", uid);
            body.put("device_id", device_id);
            body.put("os", os);
        } catch (JSONException e) {
            Log.e(BeaconServerSender.class.toString(), "JSONException while creating the body message: " + e.getMessage());
        }

        BeaconServerSender sender = new BeaconServerSender(SmartAlertApplication.getAppContext(), "users/mobile_user");
        sender.execute(body);
    }

    /**
     * Create or Update a beacon
     */
    public static void updateBeacons(Context context) {
        //TODO: for now I'm skipping if the beacons exist, but the plan is to change this and use SQLLite
        List<CMSBeacon> beacons = null;
        try {
            beacons = CMSBeacon.getBeacons();

        } catch (JSONException e) {
            Log.e(BeaconServerSender.class.toString(), "JSONException while trying to get the beacons: " + e.getMessage());
        }
        if (beacons == null || beacons.size() == 0) {
            BeaconServerSender sender = new BeaconServerSender(context, "beacons.json", BEACON_RESPONSE);
            sender.execute((JSONObject) null);
        }
    }
}
