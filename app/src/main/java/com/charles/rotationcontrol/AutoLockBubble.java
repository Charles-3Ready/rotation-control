package com.charles.rotationcontrol;

import android.accessibilityservice.AccessibilityService;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * Temporary floating lock control while Auto (sensor) is active.
 * Appears briefly, then disappears. Tap toggles freeze of current orientation.
 */
public final class AutoLockBubble {
    private static final String TAG = "AutoLockBubble";
    private static final long HIDE_AFTER_MS = 2800L;

    public interface Callback {
        void onLockToggled(boolean locked);
    }

    private final AccessibilityService service;
    private final WindowManager windowManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Callback callback;

    private FrameLayout root;
    private ImageView icon;
    private boolean locked;
    private boolean attached;
    private final Runnable hideRunnable = this::hide;

    public AutoLockBubble(AccessibilityService service, Callback callback) {
        this.service = service;
        this.callback = callback;
        this.windowManager = (WindowManager) service.getSystemService(AccessibilityService.WINDOW_SERVICE);
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
        if (icon != null) {
            icon.setImageResource(locked ? R.drawable.ic_lock_closed : R.drawable.ic_lock_open);
            icon.setContentDescription(locked
                    ? service.getString(R.string.auto_lock_unlock)
                    : service.getString(R.string.auto_lock_lock));
        }
    }

    /** Show bubble for a few seconds, then fade/remove. */
    public void flash() {
        mainHandler.post(() -> {
            ensureAttached();
            if (root == null) {
                return;
            }
            root.setAlpha(1f);
            root.setVisibility(View.VISIBLE);
            setLocked(locked);
            mainHandler.removeCallbacks(hideRunnable);
            mainHandler.postDelayed(hideRunnable, HIDE_AFTER_MS);
        });
    }

    public void hide() {
        mainHandler.removeCallbacks(hideRunnable);
        if (root != null) {
            root.animate()
                    .alpha(0f)
                    .setDuration(220)
                    .withEndAction(() -> {
                        if (root != null) {
                            root.setVisibility(View.GONE);
                            root.setAlpha(1f);
                        }
                    })
                    .start();
        }
    }

    public void remove() {
        mainHandler.removeCallbacks(hideRunnable);
        locked = false;
        if (!attached || root == null || windowManager == null) {
            return;
        }
        try {
            windowManager.removeViewImmediate(root);
        } catch (Exception e) {
            Log.w(TAG, "remove failed", e);
        }
        root = null;
        icon = null;
        attached = false;
    }

    private void ensureAttached() {
        if (attached || windowManager == null) {
            return;
        }
        int size = dp(48);
        int margin = dp(20);

        root = new FrameLayout(service);
        root.setBackgroundResource(R.drawable.bg_lock_bubble);
        root.setClickable(true);
        root.setFocusable(false);
        root.setElevation(dp(6));

        icon = new ImageView(service);
        icon.setImageResource(R.drawable.ic_lock_open);
        int pad = dp(12);
        icon.setPadding(pad, pad, pad, pad);
        root.addView(icon, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        root.setOnClickListener(v -> {
            locked = !locked;
            setLocked(locked);
            Log.i(TAG, "Auto orientation " + (locked ? "LOCKED" : "unlocked"));
            if (callback != null) {
                callback.onLockToggled(locked);
            }
            // Brief linger so user sees the new icon, then disappear.
            mainHandler.removeCallbacks(hideRunnable);
            mainHandler.postDelayed(hideRunnable, 1200L);
        });

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                size,
                size,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        lp.gravity = Gravity.BOTTOM | Gravity.END;
        lp.x = margin;
        lp.y = margin + dp(48);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        // Unspecified orientation — force-orientation is the other 1×1 overlay.
        lp.screenOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;

        try {
            windowManager.addView(root, lp);
            attached = true;
        } catch (Exception e) {
            Log.e(TAG, "addView failed", e);
            root = null;
            icon = null;
            attached = false;
        }
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                service.getResources().getDisplayMetrics()
        ));
    }
}
