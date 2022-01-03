package com.example.android.wearable.complications;

import static com.example.android.wearable.complications.Constants.*;

import android.os.Handler;
import android.os.Message;

public class UpdateHandler extends Handler {
    private final ComplicationWatchFaceService.Engine engine;

    public UpdateHandler(ComplicationWatchFaceService.Engine engine) {
        this.engine = engine;
    }

    @Override
    public void handleMessage(Message message) {
        engine.invalidate();
        if (engine.shouldTimerBeRunning()) {
            long timeMs = System.currentTimeMillis();
            long delayMs =
                    INTERACTIVE_UPDATE_RATE_MS
                            - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
            this.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
        }
    }
}
