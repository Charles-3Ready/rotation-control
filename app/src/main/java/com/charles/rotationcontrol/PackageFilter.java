package com.charles.rotationcontrol;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Classifies packages for orientation control.
 *
 * <ul>
 *   <li><b>Transient overlays</b> (Game Space, SystemUI, IME): ignore — do not unlock
 *       while a game is still underneath.</li>
 *   <li><b>Home / desktop</b> (launchers): leaving a forced app — restore system
 *       rotation so landscape does not stick to the whole tablet.</li>
 *   <li><b>Real apps</b>: apply that package's rule (or system default).</li>
 * </ul>
 */
public final class PackageFilter {
    private static final Set<String> OVERLAYS = new HashSet<>(Arrays.asList(
            "com.android.systemui",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.android.phone",
            "com.android.server.telecom",
            "com.android.providers.media",
            "com.android.captiveportallogin",
            "io.chaldeaprjkt.gamespace",
            "com.libremobileos.freeform",
            "com.libremobileos.sidebar",
            "com.android.axion.quicklook",
            "com.android.axion.sandbox",
            "org.protonaosp.columbus"
    ));

    private static final Set<String> HOMES = new HashSet<>(Arrays.asList(
            "com.android.launcher",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.sec.android.app.launcher",
            "com.miui.home",
            "com.huawei.android.launcher",
            "com.oppo.launcher",
            "com.teslacoilsw.launcher",
            "bitpit.launcher",
            "com.microsoft.launcher",
            "com.nova.launcher"
    ));

    private PackageFilter() {
    }

    /**
     * Status bar, Game Space HUD, keyboards, etc. — never change rotation for these.
     */
    public static boolean isTransientOverlay(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return true;
        }
        if (OVERLAYS.contains(packageName)) {
            return true;
        }
        String p = packageName.toLowerCase(Locale.US);
        if (p.contains("inputmethod") || p.contains(".ime") || p.endsWith(".keyboard")
                || p.contains("gboard") || p.contains("swiftkey")
                || p.contains("sogou") || p.contains("baidu.input")
                || (p.contains("latin") && p.contains("input"))) {
            return true;
        }
        if (p.contains("systemui") || p.contains("edgepanel") || p.contains("smartbar")
                || p.contains("navigationbar") || p.contains("screenshot")
                || p.contains("gamespace") || p.contains("game_space") || p.contains("gameassistant")
                || p.contains("freeform") || p.contains("sidebar") || p.contains("taskbar")
                || p.contains("permissioncontroller") || p.contains("biometric")
                || p.contains("keyguard") || p.contains("setupwizard")) {
            return true;
        }
        return false;
    }

    /**
     * Launcher / home — user left the forced app; restore system orientation.
     */
    public static boolean isHome(String packageName) {
        if (packageName == null) {
            return false;
        }
        if (HOMES.contains(packageName)) {
            return true;
        }
        String p = packageName.toLowerCase(Locale.US);
        return p.contains("launcher") && !p.contains("game");
    }

    /**
     * Our own settings UI — restore system so the tablet is usable while configuring.
     */
    public static boolean isSelf(String packageName) {
        return "com.charles.rotationcontrol".equals(packageName)
                || "com.charles.handsfree".equals(packageName);
    }

    /**
     * @deprecated use {@link #isTransientOverlay(String)}
     */
    public static boolean shouldIgnore(String packageName) {
        return isTransientOverlay(packageName);
    }
}
