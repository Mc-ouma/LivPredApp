package com.soccertips.predictx.admob

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

// Helper function to check consent status efficiently
private fun canShowAdsWithConsent(activity: Activity): Boolean {
    return try {
        val consentInfo = com.google.android.ump.UserMessagingPlatform.getConsentInformation(activity)
        val canRequest = consentInfo.canRequestAds()
        Timber.d("Consent check: canRequestAds = $canRequest, status = ${consentInfo.consentStatus}")
        canRequest
    } catch (e: Exception) {
        Timber.e("Error checking consent: ${e.message}")
        // Default to false for safety - don't show ads if we can't verify consent
        false
    }
}

// Helper function to check if device/manufacturer has known AdActivity issues
private fun isProblematicDeviceForFullScreenAds(): Boolean {
    val manufacturer = Build.MANUFACTURER.lowercase()
    val model = Build.MODEL.lowercase()
    val brand = Build.BRAND.lowercase()

    // Known problematic manufacturers/devices for AdActivity
    val problematicManufacturers = listOf(
        "huawei",  // HwPhoneWindow issues
        "honor",   // Huawei sub-brand
        "oppo",    // ColorOS modifications
        "vivo",    // FuntouchOS modifications  
        "realme"   // RealmeUI modifications
    )

    // Check if manufacturer is in the problematic list
    if (problematicManufacturers.any { manufacturer.contains(it) || brand.contains(it) }) {
        Timber.tag("AdSafety").w("Detected potentially problematic device: $manufacturer $model")
        return true
    }

    // Additional check for specific Android versions with high crash rates
    if (Build.VERSION.SDK_INT in 28..30) { // Android 9, 10, 11
        Timber.tag("AdSafety").d("Device on Android ${Build.VERSION.SDK_INT} - monitoring for issues")
    }

    return false
}

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

    // Check if device is safe for full screen ads
    fun isSafeForFullScreenAds(): Boolean {
        return !isProblematicDeviceForFullScreenAds()
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

@Singleton
class InterstitialAdManager
@Inject
constructor(private val applicationContext: Context, private val adStateManager: AdStateManager) {
    private var interstitialAd: InterstitialAd? = null

    // Get ad unit ID lazily to avoid accessing string resources from non-visual context
    private val adUnitId: String by lazy {
        currentActivityContext?.getString(R.string.interstitial_id)
            ?: applicationContext.getString(R.string.interstitial_id)
    }

    // Reactive state for ad readiness
    private val _isAdReady = MutableStateFlow(false)
    val isAdReady: StateFlow<Boolean> = _isAdReady.asStateFlow()

    // Track current activity context for loading ads
    private var currentActivityContext: Activity? = null

    // Flag to control whether to use Activity context for ad loading
    private var useActivityContextForLoading = true

    // Track ad loading state to prevent multiple simultaneous loads
    private var isAdLoading = false
    private var retryAttempt = 0
    private val maxRetries = 5

    // Method to set the current activity context
    fun setActivityContext(activity: Activity?) {
        currentActivityContext = activity
        // Preload ad when activity context is set (important for reducing latency)
        if (activity != null && interstitialAd == null && !isAdLoading) {
            loadInterstitialAd()
        }
    }

    // Method to configure whether to use activity context
    fun useActivityContextForAdLoading(enable: Boolean) {
        useActivityContextForLoading = enable
    }

    // Method to request ad loading if needed (called after dismissal or failure)
    fun loadAdIfNeeded() {
        if (interstitialAd == null && !isAdLoading && currentActivityContext != null) {
            loadInterstitialAd()
        }
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
        val activity = currentActivityContext
        if (activity == null) {
            Timber.tag("InterstitialAd")
                .w("No active activity context; deferring interstitial load")
            return
        }

        if (activity.isFinishing || activity.isDestroyed) {
            Timber.tag("InterstitialAd")
                .w("Activity is finishing or destroyed; deferring interstitial load")
            return
        }

        val contextToUse: Context = activity

        isAdLoading = true
        Timber.tag("InterstitialAd").d("Starting to load interstitial ad...")
        try {
            FirebaseCrashlytics.getInstance().log("Interstitial: load start")
        } catch (_: Exception) {
        }

        InterstitialAd.load(
            contextToUse,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Timber.tag("InterstitialAd").d("Ad loaded successfully")
                    try {
                        FirebaseCrashlytics.getInstance().log("Interstitial: onAdLoaded")
                    } catch (_: Exception) {
                    }
                    interstitialAd = ad
                    _isAdReady.value = true
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
                                } catch (_: Exception) {
                                }
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
                                } catch (_: Exception) {
                                }
                                adStateManager.setFullScreenAdShowing(false)
                                interstitialAd = null
                                _isAdReady.value = false
                                retryAttempt = 0 // Reset retry counter
                                loadAdIfNeeded() // Preload next ad immediately
                            }

                            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                                Timber.tag("InterstitialAd")
                                    .e("Failed to show ad: ${error.message} (code: ${error.code})")
                                try {
                                    FirebaseCrashlytics.getInstance()
                                        .log(
                                            "Interstitial: onAdFailedToShow ${error.code}"
                                        )
                                } catch (_: Exception) {
                                }
                                adStateManager.setFullScreenAdShowing(false)
                                interstitialAd = null
                                _isAdReady.value = false
                                loadAdIfNeeded() // Try to preload next ad
                            }
                        }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Timber.tag("InterstitialAd").e("Failed to load ad: ${error.message} (code: ${error.code})")
                    try {
                        FirebaseCrashlytics.getInstance()
                            .log("Interstitial: onAdFailedToLoad ${error.code}")
                    } catch (_: Exception) {
                    }
                    interstitialAd = null
                    _isAdReady.value = false
                    isAdLoading = false
                    retryAttempt++

                    // Exponential backoff retry (2s, 4s, 8s, 16s, 32s)
                    if (retryAttempt <= maxRetries) {
                        val delay = TimeUnit.SECONDS.toMillis(2.0.pow(retryAttempt).toLong())
                        Timber.tag("InterstitialAd")
                            .d("Retrying ad load in $delay ms (attempt $retryAttempt)")
                        Handler(Looper.getMainLooper()).postDelayed({ loadAdIfNeeded() }, delay)
                    } else {
                        Timber.tag("InterstitialAd")
                            .e("Exceeded max retry attempts for interstitial ad loading.")
                        // Reset retry counter after 5 minutes to allow future retries
                        Handler(Looper.getMainLooper()).postDelayed({
                            retryAttempt = 0
                            Timber.tag("InterstitialAd").d("Retry counter reset after cooldown")
                        }, 300000) // 5 minutes
                    }
                }
            }
        )
    }

    // Method that accepts a callback to execute after ad is dismissed
    fun showInterstitialAdWithCallback(activity: Activity, onAdDismissed: () -> Unit) {
        Timber.tag("InterstitialAd").d("showInterstitialAdWithCallback called")
        Timber.tag("InterstitialAd").d("Activity: ${activity.javaClass.simpleName}")
        Timber.tag("InterstitialAd").d("Activity isFinishing: ${activity.isFinishing}")
        Timber.tag("InterstitialAd").d("Activity isDestroyed: ${activity.isDestroyed}")
        Timber.tag("InterstitialAd")
            .d("Ad state manager showing: ${adStateManager.isFullScreenAdShowing()}")
        Timber.tag("InterstitialAd").d("Interstitial ad object: $interstitialAd")

        // CRITICAL: Check if device is safe for full screen ads
        if (!adStateManager.isSafeForFullScreenAds()) {
            Timber.tag("InterstitialAd").w("Skipping ad on problematic device: ${Build.MANUFACTURER} ${Build.MODEL}")
            try {
                FirebaseCrashlytics.getInstance()
                    .log("Interstitial: Skipped on ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.SDK_INT})")
            } catch (_: Exception) {
            }
            onAdDismissed()
            return
        }

        // CRITICAL: Validate activity state before showing ad to prevent crashes
        if (activity.isFinishing || activity.isDestroyed) {
            Timber.tag("InterstitialAd").w("Cannot show ad - activity is finishing or destroyed")
            onAdDismissed()
            return
        }

        // Validate that activity has a valid window and decor view
        try {
            val window = activity.window
            if (window == null) {
                Timber.tag("InterstitialAd").w("Cannot show ad - activity window is null")
                onAdDismissed()
                return
            }

            val decorView = window.decorView
            if (decorView == null || !decorView.isAttachedToWindow) {
                Timber.tag("InterstitialAd").w("Cannot show ad - decor view not attached to window")
                onAdDismissed()
                return
            }
        } catch (e: Exception) {
            Timber.tag("InterstitialAd").e("Error validating activity window state: ${e.message}")
            onAdDismissed()
            return
        }

        // Check MobileAds initialization status
        try {
            val initStatus = com.google.android.gms.ads.MobileAds.getInitializationStatus()
            Timber.tag("InterstitialAd")
                .d("MobileAds initialization status: ${initStatus?.adapterStatusMap}")
        } catch (e: Exception) {
            Timber.tag("InterstitialAd").e("Error checking MobileAds init status: ${e.message}")
        }

        // Check consent status before showing ad
        if (!canShowAdsWithConsent(activity)) {
            Timber.tag("InterstitialAd").w("Cannot show ad - consent not granted or not available")
            onAdDismissed()
            return
        }

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
            loadAdIfNeeded() // Attempt to load a new ad if the current one is null
            onAdDismissed() // Execute callback immediately since we can't show an ad
            return
        }

        // Set a new callback that will trigger our navigation callback when ad is dismissed
        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Timber.tag("InterstitialAd").d("Ad showed full screen content")
                    adStateManager.setFullScreenAdShowing(true)
                }

                override fun onAdDismissedFullScreenContent() {
                    Timber.tag("InterstitialAd").d("Ad dismissed full screen content")
                    adStateManager.setFullScreenAdShowing(false)

                    interstitialAd = null
                    _isAdReady.value = false
                    retryAttempt = 0 // Reset retry counter after successful show
                    loadAdIfNeeded() // Preload next ad immediately

                    // Execute the provided callback when ad is dismissed
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Timber.tag("InterstitialAd").e("Failed to show ad: ${error.message} (code: ${error.code})")
                    adStateManager.setFullScreenAdShowing(false)

                    interstitialAd = null
                    _isAdReady.value = false
                    loadAdIfNeeded() // Try to preload next ad

                    // Execute the provided callback when ad fails to show
                    onAdDismissed()
                }
            }

        Timber.tag("InterstitialAd").d("Showing interstitial ad with callback")
        Timber.tag("InterstitialAd").d("About to call ad.show() with activity: $activity")
        try {
            FirebaseCrashlytics.getInstance().log("Interstitial: show with callback")
        } catch (_: Exception) {
        }

        try {
            Timber.tag("InterstitialAd").d("Calling ad.show(activity) now...")
            ad.show(activity)
            Timber.tag("InterstitialAd").d("ad.show(activity) called successfully")
        } catch (e: Exception) {
            Timber.tag("InterstitialAd")
                .e("Exception showing interstitial ad with callback: ${e.message}")
            Timber.tag("InterstitialAd")
                .e("Exception stack trace: ${e.stackTrace.joinToString("\n")}")
            try {
                FirebaseCrashlytics.getInstance().recordException(e)
                FirebaseCrashlytics.getInstance()
                    .log("Interstitial with callback: show exception - ${e.message}")
            } catch (_: Exception) {
            }
            // Clean up state
            adStateManager.setFullScreenAdShowing(false)
            interstitialAd = null
            _isAdReady.value = false
            // Execute callback and try to load a new ad
            onAdDismissed()
            loadAdIfNeeded()
        }
    }

    fun isCurrentlyLoading(): Boolean = isAdLoading
}

