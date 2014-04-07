/*
 * Copyright 2012 Urban Airship and Contributors
 */

package com.slalomdigital.smartalert;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.*;
import com.actionbarsherlock.app.SherlockFragment;
import com.slalomdigital.smartalert.beacons.Item;
import com.urbanairship.Logger;
import com.urbanairship.UAirship;
import com.urbanairship.richpush.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

@SuppressLint("SetJavaScriptEnabled")
public class BeaconActivity extends Activity {
    private class BeaconWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
/*
            if (Uri.parse(url).getHost().equals("www.example.com")) {
                // This is my web site, so do not override; let my WebView load the page
                return false;
            }
            // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
*/
            return false;
        }
    }

    private static final String EXTRA_URL = "url";
    WebView browser;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.beacon_message);

        this.browser = (WebView) findViewById(R.id.browser);
        this.initializeBrowser();
        Intent intent = getIntent();
        String url = intent.getStringExtra(BeaconActivity.EXTRA_URL);
        this.browser.loadUrl(url);
        return;
    }

    // helpers
    private void initializeBrowser() {
        this.browser.getSettings().setJavaScriptEnabled(true);
        this.browser.getSettings().setDomStorageEnabled(true);
        this.browser.getSettings().setAppCacheEnabled(true);
        this.browser.getSettings().setAllowFileAccess(true);
        this.browser.getSettings().setAppCacheMaxSize(1024 * 1024 * 8);
        this.browser.getSettings().setAppCachePath(SmartAlertApplication.getAppContext().getCacheDir().getAbsolutePath());
        this.browser.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        this.browser.setWebChromeClient(new WebChromeClient());
        this.browser.setWebViewClient(new BeaconWebViewClient());
    }

    /**
     * Called when the user has arrived at the beacon
     */
    public static void showArrivalItem(Item item) {
        item.shown = true;
        Intent intent = new Intent(SmartAlertApplication.getAppContext(), BeaconActivity.class);
        intent.putExtra(EXTRA_URL, item.content);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        SmartAlertApplication.getAppContext().startActivity(intent);
    }

    /**
     * Called when the user leaves the beacon
     */
    public static void showExitItem(Item item) {
        item.shown = false;
        Intent intent = new Intent(SmartAlertApplication.getAppContext(), BeaconActivity.class);
        intent.putExtra(EXTRA_URL, "http://www.yahoo.com");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        SmartAlertApplication.getAppContext().startActivity(intent);
    }
}
