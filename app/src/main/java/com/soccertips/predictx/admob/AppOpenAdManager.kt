package com.soccertips.predictx.admob

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.soccertips.predictx.R
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

// AppOpenAd
@Singleton
class AppOpenAdManager
@Inject
constructor(private val context: Context, private val adStateManager: AdStateManager) {
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var loadTime: Long = 0

    // Lazy load ad unit ID only when actually needed, not during construction
    private val adUnitId: String by lazy { context.getString(R.string.appOpen_id) }

    // Track current activity context for loading ads
    private var currentActivityContext: Activity? = null

    // Track whether the app is in foreground to avoid showing ads when app is in background
    private var isAppInForeground = false

    // Track when the app was sent to background
    private var wasAppInBackground = false

    // Track ad impression events
    private var adImpressionListener: (() -> Unit)? = null

    // Track ad failure events for analytics
    private var adFailureListener: ((String) -> Unit)? = null

    // Handler for scheduling load retries — stored so pending callbacks can be cancelled
    private val retryHandler = Handler(Looper.getMainLooper())
    private val retryRunnable = Runnable {
        if (!isAdAvailable() && !isLoadingAd) {
            loadAppOpenAd()
        }
    }

    companion object {
        private const val AD_TIMEOUT = 4 * 60 * 60 * 1000L // 4 hours in milliseconds

        // Controls whether to show ads on app resume
        private var showAdOnAppResume = true

        // Controls whether to show ads on cold start
        private var showAdOnAppStart = true

        // Minimum elapsed time between showing ads (in milliseconds)
        private const val MIN_AD_DISPLAY_INTERVAL = 1 * 60 * 1000L // 1 minute
    }

    // Track when the last ad was shown to prevent excessive ad displays
    private var lastAdDisplayTime: Long = 0

    // Initial load is triggered explicitly by the application after user consent

    // Method to set the current activity context
    fun setActivityContext(activity: Activity?) {
        currentActivityContext = activity
    }

    fun setAdImpressionListener(listener: () -> Unit) {
        adImpressionListener = listener
    }

    fun setAdFailureListener(listener: (String) -> Unit) {
        adFailureListener = listener
    }

    fun shouldShowAdOnAppStart(isFirstLaunch: Boolean): Boolean {
        val available = isAdAvailable()
        val intervalOk = canShowAdBasedOnInterval()
        val noOtherAd = !adStateManager.isFullScreenAdShowing()
        val eligible = showAdOnAppStart &&
                !isFirstLaunch &&
                available &&
                intervalOk &&
                noOtherAd

        Timber.tag("AppOpenAd").d(
            "shouldShowAdOnAppStart check: eligible=$eligible (enabled=$showAdOnAppStart, isFirstLaunch=$isFirstLaunch, adAvailable=$available, intervalOk=$intervalOk, noOtherFullScreenAd=$noOtherAd)"
        )
        return eligible
    }

    fun shouldShowAdOnAppResume(): Boolean {
        val available = isAdAvailable()
        val intervalOk = canShowAdBasedOnInterval()
        val noOtherAd = !adStateManager.isFullScreenAdShowing()
        val eligible = showAdOnAppResume &&
                wasAppInBackground &&
                available &&
                intervalOk &&
                noOtherAd

        val elapsed = System.currentTimeMillis() - lastAdDisplayTime
        Timber.tag("AppOpenAd").d(
            "shouldShowAdOnAppResume check: eligible=$eligible (enabled=$showAdOnAppResume, wasBackgrounded=$wasAppInBackground, adAvailable=$available, intervalOk=$intervalOk [elapsed=${elapsed}ms / min ${MIN_AD_DISPLAY_INTERVAL}ms], noOtherFullScreenAd=$noOtherAd)"
        )
        return eligible
    }

    fun onAppBackgrounded() {
        Timber.tag("AppOpenAd").d("onAppBackgrounded: App moved to background")
        isAppInForeground = false
        wasAppInBackground = true
    }

    fun onAppForegrounded() {
        Timber.tag("AppOpenAd").d("onAppForegrounded: App returned to foreground")
        isAppInForeground = true
    }

    private fun canShowAdBasedOnInterval(): Boolean {
        val currentTime = System.currentTimeMillis()
        return currentTime - lastAdDisplayTime >= MIN_AD_DISPLAY_INTERVAL
    }

    fun loadAppOpenAd() {
        if (isLoadingAd) {
            Timber.tag("AppOpenAd").d("loadAppOpenAd: Ad is already loading, skipping duplicate load request")
            return
        }
        if (isAdAvailable()) {
            Timber.tag("AppOpenAd").d("loadAppOpenAd: Valid ad already cached and available, skipping load")
            return
        }

        val activity = currentActivityContext
        if (activity == null) {
            Timber.tag("AppOpenAd").w("loadAppOpenAd: No active activity context; skipping load")
            return
        }

        if (activity.isFinishing || activity.isDestroyed) {
            Timber.tag("AppOpenAd").w("loadAppOpenAd: Activity is finishing or destroyed; skipping load")
            return
        }

        isLoadingAd = true
        Timber.tag("AppOpenAd").d("loadAppOpenAd: Requesting App Open ad from AdMob...")
        try {
            FirebaseCrashlytics.getInstance().log("AppOpen: load start")
        } catch (_: Exception) {
        }
        val request = AdRequest.Builder(adUnitId).build()

        AppOpenAd.load(
            request,
            object : AdLoadCallback<AppOpenAd> {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Timber.tag("AppOpenAd").d("Callback: onAdLoaded - App open ad loaded successfully (ready for app resume)")
                    try {
                        FirebaseCrashlytics.getInstance().log("AppOpen: onAdLoaded")
                    } catch (_: Exception) {
                    }
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = System.currentTimeMillis()
                    attachAdCallbacks(ad, null)
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Timber.tag("AppOpenAd")
                        .e("Callback: onAdFailedToLoad - Error: ${adError.message} (code: ${adError.code})")
                    try {
                        FirebaseCrashlytics.getInstance()
                            .log("AppOpen: onAdFailedToLoad ${adError.code}")
                    } catch (_: Exception) {
                    }
                    adFailureListener?.invoke(adError.message)
                    isLoadingAd = false

                    // Retry loading after a delay if error is retryable
                    if (adError.code != LoadAdError.ErrorCode.NETWORK_ERROR &&
                        adError.code != LoadAdError.ErrorCode.NO_FILL
                    ) {
                        scheduleAdLoadRetry()
                    }
                }
            }
        )
    }

    private fun attachAdCallbacks(
        ad: AppOpenAd,
        onShowAdCompleteListener: (() -> Unit)?
    ) {
        ad.adEventCallback = object : AppOpenAdEventCallback {
            override fun onAdDismissedFullScreenContent() {
                Timber.tag("AppOpenAd").d("Callback: onAdDismissedFullScreenContent - App open ad dismissed")
                try {
                    FirebaseCrashlytics.getInstance().log("AppOpen: onAdDismissedFullScreenContent")
                } catch (_: Exception) {
                }
                adStateManager.setFullScreenAdShowing(false)
                appOpenAd = null
                onShowAdCompleteListener?.invoke()
            }

            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                Timber.tag("AppOpenAd")
                    .e("Callback: onAdFailedToShowFullScreenContent - Error: ${fullScreenContentError.message} (code: ${fullScreenContentError.code})")
                try {
                    FirebaseCrashlytics.getInstance().log("AppOpen: onAdFailedToShow ${fullScreenContentError.code}")
                } catch (_: Exception) {
                }
                adStateManager.setFullScreenAdShowing(false)
                adFailureListener?.invoke(fullScreenContentError.message)
                appOpenAd = null
                onShowAdCompleteListener?.invoke()
            }

            override fun onAdShowedFullScreenContent() {
                Timber.tag("AppOpenAd").d("Callback: onAdShowedFullScreenContent - App open ad presented full screen")
                try {
                    FirebaseCrashlytics.getInstance().log("AppOpen: onAdShowedFullScreenContent")
                } catch (_: Exception) {
                }
                adStateManager.setFullScreenAdShowing(true)
                lastAdDisplayTime = System.currentTimeMillis()
                wasAppInBackground = false
            }

            override fun onAdImpression() {
                Timber.tag("AppOpenAd").d("Callback: onAdImpression - App open ad impression recorded")
                try {
                    FirebaseCrashlytics.getInstance().log("AppOpen: onAdImpression")
                } catch (_: Exception) {
                }
                adImpressionListener?.invoke()
            }

            override fun onAdClicked() {
                Timber.tag("AppOpenAd").d("Callback: onAdClicked - App open ad clicked")
            }
        }
    }

    private fun scheduleAdLoadRetry() {
        // Cancel any pending retry before scheduling a new one to prevent stacked callbacks
        retryHandler.removeCallbacks(retryRunnable)
        retryHandler.postDelayed(retryRunnable, 60_000) // Retry after 1 minute
    }

    fun showAdIfAvailable(activity: Activity, onShowAdCompleteListener: () -> Unit) {
        // Skip ads for subscribers
        if (adStateManager.isSubscribed()) {
            Timber.tag("AppOpenAd").d("showAdIfAvailable: User is subscribed, skipping app open ad")
            onShowAdCompleteListener()
            return
        }

        // CRITICAL: Check if device is safe for full screen ads
        if (!adStateManager.isSafeForFullScreenAds()) {
            Timber.tag("AppOpenAd").w("showAdIfAvailable: Skipping ad on problematic device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            try {
                FirebaseCrashlytics.getInstance().log("AppOpen: Skipped on ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.SDK_INT})")
            } catch (_: Exception) {}
            onShowAdCompleteListener()
            return
        }

        // Check consent status before showing ad
        if (!canShowAdsWithConsent(activity)) {
            Timber.tag("AppOpenAd").w("showAdIfAvailable: Cannot show ad - consent not granted")
            try {
                FirebaseCrashlytics.getInstance().log("AppOpen: Skipped - no consent")
            } catch (_: Exception) {}
            onShowAdCompleteListener()
            return
        }

        // CRITICAL: Validate activity state before showing ad to prevent crashes
        if (activity.isFinishing || activity.isDestroyed) {
            Timber.tag("AppOpenAd").w("showAdIfAvailable: Cannot show ad - activity is finishing or destroyed")
            onShowAdCompleteListener()
            return
        }

        // Validate that activity has a valid window and decor view
        try {
            val window = activity.window
            if (window == null) {
                Timber.tag("AppOpenAd").w("showAdIfAvailable: Cannot show ad - activity window is null")
                onShowAdCompleteListener()
                return
            }

            val decorView = window.decorView
            if (!decorView.isAttachedToWindow) {
                Timber.tag("AppOpenAd")
                        .w("showAdIfAvailable: Cannot show ad - decor view not attached to window")
                onShowAdCompleteListener()
                return
            }
        } catch (e: Exception) {
            Timber.tag("AppOpenAd").e("showAdIfAvailable: Error validating activity window state: ${e.message}")
            onShowAdCompleteListener()
            return
        }

        // Check if any other full screen ad is currently showing
        if (adStateManager.isFullScreenAdShowing()) {
            Timber.tag("AppOpenAd")
                    .d(
                            "showAdIfAvailable: Skipped showing app open ad because another full screen ad is already showing"
                    )
            try {
                FirebaseCrashlytics.getInstance().log("AppOpen: show skipped - another ad showing")
            } catch (_: Exception) {}
            onShowAdCompleteListener()
            return
        }

        if (!isAdAvailable()) {
            Timber.tag("AppOpenAd").d("showAdIfAvailable: App open ad not available (preloading for next resume)")
            try {
                FirebaseCrashlytics.getInstance()
                        .log("AppOpen: show requested but ad not available")
            } catch (_: Exception) {}
            onShowAdCompleteListener()
            loadAppOpenAd()
            return
        }

        // Don't show ad if app is in background
        if (!isAppInForeground) {
            Timber.tag("AppOpenAd").d("showAdIfAvailable: App is in background, not showing ad")
            try {
                FirebaseCrashlytics.getInstance()
                        .log("AppOpen: show skipped - app not in foreground")
            } catch (_: Exception) {}
            onShowAdCompleteListener()
            return
        }

        appOpenAd?.let { attachAdCallbacks(it, onShowAdCompleteListener) }

        Timber.tag("AppOpenAd").d("showAdIfAvailable: Presenting app open ad on ${activity.localClassName}")
        try {
            FirebaseCrashlytics.getInstance().log("AppOpen: show")
        } catch (_: Exception) {
        }

        try {
            appOpenAd?.show(activity)
        } catch (e: Exception) {
            Timber.tag("AppOpenAd").e("showAdIfAvailable: Exception showing app open ad: ${e.message}")
            try {
                FirebaseCrashlytics.getInstance().recordException(e)
                FirebaseCrashlytics.getInstance().log("AppOpen: show exception - ${e.message}")
            } catch (_: Exception) {}
            // Clean up state and notify completion
            adStateManager.setFullScreenAdShowing(false)
            appOpenAd = null
            onShowAdCompleteListener()
            // Try to load a new ad
            loadAppOpenAd()
        }
    }

    fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo()
    }

    private fun wasLoadTimeLessThanNHoursAgo(): Boolean {
        val dateDifference = System.currentTimeMillis() - loadTime
        return dateDifference < AD_TIMEOUT
    }
}
