package com.example.android.wearable.complications;

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
import android.os.Message;
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
import java.util.concurrent.TimeUnit;

public class ComplicationWatchFaceService extends CanvasWatchFaceService {

    private static final String TAG = "ComplicationWatchFace";

    private static final int[][] COMPLICATION_SUPPORTED_TYPES = {
        {
            ComplicationData.TYPE_RANGED_VALUE,
            ComplicationData.TYPE_ICON,
            ComplicationData.TYPE_SHORT_TEXT,
            ComplicationData.TYPE_SMALL_IMAGE
        },
        {
            ComplicationData.TYPE_RANGED_VALUE,
            ComplicationData.TYPE_ICON,
            ComplicationData.TYPE_SHORT_TEXT,
            ComplicationData.TYPE_SMALL_IMAGE
        },
        {
            ComplicationData.TYPE_RANGED_VALUE,
            ComplicationData.TYPE_ICON,
            ComplicationData.TYPE_SHORT_TEXT,
            ComplicationData.TYPE_SMALL_IMAGE
        },
        {
            ComplicationData.TYPE_RANGED_VALUE,
            ComplicationData.TYPE_ICON,
            ComplicationData.TYPE_SHORT_TEXT,
            ComplicationData.TYPE_SMALL_IMAGE
        },
        {
            ComplicationData.TYPE_RANGED_VALUE,
            ComplicationData.TYPE_ICON,
            ComplicationData.TYPE_SHORT_TEXT,
            ComplicationData.TYPE_SMALL_IMAGE
        },
        {
            ComplicationData.TYPE_RANGED_VALUE,
            ComplicationData.TYPE_ICON,
            ComplicationData.TYPE_LARGE_IMAGE,
            ComplicationData.TYPE_LONG_TEXT
        },
        {
            ComplicationData.TYPE_RANGED_VALUE,
            ComplicationData.TYPE_ICON,
            ComplicationData.TYPE_SHORT_TEXT,
            ComplicationData.TYPE_SMALL_IMAGE,
            ComplicationData.TYPE_LARGE_IMAGE,
            ComplicationData.TYPE_LONG_TEXT
        },
        {
                ComplicationData.TYPE_RANGED_VALUE,
                ComplicationData.TYPE_ICON,
                ComplicationData.TYPE_SHORT_TEXT,
                ComplicationData.TYPE_SMALL_IMAGE
        },
        {
                ComplicationData.TYPE_RANGED_VALUE,
                ComplicationData.TYPE_ICON,
                ComplicationData.TYPE_SHORT_TEXT,
                ComplicationData.TYPE_SMALL_IMAGE
        },
        {
                ComplicationData.TYPE_RANGED_VALUE,
                ComplicationData.TYPE_ICON,
                ComplicationData.TYPE_SHORT_TEXT,
                ComplicationData.TYPE_SMALL_IMAGE
        },
        {
                ComplicationData.TYPE_RANGED_VALUE,
                ComplicationData.TYPE_ICON,
                ComplicationData.TYPE_SHORT_TEXT,
                ComplicationData.TYPE_SMALL_IMAGE
        }
    };

    // Used by {@link ComplicationConfigActivity} to retrieve all complication ids.
    static int[] getComplicationIds() {
        return COMPLICATION_IDS;
    }

    // Used by {@link ComplicationConfigActivity} to retrieve complication types supported by
    // location.
    static int[] getSupportedComplicationTypes(
            ComplicationConfigActivity.ComplicationLocation complicationLocation) {
        // Add any other supported locations here.
        switch (complicationLocation) {
            case RIGHT:
                return COMPLICATION_SUPPORTED_TYPES[RIGHT_COMPLICATION_ID];
            case TOP_RIGHT:
                return COMPLICATION_SUPPORTED_TYPES[TOP_RIGHT_COMPLICATION_ID];
            case TOP_RIGHT_RANGED:
                return COMPLICATION_SUPPORTED_TYPES[TOP_RIGHT_RANGED_COMPLICATION_ID];
            case TOP:
                return COMPLICATION_SUPPORTED_TYPES[TOP_COMPLICATION_ID];
            case TOP_LEFT:
                return COMPLICATION_SUPPORTED_TYPES[TOP_LEFT_COMPLICATION_ID];
            case TOP_LEFT_RANGED:
                return COMPLICATION_SUPPORTED_TYPES[TOP_LEFT_RANGED_COMPLICATION_ID];
            case LEFT:
                return COMPLICATION_SUPPORTED_TYPES[LEFT_COMPLICATION_ID];
            case BOTTOM:
                return COMPLICATION_SUPPORTED_TYPES[BOTTOM_COMPLICATION_ID];
            case BOTTOM_RIGHT_RANGED:
                return COMPLICATION_SUPPORTED_TYPES[BOTTOM_RIGHT_RANGED_COMPLICATION_ID];
            case BOTTOM_LEFT_RANGED:
                return COMPLICATION_SUPPORTED_TYPES[BOTTOM_LEFT_RANGED_COMPLICATION_ID];
            case CENTER:
                return COMPLICATION_SUPPORTED_TYPES[CENTER_COMPLICATION_ID];
            default:
                return new int[] {};
        }
    }

