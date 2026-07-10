package com.charles.rotationcontrol;

import android.accessibilityservice.AccessibilityService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Surface;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Applies orientation via accessibility overlay (primary) + WRITE_SETTINGS (secondary).
 * Auto (sensor) uses {@link SensorAutoController} to apply the same fixed-orientation
 * path that works for Landscape (FULL_SENSOR is ignored on many tablets).
 * No root.
 */
public final class OrientationApplier {
    private static final String TAG = "OrientationApplier";

    private final Context appContext;
    private final RuleStore store;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean reentrancyGuard = new AtomicBoolean(false);
    private final OrientationOverlay overlay;
    private final SensorAutoController sensorAuto;
    private final AutoLockBubble lockBubble;

    private OrientationMode lastApplied = OrientationMode.SYSTEM;
    private String lastPackage;
    private ContentObserver settingsObserver;
    private int lastSensorActivityOrient = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    private boolean autoOrientationLocked;

    public OrientationApplier(Context context) {
        appContext = context.getApplicationContext();
        store = new RuleStore(appContext);
        if (context instanceof AccessibilityService) {
            AccessibilityService svc = (AccessibilityService) context;
            overlay = new OrientationOverlay(svc);
            lockBubble = new AutoLockBubble(svc, this::onAutoLockToggled);
        } else {
            overlay = null;
            lockBubble = null;
        }
        sensorAuto = new SensorAutoController(appContext, this::onSensorOrientation);
    }

    public boolean canWrite() {
        return Settings.System.canWrite(appContext);
    }

    public OrientationMode getLastApplied() {
        return lastApplied;
    }

    public String getLastPackage() {
        return lastPackage;
    }

