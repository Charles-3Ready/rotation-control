package com.charles.rotationcontrol;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.List;

/**
 * Applies orientation only while a ruled app is in the foreground.
 * Leaving to home / another default app restores the previous system rotation
 * so landscape does not stick to the whole tablet.
 */
public class RotationAccessibilityService extends AccessibilityService {
    private static final String TAG = "RotationA11y";
    private static final long DEBOUNCE_MS = 280L;
    private static final long HOLD_INTERVAL_MS = 1500L;

    private static volatile RotationAccessibilityService instance;

    private OrientationApplier applier;
    private final Handler handler = new Handler(Looper.getMainLooper());
    /** Last package we actually applied a rule for (real app or home). */
    private String lastTargetPackage;
    private String pendingPackage;
    private final Runnable debounceApply = this::flushPendingPackage;
    private final Runnable holdEnforce = this::enforceHold;

    public static boolean isRunning() {
        return instance != null;
    }

    public static RotationAccessibilityService getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        applier = new OrientationApplier(this);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) {
            info = new AccessibilityServiceInfo();
        }
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                | AccessibilityEvent.TYPE_WINDOWS_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                | AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                | AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        info.notificationTimeout = 50;
        setServiceInfo(info);
        applier.startWatching();
        Log.i(TAG, "Service connected");
        applyCurrentForeground();
        handler.removeCallbacks(holdEnforce);
        handler.postDelayed(holdEnforce, HOLD_INTERVAL_MS);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) {
            return;
        }
        int type = event.getEventType();
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && type != AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            return;
        }

        String eventPkg = null;
        CharSequence cs = event.getPackageName();
        if (cs != null) {
            eventPkg = cs.toString();
        }

        // Our own package fires WINDOW events when the force-orientation overlay is
        // added/updated. Never trust eventPkg == self — always re-resolve windows.
        if (PackageFilter.isSelf(eventPkg) || PackageFilter.isTransientOverlay(eventPkg)) {
            String under = resolveTargetPackage();
            if (under != null) {
                scheduleApply(under);
            }
            return;
        }

        // Prefer full window resolve over event package (more accurate on multi-window).
        String pkg = resolveTargetPackage();
        if (pkg == null) {
            pkg = eventPkg;
        }
        if (pkg == null || PackageFilter.isTransientOverlay(pkg)) {
            return;
        }
        // Self as sole target only when user is actually in our UI (no other app).
        scheduleApply(pkg);
    }

    @Override
    public void onInterrupt() {
        // no-op
    }

    @Override
    public boolean onUnbind(Intent intent) {
        instance = null;
        handler.removeCallbacks(debounceApply);
        handler.removeCallbacks(holdEnforce);
        if (applier != null) {
            applier.stopWatching();
            applier.forceRestore();
        }
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        instance = null;
        handler.removeCallbacks(debounceApply);
        handler.removeCallbacks(holdEnforce);
        if (applier != null) {
            applier.stopWatching();
        }
        super.onDestroy();
    }

    public void applyCurrentForeground() {
        String pkg = resolveTargetPackage();
        if (pkg == null || PackageFilter.isTransientOverlay(pkg)) {
            return;
        }
        scheduleApply(pkg);
    }

    /** Pause rules and clear overlay + restore system rotation. */
    public void forceRestore() {
        if (applier != null) {
            applier.forceRestore();
        }
    }

    private void scheduleApply(String packageName) {
        if (packageName == null) {
            return;
        }
        pendingPackage = packageName;
        handler.removeCallbacks(debounceApply);
        handler.postDelayed(debounceApply, DEBOUNCE_MS);
    }

    private void flushPendingPackage() {
        String pkg = pendingPackage;
        if (pkg == null || PackageFilter.isTransientOverlay(pkg)) {
            return;
        }

        // If we have an active forced app and the pending target is our own UI,
        // keep the force — overlay addView falsely reports our package as foreground.
        if (PackageFilter.isSelf(pkg)
                && lastTargetPackage != null
                && !PackageFilter.isSelf(lastTargetPackage)
                && !PackageFilter.isHome(lastTargetPackage)
                && applier != null
                && applier.getLastApplied() != null
                && applier.getLastApplied() != OrientationMode.SYSTEM) {
            Log.i(TAG, "Ignoring self foreground while holding rule for " + lastTargetPackage);
            return;
        }

        if (pkg.equals(lastTargetPackage)) {
            applier.applyForPackage(pkg, false);
            return;
        }
        lastTargetPackage = pkg;
        Log.i(TAG, "Foreground target: " + pkg
                + (PackageFilter.isHome(pkg) ? " (home → restore)" : "")
                + (PackageFilter.isSelf(pkg) ? " (self → restore)" : ""));
        applier.applyForPackage(pkg, true);
    }

    /**
     * Only re-assert while a non-default rule is active and that app is still target.
     * Re-resolves foreground so we do not keep landscaping the launcher.
     */
    private void enforceHold() {
        try {
            String current = resolveTargetPackage();
            if (current == null
                    || PackageFilter.isTransientOverlay(current)
                    || PackageFilter.isSelf(current)) {
                // Overlay noise / self UI — re-hold previous forced app if any.
                if (lastTargetPackage != null
                        && !PackageFilter.isHome(lastTargetPackage)
                        && !PackageFilter.isSelf(lastTargetPackage)
                        && !PackageFilter.isTransientOverlay(lastTargetPackage)) {
                    applier.applyForPackage(lastTargetPackage, false);
                }
                return;
            }
            if (!current.equals(lastTargetPackage)) {
                lastTargetPackage = current;
                Log.i(TAG, "Hold saw switch → " + current);
            }
            applier.applyForPackage(current, false);
        } finally {
            handler.postDelayed(holdEnforce, HOLD_INTERVAL_MS);
        }
    }

    /**
     * Best package to control rotation for: focused real app, else home.
     * Never returns our own package if any other real app window is present
     * (accessibility overlay windows report our package and would cancel rules).
     */
    private String resolveTargetPackage() {
        try {
            List<AccessibilityWindowInfo> windows = getWindows();
            if (windows == null) {
                return null;
            }
            String focusedApp = null;
            String activeApp = null;
            String anyApp = null;
            String home = null;
            String self = null;
            for (AccessibilityWindowInfo window : windows) {
                if (window == null) {
                    continue;
                }
                int wtype = window.getType();
                // Skip pure system / accessibility chrome; keep APPLICATION.
                if (wtype != AccessibilityWindowInfo.TYPE_APPLICATION) {
                    continue;
                }
                android.view.accessibility.AccessibilityNodeInfo root = window.getRoot();
                if (root == null) {
                    continue;
                }
                CharSequence pkgCs = root.getPackageName();
                root.recycle();
                if (pkgCs == null) {
                    continue;
                }
                String pkg = pkgCs.toString();
                if (PackageFilter.isTransientOverlay(pkg)) {
                    continue;
                }
                if (PackageFilter.isSelf(pkg)) {
                    // Remember only if user is actually in our activity UI.
                    if (window.isFocused() || window.isActive()) {
                        self = pkg;
                    }
                    continue;
                }
                if (PackageFilter.isHome(pkg)) {
                    if (window.isFocused() || window.isActive()) {
                        home = pkg;
                    } else if (home == null) {
                        home = pkg;
                    }
                    continue;
                }
                if (anyApp == null) {
                    anyApp = pkg;
                }
                if (window.isFocused()) {
                    focusedApp = pkg;
                }
                if (window.isActive()) {
                    activeApp = pkg;
                }
            }
            if (focusedApp != null) {
                return focusedApp;
            }
            if (activeApp != null) {
                return activeApp;
            }
            if (anyApp != null) {
                return anyApp;
            }
            // Only treat self as foreground when no other app window exists.
            if (self != null) {
                return self;
            }
            return home;
        } catch (Exception e) {
            Log.w(TAG, "resolveTargetPackage failed", e);
        }
        return null;
    }
}
