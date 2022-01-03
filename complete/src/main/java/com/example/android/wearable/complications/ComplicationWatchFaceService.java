package com.example.android.wearable.complications;

import static com.example.android.wearable.complications.ComplicationLocation.*;
import static com.example.android.wearable.complications.Constants.*;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.rendering.ComplicationDrawable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;

import androidx.core.content.ContextCompat;

import java.util.Calendar;
import java.util.TimeZone;

public class ComplicationWatchFaceService extends CanvasWatchFaceService {

    private static final String TAG = "ComplicationWatchFace";
    private static Engine engine;

    // Used by {@link ComplicationConfigActivity} to retrieve complication types supported by
    // location.
    static int[] getSupportedComplicationTypes(
            ComplicationLocation complicationLocation) {
        // Add any other supported locations here.
        if (complicationLocation== BOTTOM)
            return LARGE_COMPLICATION_TYPES;
        else
            return NORMAL_COMPLICATION_TYPES;

    }

    @Override
    public Engine onCreateEngine() {
        engine = new Engine();
        return engine;
    }

    public static Engine getEngine() {
        return engine;
    }

    public class Engine extends CanvasWatchFaceService.Engine {

        private int BOTTOM_ROW_ITEM_SIZE = 24;

        private Calendar calendar;
        private boolean mRegisteredTimeZoneReceiver = false;

        private float centerX;
        private float centerY;
        private int complicationMargin;

        private Paint backgroundPaint;
        private Paint centerPaint;
        private Paint bottomPaint;
        private BackgroundDividerDrawable backgroundDividerDrawable;

        private boolean isAmbientMode;
        private boolean isHollowMode;


        /*
         * Whether the display supports fewer bits for each color in ambient mode.
         * When true, we disable anti-aliasing in ambient mode.
         */
        private boolean hasLowBitAmbient;

        /*
         * Whether the display supports burn in protection in ambient mode.
         * When true, remove the persistent images in ambient mode.
         */
        private boolean hasBurnInProtection;

        /* Maps complication ids to corresponding ComplicationDrawable that renders the
         * the complication data on the watch face.
         */
        private ComplicationDrawable[] mComplicationDrawables;

        // Stores the ranged complication on the edge of the screen
        private ArcComplication[] mRangedComplications;

        /* Maps active complication ids to the data for that complication. Note: Data will only be
         * present if the user has chosen a provider via the settings activity for the watch face.
         */
        private ComplicationData[] complicationData;

