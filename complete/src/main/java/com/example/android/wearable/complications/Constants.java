package com.example.android.wearable.complications;

public class Constants {
    public static final int RIGHT_COMPLICATION_ID = 0;
    public static final int TOP_RIGHT_COMPLICATION_ID = 1;
    public static final int TOP_COMPLICATION_ID = 2;
    public static final int TOP_LEFT_COMPLICATION_ID = 3;
    public static final int LEFT_COMPLICATION_ID = 4;
    public static final int BOTTOM_COMPLICATION_ID = 5;
    public static final int CENTER_COMPLICATION_ID = 6;
    public static final int TOP_RIGHT_RANGED_COMPLICATION_ID = 7;
    public static final int BOTTOM_RIGHT_RANGED_COMPLICATION_ID = 8;
    public static final int BOTTOM_LEFT_RANGED_COMPLICATION_ID = 9;
    public static final int TOP_LEFT_RANGED_COMPLICATION_ID = 10;

    // Ranged complication IDs start from 7. Used for arrays
    public static int RANGED_ID_OFFSET = 7;
    public static int RANGE_COMPLICATION_COUNT = 4;

    public static final int[] COMPLICATION_IDS = {
            RIGHT_COMPLICATION_ID,
            TOP_RIGHT_COMPLICATION_ID,
            TOP_COMPLICATION_ID,
            TOP_LEFT_COMPLICATION_ID,
            LEFT_COMPLICATION_ID,
            BOTTOM_COMPLICATION_ID,
            CENTER_COMPLICATION_ID,
            TOP_RIGHT_RANGED_COMPLICATION_ID,
            BOTTOM_RIGHT_RANGED_COMPLICATION_ID,
            BOTTOM_LEFT_RANGED_COMPLICATION_ID,
            TOP_LEFT_RANGED_COMPLICATION_ID
    };
}
