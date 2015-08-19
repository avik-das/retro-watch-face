/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.avikdas.eightbitwatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class EightBitWatchFace extends CanvasWatchFaceService {
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        final Handler mUpdateTimeHandler = new EngineHandler(this);

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getTimeZone(intent.getStringExtra("time-zone")));
                invalidate();
            }
        };

        boolean mRegisteredTimeZoneReceiver = false;

        boolean mAmbient;

        Calendar mCalendar;

        // graphic objects
        Bitmap mBackgroundBitmap;
        Bitmap mBackgroundScaledBitmap;
        Bitmap mFontBitmap;
        Paint ambientBackgroundPaint;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            // configure the system UI
            setWatchFaceStyle(
                    new WatchFaceStyle.Builder(EightBitWatchFace.this)
                            .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                            .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                            .setShowSystemUiTime(false)
                            .build()
            );

            Resources resources = EightBitWatchFace.this.getResources();

            // load the background image
            Drawable backgroundDrawable = resources.getDrawable(R.drawable.bg, null);
            assert backgroundDrawable != null;
            mBackgroundBitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();

            // load the font image
            Drawable fontDrawable = resources.getDrawable(R.drawable.font, null);
            assert fontDrawable != null;
            mFontBitmap = ((BitmapDrawable) fontDrawable).getBitmap();

            // create graphics styles
            ambientBackgroundPaint = new Paint();
            ambientBackgroundPaint.setARGB(255, 0, 0, 0);

            // allocate a Calendar instance to calculate the local time using the UTC time and the
            // time zone
            mCalendar = Calendar.getInstance();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            EightBitWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            EightBitWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (mBackgroundScaledBitmap == null ||
                    mBackgroundScaledBitmap.getWidth() != width ||
                    mBackgroundScaledBitmap.getHeight() != height) {
                // All of the below assumes a 1:1 aspect ratio. In the future, we may want to crop.

                mBackgroundScaledBitmap = Bitmap.createScaledBitmap(
                        mBackgroundBitmap,
                        width,
                        height,
                        /* filter = */ false
                );
            }

            super.onSurfaceChanged(holder, format, width, height);
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Update the time
            mCalendar.setTimeInMillis(System.currentTimeMillis());

            int width = bounds.width();
            int height = bounds.height();

            if (isInAmbientMode()) {
                drawAmbientMode(canvas, width, height);
            } else {
                drawInteractiveMode(canvas, width);
            }
        }

        private void drawInteractiveMode(Canvas canvas, int width) {
            canvas.drawBitmap(mBackgroundScaledBitmap, 0, 0, null);
            drawDigits(canvas, width);
        }

        private void drawAmbientMode(Canvas canvas, int width, int height) {
            canvas.drawRect(new Rect(0, 0, width, height), ambientBackgroundPaint);
            drawDigits(canvas, width);
        }

        private void drawDigits(Canvas canvas, int totalWidth) {
            // All of the below assumes square characters. This does not depend on the size of the
            // display.

            int hour = mCalendar.get(Calendar.HOUR);
            if (hour == 0) {
                hour = 12;
            }

            int minute = mCalendar.get(Calendar.MINUTE);
            int second = mCalendar.get(Calendar.SECOND);

            // First digit of the hour
            canvas.drawBitmap(
                    mFontBitmap,
                    srcRectForDigit(hour / 10),
                    new Rect(
                            (int) (totalWidth * 0.5f),
                            (int) (totalWidth * 0.1f),
                            (int) (totalWidth * 0.7f),
                            (int) (totalWidth * 0.3f)
                    ),
                    null
            );

            // Second digit of the hour
            canvas.drawBitmap(
                    mFontBitmap,
                    srcRectForDigit(hour % 10),
                    new Rect(
                            (int) (totalWidth * 0.7f),
                            (int) (totalWidth * 0.1f),
                            (int) (totalWidth * 0.9f),
                            (int) (totalWidth * 0.3f)
                    ),
                    null
            );

            // First digit of the minute
            canvas.drawBitmap(
                    mFontBitmap,
                    srcRectForDigit(minute / 10),
                    new Rect(
                            (int) (totalWidth * 0.5f),
                            (int) (totalWidth * 0.35f),
                            (int) (totalWidth * 0.7f),
                            (int) (totalWidth * 0.55f)
                    ),
                    null
            );

            // Second digit of the minute
            canvas.drawBitmap(
                    mFontBitmap,
                    srcRectForDigit(minute % 10),
                    new Rect(
                            (int) (totalWidth * 0.7f),
                            (int) (totalWidth * 0.35f),
                            (int) (totalWidth * 0.9f),
                            (int) (totalWidth * 0.55f)
                    ),
                    null
            );

            // Only draw the seconds in interactive mode
            if (!isInAmbientMode()) {
                // First digit of the second
                canvas.drawBitmap(
                        mFontBitmap,
                        srcRectForDigit(second / 10),
                        new Rect(
                                (int) (totalWidth * 0.55f),
                                (int) (totalWidth * 0.6f),
                                (int) (totalWidth * 0.65f),
                                (int) (totalWidth * 0.7f)
                        ),
                        null
                );

                // Second digit of the second
                canvas.drawBitmap(
                        mFontBitmap,
                        srcRectForDigit(second % 10),
                        new Rect(
                                (int) (totalWidth * 0.75f),
                                (int) (totalWidth * 0.6f),
                                (int) (totalWidth * 0.85f),
                                (int) (totalWidth * 0.7f)
                        ),
                        null
                );
            }
        }

        private Rect srcRectForDigit(int digit) {
            // Assumes 0 <= digit < 10,

            // All of the below assumes square characters. This does not depend on the size of the
            // display.

            int x = mFontBitmap.getHeight() * digit;

            return new Rect(
                    x,
                    0,
                    x + mFontBitmap.getHeight(),
                    mFontBitmap.getHeight()
            );
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<EightBitWatchFace.Engine> mWeakReference;

        public EngineHandler(EightBitWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            EightBitWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }
}
