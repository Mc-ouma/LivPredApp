package com.soccertips.predictx.util

import android.os.Build
import android.os.StrictMode
import timber.log.Timber

/**
 * Utility class for configuring StrictMode policies to detect unsafe intent launches
 * and other potential issues in the app.
 */
object StrictModeUtil {

    /**
     * Configure StrictMode to detect unsafe intent launches.
     * This helps identify intents that don't comply with Android 15's new security measures,
     * such as intents without actions or that don't match intent-filters properly.
     * This feature is only available on Android 15 (API level 35) and higher.
     */
    fun enableStrictModeForIntentViolations() {
        val builder = StrictMode.VmPolicy.Builder()

        // detectUnsafeIntentLaunch is only available on Android 15 (API 35) and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Try-catch added for extra safety since this is a new API
            try {
                builder.detectUnsafeIntentLaunch()
            } catch (e: NoSuchMethodError) {
                // Log or handle the error if method is unavailable despite API check
                Timber.e("detectUnsafeIntentLaunch method not available: ${e.message}")
            }
        }

        StrictMode.setVmPolicy(
            builder
                .penaltyLog() // Log violations to logcat
                .build()
        )
    }
}