    /*
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        private static final int MSG_UPDATE_TIME = 0;
        private int BOTTOM_ROW_ITEM_SIZE = 24;
        Context context;

        private Calendar mCalendar;
        private boolean mRegisteredTimeZoneReceiver = false;

        private float mCenterX;
        private float mCenterY;
        private int mComplicationMargin;

        private Paint mBackgroundPaint;
        private Paint mLinePaint;
        private Paint mCenterPaint;
        private Paint mBottomPaint;

        private Rect bottomBounds;
        private Rect rightBounds;

        private RectF rangeBoundsF;

        private float l1StartX, l1StartY, l2StartX, l2StartY, l3StartX, l3StartY,
                l4StartX, l4StartY, l5StartX, l5StartY, l6StartX, l6StartY,
                l1EndX, l1EndY, l2EndX, l2EndY, l3EndX, l3EndY,
                l4EndX, l4EndY, l5EndX, l5EndY, l6EndX, l6EndY;
        private float rangeWidthF;
        private int rangeOffset;
        private boolean mAmbient;

        /*
         * Whether the display supports fewer bits for each color in ambient mode.
         * When true, we disable anti-aliasing in ambient mode.
         */
        private boolean mLowBitAmbient;

        /*
         * Whether the display supports burn in protection in ambient mode.
         * When true, remove the background in ambient mode.
         */
        private boolean mBurnInProtection;

        /* Maps complication ids to corresponding ComplicationDrawable that renders the
         * the complication data on the watch face.
         */
        private ComplicationDrawable[] mComplicationDrawables;

        // Stores the ranged complication on the edge of the screen
        private ArcComplication[] mRangedComplications;

        /* Maps active complication ids to the data for that complication. Note: Data will only be
         * present if the user has chosen a provider via the settings activity for the watch face.
         */
        private ComplicationData[] mComplicationData;