@Singleton
class RewardedAdManager
@Inject
constructor(private val context: Context, private val adStateManager: AdStateManager) {
    private var rewardedAd: RewardedAd? = null

    // Lazy load ad unit ID only when actually needed, not during construction
    private val adUnitId: String by lazy { context.getString(R.string.reward) }

    // Track ad loading state
    private var isAdLoading = false

    // Retry logic for failed loads
    private var retryAttempt = 0
    private val maxRetries = 5

    // Reactive state for ad readiness
    private val _isAdReady = MutableStateFlow(false)
    val isAdReady: StateFlow<Boolean> = _isAdReady.asStateFlow()

    // Track current activity context for loading ads
    private var currentActivityContext: Activity? = null

    // Flag to control whether to use Activity context for ad loading
    private var useActivityContextForLoading = true

    // Method to set the current activity context
    fun setActivityContext(activity: Activity?) {
        currentActivityContext = activity
        // Preload ad when activity context is set (important for reducing latency)
        if (activity != null && rewardedAd == null && !isAdLoading) {
            loadRewardedAd()
        }
    }

    // Method to configure whether to use activity context
    fun useActivityContextForAdLoading(enable: Boolean) {
        useActivityContextForLoading = enable
    }

    // Method to request ad loading if needed
    fun loadAdIfNeeded() {
        if (rewardedAd == null && !isAdLoading && currentActivityContext != null) {
            loadRewardedAd()
        }
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
        val activity = currentActivityContext
        if (activity == null) {
            Timber.tag("RewardedAd").w("No active activity context; deferring rewarded load")
            return
        }

        if (activity.isFinishing || activity.isDestroyed) {
            Timber.tag("RewardedAd")
                .w("Activity is finishing or destroyed; deferring rewarded load")
            return
        }

        val contextToUse: Context = activity

        isAdLoading = true
        Timber.tag("RewardedAd").d("Starting to load rewarded ad...")
        try {
            FirebaseCrashlytics.getInstance().log("Rewarded: load start")
        } catch (_: Exception) {
        }

        RewardedAd.load(
            contextToUse,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Timber.tag("RewardedAd").d("Rewarded ad loaded successfully")
                    try {
                        FirebaseCrashlytics.getInstance().log("Rewarded: onAdLoaded")
                    } catch (_: Exception) {
                    }
                    rewardedAd = ad
                    _isAdReady.value = true
                    isAdLoading = false
                    retryAttempt = 0 // Reset retry counter on success
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Timber.tag("RewardedAd")
                        .e("Failed to load rewarded ad: ${error.message} (code: ${error.code})")
                    try {
                        FirebaseCrashlytics.getInstance()
                            .log("Rewarded: onAdFailedToLoad ${error.code}")
                    } catch (_: Exception) {
                    }
                    rewardedAd = null
                    _isAdReady.value = false
                    isAdLoading = false
                    retryAttempt++

                    // Exponential backoff retry (2s, 4s, 8s, 16s, 32s)
                    if (retryAttempt <= maxRetries) {
                        val delay = TimeUnit.SECONDS.toMillis(2.0.pow(retryAttempt).toLong())
                        Timber.tag("RewardedAd")
                            .d("Retrying rewarded ad load in $delay ms (attempt $retryAttempt)")
                        Handler(Looper.getMainLooper()).postDelayed({ loadAdIfNeeded() }, delay)
                    } else {
                        Timber.tag("RewardedAd")
                            .e("Exceeded max retry attempts for rewarded ad loading.")
                        // Reset retry counter after some time to allow future retries
                        Handler(Looper.getMainLooper()).postDelayed({
                            retryAttempt = 0
                        }, 300000) // Reset after 5 minutes
                    }
                }
            }
        )
    }

    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: (rewardItem: RewardItem) -> Unit,
        onFailure: () -> Unit
    ) {
        Timber.tag("RewardedAd").d("showRewardedAd called")
        Timber.tag("RewardedAd").d("Activity: ${activity.javaClass.simpleName}")
        Timber.tag("RewardedAd").d("Activity isFinishing: ${activity.isFinishing}")
        Timber.tag("RewardedAd").d("Activity isDestroyed: ${activity.isDestroyed}")
        Timber.tag("RewardedAd")
            .d("Ad state manager showing: ${adStateManager.isFullScreenAdShowing()}")
        Timber.tag("RewardedAd").d("Rewarded ad object: $rewardedAd")

        // CRITICAL: Check if device is safe for full screen ads
        if (!adStateManager.isSafeForFullScreenAds()) {
            Timber.tag("RewardedAd").w("Skipping ad on problematic device: ${Build.MANUFACTURER} ${Build.MODEL}")
            try {
                FirebaseCrashlytics.getInstance()
                    .log("Rewarded: Skipped on ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.SDK_INT})")
            } catch (_: Exception) {
            }
            onFailure()
            return
        }

        // Check MobileAds initialization status
        try {
            val initStatus = com.google.android.gms.ads.MobileAds.getInitializationStatus()
            Timber.tag("RewardedAd")
                .d("MobileAds initialization status: ${initStatus?.adapterStatusMap}")
        } catch (e: Exception) {
            Timber.tag("RewardedAd").e("Error checking MobileAds init status: ${e.message}")
        }

        // CRITICAL: Validate activity state before showing ad to prevent crashes
        if (activity.isFinishing || activity.isDestroyed) {
            Timber.tag("RewardedAd").w("Cannot show ad - activity is finishing or destroyed")
            onFailure()
            return
        }

        // Check consent status before showing ad
        if (!canShowAdsWithConsent(activity)) {
            Timber.tag("RewardedAd").w("Cannot show ad - consent not granted or not available")
            onFailure()
            return
        }

        // Don't show if another full screen ad is showing
        if (adStateManager.isFullScreenAdShowing()) {
            Timber.tag("RewardedAd")
                .d("Skipped showing ad because another full screen ad is already showing")
            try {
                FirebaseCrashlytics.getInstance().log("Rewarded: show skipped - another ad showing")
            } catch (_: Exception) {
            }
            onFailure()
            return
        }

        if (rewardedAd == null) {
            Timber.tag("RewardedAd").e("Attempted to show rewarded ad, but ad was null")
            try {
                FirebaseCrashlytics.getInstance().log("Rewarded: show requested but ad=null")
            } catch (_: Exception) {
            }
            loadRewardedAd()
            onFailure()
            return
        }

        Timber.tag("RewardedAd").d("Attempting to show rewarded ad now...")
        try {
            FirebaseCrashlytics.getInstance().log("Rewarded: show")
        } catch (_: Exception) {
        }

        // Set up proper edge-to-edge handling for rewarded ads
        rewardedAd?.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Timber.tag("RewardedAd").d("Rewarded ad showed full screen content")
                    try {
                        FirebaseCrashlytics.getInstance()
                            .log("Rewarded: onAdShowedFullScreenContent")
                    } catch (_: Exception) {
                    }
                    adStateManager.setFullScreenAdShowing(true)
                }

                override fun onAdDismissedFullScreenContent() {
                    Timber.tag("RewardedAd").d("Rewarded ad dismissed full screen content")
                    try {
                        FirebaseCrashlytics.getInstance()
                            .log("Rewarded: onAdDismissedFullScreenContent")
                    } catch (_: Exception) {
                    }
                    adStateManager.setFullScreenAdShowing(false)

                    rewardedAd = null
                    _isAdReady.value = false
                    retryAttempt = 0 // Reset retry counter
                    loadAdIfNeeded() // Preload next ad immediately
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Timber.tag("RewardedAd")
                        .e("Failed to show rewarded ad: ${error.message}")
                    try {
                        FirebaseCrashlytics.getInstance()
                            .log("Rewarded: onAdFailedToShow ${error.code}")
                    } catch (_: Exception) {
                    }
                    adStateManager.setFullScreenAdShowing(false)

                    rewardedAd = null
                    _isAdReady.value = false
                    loadAdIfNeeded() // Try to preload next ad
                }
            }

        try {
            Timber.tag("RewardedAd").d("Calling rewardedAd.show() now...")
            rewardedAd?.show(activity) { rewardItem ->
                Timber.tag("RewardedAd")
                    .d("User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                try {
                    FirebaseCrashlytics.getInstance()
                        .log(
                            "Rewarded: onUserEarnedReward amount=${rewardItem.amount} type=${rewardItem.type}"
                        )
                } catch (_: Exception) {
                }
                onRewardEarned(rewardItem)
            }
                ?: run {
                    Timber.tag("RewardedAd").e("The rewarded ad wasn't ready yet.")
                    loadAdIfNeeded() // Attempt to load a new ad if the current one is null
                    onFailure()
                }
        } catch (e: Exception) {
            Timber.tag("RewardedAd").e("Exception showing rewarded ad: ${e.message}")
            try {
                FirebaseCrashlytics.getInstance().recordException(e)
                FirebaseCrashlytics.getInstance().log("Rewarded: show exception - ${e.message}")
            } catch (_: Exception) {
            }
            // Clean up state
            adStateManager.setFullScreenAdShowing(false)
            rewardedAd = null
            _isAdReady.value = false
            // Try to load a new ad
            loadAdIfNeeded()
            onFailure()
        }
    }

    fun isAdLoading(): Boolean = this.isAdLoading
}
