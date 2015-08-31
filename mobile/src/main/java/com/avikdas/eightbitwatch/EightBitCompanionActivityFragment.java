package com.avikdas.eightbitwatch;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.Set;

/**
 * A placeholder fragment containing a simple view.
 */
public class EightBitCompanionActivityFragment extends Fragment implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String LOG_TAG = "ConfigurationFragment";
    private static final String EIGHTBIT_WATCH_FACE_CAPABILITY_NAME = "eightbit-watch-face";
    private static final String EIGHTBIT_WATCH_FACE_CONFIG_PATH = "/eightbit-watch-face/config";
    private static final String KEY_DAY_NIGHT_MODE = "day-night-mode";

    private GoogleApiClient mGoogleApiClient;
    private String mPeerId;

    // Views
    private ViewGroup mDayNightModeButtonGroup;

    // State
    // TODO: this will be serialized in the future
    private DayNightMode mDayNightMode = DayNightMode.AUTO_MODE;

    public EightBitCompanionActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_eight_bit_watch_face_settings, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mDayNightModeButtonGroup = (ViewGroup) view.findViewById(R.id.day_night_mode_button_group);
        setUpDayNightButtonGroupListeners();
    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    private void setUpDayNightButtonGroupListeners() {
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = mDayNightModeButtonGroup.indexOfChild(v);
                updateDayNightMode(DayNightMode.values()[index]);
                renderDayNightCheck();
            }
        };

        for (int i = 0; i < mDayNightModeButtonGroup.getChildCount(); i++) {
            View buttonGroup = mDayNightModeButtonGroup.getChildAt(i);
            buttonGroup.setOnClickListener(clickListener);
        }
    }

    private void renderDayNightCheck() {
        // The button group consists of a series of groups, each containing the main image (always
        // visible) and the check overlay (conditionally visible).

        for (int i = 0; i < mDayNightModeButtonGroup.getChildCount(); i++) {
            ViewGroup buttonGroup = (ViewGroup) mDayNightModeButtonGroup.getChildAt(i);

            View checkOverlay = buttonGroup.getChildAt(1);
            checkOverlay.setVisibility(
                    mDayNightMode.ordinal() == i ? View.VISIBLE : View.INVISIBLE
            );
        }
    }

    private void updateDayNightMode(DayNightMode newMode) {
        mDayNightMode = newMode;

        DataMap config = new DataMap();
        config.putInt(KEY_DAY_NIGHT_MODE, mDayNightMode.ordinal());
        sendConfigUpdate(config);
    }

    private void pickPeer() {
        Wearable.CapabilityApi.getCapability(
                mGoogleApiClient,
                EIGHTBIT_WATCH_FACE_CAPABILITY_NAME,
                CapabilityApi.FILTER_REACHABLE
        ).setResultCallback(new ResultCallback<CapabilityApi.GetCapabilityResult>() {
            @Override
            public void onResult(CapabilityApi.GetCapabilityResult result) {
                updatePeerId(result.getCapability());
            }
        });

        CapabilityApi.CapabilityListener capabilityListener =
                new CapabilityApi.CapabilityListener() {
                    @Override
                    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
                        updatePeerId(capabilityInfo);
                    }
                };

        Wearable.CapabilityApi.addCapabilityListener(
                mGoogleApiClient,
                capabilityListener,
                EIGHTBIT_WATCH_FACE_CAPABILITY_NAME
        );
    }

    private void updatePeerId(CapabilityInfo capabilityInfo) {
        Set<Node> connectedNodes = capabilityInfo.getNodes();

        // If the existing peer ID is still valid and continues to be the nearest node, we'll be
        // able to find it again. If we can't find it, it's better to not keep the old ID around.
        mPeerId = null;
        for (Node node : connectedNodes) {
            if (node.isNearby()) {
                mPeerId = node.getId();
                break;
            }

            mPeerId = node.getId();
        }

        if (mPeerId == null) {
            log("Unable to find peer with required capability");
        } else {
            log("Found peer: " + mPeerId);
        }
    }

    private void sendConfigUpdate(DataMap data) {
        if (mPeerId == null) {
            return;
        }

        byte[] rawData = data.toByteArray();
        Wearable.MessageApi.sendMessage(
                mGoogleApiClient,
                mPeerId,
                EIGHTBIT_WATCH_FACE_CONFIG_PATH,
                rawData
        ).setResultCallback(
                new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        if (sendMessageResult.getStatus().isSuccess()) {
                            log("Message sent");
                        } else {
                            log("Message failed to send");
                        }
                    }
                }
        );
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        log("onConnected: " + connectionHint);
        pickPeer();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        log("onSuspended: " + cause);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        log("onConnectionFailed: " + connectionResult);
    }

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
    }

    private enum DayNightMode {
        DAY_MODE,
        NIGHT_MODE,
        AUTO_MODE
    }
}