        private final BroadcastReceiver mTimeZoneReceiver =
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        mCalendar.setTimeZone(TimeZone.getDefault());
                        invalidate();
                    }
                };

        // Handler to update the time once a second in interactive mode.
        private final Handler mUpdateTimeHandler =
                new Handler() {
                    @Override
                    public void handleMessage(Message message) {
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs =
                                    INTERACTIVE_UPDATE_RATE_MS
                                            - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                    }
                };

        private void setTextSizeForWidth(Paint paint, float desiredWidth,
                                                String text) {

            // Pick a reasonably large value for the test. Larger values produce
            // more accurate results, but may cause problems with hardware
            // acceleration. But there are workarounds for that, too; refer to
            // http://stackoverflow.com/questions/6253528/font-size-too-large-to-fit-in-cache
            final float testTextSize = 48f;

            // Get the bounds of the text, using our testTextSize.
            paint.setTextSize(testTextSize);
            Rect bounds = new Rect();
            paint.getTextBounds(text, 0, text.length(), bounds);

            // Calculate the desired size as a proportion of our testTextSize.
            float desiredTextSize = testTextSize * desiredWidth / bounds.width();

            // Set the paint for that size.
            paint.setTextSize(desiredTextSize);
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            context = getApplicationContext();

            setWatchFaceStyle(
                    new WatchFaceStyle.Builder(ComplicationWatchFaceService.this)
                            .setAcceptsTapEvents(true)
                            .build());

            mCalendar = Calendar.getInstance();

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(Color.BLACK);

            mLinePaint = new Paint();
            mLinePaint.setStyle(Paint.Style.STROKE);
            mLinePaint.setStrokeWidth(1f);
            mLinePaint.setColor(Color.WHITE);
            mLinePaint.setAntiAlias(true);

            mCenterPaint = new Paint();
            mCenterPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mCenterPaint.setStrokeWidth(1f);
            mCenterPaint.setTextAlign(Paint.Align.CENTER);
            mCenterPaint.setColor(Color.WHITE);
            mCenterPaint.setAntiAlias(true);

            mBottomPaint = new Paint();
            mBottomPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mBottomPaint.setStrokeWidth(1f);
            mBottomPaint.setTextAlign(Paint.Align.CENTER);
            mBottomPaint.setTextSize(BOTTOM_ROW_ITEM_SIZE);
            mBottomPaint.setColor(Color.WHITE);
            mBottomPaint.setAntiAlias(true);

            initializeComplications();
        }

        private void initializeComplications() {
            mComplicationData = new ComplicationData[COMPLICATION_IDS.length];
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
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);

            // Updates complications to properly render in ambient mode based on the
            // screen's capabilities.
            ComplicationDrawable complicationDrawable;

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationDrawable = mComplicationDrawables[i];

                if(complicationDrawable != null) {
                    complicationDrawable.setLowBitAmbient(mLowBitAmbient);
                    complicationDrawable.setBurnInProtection(mBurnInProtection);
                }
            }
        }

        @Override
        public void onComplicationDataUpdate(
                int complicationId, ComplicationData complicationData) {
            // Adds/updates active complication data in the array.
            mComplicationData[complicationId] = complicationData;

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
                complicationData = mComplicationData[i];

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
                    mComplicationData[complicationId];

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

            mAmbient = inAmbientMode;

            if(mAmbient) {
                mCenterPaint.setStyle(Paint.Style.FILL);
                mCenterPaint.setAntiAlias(false);
                mBottomPaint.setStyle(Paint.Style.FILL);
                mBottomPaint.setAntiAlias(false);

            } else {
                mCenterPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                mCenterPaint.setAntiAlias(true);
                mBottomPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                mBottomPaint.setAntiAlias(true);
            }

            // Update drawable complications' ambient state.
            // Note: ComplicationDrawable handles switching between active/ambient colors, we just
            // have to inform it to enter ambient mode.
            ComplicationDrawable complicationDrawable;

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationDrawable = mComplicationDrawables[i];
                complicationDrawable.setInAmbientMode(mAmbient);
            }

            for (int i = 0; i < mRangedComplications.length; i++) {
                mRangedComplications[i].setAmbientMode(mAmbient);
            }

            // Check and trigger whether or not timer should be running (only in active mode).
            updateTimer();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            /*
             * Find the coordinates of the center point on the screen.
             * Ignore the window insets so that, on round watches
             * with a "chin", the watch face is centered on the entire screen,
             * not just the usable portion.
             */
            mCenterX = width / 2f;
            mCenterY = height / 2f;

            /* We suggest using at least 1/4 of the screen width for circular (or squared)
             * complications and 2/3 of the screen width for wide rectangular complications for
             * better readability.
             */

            int sizeOfComplication = width / 4;
            int midpointOfScreen = width / 2;
            mCenterPaint.setTextSize(width / 8f);

            setTextSizeForWidth(mCenterPaint, width/4, "22:22");

            mBottomPaint.setTextSize(sizeOfComplication / 4f);
            BOTTOM_ROW_ITEM_SIZE = sizeOfComplication / 3;
            mComplicationMargin = sizeOfComplication / 18;

            rangeWidthF = width / 20f;
            int rangeWidth = width / 20;
            float rangeOffsetF = rangeWidthF / 2;
            rangeOffset = rangeWidth / 2;

            rangeBoundsF = new RectF(rangeOffsetF, rangeOffsetF, width - rangeOffsetF, height - rangeOffsetF);

            int radialMarginOffset = (midpointOfScreen - sizeOfComplication) / 2;
            int verticalOffset = midpointOfScreen - (sizeOfComplication / 2);

            rightBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
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

            bottomBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
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

            Rect topRightRangedBounds =
                    // Left, Top, Right, Bottom
                    new Rect((int) rangeBoundsF.centerX() - rangeOffset,
                            0,
                            (int) rangeBoundsF.centerX() + rangeOffset,
                            rangeWidth);

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

            float cos22p5 = 0.924f;
            float sin22p5 = 0.383f;
            float offsetX = mCenterX + (float) width /4;

            //sinus part zeroes out
            //xrot=cos(θ)⋅(x−cx)−sin(θ)⋅(y−cy)+cx
            //cosine part zeroes out
            //yrot=sin(θ)⋅(x−cx)+cos(θ)⋅(y−cy)+cy

            l1StartX = cos22p5 * (offsetX - mCenterX) + mCenterX;
            l1StartY = -sin22p5 * ((float) width - offsetX) + mCenterY;
            l1EndX = cos22p5 * ((float) width - mCenterX) + mCenterX;
            l1EndY = -sin22p5 * ((float) width - mCenterX) + mCenterY;

            l2StartX = -cos22p5 * (offsetX - mCenterX) + mCenterX;
            l2StartY = -sin22p5 * ((float) width - offsetX) + mCenterY;
            l2EndX = -cos22p5 * ((float) width - mCenterX) + mCenterX;
            l2EndY = -sin22p5 * ((float) width - mCenterX) + mCenterY;

            l3StartX = sin22p5 * (offsetX - mCenterX) + mCenterX;
            l3StartY = -cos22p5 * ((float) width - offsetX) + mCenterY;
            l3EndX = sin22p5 * ((float) width - mCenterX) + mCenterX;
            l3EndY = -cos22p5 * ((float) width - mCenterX) + mCenterY;

            l4StartX = -sin22p5 * (offsetX - mCenterX) + mCenterX;
            l4StartY = -cos22p5 * ((float) width - offsetX) + mCenterY;
            l4EndX = -sin22p5 * ((float) width - mCenterX) + mCenterX;
            l4EndY = -cos22p5 * ((float) width - mCenterX) + mCenterY;

            l5StartX = -cos22p5 * (offsetX - mCenterX) + mCenterX;
            l5StartY = sin22p5 * ((float) width - offsetX) + mCenterY;
            l5EndX = -cos22p5 * ((float) width - mCenterX) + mCenterX;
            l5EndY = sin22p5 * ((float) width - mCenterX) + mCenterY;

            l6StartX = cos22p5 * (offsetX - mCenterX) + mCenterX;
            l6StartY = sin22p5 * ((float) width - offsetX) + mCenterY;
            l6EndX = cos22p5 * ((float) width - mCenterX) + mCenterX;
            l6EndY = sin22p5 * ((float) width - mCenterX) + mCenterY;

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
        }

        private int lastMinute;

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            int currentMinute = mCalendar.get(Calendar.MINUTE);

            if(currentMinute != lastMinute) {
                drawBackground(canvas);

                if (mComplicationData[CENTER_COMPLICATION_ID] != null) {
                    if (mComplicationData[CENTER_COMPLICATION_ID].getShortText() != null)
                        canvas.drawText(mComplicationData[CENTER_COMPLICATION_ID].getShortText().getText(getApplicationContext(), now).toString(), mCenterX, mCenterY + mComplicationMargin, mCenterPaint);
                }

                if (!mAmbient) {
                    drawQuadrants(canvas);
                    drawComplications(canvas, now);
                }

                for (int i = 0; i < mRangedComplications.length; i++) {
                    ComplicationData complicationData = mComplicationData[i + RANGED_ID_OFFSET];

                    mRangedComplications[i].draw(canvas, complicationData);
                }
            }
        }

        private void drawQuadrants(Canvas canvas) {
            canvas.drawLine(l1StartX, l1StartY, l1EndX, l1EndY, mLinePaint);
            canvas.drawLine(l2StartX, l2StartY, l2EndX, l2EndY, mLinePaint);
            canvas.drawLine(l3StartX, l3StartY, l3EndX, l3EndY, mLinePaint);
            canvas.drawLine(l4StartX, l4StartY, l4EndX, l4EndY, mLinePaint);
            canvas.drawLine(l5StartX, l5StartY, l5EndX, l5EndY, mLinePaint);
            canvas.drawLine(l6StartX, l6StartY, l6EndX, l6EndY, mLinePaint);
        }

        private void drawComplications(Canvas canvas, long currentTimeMillis) {
            for (int i = 0; i < CENTER_COMPLICATION_ID; i++) {
                drawComplication(canvas, i, currentTimeMillis);
            }
        }

        private void drawComplication(Canvas canvas, int complicationId, long currentTimeMillis) {
            ComplicationDrawable complicationDrawable = mComplicationDrawables[complicationId];
            complicationDrawable.draw(canvas, currentTimeMillis);
        }

        private void drawBackground(Canvas canvas) {
            if (mAmbient && (mLowBitAmbient || mBurnInProtection)) {
                canvas.drawColor(Color.BLUE);
            } else {
                canvas.drawPaint(mBackgroundPaint);
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            /*
             * Whether the timer should be running depends on whether we're visible
             * (as well as whether we're in ambient mode),
             * so we may need to start or stop the timer.
             */
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

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }
    }
}
