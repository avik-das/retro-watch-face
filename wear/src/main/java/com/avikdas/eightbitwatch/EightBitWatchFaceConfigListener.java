package com.avikdas.eightbitwatch;

import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

public class EightBitWatchFaceConfigListener
        extends WearableListenerService {
    private static final String LOG_TAG = "ConfigListener";
    private static final String EIGHTBIT_WATCH_FACE_CONFIG_PATH = "/eightbit-watch-face/config";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (!EIGHTBIT_WATCH_FACE_CONFIG_PATH.equals(messageEvent.getPath())) {
            Log.d(LOG_TAG, "Got unknown message");
            return;
        }

        byte[] data = messageEvent.getData();
        char[] chars = new char[data.length];
        for (int i = 0; i < data.length; i++) {
            chars[i] = (char) data[i];
        }

        Log.d(LOG_TAG, "Got config message with data: " + String.valueOf(chars));
    }
}
