package com.soccertips.predictx.admob

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.soccertips.predictx.R
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import timber.log.Timber

// Global singleton to track ad state across different ad types
@Singleton
class AdStateManager @Inject constructor() {
    // Tracks if any full screen ad is currently showing
    private var isAnyFullScreenAdShowing = false

    // Lock any full screen ad from showing
    fun setFullScreenAdShowing(isShowing: Boolean) {
        isAnyFullScreenAdShowing = isShowing
        Timber.tag("AdStateManager").d("Full screen ad state: $isAnyFullScreenAdShowing")
    }

    // Check if any full screen ad is currently showing
    fun isFullScreenAdShowing(): Boolean {
        return isAnyFullScreenAdShowing
    }
}

@Composable
fun BannerAdView(
        modifier: Modifier = Modifier,
        adUnitId: String = stringResource(R.string.banner_id) // Test banner ID
) {
    val context = LocalContext.current
    AndroidView(
            modifier =
                    modifier.fillMaxWidth()
                            // Add WindowInsets.navigationBars padding to avoid overlap with
                            // navigation buttons
                            .windowInsetsPadding(
                                    androidx.compose.foundation.layout.WindowInsets.navigationBars
                            ),
            factory = { factoryContext ->
                AdView(factoryContext).apply {
                    setAdSize(
                            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, 360)
                    )
                    this.adUnitId = adUnitId
                    loadAd(AdRequest.Builder().build())
                }
            }
    )
}

@Composable
fun InlineBannerAdView(
        modifier: Modifier = Modifier,
        adUnitId: String = stringResource(R.string.banner_id)
) {
    val context = LocalContext.current
    AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { factoryContext ->
                AdView(factoryContext).apply {
                    setAdSize(AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(context, 360))
                    this.adUnitId = adUnitId
                    loadAd(AdRequest.Builder().build())
                }
            }
    )
}

