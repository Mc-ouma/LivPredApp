package com.soccertips.predictx.util

import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class StartupTimeTracker @Inject constructor() {

    companion object {
        private var appStartTime: Long = 0
        private var processStartTime: Long = 0

        fun recordProcessStart() {
            processStartTime = SystemClock.elapsedRealtime()
        }

        fun recordAppStart() {
            appStartTime = SystemClock.elapsedRealtime()
        }
    }

    fun recordFirstFrameRendered() {
        val currentTime = SystemClock.elapsedRealtime()
        val coldStartTime = currentTime - processStartTime
        val appInitTime = currentTime - appStartTime

        Timber.i("Cold Start Performance:")
        Timber.i("- Process to first frame: ${coldStartTime}ms")
        Timber.i("- App init to first frame: ${appInitTime}ms")

        // Log to analytics if needed
        recordStartupMetrics(coldStartTime, appInitTime)
    }

    private fun recordStartupMetrics(coldStartTime: Long, appInitTime: Long) {
        // You can send these metrics to Firebase Analytics or Crashlytics
        try {
            // Example: Firebase Analytics
            // analytics.logEvent("app_startup_time", Bundle().apply {
            //     putLong("cold_start_ms", coldStartTime)
            //     putLong("app_init_ms", appInitTime)
            // })
        } catch (e: Exception) {
            Timber.w(e, "Failed to record startup metrics")
        }
    }
}
