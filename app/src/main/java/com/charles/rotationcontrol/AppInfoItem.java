package com.charles.rotationcontrol;

import android.graphics.drawable.Drawable;

public final class AppInfoItem {
    public final String packageName;
    public final String label;
    public final Drawable icon;
    public OrientationMode mode;

    public AppInfoItem(String packageName, String label, Drawable icon, OrientationMode mode) {
        this.packageName = packageName;
        this.label = label;
        this.icon = icon;
        this.mode = mode;
    }
}
