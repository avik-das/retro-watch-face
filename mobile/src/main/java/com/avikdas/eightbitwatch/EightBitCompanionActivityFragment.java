package com.avikdas.eightbitwatch;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A placeholder fragment containing a simple view.
 */
public class EightBitCompanionActivityFragment extends Fragment {
    // Views
    private ViewGroup mDayNightModeButtonGroup;

    // State
    // TODO: this will be serialized in the future
    private DayNightMode mDayNightMode = DayNightMode.AUTO_MODE;

    public EightBitCompanionActivityFragment() {
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

    private void setUpDayNightButtonGroupListeners() {
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = mDayNightModeButtonGroup.indexOfChild(v);
                mDayNightMode = DayNightMode.values()[index];
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

    private enum DayNightMode {
        DAY_MODE,
        NIGHT_MODE,
        AUTO_MODE
    }
}
