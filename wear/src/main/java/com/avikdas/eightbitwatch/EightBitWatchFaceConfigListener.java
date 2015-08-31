package com.avikdas.eightbitwatch;

import android.util.Log;

import com.google.android.gms.wearable.DataMap;
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

        DataMap configChanges = DataMap.fromByteArray(messageEvent.getData());
        // TODO: I'm not making these into constants because really, these configuration items will
        // be handled by the watch face service, not here.
        if (configChanges.containsKey("day-night-mode")) {
            String newMode;
            switch (configChanges.getInt("day-night-mode")) {
                case 0:
                    newMode = "day mode";
                    break;
                case 1:
                    newMode = "night mode";
                    break;
                case 2:
                    newMode = "auto mode";
                    break;
                default:
                    newMode = "unknown";
            }

            Log.d(LOG_TAG, "Switching to " + newMode);
        }
    }
}
