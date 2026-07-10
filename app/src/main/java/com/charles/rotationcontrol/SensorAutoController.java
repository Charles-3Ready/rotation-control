package com.charles.rotationcontrol;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;

/**
 * Implements Auto (sensor) by mapping the physical device tilt to a fixed
 * overlay orientation. Many tablets ignore {@code SCREEN_ORIENTATION_FULL_SENSOR}
 * on accessibility overlays, but fixed Landscape/Portrait works (e.g. Hearthstone).
 */
public final class SensorAutoController {
    private static final String TAG = "SensorAuto";
    private static final int HYSTERESIS_DEG = 25;
    private static final long MIN_SWITCH_MS = 350L;

    public interface Listener {
        void onSensorOrientation(int activityInfoOrientation, int userRotation);
    }

    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Listener listener;
    private OrientationEventListener eventListener;
    private boolean enabled;
    private int lastBucket = -1;
    private long lastSwitchAt;

    public SensorAutoController(Context context, Listener listener) {
        this.appContext = context.getApplicationContext();
        this.listener = listener;
    }

    public void start() {
        if (enabled) {
            return;
        }
        if (eventListener == null) {
            eventListener = new OrientationEventListener(appContext) {
                @Override
                public void onOrientationChanged(int orientation) {
                    if (!enabled || orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                        return;
                    }
                    handleDegrees(orientation);
                }
            };
        }
        if (eventListener.canDetectOrientation()) {
            eventListener.enable();
            enabled = true;
            lastBucket = -1;
            Log.i(TAG, "Sensor auto started");
        } else {
            Log.w(TAG, "Device cannot detect orientation — Auto will not track tilt");
        }
    }

    public void stop() {
        enabled = false;
        if (eventListener != null) {
            eventListener.disable();
        }
        lastBucket = -1;
        Log.i(TAG, "Sensor auto stopped");
    }

    public boolean isEnabled() {
        return enabled;
    }

    private void handleDegrees(int degrees) {
        int bucket = bucketFor(degrees, lastBucket);
        if (bucket < 0 || bucket == lastBucket) {
            return;
        }
        long now = System.currentTimeMillis();
        if (lastBucket >= 0 && (now - lastSwitchAt) < MIN_SWITCH_MS) {
            return;
        }
        lastBucket = bucket;
        lastSwitchAt = now;
        final int activityOrient = activityInfoForBucket(bucket);
        final int userRot = userRotationForBucket(bucket);
        mainHandler.post(() -> {
            if (!enabled) {
                return;
            }
            Log.i(TAG, "Tilt → bucket=" + bucket + " orient=" + activityOrient
                    + " userRot=" + userRot + " (deg=" + degrees + ")");
            listener.onSensorOrientation(activityOrient, userRot);
        });
    }

    /**
     * Four buckets with hysteresis so it does not thrash near 45° boundaries.
     * 0=portrait, 1=landscape (90), 2=reverse portrait, 3=reverse landscape (270).
     */
    static int bucketFor(int degrees, int lastBucket) {
        int d = ((degrees % 360) + 360) % 360;
        if (lastBucket >= 0) {
            int center = lastBucket * 90;
            int delta = Math.abs(d - center);
            if (delta > 180) {
                delta = 360 - delta;
            }
            // Stay in current bucket until well past the edge.
            if (delta < (45 + HYSTERESIS_DEG)) {
                return lastBucket;
            }
        }
        if (d >= 315 || d < 45) {
            return 0;
        }
        if (d < 135) {
            return 1;
        }
        if (d < 225) {
            return 2;
        }
        return 3;
    }

    /**
     * iPlay / many tablets report sensor degrees opposite to phone convention —
     * landscape buckets are swapped so tilt direction matches the screen.
     */
    static int activityInfoForBucket(int bucket) {
        switch (bucket) {
            case 1:
                return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
            case 2:
                return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
            case 3:
                return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            case 0:
            default:
                return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        }
    }

    static int userRotationForBucket(int bucket) {
        switch (bucket) {
            case 1:
                return Surface.ROTATION_270;
            case 2:
                return Surface.ROTATION_180;
            case 3:
                return Surface.ROTATION_90;
            case 0:
            default:
                return Surface.ROTATION_0;
        }
    }
}