    public void startWatching() {
        if (settingsObserver != null) {
            return;
        }
        settingsObserver = new ContentObserver(mainHandler) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                if (reentrancyGuard.get()) {
                    return;
                }
                if (!store.isMasterEnabled()) {
                    return;
                }
                if (lastApplied == OrientationMode.SYSTEM || lastPackage == null) {
                    return;
                }
                // Auto is driven by the sensor controller + fixed locks; ignore
                // USER_ROTATION chatter. Re-assert only if our lock was cleared.
                if (lastApplied.enablesSensor()) {
                    if (canWrite() && isAutoRotateOn()) {
                        // Something turned auto-rotate on; re-lock to sensor bucket.
                        mainHandler.post(() -> applyForPackage(lastPackage, true));
                    }
                    return;
                }
                if (matchesCurrent(lastApplied)) {
                    return;
                }
                Log.i(TAG, "Settings changed under locked rule; re-applying "
                        + lastApplied + " for " + lastPackage);
                mainHandler.post(() -> applyForPackage(lastPackage, true));
            }
        };
        ContentResolver cr = appContext.getContentResolver();
        cr.registerContentObserver(
                Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION),
                false,
                settingsObserver
        );
        cr.registerContentObserver(
                Settings.System.getUriFor(Settings.System.USER_ROTATION),
                false,
                settingsObserver
        );
    }

    public void stopWatching() {
        if (settingsObserver != null) {
            appContext.getContentResolver().unregisterContentObserver(settingsObserver);
            settingsObserver = null;
        }
        sensorAuto.stop();
        autoOrientationLocked = false;
        if (lockBubble != null) {
            lockBubble.remove();
        }
        if (overlay != null) {
            overlay.remove();
        }
    }

    public void applyForPackage(String packageName) {
        applyForPackage(packageName, false);
    }

    public void applyForPackage(String packageName, boolean force) {
        if (!store.isMasterEnabled()) {
            return;
        }
        if (packageName == null) {
            return;
        }

        OrientationMode mode = store.getMode(packageName);
        if (!force && packageName.equals(lastPackage) && mode == lastApplied) {
            if (mode == OrientationMode.SYSTEM || matchesCurrent(mode)) {
                // Keep sensor listener alive while still on AUTO (unless frozen).
                if (mode.enablesSensor()
                        && !autoOrientationLocked
                        && !sensorAuto.isEnabled()) {
                    sensorAuto.start();
                }
                return;
            }
            Log.i(TAG, "Mismatch detected; re-applying " + mode + " for " + packageName);
        }

        try {
            reentrancyGuard.set(true);
            if (mode == OrientationMode.SYSTEM) {
                sensorAuto.stop();
                autoOrientationLocked = false;
                lastSensorActivityOrient = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
                if (lockBubble != null) {
                    lockBubble.remove();
                }
                if (overlay != null) {
                    overlay.remove();
                }
                restoreSnapshotIfNeeded();
                lastApplied = OrientationMode.SYSTEM;
                lastPackage = packageName;
                Log.i(TAG, "Restored system rotation for " + packageName
                        + " (overlay off, write=" + canWrite() + ")");
                return;
            }

            ensureSnapshot();

            if (mode.enablesSensor()) {
                // Auto: same fixed-lock mechanism as Landscape, driven by tilt.
                boolean packageChanged = !packageName.equals(lastPackage)
                        || lastApplied != OrientationMode.AUTO;
                lastApplied = OrientationMode.AUTO;
                lastPackage = packageName;
                if (packageChanged) {
                    // Fresh app entry: unlock auto-follow so tilt works again.
                    autoOrientationLocked = false;
                    if (lockBubble != null) {
                        lockBubble.setLocked(false);
                    }
                }
                if (!autoOrientationLocked) {
                    sensorAuto.start();
                } else {
                    sensorAuto.stop();
                }
                // Immediate apply from current display rotation so we don't wait for tilt.
                applyFixedLock(guessActivityFromCurrentUserRotation(), currentUserRotation());
                if (lockBubble != null) {
                    lockBubble.flash();
                }
                Log.i(TAG, "Applied AUTO (sensor-driven) for " + packageName
                        + " locked=" + autoOrientationLocked
                        + " write=" + canWrite() + " overlay=" + (overlay != null));
            } else {
                sensorAuto.stop();
                autoOrientationLocked = false;
                lastSensorActivityOrient = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
                if (lockBubble != null) {
                    lockBubble.remove();
                }
                int activityOrient = mode.activityInfoOrientation();
                Integer userRot = mode.userRotationOrNull();
                applyFixedLock(activityOrient, userRot != null ? userRot : Surface.ROTATION_0);
                lastApplied = mode;
                lastPackage = packageName;
                Log.i(TAG, "Applied " + mode + " for " + packageName
                        + " (auto=" + (isAutoRotateOn() ? 1 : 0)
                        + " user=" + currentUserRotation()
                        + " write=" + canWrite()
                        + " overlay=" + (overlay != null) + ")");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to apply orientation", e);
        } finally {
            mainHandler.postDelayed(() -> reentrancyGuard.set(false), 400);
        }
    }

    public void forceRestore() {
        try {
            reentrancyGuard.set(true);
            sensorAuto.stop();
            autoOrientationLocked = false;
            lastSensorActivityOrient = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
            if (lockBubble != null) {
                lockBubble.remove();
            }
            if (overlay != null) {
                overlay.remove();
            }
            restoreSnapshotIfNeeded();
            lastApplied = OrientationMode.SYSTEM;
            lastPackage = null;
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to restore settings", e);
        } finally {
            mainHandler.postDelayed(() -> reentrancyGuard.set(false), 400);
        }
    }

    private void onAutoLockToggled(boolean locked) {
        autoOrientationLocked = locked;
        if (lastApplied != OrientationMode.AUTO) {
            return;
        }
        if (locked) {
            sensorAuto.stop();
            Log.i(TAG, "Auto tilt frozen at orient=" + lastSensorActivityOrient);
        } else {
            sensorAuto.start();
            Log.i(TAG, "Auto tilt resumed");
        }
    }

    private void onSensorOrientation(int activityInfoOrientation, int userRotation) {
        if (lastApplied != OrientationMode.AUTO) {
            return;
        }
        if (autoOrientationLocked) {
            return;
        }
        if (!store.isMasterEnabled()) {
            return;
        }
        if (activityInfoOrientation == lastSensorActivityOrient) {
            return;
        }
        try {
            reentrancyGuard.set(true);
            applyFixedLock(activityInfoOrientation, userRotation);
            lastSensorActivityOrient = activityInfoOrientation;
            // Peek the lock control briefly after a re-orient.
            if (lockBubble != null) {
                lockBubble.flash();
            }
        } finally {
            mainHandler.postDelayed(() -> reentrancyGuard.set(false), 300);
        }
    }

    /**
     * Proven path (Hearthstone Landscape): fixed overlay orientation +
     * accelerometer off + USER_ROTATION pin.
     */
    private void applyFixedLock(int activityInfoOrientation, int userRotation) {
        if (overlay != null) {
            overlay.apply(activityInfoOrientation);
        }
        if (canWrite()) {
            putInt(Settings.System.ACCELEROMETER_ROTATION, 0);
            putInt(Settings.System.USER_ROTATION, userRotation);
        }
        lastSensorActivityOrient = activityInfoOrientation;
    }

    private int guessActivityFromCurrentUserRotation() {
        int user = currentUserRotation();
        switch (user) {
            case Surface.ROTATION_90:
                return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            case Surface.ROTATION_180:
                return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
            case Surface.ROTATION_270:
                return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
            case Surface.ROTATION_0:
            default:
                return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        }
    }

    private boolean matchesCurrent(OrientationMode mode) {
        if (mode.enablesSensor()) {
            // Sensor-driven: consider matched if auto is off (we lock) and overlay exists.
            return !isAutoRotateOn() && overlay != null;
        }
        Integer wanted = mode.userRotationOrNull();
        if (wanted == null) {
            return true;
        }
        if (overlay != null && !isAutoRotateOn() && currentUserRotation() == wanted) {
            return true;
        }
        if (overlay != null) {
            // Overlay present; avoid thrash on hold ticks even if USER_ROTATION lags.
            return true;
        }
        return !isAutoRotateOn() && currentUserRotation() == wanted;
    }

    private boolean isAutoRotateOn() {
        return Settings.System.getInt(
                appContext.getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION,
                1
        ) == 1;
    }

    private int currentUserRotation() {
        return Settings.System.getInt(
                appContext.getContentResolver(),
                Settings.System.USER_ROTATION,
                Surface.ROTATION_0
        );
    }

    private void ensureSnapshot() {
        if (store.hasGlobalSnapshot()) {
            return;
        }
        if (!canWrite()) {
            store.saveGlobalSnapshot(true, Surface.ROTATION_0);
            return;
        }
        store.saveGlobalSnapshot(isAutoRotateOn(), currentUserRotation());
    }

    private void restoreSnapshotIfNeeded() {
        if (!store.hasGlobalSnapshot()) {
            return;
        }
        boolean auto = store.getSavedGlobalAuto();
        int user = store.getSavedGlobalUser();
        if (canWrite()) {
            putInt(Settings.System.ACCELEROMETER_ROTATION, auto ? 1 : 0);
            putInt(Settings.System.USER_ROTATION, user);
        }
        store.clearGlobalSnapshot();
        Log.i(TAG, "Restored global rotation snapshot (auto=" + auto + " user=" + user + ")");
    }

    private void putInt(String key, int value) {
        boolean ok = Settings.System.putInt(appContext.getContentResolver(), key, value);
        if (!ok) {
            Log.w(TAG, "Settings.System.putInt failed for " + key + "=" + value);
        }
    }
}
