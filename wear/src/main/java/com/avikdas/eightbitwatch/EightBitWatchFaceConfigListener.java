package com.avikdas.eightbitwatch;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

public class EightBitWatchFaceConfigListener
        extends WearableListenerService {
    private static final String LOG_TAG = "ConfigListener";
    private static final String EIGHTBIT_WATCH_FACE_CONFIG_PATH = "/eightbit-watch-face/config";

    public static final String ACTION_CONFIG_CHANGE = "config-change";
    public static final String CONFIG_DAY_NIGHT_MODE = "day-night-mode";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (!EIGHTBIT_WATCH_FACE_CONFIG_PATH.equals(messageEvent.getPath())) {
            Log.d(LOG_TAG, "Got unknown message");
            return;
        }

        DataMap configChanges = DataMap.fromByteArray(messageEvent.getData());

        boolean addedAnyKeys = false;
        Intent intent = new Intent(ACTION_CONFIG_CHANGE);

        if (configChanges.containsKey(CONFIG_DAY_NIGHT_MODE)) {
            intent.putExtra(CONFIG_DAY_NIGHT_MODE, configChanges.getInt(CONFIG_DAY_NIGHT_MODE));
            addedAnyKeys = true;
        }

        if (addedAnyKeys) {
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }
    }
}
