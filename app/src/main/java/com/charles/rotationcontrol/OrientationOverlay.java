package com.charles.rotationcontrol;

import android.accessibilityservice.AccessibilityService;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

/**
 * Invisible 1×1 overlay whose {@link WindowManager.LayoutParams#screenOrientation}
 * forces display orientation. Works without root via
 * {@link WindowManager.LayoutParams#TYPE_ACCESSIBILITY_OVERLAY} (no extra permission).
 * This is what makes Auto / Landscape work when apps lock their own orientation.
 */
public final class OrientationOverlay {
    private static final String TAG = "OrientationOverlay";

    private final AccessibilityService service;
    private final WindowManager windowManager;
    private View overlayView;
    private int currentOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;

    public OrientationOverlay(AccessibilityService service) {
        this.service = service;
        this.windowManager = (WindowManager) service.getSystemService(AccessibilityService.WINDOW_SERVICE);
    }

    public synchronized void apply(int screenOrientation) {
        if (windowManager == null) {
            Log.w(TAG, "No WindowManager");
            return;
        }
        if (screenOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            remove();
            return;
        }
        if (overlayView != null && currentOrientation == screenOrientation) {
            return;
        }
        try {
            if (overlayView == null) {
                overlayView = new View(service);
                overlayView.setBackgroundColor(0x00000000);
                windowManager.addView(overlayView, buildParams(screenOrientation));
            } else {
                WindowManager.LayoutParams lp =
                        (WindowManager.LayoutParams) overlayView.getLayoutParams();
                lp.screenOrientation = screenOrientation;
                windowManager.updateViewLayout(overlayView, lp);
            }
            currentOrientation = screenOrientation;
            Log.i(TAG, "Overlay orientation → " + screenOrientation);
        } catch (Exception e) {
            Log.e(TAG, "Failed to apply overlay orientation " + screenOrientation, e);
            // Stale view after service restart — clear and retry once.
            try {
                if (overlayView != null) {
                    windowManager.removeViewImmediate(overlayView);
                }
            } catch (Exception ignored) {
            }
            overlayView = null;
            currentOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
            try {
                overlayView = new View(service);
                overlayView.setBackgroundColor(0x00000000);
                windowManager.addView(overlayView, buildParams(screenOrientation));
                currentOrientation = screenOrientation;
                Log.i(TAG, "Overlay orientation (retry) → " + screenOrientation);
            } catch (Exception e2) {
                Log.e(TAG, "Overlay retry failed", e2);
                overlayView = null;
            }
        }
    }

    public synchronized void remove() {
        if (overlayView == null || windowManager == null) {
            currentOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
            return;
        }
        try {
            windowManager.removeViewImmediate(overlayView);
            Log.i(TAG, "Overlay removed");
        } catch (Exception e) {
            Log.w(TAG, "removeView failed", e);
        }
        overlayView = null;
        currentOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    }

    private static WindowManager.LayoutParams buildParams(int screenOrientation) {
        int type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
        // NOT_FOCUSABLE is critical: if the overlay steals focus, a11y reports our
        // package as foreground and we immediately restore (cancelling Auto).
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            flags |= WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR;
        }
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                1,
                1,
                type,
                flags,
                PixelFormat.TRANSLUCENT
        );
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = 0;
        lp.y = 0;
        lp.screenOrientation = screenOrientation;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        return lp;
    }
}