        private final BroadcastReceiver mTimeZoneReceiver =
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        calendar.setTimeZone(TimeZone.getDefault());
                        invalidate();
                    }
                };

        // Handler to update the time once a second in interactive mode.
        private final Handler mUpdateTimeHandler = new UpdateHandler(this);

        private void setTextSizeForWidth(Paint paint, float desiredWidth) {

            // Pick a reasonably large value for the test. Larger values produce
            // more accurate results, but may cause problems with hardware
            // acceleration. But there are workarounds for that, too; refer to
            // http://stackoverflow.com/questions/6253528/font-size-too-large-to-fit-in-cache
            final float testTextSize = 48f;

            // Get the bounds of the text, using our testTextSize.
            paint.setTextSize(testTextSize);
            Rect bounds = new Rect();
            paint.getTextBounds("22:22", 0, "22:22".length(), bounds);

            // Calculate the desired size as a proportion of our testTextSize.
            float desiredTextSize = testTextSize * desiredWidth / bounds.width();

            // Set the paint for that size.
            paint.setTextSize(desiredTextSize);
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(
                    new WatchFaceStyle.Builder(ComplicationWatchFaceService.this)
                            .setAcceptsTapEvents(true)
                            .build());

            calendar = Calendar.getInstance();

            backgroundPaint = new Paint();
            backgroundPaint.setColor(Color.BLACK);

            centerPaint = new Paint();
            centerPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            centerPaint.setStrokeWidth(1f);
            centerPaint.setTextAlign(Paint.Align.CENTER);
            centerPaint.setColor(Color.WHITE);
            centerPaint.setAntiAlias(true);

            bottomPaint = new Paint();
            bottomPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            bottomPaint.setStrokeWidth(1f);
            bottomPaint.setTextAlign(Paint.Align.CENTER);
            bottomPaint.setTextSize(BOTTOM_ROW_ITEM_SIZE);
            bottomPaint.setColor(Color.WHITE);
            bottomPaint.setAntiAlias(true);

            initializeComplications();
        }

        public boolean getHollowMode() {
            return isHollowMode;
        }

        public void setHollowMode(boolean hollow) {
            isHollowMode = hollow;
            if(hollow) {
                centerPaint.setStyle(Paint.Style.STROKE);
                bottomPaint.setStyle(Paint.Style.STROKE);
                for (ArcComplication mRangedComplication : mRangedComplications) {
                    mRangedComplication.setHollow(true);
                }
            } else {
                centerPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                bottomPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                for (ArcComplication mRangedComplication : mRangedComplications) {
                    mRangedComplication.setHollow(false);
                }
            }
            invalidate();
        }

        private void initializeComplications() {
            complicationData = new ComplicationData[COMPLICATION_IDS.length];
            mComplicationDrawables = new ComplicationDrawable[COMPLICATION_IDS.length];
            mRangedComplications = new ArcComplication[RANGE_COMPLICATION_COUNT];

            for(int i = 0; i < COMPLICATION_IDS.length; i++) {
                initializeComplication(i);
            }

            setActiveComplications(COMPLICATION_IDS);
        }

        private void initializeComplication(int complicationId) {
            ComplicationDrawable complicationDrawable =
                    (ComplicationDrawable) getDrawable(R.drawable.custom_complication_styles);
            if (complicationDrawable != null) {
                complicationDrawable.setContext(getApplicationContext());
                mComplicationDrawables[complicationId] = complicationDrawable;
            }
        }
        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            hasLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            hasBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);

            ComplicationDrawable complicationDrawable;

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationDrawable = mComplicationDrawables[i];

                if(complicationDrawable != null) {
                    complicationDrawable.setLowBitAmbient(hasLowBitAmbient);
                    complicationDrawable.setBurnInProtection(hasBurnInProtection);
                }
            }
        }

        @Override
        public void onComplicationDataUpdate(
                int complicationId, ComplicationData complicationData) {
            // Adds/updates active complication data in the array.
            this.complicationData[complicationId] = complicationData;

            // Updates correct ComplicationDrawable with updated data.
            ComplicationDrawable complicationDrawable =
                    mComplicationDrawables[complicationId];
            complicationDrawable.setComplicationData(complicationData);

            invalidate();
        }

        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            if (tapType == TAP_TYPE_TAP) {
                int tappedComplicationId = getTappedComplicationId(x, y);
                if (tappedComplicationId != -1) {
                    onComplicationTap(tappedComplicationId);
                }
            }
        }

        /*
         * Determines if tap inside a complication area or returns -1.
         */
        private int getTappedComplicationId(int x, int y) {
            ComplicationData complicationData;
            ComplicationDrawable complicationDrawable;

            long currentTimeMillis = System.currentTimeMillis();

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationData = this.complicationData[i];

                if ((complicationData != null)
                        && (complicationData.isActive(currentTimeMillis))
                        && (complicationData.getType() != ComplicationData.TYPE_NOT_CONFIGURED)
                        && (complicationData.getType() != ComplicationData.TYPE_EMPTY)) {

                    complicationDrawable = mComplicationDrawables[i];
                    Rect complicationBoundingRect = complicationDrawable.getBounds();

                    if (complicationBoundingRect.width() > 0) {
                        if (complicationBoundingRect.contains(x, y)) {
                            return i;
                        }
                    } else {
                        Log.e(TAG, "Not a recognized complication id.");
                    }
                }
            }
            return -1;
        }

        // Fires PendingIntent associated with complication (if it has one).
        private void onComplicationTap(int complicationId) {
            ComplicationData complicationData =
                    this.complicationData[complicationId];

            if (complicationData != null) {

                if (complicationData.getTapAction() != null) {
                    try {
                        complicationData.getTapAction().send();
                    } catch (PendingIntent.CanceledException e) {
                        Log.e(TAG, "onComplicationTap() tap action error: " + e);
                    }

                } else if (complicationData.getType() == ComplicationData.TYPE_NO_PERMISSION) {

                    // Watch face does not have permission to receive complication data, so launch
                    // permission request.
                    ComponentName componentName =
                            new ComponentName(
                                    getApplicationContext(), ComplicationWatchFaceService.class);

                    Intent permissionRequestIntent =
                            ComplicationHelperActivity.createPermissionRequestHelperIntent(
                                    getApplicationContext(), componentName);

                    startActivity(permissionRequestIntent);
                }

            }
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            isAmbientMode = inAmbientMode;

            if(isAmbientMode) {
                if(hasLowBitAmbient) {
                    centerPaint.setAntiAlias(false);
                    bottomPaint.setAntiAlias(false);
                }

                centerPaint.setStyle(Paint.Style.FILL);
                bottomPaint.setStyle(Paint.Style.FILL);

            } else {
                if(hasLowBitAmbient) {
                    centerPaint.setAntiAlias(true);
                    bottomPaint.setAntiAlias(true);
                }

                centerPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                bottomPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            }

            // Update drawable complications' ambient state.
            // Note: ComplicationDrawable handles switching between active/ambient colors, we just
            // have to inform it to enter ambient mode.
            ComplicationDrawable complicationDrawable;

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationDrawable = mComplicationDrawables[i];
                complicationDrawable.setInAmbientMode(isAmbientMode);
            }

            for (ArcComplication mRangedComplication : mRangedComplications) {
                mRangedComplication.setAmbientMode(isAmbientMode);
            }

            // Check and trigger whether or not timer should be running (only in active mode).
            updateTimer();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            Context context = getApplicationContext();

            centerX = width / 2f;
            centerY = height / 2f;
            backgroundDividerDrawable = new BackgroundDividerDrawable(width, height);

            /* We suggest using at least 1/4 of the screen width for circular (or squared)
             * complications and 2/3 of the screen width for wide rectangular complications for
             * better readability */

            int sizeOfComplication = width / 4;
            int midpointOfScreen = width / 2;
            centerPaint.setTextSize(width / 8f);

            setTextSizeForWidth(centerPaint, width/4f);

            bottomPaint.setTextSize(sizeOfComplication / 4f);
            BOTTOM_ROW_ITEM_SIZE = sizeOfComplication / 3;
            complicationMargin = sizeOfComplication / 18;

            float rangeWidthF = width / 20f;
            int rangeThickness = width / 20;
            float rangeOffsetF = rangeWidthF / 2;
            int rangeOffset = rangeThickness / 2;

            RectF rangeBoundsF = new RectF(rangeOffsetF, rangeOffsetF, width - rangeOffsetF, height - rangeOffsetF);

            int radialMarginOffset = (midpointOfScreen - sizeOfComplication) / 2;
            int verticalOffset = midpointOfScreen - (sizeOfComplication / 2);

            //region Center bounds and complications

            // Left, Top, Right, Bottom
            Rect rightBounds = new Rect(
                    (width - sizeOfComplication),
                    verticalOffset,
                    (width) - (int) rangeWidthF,
                    (verticalOffset + sizeOfComplication));
            ComplicationDrawable rightComplicationDrawable =
                    mComplicationDrawables[RIGHT_COMPLICATION_ID];
            rightComplicationDrawable.setBounds(rightBounds);

            Rect topRightBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            (width - radialMarginOffset - sizeOfComplication),
                            (radialMarginOffset + (int)rangeOffsetF),
                            (width - radialMarginOffset - (int) rangeOffsetF),
                            (radialMarginOffset + sizeOfComplication));
            ComplicationDrawable topRightComplicationDrawable =
                    mComplicationDrawables[TOP_RIGHT_COMPLICATION_ID];
            topRightComplicationDrawable.setBounds(topRightBounds);

            Rect topBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            (midpointOfScreen - radialMarginOffset),
                            (0),
                            (midpointOfScreen + radialMarginOffset),
                            (sizeOfComplication + radialMarginOffset));
            ComplicationDrawable topComplicationDrawable =
                    mComplicationDrawables[TOP_COMPLICATION_ID];
            topComplicationDrawable.setBounds(topBounds);

            Rect topLeftBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            (radialMarginOffset + (int) rangeOffsetF),
                            (radialMarginOffset + (int) rangeOffsetF),
                            (radialMarginOffset + sizeOfComplication),
                            (radialMarginOffset + sizeOfComplication));
            ComplicationDrawable topLeftComplicationDrawable =
                    mComplicationDrawables[TOP_LEFT_COMPLICATION_ID];
            topLeftComplicationDrawable.setBounds(topLeftBounds);

            Rect leftBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            (int) rangeWidthF,
                            verticalOffset,
                            (sizeOfComplication),
                            (verticalOffset + sizeOfComplication));
            ComplicationDrawable leftComplicationDrawable =
                    mComplicationDrawables[LEFT_COMPLICATION_ID];
            leftComplicationDrawable.setBounds(leftBounds);

            // Left, Top, Right, Bottom
            Rect bottomBounds = new Rect(
                    radialMarginOffset,
                    leftBounds.bottom,
                    (width - radialMarginOffset),
                    leftBounds.bottom + sizeOfComplication);
            ComplicationDrawable bottomComplicationDrawable =
                    mComplicationDrawables[BOTTOM_COMPLICATION_ID];
            bottomComplicationDrawable.setBounds(bottomBounds);

            Rect centerBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            leftBounds.right,
                            topBounds.bottom,
                            rightBounds.left,
                            bottomBounds.top);
            ComplicationDrawable centerComplicationDrawable =
                    mComplicationDrawables[CENTER_COMPLICATION_ID];
            centerComplicationDrawable.setBounds(centerBounds);

            //endregion

            //region Arc bounds and complications

            Rect topRightRangedBounds =
                    // Left, Top, Right, Bottom
                    new Rect((int) rangeBoundsF.centerX() - rangeOffset,
                            0,
                            (int) rangeBoundsF.centerX() + rangeOffset,
                            rangeThickness);

            Rect topLeftRangedBounds = new Rect((int) rangeBoundsF.left - rangeOffset,
                    (int) rangeBoundsF.centerY() - rangeOffset,
                    (int) rangeBoundsF.left + rangeOffset,
                    (int) rangeBoundsF.centerY() + rangeOffset);

            Rect bottomRightRangedBounds = new Rect((int) rangeBoundsF.right - rangeOffset,
                    (int) rangeBoundsF.centerY() - rangeOffset,
                    (int) rangeBoundsF.right + rangeOffset,
                    (int) rangeBoundsF.centerY() + rangeOffset);

            Rect bottomLeftRangedBounds = new Rect((int) rangeBoundsF.centerX() - rangeOffset,
                    (int) rangeBoundsF.bottom - rangeOffset,
                    (int) rangeBoundsF.centerX() + rangeOffset,
                    (int) rangeBoundsF.bottom + rangeOffset);


            //Order matters when we add the complication in the array because we iterate in a clockwise direction
            ArcComplication topRightRanged = new ArcComplication(context, rangeBoundsF, topRightRangedBounds, rangeWidthF,
                    ContextCompat.getColor(context, R.color.purple), ContextCompat.getColor(context, R.color.light_purple),
                    280, 70);
            mRangedComplications[0] = topRightRanged;

            ArcComplication bottomRightRanged = new ArcComplication(context, rangeBoundsF, bottomRightRangedBounds, rangeWidthF,
                    ContextCompat.getColor(context, R.color.yellow), ContextCompat.getColor(context, R.color.light_yellow),
                    10, 70);
            mRangedComplications[1] = bottomRightRanged;

            ArcComplication bottomLeftRanged = new ArcComplication(context, rangeBoundsF, bottomLeftRangedBounds, rangeWidthF,
                    ContextCompat.getColor(context, R.color.red), ContextCompat.getColor(context, R.color.light_red),
                    100, 70);
            mRangedComplications[2] = bottomLeftRanged;

            ArcComplication topLeftRanged = new ArcComplication(context, rangeBoundsF, topLeftRangedBounds, rangeWidthF,
                    ContextCompat.getColor(context, R.color.green), ContextCompat.getColor(context, R.color.light_green),
                    190, 70);
            mRangedComplications[3] = topLeftRanged;

            //endregion
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            calendar.setTimeInMillis(now);

            drawBackground(canvas);

            if (complicationData[CENTER_COMPLICATION_ID] != null) {
                if (complicationData[CENTER_COMPLICATION_ID].getShortText() != null)
                    canvas.drawText(complicationData[CENTER_COMPLICATION_ID].getShortText().getText(
                            getApplicationContext(), now).toString(),
                            centerX, centerY + complicationMargin, centerPaint);
            }

            if (!isAmbientMode) {
                backgroundDividerDrawable.draw(canvas);
                drawComplications(canvas, now);
            }

            for (int i = 0; i < mRangedComplications.length; i++) {
                ComplicationData complicationData = this.complicationData[i + RANGED_ID_OFFSET];

                mRangedComplications[i].draw(canvas, complicationData);
            }
        }

        private void drawComplications(Canvas canvas, long currentTimeMillis) {
            for (int i = 0; i < CENTER_COMPLICATION_ID; i++) {
                ComplicationDrawable complicationDrawable = mComplicationDrawables[i];
                complicationDrawable.draw(canvas, currentTimeMillis);
            }
        }

        private void drawBackground(Canvas canvas) {
            if (isAmbientMode && (hasLowBitAmbient || hasBurnInProtection)) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawPaint(backgroundPaint);
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                // Update time zone in case it changed while we weren't visible.
                calendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            ComplicationWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            ComplicationWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        public boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }
    }
}
