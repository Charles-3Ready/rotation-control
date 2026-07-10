package com.charles.rotationcontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Accessibility services restart with the system when the user has enabled them.
 * This receiver is a lightweight hook for future foreground work if needed.
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        Log.i(TAG, "Boot completed — enable Accessibility for Rotation Control if rules should apply");
    }
}
