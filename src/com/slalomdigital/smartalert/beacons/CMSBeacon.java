package com.slalomdigital.smartalert.beacons;

import android.content.Context;
import android.content.SharedPreferences;
import com.slalomdigital.smartalert.SmartAlertApplication;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A beacon from the CMS
 * Created by aaronc on 4/2/14.
 */
public class CMSBeacon {
    long firstSeen = 0;
    long lastSeen = 0;
    public int id;
    public String uuid;
    public int major;
    public int minor;
    public Item item;
    public String url;

    public static final String BEACON_PREFERENCE = "beacons";

    public CMSBeacon(JSONObject jsonObject) throws JSONException {
        if (jsonObject.has("first_seen")) {
            firstSeen = jsonObject.getLong("first_seen");
        }
        if (jsonObject.has("last_seen")) {
            lastSeen = jsonObject.getLong("last_seen");
        }
        id = jsonObject.getInt("id");
        uuid = jsonObject.getString("uuid");
        major = jsonObject.getInt("major");
        minor = jsonObject.getInt("minor");
        item = new Item(jsonObject.getJSONObject("item"));
    }

    private static List<CMSBeacon> getBeacons(JSONArray jsonBeacons) throws JSONException {
        ArrayList<CMSBeacon> beacons = new ArrayList<CMSBeacon>();
        for (int i = 0; i < jsonBeacons.length(); i++) {
            beacons.add(new CMSBeacon(jsonBeacons.getJSONObject(i)));
        }
        return beacons;
    }

    public static List<CMSBeacon> getBeacons() throws JSONException {
        SharedPreferences beaconPrefs = SmartAlertApplication.getAppContext().getSharedPreferences(CMSBeacon.BEACON_PREFERENCE, 0);
        JSONArray jsonBeacons = new JSONArray(beaconPrefs.getString("json", "[]"));
        return getBeacons(jsonBeacons);
    }

    public static void setBeacons(JSONArray jsonArray) throws JSONException, IOException {
        SharedPreferences beaconPrefs = SmartAlertApplication.getAppContext().getSharedPreferences(CMSBeacon.BEACON_PREFERENCE, 0);
        SharedPreferences.Editor editor = beaconPrefs.edit();
        editor.putString("json", jsonArray.toString());
        editor.commit();
    }

    public static void setBeacons(List<CMSBeacon> beacons) throws JSONException, IOException {
        JSONArray jsonBeacons = new JSONArray();
        for (CMSBeacon beacon: beacons) {
            jsonBeacons.put(beacon.toJSON());
        }
        setBeacons(jsonBeacons);
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject object = new JSONObject();
        if (firstSeen != 0) {
            object.put("first_seen", firstSeen);
        }
        if (lastSeen != 0) {
            object.put("last_seen", lastSeen);
        }
        object.put("id", id);
        object.put("uuid", uuid);
        object.put("major", major);
        object.put("minor", minor);
        object.put("item", item.toJSON());
        return object;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CMSBeacon cmsBeacon = (CMSBeacon) o;

        if (major != cmsBeacon.major) return false;
        if (minor != cmsBeacon.minor) return false;
        if (!uuid.equals(cmsBeacon.uuid)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = uuid.hashCode();
        result = 31 * result + major;
        result = 31 * result + minor;
        return result;
    }
}
