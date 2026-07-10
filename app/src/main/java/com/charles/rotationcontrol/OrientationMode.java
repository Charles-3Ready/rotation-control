package com.charles.rotationcontrol;

import android.content.pm.ActivityInfo;
import android.view.Surface;

/**
 * Per-app orientation rule.
 * SYSTEM leaves device settings alone (restores snapshot taken before override).
 */
public enum OrientationMode {
    SYSTEM("System default"),
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape"),
    REVERSE_PORTRAIT("Reverse portrait"),
    REVERSE_LANDSCAPE("Reverse landscape"),
    AUTO("Auto (sensor)");

    public final String label;

    OrientationMode(String label) {
        this.label = label;
    }

    public static OrientationMode fromKey(String key) {
        if (key == null || key.isEmpty()) {
            return SYSTEM;
        }
        try {
            return OrientationMode.valueOf(key);
        } catch (IllegalArgumentException e) {
            return SYSTEM;
        }
    }

    /** User rotation constant for locked modes; ignored for SYSTEM / AUTO. */
    public Integer userRotationOrNull() {
        switch (this) {
            case PORTRAIT:
                return Surface.ROTATION_0;
            case LANDSCAPE:
                return Surface.ROTATION_90;
            case REVERSE_PORTRAIT:
                return Surface.ROTATION_180;
            case REVERSE_LANDSCAPE:
                return Surface.ROTATION_270;
            default:
                return null;
        }
    }

    /**
     * ActivityInfo orientation for the accessibility overlay window.
     * {@link ActivityInfo#SCREEN_ORIENTATION_UNSPECIFIED} means remove overlay.
     */
    public int activityInfoOrientation() {
        switch (this) {
            case PORTRAIT:
                return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            case LANDSCAPE:
                return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            case REVERSE_PORTRAIT:
                return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
            case REVERSE_LANDSCAPE:
                return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
            case AUTO:
                // Not used for apply — Auto is sensor-driven fixed locks.
                // Kept as FULL_SENSOR only as a label fallback.
                return ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR;
            case SYSTEM:
            default:
                return ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        }
    }

    public boolean locksRotation() {
        return userRotationOrNull() != null;
    }

    public boolean enablesSensor() {
        return this == AUTO;
    }
}
