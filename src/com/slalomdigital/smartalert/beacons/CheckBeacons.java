package com.slalomdigital.smartalert.beacons;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;
import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.Utils;
import com.slalomdigital.smartalert.BeaconActivity;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Checks for estimote BLE beacons in the background.  (pretty sweet right?)
 * User: aaronc
 * Date: 6/6/13
 * Time: 10:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class CheckBeacons extends Service {
    private static final String TAG = CheckBeacons.class.getSimpleName();

    private static final String ESTIMOTE_BEACON_PROXIMITY_UUID = "B9407F30-F5F8-466E-AFF9-25556B57FE6D";
    private static final String ESTIMOTE_IOS_PROXIMITY_UUID = "8492E75F-4FD6-469D-B132-043FE94921D8";
    private static final Region ALL_ESTIMOTE_BEACONS_REGION = new Region("rid", null, null, null);

    private static final double MIN_DISTANCE = 0.5; //Minimum distance in meters for a beacon

    // Timestamp to check from...
    private BeaconManager beaconManager;

    @Override
    public void onCreate() {
            // Check for beacons...
            beaconManager = new BeaconManager(this);
            if (beaconManager.hasBluetooth() && beaconManager.isBluetoothEnabled()) {
                beaconManager.setRangingListener(new BeaconManager.RangingListener() {
                    @Override
                    public void onBeaconsDiscovered(Region region, final List<Beacon> rangedBeacons) {
                        long timestamp = System.currentTimeMillis();
                        //Send the beacons seen to the server...
                        List<CMSBeacon> cmsBeacons;
                        try {
                            // Get the CMS beacons...
                            cmsBeacons = CMSBeacon.getBeacons();
                        } catch (JSONException e) {
                            Log.e(TAG, "JSONException while getting beacons from shared prefs shouldn't happen.");
                            return;
                        }

                        // Check for beacons in range...
                        for (Beacon rangedBeacon : rangedBeacons) {
                            if (Utils.computeAccuracy(rangedBeacon) < MIN_DISTANCE) {
                                for (CMSBeacon cmsBeacon : cmsBeacons) {
                                    if (rangedBeacon.getProximityUUID().equalsIgnoreCase(cmsBeacon.uuid) &&
                                        rangedBeacon.getMajor() == cmsBeacon.major &&
                                        rangedBeacon.getMinor() == cmsBeacon.minor) {
                                        cmsBeacon.lastSeen = timestamp;
                                        //Is this the first time the beacon was seen?
                                        if (cmsBeacon.firstSeen == 0) {
                                            cmsBeacon.firstSeen = cmsBeacon.lastSeen;
                                        }

                                        // Check to see if we should show the arrival content...
                                        if (!cmsBeacon.item.shown &&
                                            cmsBeacon.item.show_after_seconds * 1000 <= cmsBeacon.lastSeen - cmsBeacon.firstSeen) {
                                            BeaconActivity.showArrivalItem(cmsBeacon.item);
                                        }
                                    }
                                }
                            }
                        }

                        // Check for beacons gone out of range...
                        for (CMSBeacon cmsBeacon: cmsBeacons) {
                            if (cmsBeacon.item.shown && (cmsBeacon.lastSeen + (cmsBeacon.item.show_after_seconds * 1000)) < timestamp) {
                                cmsBeacon.lastSeen = 0;
                                cmsBeacon.firstSeen = 0;
                                BeaconActivity.showExitItem(cmsBeacon.item);
                            }
                        }

                        // Write the beacons back in case there were updates...
                        try {
                            CMSBeacon.setBeacons(cmsBeacons);
                        } catch (JSONException e) {
                            Log.e(TAG, "JSONException while saving beacons to shared prefs shouldn't happen.");
                        } catch (IOException e) {
                            Log.e(TAG, "IOException while saving beacons to shared prefs: " + e.getMessage());
                        }
                    }
                });
            } else {
                beaconManager = null;
            }
    }

    private List<Beacon> filterBeacons(List<Beacon> beacons) {
        List<Beacon> filteredBeacons = new ArrayList<Beacon>(beacons.size());
        for (Beacon beacon : beacons) {
            if (beacon.getProximityUUID().equalsIgnoreCase(ESTIMOTE_BEACON_PROXIMITY_UUID)
                    || beacon.getProximityUUID().equalsIgnoreCase(ESTIMOTE_IOS_PROXIMITY_UUID)) {
                filteredBeacons.add(beacon);
            }
        }
        return filteredBeacons;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (beaconManager != null) {
            connectToService();
            return START_NOT_STICKY;
        } else {
            stopSelf();
            return START_NOT_STICKY;
        }
    }

    public IBinder onBind(Intent intent) {
        // This service is pretending to be the server for SmartAlert
        // so it doesn't interact with the application at all it just runs
        // while the application is running.
        return null;
    }

    @Override
    public void onDestroy() {
        if (beaconManager != null) {
            try {
                beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS_REGION);
                Log.d(TAG, "Stopping estimote scanning.");
            } catch (RemoteException e) {
                Log.d(TAG, "Error while stopping ranging", e);
            }
            beaconManager.disconnect();
        }

        super.onDestroy();
    }

    private void connectToService() {
        /* Toast.makeText(this, "Scanning for estimotes...",
                Toast.LENGTH_LONG).show(); */
        Log.d(TAG, "Scanning for estimotes...");
        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                try {
                    beaconManager.startRanging(ALL_ESTIMOTE_BEACONS_REGION);
                } catch (RemoteException e) {
                    Toast.makeText(CheckBeacons.this, "Cannot start ranging, something terrible happened",
                            Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Cannot start ranging", e);
                }
            }
        });
    }


}
