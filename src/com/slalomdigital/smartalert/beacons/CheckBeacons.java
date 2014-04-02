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
import org.json.JSONObject;

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

    // Timestamp to check from...
    public static final String LAST_LIKE_PREFERENCE = "last_like";
    private BeaconManager beaconManager;
    private Beacon beacon;


    @Override
    public void onCreate() {
        // Check for new beacons...

        beaconManager = new BeaconManager(this);
        if (beaconManager.hasBluetooth() && beaconManager.isBluetoothEnabled()) {
            beaconManager.setRangingListener(new BeaconManager.RangingListener() {
                @Override
                public void onBeaconsDiscovered(Region region, final List<Beacon> rangedBeacons) {
                    //Send the beacons seen to the server...
                    for (Beacon rangedBeacon : rangedBeacons) {

                        BeaconServerSender.updateBeacon();
                    }
                }
            });
        }
        else {
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
            return START_STICKY;
        }
        else {
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
