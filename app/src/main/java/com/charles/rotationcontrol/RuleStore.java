package com.charles.rotationcontrol;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Package name → {@link OrientationMode}. SYSTEM is not stored (absence = default).
 */
public final class RuleStore {
    private static final String PREFS = "rotation_rules";
    private static final String KEY_MASTER = "master_enabled";
    private static final String KEY_ONBOARDING_DONE = "onboarding_done_v2";
    private static final String KEY_GLOBAL_AUTO = "saved_global_auto";
    private static final String KEY_GLOBAL_USER = "saved_global_user";
    private static final String KEY_HAS_SNAPSHOT = "has_global_snapshot";
    /** Legacy key from removed Allow rotation toggle — ignored / cleaned on touch. */
    private static final String KEY_ALLOW_ROTATION_LEGACY = "allow_rotation";

    private final SharedPreferences prefs;

    public RuleStore(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (prefs.contains(KEY_ALLOW_ROTATION_LEGACY)) {
            prefs.edit().remove(KEY_ALLOW_ROTATION_LEGACY).apply();
        }
    }

    public boolean isMasterEnabled() {
        return prefs.getBoolean(KEY_MASTER, true);
    }

    public void setMasterEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_MASTER, enabled).apply();
    }

    public boolean isOnboardingDone() {
        return prefs.getBoolean(KEY_ONBOARDING_DONE, false);
    }

    public void setOnboardingDone(boolean done) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, done).apply();
    }

    public OrientationMode getMode(String packageName) {
        if (packageName == null) {
            return OrientationMode.SYSTEM;
        }
        return OrientationMode.fromKey(prefs.getString(packageName, null));
    }

    public void setMode(String packageName, OrientationMode mode) {
        if (packageName == null) {
            return;
        }
        SharedPreferences.Editor editor = prefs.edit();
        if (mode == null || mode == OrientationMode.SYSTEM) {
            editor.remove(packageName);
        } else {
            editor.putString(packageName, mode.name());
        }
        editor.apply();
    }

    public Map<String, OrientationMode> allRules() {
        Map<String, OrientationMode> map = new HashMap<>();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            String key = entry.getKey();
            if (isMetaKey(key)) {
                continue;
            }
            Object value = entry.getValue();
            if (value instanceof String) {
                OrientationMode mode = OrientationMode.fromKey((String) value);
                if (mode != OrientationMode.SYSTEM) {
                    map.put(key, mode);
                }
            }
        }
        return map;
    }

    public int ruleCount() {
        return allRules().size();
    }

    public Set<String> packagesWithRules() {
        return Collections.unmodifiableSet(allRules().keySet());
    }

    public void saveGlobalSnapshot(boolean autoRotateOn, int userRotation) {
        prefs.edit()
                .putBoolean(KEY_HAS_SNAPSHOT, true)
                .putBoolean(KEY_GLOBAL_AUTO, autoRotateOn)
                .putInt(KEY_GLOBAL_USER, userRotation)
                .apply();
    }

    public boolean hasGlobalSnapshot() {
        return prefs.getBoolean(KEY_HAS_SNAPSHOT, false);
    }

    public boolean getSavedGlobalAuto() {
        return prefs.getBoolean(KEY_GLOBAL_AUTO, true);
    }

    public int getSavedGlobalUser() {
        return prefs.getInt(KEY_GLOBAL_USER, 0);
    }

    public void clearGlobalSnapshot() {
        prefs.edit()
                .remove(KEY_HAS_SNAPSHOT)
                .remove(KEY_GLOBAL_AUTO)
                .remove(KEY_GLOBAL_USER)
                .apply();
    }

    private static boolean isMetaKey(String key) {
        return KEY_MASTER.equals(key)
                || KEY_ONBOARDING_DONE.equals(key)
                || KEY_ALLOW_ROTATION_LEGACY.equals(key)
                || KEY_GLOBAL_AUTO.equals(key)
                || KEY_GLOBAL_USER.equals(key)
                || KEY_HAS_SNAPSHOT.equals(key);
    }
}