class InterstitialAdManager
@Inject
constructor(private val applicationContext: Context, private val adStateManager: AdStateManager) {
    private var interstitialAd: InterstitialAd? = null

    // Get ad unit ID lazily to avoid accessing string resources from non-visual context
    private val adUnitId: String by lazy {
        currentActivityContext?.getString(R.string.interstitial_id)
                ?: applicationContext.getString(R.string.interstitial_id)
    }

    // Track current activity context for loading ads
    private var currentActivityContext: Activity? = null

    // Flag to control whether to use Activity context for ad loading
    private var useActivityContextForLoading = true

    // Track ad loading state to prevent multiple simultaneous loads
    private var isAdLoading = false
    private var retryAttempt = 0

    // Removed eager init load to avoid using non-visual context on Android 14+
    // init { loadInterstitialAd() }

    // Method to set the current activity context
    fun setActivityContext(activity: Activity?) {
        currentActivityContext = activity
        // Proactively load an ad if one isn't available and we now have an activity context
        if (activity != null && interstitialAd == null && !isAdLoading) {
            loadInterstitialAd()
        }
    }

    // Method to configure whether to use activity context
    fun useActivityContextForAdLoading(enable: Boolean) {
        useActivityContextForLoading = enable
    }

    fun loadInterstitialAd() {
        if (isAdLoading) {
            Timber.tag("InterstitialAd").d("Ad is already loading.")
            return
        }

        if (interstitialAd != null) {
            Timber.tag("InterstitialAd").d("Ad is already loaded.")
            return
        }

        // Choose the appropriate context for loading. If Activity context is required but
        // unavailable, skip loading to avoid using a non-visual context.
        val contextToUse: Context =
                if (useActivityContextForLoading) {
                    val act = currentActivityContext
                    if (act == null) {
                        Timber.tag("InterstitialAd")
                                .w("Activity context unavailable; deferring interstitial load")
                        return
                    }
                    act
                } else {
                    applicationContext
                }

        isAdLoading = true
        Timber.tag("InterstitialAd").d("Starting to load interstitial ad...")
        try {
            FirebaseCrashlytics.getInstance().log("Interstitial: load start")
        } catch (_: Exception) {}

        InterstitialAd.load(
                contextToUse,
                adUnitId,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        Timber.tag("InterstitialAd").d("Ad loaded successfully")
                        try {
                            FirebaseCrashlytics.getInstance().log("Interstitial: onAdLoaded")
                        } catch (_: Exception) {}
                        interstitialAd = ad
                        isAdLoading = false
                        retryAttempt = 0 // Reset retry counter on success

                        ad.fullScreenContentCallback =
                                object : FullScreenContentCallback() {
                                    override fun onAdShowedFullScreenContent() {
                                        Timber.tag("InterstitialAd")
                                                .d("Ad showed full screen content")
                                        try {
                                            FirebaseCrashlytics.getInstance()
                                                    .log(
                                                            "Interstitial: onAdShowedFullScreenContent"
                                                    )
                                        } catch (_: Exception) {}
                                        adStateManager.setFullScreenAdShowing(true)
                                    }

                                    override fun onAdDismissedFullScreenContent() {
                                        Timber.tag("InterstitialAd")
                                                .d("Ad dismissed full screen content")
                                        try {
                                            FirebaseCrashlytics.getInstance()
                                                    .log(
                                                            "Interstitial: onAdDismissedFullScreenContent"
                                                    )
                                        } catch (_: Exception) {}
                                        adStateManager.setFullScreenAdShowing(false)
                                        interstitialAd = null
                                        loadInterstitialAd() // Load a new ad for next time
                                    }

                                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                                        Timber.tag("InterstitialAd")
                                                .e("Failed to show ad: ${'$'}{error.message}")
                                        try {
                                            FirebaseCrashlytics.getInstance()
                                                    .log(
                                                            "Interstitial: onAdFailedToShow ${'$'}{error.code}"
                                                    )
                                        } catch (_: Exception) {}
                                        adStateManager.setFullScreenAdShowing(false)
                                        interstitialAd = null
                                    }
                                }
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Timber.tag("InterstitialAd").e("Failed to load ad: ${'$'}{error.message}")
                        try {
                            FirebaseCrashlytics.getInstance()
                                    .log("Interstitial: onAdFailedToLoad ${'$'}{error.code}")
                        } catch (_: Exception) {}
                        interstitialAd = null
                        isAdLoading = false
                        retryAttempt++

                        val maxRetries = 5
                        if (retryAttempt <= maxRetries) {
                            val delay = TimeUnit.SECONDS.toMillis(2.0.pow(retryAttempt).toLong())
                            Timber.tag("InterstitialAd")
                                    .d(
                                            "Retrying ad load in ${'$'}delay ms (attempt ${'$'}retryAttempt)"
                                    )
                            Handler(Looper.getMainLooper())
                                    .postDelayed({ loadInterstitialAd() }, delay)
                        } else {
                            Timber.tag("InterstitialAd")
                                    .e("Exceeded max retry attempts for ad loading.")
                        }
                    }
                }
        )
    }

    fun showInterstitialAd(activity: Activity) {
        // Don't show if another full screen ad is showing
        if (adStateManager.isFullScreenAdShowing()) {
            Timber.tag("InterstitialAd")
                    .d("Skipped showing ad because another full screen ad is already showing")
            return
        }

        val ad = interstitialAd
        if (ad == null) {
            Timber.tag("InterstitialAd").e("The interstitial ad wasn't ready yet.")
            try {
                FirebaseCrashlytics.getInstance().log("Interstitial: show requested but ad=null")
            } catch (_: Exception) {}
            loadInterstitialAd() // Attempt to load a new ad if the current one is null
            return
        }

        // Configure the ad for proper edge-to-edge display
        ad.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdShowedFullScreenContent() {
                        Timber.tag("InterstitialAd").d("Ad showed full screen content")
                        adStateManager.setFullScreenAdShowing(true)

                        // Handle edge-to-edge display for the ad
                        handleEdgeToEdgeForAd(activity, true)
                    }

                    override fun onAdDismissedFullScreenContent() {
                        Timber.tag("InterstitialAd").d("Ad dismissed full screen content")
                        adStateManager.setFullScreenAdShowing(false)

                        // Restore edge-to-edge display settings
                        handleEdgeToEdgeForAd(activity, false)

                        interstitialAd = null
                        loadInterstitialAd() // Load a new ad for next time
                    }

                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        Timber.tag("InterstitialAd").e("Failed to show ad: ${'$'}{error.message}")
                        adStateManager.setFullScreenAdShowing(false)

                        // Restore edge-to-edge display settings
                        handleEdgeToEdgeForAd(activity, false)

                        interstitialAd = null
                    }
                }

        try {
            ad.show(activity)
        } catch (e: Exception) {
            Timber.tag("InterstitialAd").e("Exception showing interstitial ad: ${e.message}")
            try {
                FirebaseCrashlytics.getInstance().recordException(e)
                FirebaseCrashlytics.getInstance().log("Interstitial: show exception - ${e.message}")
            } catch (_: Exception) {}
            // Clean up state
            adStateManager.setFullScreenAdShowing(false)
            handleEdgeToEdgeForAd(activity, false)
            interstitialAd = null
            // Try to load a new ad
            loadInterstitialAd()
        }
    }

    // New method that accepts a callback to execute after ad is dismissed
    fun showInterstitialAdWithCallback(activity: Activity, onAdDismissed: () -> Unit) {
        // Don't show if another full screen ad is showing
        if (adStateManager.isFullScreenAdShowing()) {
            Timber.tag("InterstitialAd")
                    .d("Skipped showing ad because another full screen ad is already showing")
            onAdDismissed() // Execute callback immediately if we can't show the ad
            return
        }

        val ad = interstitialAd
        if (ad == null) {
            Timber.tag("InterstitialAd").e("The interstitial ad wasn't ready yet.")
            loadInterstitialAd() // Attempt to load a new ad if the current one is null
            onAdDismissed() // Execute callback immediately since we can't show an ad
            return
        }

        // Set a new callback that will trigger our navigation callback when ad is dismissed
        ad.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdShowedFullScreenContent() {
                        Timber.tag("InterstitialAd").d("Ad showed full screen content")
                        adStateManager.setFullScreenAdShowing(true)

                        // Handle edge-to-edge display for the ad
                        handleEdgeToEdgeForAd(activity, true)
                    }

                    override fun onAdDismissedFullScreenContent() {
                        Timber.tag("InterstitialAd").d("Ad dismissed full screen content")
                        adStateManager.setFullScreenAdShowing(false)

                        // Restore edge-to-edge display settings
                        handleEdgeToEdgeForAd(activity, false)

                        interstitialAd = null
                        loadInterstitialAd() // Load a new ad for next time

                        // Execute the provided callback when ad is dismissed
                        onAdDismissed()
                    }

                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        Timber.tag("InterstitialAd").e("Failed to show ad: ${'$'}{error.message}")
                        adStateManager.setFullScreenAdShowing(false)

                        // Restore edge-to-edge display settings
                        handleEdgeToEdgeForAd(activity, false)

                        interstitialAd = null

                        // Execute the provided callback when ad fails to show
                        onAdDismissed()
                    }
                }

        Timber.tag("InterstitialAd").d("Showing interstitial ad with callback")
        try {
            FirebaseCrashlytics.getInstance().log("Interstitial: show with callback")
        } catch (_: Exception) {}

        try {
            ad.show(activity)
        } catch (e: Exception) {
            Timber.tag("InterstitialAd")
                    .e("Exception showing interstitial ad with callback: ${e.message}")
            try {
                FirebaseCrashlytics.getInstance().recordException(e)
                FirebaseCrashlytics.getInstance()
                        .log("Interstitial with callback: show exception - ${e.message}")
            } catch (_: Exception) {}
            // Clean up state
            adStateManager.setFullScreenAdShowing(false)
            handleEdgeToEdgeForAd(activity, false)
            interstitialAd = null
            // Execute callback and try to load a new ad
            onAdDismissed()
            loadInterstitialAd()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun handleEdgeToEdgeForAd(activity: Activity, isAdShowing: Boolean) {
        val window = activity.window
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val controller = window.insetsController
                if (isAdShowing) {
                    controller?.hide(WindowInsets.Type.systemBars())
                    controller?.systemBarsBehavior =
                            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    // When ad is dismissed, restore system bars
                    controller?.show(WindowInsets.Type.systemBars())
                    controller?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT
                }
            } else {
                @Suppress("DEPRECATION")
                if (isAdShowing) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                    window.decorView.systemUiVisibility =
                            (View.SYSTEM_UI_FLAG_FULLSCREEN or
                                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                    window.decorView.systemUiVisibility =
                            (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
                }
            }
        } catch (e: Exception) {
            Timber.tag("InterstitialAd")
                    .w("Failed to handle edge-to-edge for ad: ${'$'}{e.message}")
        }
    }

    fun isAdLoaded(): Boolean = interstitialAd != null
}

