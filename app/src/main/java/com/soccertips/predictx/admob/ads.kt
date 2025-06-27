package com.soccertips.predictx.admob

import android.app.Activity
import android.content.Context
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
import com.soccertips.predictx.R
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

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
        modifier = modifier
            .fillMaxWidth()
            // Add WindowInsets.navigationBars padding to avoid overlap with navigation buttons
            .windowInsetsPadding(
                androidx.compose.foundation.layout.WindowInsets.navigationBars
            ),
        factory = { factoryContext ->
            AdView(factoryContext).apply {
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, 360))
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
@Inject constructor(
    private val context: Context,
    private val adStateManager: AdStateManager
) {
    private var interstitialAd: InterstitialAd? = null
    private val adUnitId = context.getString(R.string.interstitial_id) // Test interstitial ID

    init {
        loadInterstitialAd()
    }

    fun loadInterstitialAd() {
        InterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Timber.tag("InterstitialAd").d("Ad loaded successfully")
                    interstitialAd = ad

                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdShowedFullScreenContent() {
                            Timber.tag("InterstitialAd").d("Ad showed full screen content")
                            adStateManager.setFullScreenAdShowing(true)
                        }

                        override fun onAdDismissedFullScreenContent() {
                            Timber.tag("InterstitialAd").d("Ad dismissed full screen content")
                            adStateManager.setFullScreenAdShowing(false)
                            interstitialAd = null
                            loadInterstitialAd() // Load a new ad for next time
                        }

                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            Timber.tag("InterstitialAd").e("Failed to show ad: ${error.message}")
                            adStateManager.setFullScreenAdShowing(false)
                            interstitialAd = null
                        }
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Timber.tag("InterstitialAd").e("Failed to load ad: ${error.message}")
                    interstitialAd = null
                }
            }
        )
    }

    fun showInterstitialAd(activity: Activity) {
        // Don't show if another full screen ad is showing
        if (adStateManager.isFullScreenAdShowing()) {
            Timber.tag("InterstitialAd").d("Skipped showing ad because another full screen ad is already showing")
            return
        }

        interstitialAd?.show(activity) ?: run {
            Timber.tag("InterstitialAd").e("The interstitial ad wasn't ready yet.")
            loadInterstitialAd() // Attempt to load a new ad if the current one is null
        }
    }

    // New method that accepts a callback to execute after ad is dismissed
    fun showInterstitialAdWithCallback(activity: Activity, onAdDismissed: () -> Unit) {
        // Don't show if another full screen ad is showing
        if (adStateManager.isFullScreenAdShowing()) {
            Timber.tag("InterstitialAd").d("Skipped showing ad because another full screen ad is already showing")
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
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Timber.tag("InterstitialAd").d("Ad showed full screen content")
                adStateManager.setFullScreenAdShowing(true)
            }

            override fun onAdDismissedFullScreenContent() {
                Timber.tag("InterstitialAd").d("Ad dismissed full screen content")
                adStateManager.setFullScreenAdShowing(false)
                interstitialAd = null
                loadInterstitialAd() // Load a new ad for next time

                // Execute the provided callback when ad is dismissed
                onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Timber.tag("InterstitialAd").e("Failed to show ad: ${error.message}")
                adStateManager.setFullScreenAdShowing(false)
                interstitialAd = null

                // Execute the provided callback when ad fails to show
                onAdDismissed()
            }
        }

        Timber.tag("InterstitialAd").d("Showing interstitial ad with callback")
        ad.show(activity)
    }

    fun isAdLoaded(): Boolean =
        interstitialAd != null

}

class RewardedAdManager @Inject constructor(
    private val context: Context,
    private val adStateManager: AdStateManager
) {
    private var rewardedAd: RewardedAd? = null
    private val adUnitId = context.getString(R.string.reward) // Test rewarded ad ID

    init {
        loadRewardedAd()
    }

    fun loadRewardedAd() {
        Timber.tag("RewardedAd").d("Starting to load rewarded ad...")
        RewardedAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Timber.tag("RewardedAd").d("Rewarded ad loaded successfully")
                    rewardedAd = ad

                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdShowedFullScreenContent() {
                            Timber.tag("RewardedAd").d("Rewarded ad showed full screen content")
                            adStateManager.setFullScreenAdShowing(true)
                        }

                        override fun onAdDismissedFullScreenContent() {
                            Timber.tag("RewardedAd").d("Rewarded ad dismissed full screen content")
                            adStateManager.setFullScreenAdShowing(false)
                            rewardedAd = null
                            loadRewardedAd() // Load a new ad for next time
                        }

                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            Timber.tag("RewardedAd")
                                .e("Failed to show rewarded ad: ${error.message}")
                            adStateManager.setFullScreenAdShowing(false)
                            rewardedAd = null
                        }
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Timber.tag("RewardedAd").e("Failed to load rewarded ad: ${error.message}")
                    rewardedAd = null
                    // Try to reload after a delay
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        loadRewardedAd()
                    }, 60000) // Retry after 1 minute
                }
            }
        )
    }

    fun showRewardedAd(activity: Activity, onRewardEarned: (rewardItem: RewardItem) -> Unit) {
        // Don't show if another full screen ad is showing
        if (adStateManager.isFullScreenAdShowing()) {
            Timber.tag("RewardedAd").d("Skipped showing ad because another full screen ad is already showing")
            return
        }

        if (rewardedAd == null) {
            Timber.tag("RewardedAd").e("Attempted to show rewarded ad, but ad was null")
            loadRewardedAd()
            return
        }

        Timber.tag("RewardedAd").d("Attempting to show rewarded ad now...")
        try {
            rewardedAd?.show(activity) { rewardItem ->
                Timber.tag("RewardedAd")
                    .d("User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                onRewardEarned(rewardItem)
            } ?: run {
                Timber.tag("RewardedAd").e("The rewarded ad wasn't ready yet.")
                loadRewardedAd() // Attempt to load a new ad if the current one is null
            }
        } catch (e: Exception) {
            Timber.tag("RewardedAd").e("Exception when showing rewarded ad: ${e.message}")
            e.printStackTrace()
            loadRewardedAd()
        }
    }

    fun isAdLoaded(): Boolean {
        val isLoaded = rewardedAd != null
        Timber.tag("RewardedAd").d("isAdLoaded() check returned: $isLoaded")
        return isLoaded
    }
}

