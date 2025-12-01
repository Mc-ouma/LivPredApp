package com.soccertips.predictx.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages device performance detection and provides optimization hints.
 *
 * This class helps identify low-RAM devices (1.5-2GB) that have higher slow cold start rates
 * and provides configuration for adaptive behavior.
 */
@Singleton
class DevicePerformanceManager @Inject constructor() {

    companion object {
        // RAM thresholds in MB
        private const val LOW_RAM_THRESHOLD_MB = 2048L // 2GB
        private const val VERY_LOW_RAM_THRESHOLD_MB = 1536L // 1.5GB

        // Cached performance tier
        @Volatile
        private var cachedPerformanceTier: PerformanceTier? = null

        @Volatile
        private var totalMemoryMB: Long = 0

        @Volatile
        private var isLowRamDevice: Boolean? = null
    }

    /**
     * Performance tier classification for adaptive behavior
     */
    enum class PerformanceTier {
        /** Very low RAM (< 1.5GB) - Aggressive optimizations */
        VERY_LOW,

        /** Low RAM (1.5-2GB) - Moderate optimizations */
        LOW,

        /** Standard RAM (2-4GB) - Minimal optimizations */
        STANDARD,

        /** High RAM (> 4GB) - No optimizations needed */
        HIGH
    }

    /**
     * Configuration for startup behavior based on device tier
     */
    data class StartupConfig(
        val deferAdInitializationMs: Long,
        val deferPreloadingMs: Long,
        val deferFirebaseMs: Long,
        val skipAggressivePreloading: Boolean,
        val reduceImageCacheSize: Boolean,
        val deferAnalyticsMs: Long,
        val useAsyncHiltInjection: Boolean,
        val skipNonEssentialInit: Boolean
    )

    /**
     * Initialize the performance manager early in app startup.
     * Call this as early as possible in Application.onCreate()
     */
    fun initialize(context: Context) {
        if (cachedPerformanceTier != null) return

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        totalMemoryMB = memoryInfo.totalMem / (1024 * 1024)
        isLowRamDevice = activityManager.isLowRamDevice

        cachedPerformanceTier = when {
            totalMemoryMB < VERY_LOW_RAM_THRESHOLD_MB || isLowRamDevice == true -> PerformanceTier.VERY_LOW
            totalMemoryMB < LOW_RAM_THRESHOLD_MB -> PerformanceTier.LOW
            totalMemoryMB < 4096 -> PerformanceTier.STANDARD
            else -> PerformanceTier.HIGH
        }

        Timber.i("DevicePerformanceManager initialized:")
        Timber.i("  - Total RAM: ${totalMemoryMB}MB")
        Timber.i("  - isLowRamDevice: $isLowRamDevice")
        Timber.i("  - Performance tier: ${cachedPerformanceTier?.name}")
        Timber.i("  - Build.MODEL: ${Build.MODEL}")
        Timber.i("  - SDK: ${Build.VERSION.SDK_INT}")
    }

    /**
     * Get the device's performance tier
     */
    fun getPerformanceTier(): PerformanceTier {
        return cachedPerformanceTier ?: PerformanceTier.STANDARD
    }

    /**
     * Check if this is a low-memory device that needs optimization
     */
    fun isLowMemoryDevice(): Boolean {
        val tier = getPerformanceTier()
        return tier == PerformanceTier.VERY_LOW || tier == PerformanceTier.LOW
    }

    /**
     * Check if this device is in the critical 1.5-2GB range
     * These devices have significantly higher slow cold start rates (20.33% vs 7.23%)
     */
    fun isCriticalMemoryDevice(): Boolean {
        return totalMemoryMB in VERY_LOW_RAM_THRESHOLD_MB until LOW_RAM_THRESHOLD_MB
    }

    /**
     * Get startup configuration based on device tier
     * Balanced to optimize cold start while minimizing ad loading latency impact
     */
    fun getStartupConfig(): StartupConfig {
        return when (getPerformanceTier()) {
            PerformanceTier.VERY_LOW -> StartupConfig(
                deferAdInitializationMs = 1500,
                deferPreloadingMs = 2500,
                deferFirebaseMs = 1500,
                skipAggressivePreloading = true,
                reduceImageCacheSize = true,
                deferAnalyticsMs = 2000,
                useAsyncHiltInjection = true,
                skipNonEssentialInit = true
            )

            PerformanceTier.LOW -> StartupConfig(
                deferAdInitializationMs = 1000,
                deferPreloadingMs = 2000,
                deferFirebaseMs = 1000,
                skipAggressivePreloading = false,  // Allow preloading for better ad availability
                reduceImageCacheSize = true,
                deferAnalyticsMs = 1500,
                useAsyncHiltInjection = true,
                skipNonEssentialInit = false
            )

            PerformanceTier.STANDARD -> StartupConfig(
                deferAdInitializationMs = 500,
                deferPreloadingMs = 1500,
                deferFirebaseMs = 800,
                skipAggressivePreloading = false,
                reduceImageCacheSize = false,
                deferAnalyticsMs = 1000,
                useAsyncHiltInjection = false,
                skipNonEssentialInit = false
            )

            PerformanceTier.HIGH -> StartupConfig(
                deferAdInitializationMs = 300,
                deferPreloadingMs = 1000,
                deferFirebaseMs = 500,
                skipAggressivePreloading = false,
                reduceImageCacheSize = false,
                deferAnalyticsMs = 500,
                useAsyncHiltInjection = false,
                skipNonEssentialInit = false
            )
        }
    }

    /**
     * Get recommended delay before showing ads on cold start
     */
    fun getAdShowDelayMs(): Long {
        return when (getPerformanceTier()) {
            PerformanceTier.VERY_LOW -> 5000
            PerformanceTier.LOW -> 4000
            PerformanceTier.STANDARD -> 2000
            PerformanceTier.HIGH -> 1000
        }
    }

    /**
     * Get the available memory in MB
     */
    fun getAvailableMemoryMB(context: Context): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.availMem / (1024 * 1024)
    }

    /**
     * Check if system is under memory pressure
     */
    fun isUnderMemoryPressure(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.lowMemory
    }

    /**
     * Get total RAM in MB
     */
    fun getTotalMemoryMB(): Long = totalMemoryMB

    /**
     * Log performance tier for debugging/analytics
     */
    fun logPerformanceInfo() {
        Timber.d("Device Performance Info:")
        Timber.d("  - Tier: ${getPerformanceTier().name}")
        Timber.d("  - Total RAM: ${totalMemoryMB}MB")
        Timber.d("  - Is Low Memory: ${isLowMemoryDevice()}")
        Timber.d("  - Is Critical Memory (1.5-2GB): ${isCriticalMemoryDevice()}")
    }
}