class RewardedAdManager
@Inject
constructor(private val context: Context, private val adStateManager: AdStateManager) {
    private var rewardedAd: RewardedAd? = null
    private val adUnitId = context.getString(R.string.reward) // Test rewarded ad ID

    // Track ad loading state
    private var isAdLoading = false

    // Track current activity context for loading ads
    private var currentActivityContext: Activity? = null

    // Flag to control whether to use Activity context for ad loading
    private var useActivityContextForLoading = true

    // Method to set the current activity context and trigger ad loading
    fun setActivityContext(activity: Activity?) {
        currentActivityContext = activity
        // Pre-load an ad as soon as we have an activity context
        if (activity != null && rewardedAd == null && !isAdLoading) {
            loadRewardedAd()
        }
    }

    // Method to configure whether to use activity context
    fun useActivityContextForAdLoading(enable: Boolean) {
        useActivityContextForLoading = enable
    }

    fun loadRewardedAd() {
        // Prevent multiple simultaneous loads
        if (isAdLoading) {
            Timber.tag("RewardedAd").d("Ad is already loading.")
            return
        }

        if (rewardedAd != null) {
            Timber.tag("RewardedAd").d("Ad is already loaded.")
            return
        }

        // Choose the appropriate context for loading. If Activity context is required but
        // unavailable, skip loading to avoid using a non-visual context.
        val contextToUse: Context =
                if (useActivityContextForLoading) {
                    val act = currentActivityContext
                    if (act == null) {
                        Timber.tag("RewardedAd")
                                .w("Activity context unavailable; deferring rewarded load")
                        return
                    }
                    act
                } else {
                    context
                }

        isAdLoading = true
        Timber.tag("RewardedAd").d("Starting to load rewarded ad...")
        try {
            FirebaseCrashlytics.getInstance().log("Rewarded: load start")
        } catch (_: Exception) {}

        RewardedAd.load(
                contextToUse,
                adUnitId,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        Timber.tag("RewardedAd").d("Rewarded ad loaded successfully")
                        try {
                            FirebaseCrashlytics.getInstance().log("Rewarded: onAdLoaded")
                        } catch (_: Exception) {}
                        rewardedAd = ad
                        isAdLoading = false
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Timber.tag("RewardedAd")
                                .e("Failed to load rewarded ad: ${'$'}{error.message}")
                        try {
                            FirebaseCrashlytics.getInstance()
                                    .log("Rewarded: onAdFailedToLoad ${'$'}{error.code}")
                        } catch (_: Exception) {}
                        rewardedAd = null
                        isAdLoading = false
                        // Try to reload after a delay
                        Handler(Looper.getMainLooper())
                                .postDelayed({ loadRewardedAd() }, 60000) // Retry after 1 minute
                    }
                }
        )
    }

    fun showRewardedAd(activity: Activity, onRewardEarned: (rewardItem: RewardItem) -> Unit) {
        // Don't show if another full screen ad is showing
        if (adStateManager.isFullScreenAdShowing()) {
            Timber.tag("RewardedAd")
                    .d("Skipped showing ad because another full screen ad is already showing")
            try {
                FirebaseCrashlytics.getInstance().log("Rewarded: show skipped - another ad showing")
            } catch (_: Exception) {}
            return
        }

        if (rewardedAd == null) {
            Timber.tag("RewardedAd").e("Attempted to show rewarded ad, but ad was null")
            try {
                FirebaseCrashlytics.getInstance().log("Rewarded: show requested but ad=null")
            } catch (_: Exception) {}
            loadRewardedAd()
            return
        }

        Timber.tag("RewardedAd").d("Attempting to show rewarded ad now...")
        try {
            FirebaseCrashlytics.getInstance().log("Rewarded: show")
        } catch (_: Exception) {}

        // Set up proper edge-to-edge handling for rewarded ads
        rewardedAd?.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdShowedFullScreenContent() {
                        Timber.tag("RewardedAd").d("Rewarded ad showed full screen content")
                        try {
                            FirebaseCrashlytics.getInstance()
                                    .log("Rewarded: onAdShowedFullScreenContent")
                        } catch (_: Exception) {}
                        adStateManager.setFullScreenAdShowing(true)

                        // Handle edge-to-edge display for the ad
                        handleEdgeToEdgeForAd(activity, true)
                    }

                    override fun onAdDismissedFullScreenContent() {
                        Timber.tag("RewardedAd").d("Rewarded ad dismissed full screen content")
                        try {
                            FirebaseCrashlytics.getInstance()
                                    .log("Rewarded: onAdDismissedFullScreenContent")
                        } catch (_: Exception) {}
                        adStateManager.setFullScreenAdShowing(false)

                        // Restore edge-to-edge display settings
                        handleEdgeToEdgeForAd(activity, false)

                        rewardedAd = null
                        loadRewardedAd() // Load a new ad for next time
                    }

                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        Timber.tag("RewardedAd")
                                .e("Failed to show rewarded ad: ${'$'}{error.message}")
                        try {
                            FirebaseCrashlytics.getInstance()
                                    .log("Rewarded: onAdFailedToShow ${'$'}{error.code}")
                        } catch (_: Exception) {}
                        adStateManager.setFullScreenAdShowing(false)

                        // Restore edge-to-edge display settings
                        handleEdgeToEdgeForAd(activity, false)

                        rewardedAd = null
                        loadRewardedAd() // Preload the next ad
                    }
                }

        try {
            rewardedAd?.show(activity) { rewardItem ->
                Timber.tag("RewardedAd")
                        .d("User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                try {
                    FirebaseCrashlytics.getInstance()
                            .log(
                                    "Rewarded: onUserEarnedReward amount=${rewardItem.amount} type=${rewardItem.type}"
                            )
                } catch (_: Exception) {}
                onRewardEarned(rewardItem)
            }
                    ?: run {
                        Timber.tag("RewardedAd").e("The rewarded ad wasn't ready yet.")
                        loadRewardedAd() // Attempt to load a new ad if the current one is null
                    }
        } catch (e: Exception) {
            Timber.tag("RewardedAd").e("Exception showing rewarded ad: ${e.message}")
            try {
                FirebaseCrashlytics.getInstance().recordException(e)
                FirebaseCrashlytics.getInstance().log("Rewarded: show exception - ${e.message}")
            } catch (_: Exception) {}
            // Clean up state
            adStateManager.setFullScreenAdShowing(false)
            handleEdgeToEdgeForAd(activity, false)
            rewardedAd = null
            // Try to load a new ad
            loadRewardedAd()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun handleEdgeToEdgeForAd(activity: Activity, isAdShowing: Boolean) {
        val window = activity.window
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val controller = window.insetsController
                if (isAdShowing) {
                    controller?.hide(WindowInsets.Type.systemBars())
                    controller?.systemBarsBehavior =
                            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    controller?.show(WindowInsets.Type.systemBars())
                    controller?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT
                }
            } else {
                @Suppress("DEPRECATION")
                if (isAdShowing) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                    window.decorView.systemUiVisibility =
                            (View.SYSTEM_UI_FLAG_FULLSCREEN or
                                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                    window.decorView.systemUiVisibility =
                            (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
                }
            }
        } catch (e: Exception) {
            Timber.tag("RewardedAd").w("Failed to handle edge-to-edge for ad: ${'$'}{e.message}")
        }
    }

    fun isAdLoaded(): Boolean {
        val isLoaded = rewardedAd != null
        Timber.tag("RewardedAd").d("isAdLoaded() check returned: $isLoaded")
        return isLoaded
    }

    fun isAdLoading(): Boolean {
        return isAdLoading
    }
}

