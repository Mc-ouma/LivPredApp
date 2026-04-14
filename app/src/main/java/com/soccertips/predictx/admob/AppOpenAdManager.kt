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

    fun onAppBackgrounded() {
        isAppInForeground = false
        wasAppInBackground = true
    }

    fun onAppForegrounded() {
        isAppInForeground = true
    }

    fun shouldShowAdOnAppStart(isFirstLaunch: Boolean): Boolean {
        return showAdOnAppStart &&
                !isFirstLaunch &&
                isAdAvailable() &&
                canShowAdBasedOnInterval() &&
                !adStateManager.isFullScreenAdShowing()
    }

    fun shouldShowAdOnAppResume(): Boolean {
        return showAdOnAppResume &&
                wasAppInBackground &&
                isAdAvailable() &&
                canShowAdBasedOnInterval() &&
                !adStateManager.isFullScreenAdShowing()
    }

    private fun canShowAdBasedOnInterval(): Boolean {
        val currentTime = System.currentTimeMillis()
        return currentTime - lastAdDisplayTime >= MIN_AD_DISPLAY_INTERVAL
    }

    fun loadAppOpenAd() {
        if (isLoadingAd || isAdAvailable()) {
            return
        }

        val activity = currentActivityContext
        if (activity == null) {
            Timber.Forest.tag("AppOpenAd").w("No active activity context; skipping app open load")
            return
        }

        if (activity.isFinishing || activity.isDestroyed) {
            Timber.Forest.tag("AppOpenAd")
                .w("Activity is finishing or destroyed; skipping app open load")
            return
        }

        isLoadingAd = true
        try {
            FirebaseCrashlytics.getInstance().log("AppOpen: load start")
        } catch (_: Exception) {
        }
        val request = AdRequest.Builder(adUnitId).build()

        AppOpenAd.load(
            request,
            object : AdLoadCallback<AppOpenAd> {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Timber.Forest.tag("AppOpenAd").d("App open ad loaded")
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
                        .d("App open ad failed to load: ${adError.message}")
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
                Timber.Forest.tag("AppOpenAd").d("App open ad dismissed")
                try {
                    FirebaseCrashlytics.getInstance().log("AppOpen: onAdDismissedFullScreenContent")
                } catch (_: Exception) {
                }
                adStateManager.setFullScreenAdShowing(false)
                appOpenAd = null
                onShowAdCompleteListener?.invoke()
                loadAppOpenAd()
            }

            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                Timber.Forest.tag("AppOpenAd")
                    .d("App open ad failed to show: ${fullScreenContentError.message}")
                try {
                    FirebaseCrashlytics.getInstance().log("AppOpen: onAdFailedToShow ${fullScreenContentError.code}")
                } catch (_: Exception) {
                }
                adStateManager.setFullScreenAdShowing(false)
                adFailureListener?.invoke(fullScreenContentError.message)
                appOpenAd = null
                onShowAdCompleteListener?.invoke()
                loadAppOpenAd()
            }

            override fun onAdShowedFullScreenContent() {
                Timber.Forest.tag("AppOpenAd").d("App open ad showed")
                try {
                    FirebaseCrashlytics.getInstance().log("AppOpen: onAdShowedFullScreenContent")
                } catch (_: Exception) {
                }
                adStateManager.setFullScreenAdShowing(true)
                lastAdDisplayTime = System.currentTimeMillis()
                wasAppInBackground = false
            }

            override fun onAdImpression() {
                Timber.Forest.tag("AppOpenAd").d("App open ad impression recorded")
                try {
                    FirebaseCrashlytics.getInstance().log("AppOpen: onAdImpression")
                } catch (_: Exception) {
                }
                adImpressionListener?.invoke()
            }
        }
    }

    private fun scheduleAdLoadRetry() {
        // Retry loading the ad after a delay
        Handler(Looper.getMainLooper())
                .postDelayed(
                        {
                            if (!isAdAvailable() && !isLoadingAd) {
                                loadAppOpenAd()
                            }
                        },
                        60000
                ) // Retry after 1 minute
    }

    fun showAdIfAvailable(activity: Activity, onShowAdCompleteListener: () -> Unit) {
        // Skip ads for subscribers
        if (adStateManager.isSubscribed()) {
            Timber.Forest.tag("AppOpenAd").d("User is subscribed, skipping app open ad")
            onShowAdCompleteListener()
            return
        }

        // CRITICAL: Check if device is safe for full screen ads
        if (!adStateManager.isSafeForFullScreenAds()) {
            Timber.Forest.tag("AppOpenAd").w("Skipping ad on problematic device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            try {
                FirebaseCrashlytics.getInstance().log("AppOpen: Skipped on ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.SDK_INT})")
            } catch (_: Exception) {}
            onShowAdCompleteListener()
            return
        }

        // Check consent status before showing ad
        try {
            val consentInfo = com.google.android.ump.UserMessagingPlatform.getConsentInformation(activity)
            if (!consentInfo.canRequestAds()) {
                Timber.Forest.tag("AppOpenAd").w("Cannot show ad - consent not granted")
                try {
                    FirebaseCrashlytics.getInstance().log("AppOpen: Skipped - no consent")
                } catch (_: Exception) {}
                onShowAdCompleteListener()
                return
            }
        } catch (e: Exception) {
            Timber.Forest.tag("AppOpenAd").e("Error checking consent: ${e.message}")
            onShowAdCompleteListener()
            return
        }

        // CRITICAL: Validate activity state before showing ad to prevent crashes
        if (activity.isFinishing || activity.isDestroyed) {
            Timber.Forest.tag("AppOpenAd").w("Cannot show ad - activity is finishing or destroyed")
            onShowAdCompleteListener()
            return
        }

        // Validate that activity has a valid window and decor view
        try {
            val window = activity.window
            if (window == null) {
                Timber.tag("AppOpenAd").w("Cannot show ad - activity window is null")
                onShowAdCompleteListener()
                return
            }

            val decorView = window.decorView
            if (!decorView.isAttachedToWindow) {
                Timber.tag("AppOpenAd")
                        .w("Cannot show ad - decor view not attached to window")
                onShowAdCompleteListener()
                return
            }
        } catch (e: Exception) {
            Timber.Forest.tag("AppOpenAd").e("Error validating activity window state: ${e.message}")
            onShowAdCompleteListener()
            return
        }

        // Check if any other full screen ad is currently showing
        if (adStateManager.isFullScreenAdShowing()) {
            Timber.Forest.tag("AppOpenAd")
                    .d(
                            "Skipped showing app open ad because another full screen ad is already showing"
                    )
            try {
                FirebaseCrashlytics.getInstance().log("AppOpen: show skipped - another ad showing")
            } catch (_: Exception) {}
            onShowAdCompleteListener()
            return
        }

        if (!isAdAvailable()) {
            Timber.Forest.tag("AppOpenAd").d("App open ad not available")
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
            Timber.Forest.tag("AppOpenAd").d("App is in background, not showing ad")
            try {
                FirebaseCrashlytics.getInstance()
                        .log("AppOpen: show skipped - app not in foreground")
            } catch (_: Exception) {}
            onShowAdCompleteListener()
            return
        }

        appOpenAd?.let { attachAdCallbacks(it, onShowAdCompleteListener) }

        Timber.Forest.tag("AppOpenAd").d("Showing app open ad")
        try {
            FirebaseCrashlytics.getInstance().log("AppOpen: show")
        } catch (_: Exception) {
        }

        try {
            appOpenAd?.show(activity)
        } catch (e: Exception) {
            Timber.Forest.tag("AppOpenAd").e("Exception showing app open ad: ${e.message}")
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
