package com.slalomdigital.smartalert.beacons;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * iBeacon CMS item
 * Created by aaronc on 4/3/14.
 */
public class Item {
    public boolean shown = false;

    public int id;
    public String spec;
    public String name;
    public String description;
    public String content;
    public int show_after_seconds;
    public int beacon_id;
    public String created_at;
    public String updated_at;

    public Item(JSONObject jsonObject) throws JSONException {
        if (jsonObject.has("shown")) {
            shown = jsonObject.getBoolean("shown");
        }
        id = jsonObject.getInt("id");
        spec = jsonObject.getString("spec");
        name = jsonObject.getString("name");
        description = jsonObject.getString("description");
        content = jsonObject.getString("content");
        show_after_seconds = jsonObject.getInt("show_after_seconds");
        beacon_id = jsonObject.getInt("beacon_id");
        created_at = jsonObject.getString("created_at");
        updated_at = jsonObject.getString("updated_at");
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("shown", shown);
        object.put("id", id);
        object.put("spec", spec);
        object.put("name", name);
        object.put("description", description);
        object.put("content", content);
        object.put("show_after_seconds", show_after_seconds) ;
        object.put("beacon_id", beacon_id);
        object.put("created_at", created_at);
        object.put("updated_at",updated_at);
        return object;
    }

}