// AppOpenAd
class AppOpenAdManager
@Inject
constructor(private val context: Context, private val adStateManager: AdStateManager) {
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var loadTime: Long = 0
    private val adUnitId = context.getString(R.string.appOpen_id) // Test App Open ID

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

    // Flag to control whether to use Activity context for ad loading
    private var useActivityContextForLoading = true

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

    // Allows configuring whether to use Activity context for ad loading
    // This helps prevent ViewConfiguration errors on Android 14+
    fun useActivityContextForAdLoading(enable: Boolean) {
        useActivityContextForLoading = enable
    }

    fun setAdImpressionListener(listener: () -> Unit) {
        adImpressionListener = listener
    }

    fun setAdFailureListener(listener: (String) -> Unit) {
        adFailureListener = listener
    }

    fun enableAppResumeAds(enable: Boolean) {
        showAdOnAppResume = enable
    }

    fun enableAppStartAds(enable: Boolean) {
        showAdOnAppStart = enable
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

        // Choose the appropriate context for loading. If Activity context is required but
        // unavailable, skip loading to avoid using a non-visual context.
        val contextToUse: Context =
                if (useActivityContextForLoading) {
                    val act = currentActivityContext
                    if (act == null) {
                        Timber.tag("AppOpenAd")
                                .w("Activity context unavailable; deferring app open load")
                        return
                    }
                    act
                } else {
                    context
                }

        isLoadingAd = true
        try {
            FirebaseCrashlytics.getInstance().log("AppOpen: load start")
        } catch (_: Exception) {}
        val request = AdRequest.Builder().build()

        AppOpenAd.load(
                contextToUse,
                adUnitId,
                request,
                object : AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        Timber.tag("AppOpenAd").d("App open ad loaded")
                        try {
                            FirebaseCrashlytics.getInstance().log("AppOpen: onAdLoaded")
                        } catch (_: Exception) {}
                        appOpenAd = ad
                        isLoadingAd = false
                        loadTime = System.currentTimeMillis()

                        ad.fullScreenContentCallback =
                                object : FullScreenContentCallback() {
                                    override fun onAdDismissedFullScreenContent() {
                                        Timber.tag("AppOpenAd").d("App open ad dismissed")
                                        try {
                                            FirebaseCrashlytics.getInstance()
                                                    .log("AppOpen: onAdDismissedFullScreenContent")
                                        } catch (_: Exception) {}
                                        adStateManager.setFullScreenAdShowing(false)
                                        appOpenAd = null
                                        loadAppOpenAd() // Load next ad
                                    }

                                    override fun onAdFailedToShowFullScreenContent(
                                            adError: AdError
                                    ) {
                                        Timber.tag("AppOpenAd")
                                                .d(
                                                        "App open ad failed to show: ${'$'}{adError.message}"
                                                )
                                        try {
                                            FirebaseCrashlytics.getInstance()
                                                    .log(
                                                            "AppOpen: onAdFailedToShow ${'$'}{adError.code}"
                                                    )
                                        } catch (_: Exception) {}
                                        adStateManager.setFullScreenAdShowing(false)
                                        adFailureListener?.invoke(adError.message)
                                        appOpenAd = null
                                        loadAppOpenAd()
                                    }

                                    override fun onAdShowedFullScreenContent() {
                                        Timber.tag("AppOpenAd").d("App open ad showed")
                                        try {
                                            FirebaseCrashlytics.getInstance()
                                                    .log("AppOpen: onAdShowedFullScreenContent")
                                        } catch (_: Exception) {}
                                        adStateManager.setFullScreenAdShowing(true)
                                        lastAdDisplayTime = System.currentTimeMillis()
                                        wasAppInBackground = false
                                        adImpressionListener?.invoke()
                                    }

                                    override fun onAdImpression() {
                                        Timber.tag("AppOpenAd").d("App open ad impression recorded")
                                        try {
                                            FirebaseCrashlytics.getInstance()
                                                    .log("AppOpen: onAdImpression")
                                        } catch (_: Exception) {}
                                        adImpressionListener?.invoke()
                                    }
                                }
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Timber.tag("AppOpenAd")
                                .d("App open ad failed to load: ${'$'}{loadAdError.message}")
                        try {
                            FirebaseCrashlytics.getInstance()
                                    .log("AppOpen: onAdFailedToLoad ${'$'}{loadAdError.code}")
                        } catch (_: Exception) {}
                        adFailureListener?.invoke(loadAdError.message)
                        isLoadingAd = false

                        // Retry loading after a delay if error is retryable
                        if (loadAdError.code != 2 /* NETWORK_ERROR */ &&
                                        loadAdError.code != 3 /* NO_FILL */
                        ) {
                            scheduleAdLoadRetry()
                        }
                    }
                }
        )
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
        // Check if any other full screen ad is currently showing
        if (adStateManager.isFullScreenAdShowing()) {
            Timber.tag("AppOpenAd")
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
            Timber.tag("AppOpenAd").d("App open ad not available")
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
            Timber.tag("AppOpenAd").d("App is in background, not showing ad")
            try {
                FirebaseCrashlytics.getInstance()
                        .log("AppOpen: show skipped - app not in foreground")
            } catch (_: Exception) {}
            onShowAdCompleteListener()
            return
        }

        // Set callback to be triggered after ad is dismissed
        appOpenAd?.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Timber.tag("AppOpenAd").d("App open ad dismissed")
                        try {
                            FirebaseCrashlytics.getInstance()
                                    .log("AppOpen: onAdDismissedFullScreenContent")
                        } catch (_: Exception) {}
                        adStateManager.setFullScreenAdShowing(false)
                        appOpenAd = null
                        onShowAdCompleteListener()
                        loadAppOpenAd() // Load next ad
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Timber.tag("AppOpenAd")
                                .d("App open ad failed to show: ${'$'}{adError.message}")
                        try {
                            FirebaseCrashlytics.getInstance()
                                    .log("AppOpen: onAdFailedToShow ${'$'}{adError.code}")
                        } catch (_: Exception) {}
                        adStateManager.setFullScreenAdShowing(false)
                        adFailureListener?.invoke(adError.message)
                        appOpenAd = null
                        onShowAdCompleteListener()
                        loadAppOpenAd()
                    }

                    override fun onAdShowedFullScreenContent() {
                        Timber.tag("AppOpenAd").d("App open ad showed")
                        try {
                            FirebaseCrashlytics.getInstance()
                                    .log("AppOpen: onAdShowedFullScreenContent")
                        } catch (_: Exception) {}
                        adStateManager.setFullScreenAdShowing(true)
                        lastAdDisplayTime = System.currentTimeMillis()
                        wasAppInBackground = false
                        adImpressionListener?.invoke()
                    }

                    override fun onAdImpression() {
                        Timber.tag("AppOpenAd").d("App open ad impression recorded")
                        try {
                            FirebaseCrashlytics.getInstance().log("AppOpen: onAdImpression")
                        } catch (_: Exception) {}
                        adImpressionListener?.invoke()
                    }
                }

        Timber.tag("AppOpenAd").d("Showing app open ad")
        try {
            FirebaseCrashlytics.getInstance().log("AppOpen: show")
        } catch (_: Exception) {}

        try {
            appOpenAd?.show(activity)
        } catch (e: Exception) {
            Timber.tag("AppOpenAd").e("Exception showing app open ad: ${e.message}")
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