//AppOpenAd
class AppOpenAdManager @Inject constructor(
    private val context: Context,
    private val adStateManager: AdStateManager
) {
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var loadTime: Long = 0
    private val adUnitId = context.getString(R.string.appOpen_id) // Test App Open ID

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

    init {
        loadAppOpenAd()
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
        return showAdOnAppStart && !isFirstLaunch && isAdAvailable() &&
               canShowAdBasedOnInterval() && !adStateManager.isFullScreenAdShowing()
    }

    fun shouldShowAdOnAppResume(): Boolean {
        return showAdOnAppResume && wasAppInBackground && isAdAvailable() &&
               canShowAdBasedOnInterval() && !adStateManager.isFullScreenAdShowing()
    }

    private fun canShowAdBasedOnInterval(): Boolean {
        val currentTime = System.currentTimeMillis()
        return currentTime - lastAdDisplayTime >= MIN_AD_DISPLAY_INTERVAL
    }

    fun loadAppOpenAd() {
        if (isLoadingAd || isAdAvailable()) {
            return
        }

        isLoadingAd = true
        val request = AdRequest.Builder().build()

        AppOpenAd.load(
            context,
            adUnitId,
            request,
            object : AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Timber.tag("AppOpenAd").d("App open ad loaded")
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = System.currentTimeMillis()

                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            Timber.tag("AppOpenAd").d("App open ad dismissed")
                            adStateManager.setFullScreenAdShowing(false)
                            appOpenAd = null
                            loadAppOpenAd() // Load next ad
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Timber.tag("AppOpenAd")
                                .d("App open ad failed to show: ${adError.message}")
                            adStateManager.setFullScreenAdShowing(false)
                            adFailureListener?.invoke(adError.message)
                            appOpenAd = null
                            loadAppOpenAd()
                        }

                        override fun onAdShowedFullScreenContent() {
                            Timber.tag("AppOpenAd").d("App open ad showed")
                            adStateManager.setFullScreenAdShowing(true)
                            lastAdDisplayTime = System.currentTimeMillis()
                            wasAppInBackground = false
                            adImpressionListener?.invoke()
                        }

                        override fun onAdImpression() {
                            Timber.tag("AppOpenAd").d("App open ad impression recorded")
                            adImpressionListener?.invoke()
                        }
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Timber.tag("AppOpenAd").d("App open ad failed to load: ${loadAdError.message}")
                    adFailureListener?.invoke(loadAdError.message)
                    isLoadingAd = false

                    // Retry loading after a delay if error is retryable
                    if (loadAdError.code != 2 /* NETWORK_ERROR */ &&
                                            loadAdError.code != 3 /* NO_FILL */) {
                        scheduleAdLoadRetry()
                    }
                }
            }
        )
    }

    private fun scheduleAdLoadRetry() {
        // Retry loading the ad after a delay
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (!isAdAvailable() && !isLoadingAd) {
                loadAppOpenAd()
            }
        }, 60000) // Retry after 1 minute
    }

    fun showAdIfAvailable(activity: Activity, onShowAdCompleteListener: () -> Unit) {
        // Check if any other full screen ad is currently showing
        if (adStateManager.isFullScreenAdShowing()) {
            Timber.tag("AppOpenAd").d("Skipped showing app open ad because another full screen ad is already showing")
            onShowAdCompleteListener()
            return
        }

        if (!isAdAvailable()) {
            Timber.tag("AppOpenAd").d("App open ad not available")
            onShowAdCompleteListener()
            loadAppOpenAd()
            return
        }

        // Don't show ad if app is in background
        if (!isAppInForeground) {
            Timber.tag("AppOpenAd").d("App is in background, not showing ad")
            onShowAdCompleteListener()
            return
        }

        // Set callback to be triggered after ad is dismissed
        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Timber.tag("AppOpenAd").d("App open ad dismissed")
                adStateManager.setFullScreenAdShowing(false)
                appOpenAd = null
                onShowAdCompleteListener()
                loadAppOpenAd() // Load next ad
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Timber.tag("AppOpenAd").d("App open ad failed to show: ${adError.message}")
                adStateManager.setFullScreenAdShowing(false)
                adFailureListener?.invoke(adError.message)
                appOpenAd = null
                onShowAdCompleteListener()
                loadAppOpenAd()
            }

            override fun onAdShowedFullScreenContent() {
                Timber.tag("AppOpenAd").d("App open ad showed")
                adStateManager.setFullScreenAdShowing(true)
                lastAdDisplayTime = System.currentTimeMillis()
                wasAppInBackground = false
                adImpressionListener?.invoke()
            }

            override fun onAdImpression() {
                Timber.tag("AppOpenAd").d("App open ad impression recorded")
                adImpressionListener?.invoke()
            }
        }

        Timber.tag("AppOpenAd").d("Showing app open ad")
        appOpenAd?.show(activity)
    }

    fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo()
    }

    private fun wasLoadTimeLessThanNHoursAgo(): Boolean {
        val dateDifference = System.currentTimeMillis() - loadTime
        return dateDifference < AD_TIMEOUT
    }
}