/*
 * Copyright 2012 Urban Airship and Contributors
 */

package com.slalomdigital.smartalert;

import android.os.Bundle;
import android.util.SparseArray;

import com.urbanairship.UrbanAirshipProvider;

public class SmartAlertInboxFragment extends InboxFragment {

    public static SmartAlertInboxFragment newInstance(int rowLayoutId, int emptyListStringId) {
        SmartAlertInboxFragment inbox = new SmartAlertInboxFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(ROW_LAYOUT_ID_KEY, rowLayoutId);
        arguments.putInt(EMPTY_LIST_STRING_KEY, emptyListStringId);
        return inbox;
    }

    @Override
    public SparseArray<String> createUIMapping() {
        SparseArray<String> mapping = new SparseArray<String>();
        mapping.put(R.id.unread_indicator, UrbanAirshipProvider.RichPush.COLUMN_NAME_UNREAD);
        mapping.put(R.id.title, UrbanAirshipProvider.RichPush.COLUMN_NAME_TITLE);
        mapping.put(R.id.date_sent, UrbanAirshipProvider.COLUMN_NAME_TIMESTAMP);
        mapping.put(R.id.message_checkbox, EMPTY_COLUMN_NAME);
        return mapping;
    }
}
