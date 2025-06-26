package com.soccertips.predictx.admob

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
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

@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    adUnitId: String = stringResource(R.string.banner_id) // Test banner ID
) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier.fillMaxWidth(),
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
@Inject constructor(private val context: Context) {
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
                        }

                        override fun onAdDismissedFullScreenContent() {
                            Timber.tag("InterstitialAd").d("Ad dismissed full screen content")
                            interstitialAd = null
                            loadInterstitialAd() // Load a new ad for next time
                        }

                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            Timber.tag("InterstitialAd").e("Failed to show ad: ${error.message}")
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
        interstitialAd?.show(activity) ?: run {
            Timber.tag("InterstitialAd").e("The interstitial ad wasn't ready yet.")
            loadInterstitialAd() // Attempt to load a new ad if the current one is null
        }
    }

    fun isAdLoaded(): Boolean =
        interstitialAd != null

}

class RewardedAdManager @Inject constructor(private val context: Context) {
    private var rewardedAd: RewardedAd? = null
    private val adUnitId = context.getString(R.string.reward) // Test rewarded ad ID

    init {
        loadRewardedAd()
    }

    fun loadRewardedAd() {
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
                        }

                        override fun onAdDismissedFullScreenContent() {
                            Timber.tag("RewardedAd").d("Rewarded ad dismissed full screen content")
                            rewardedAd = null
                            loadRewardedAd() // Load a new ad for next time
                        }

                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            Timber.tag("RewardedAd")
                                .e("Failed to show rewarded ad: ${error.message}")
                            rewardedAd = null
                        }
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Timber.tag("RewardedAd").e("Failed to load rewarded ad: ${error.message}")
                    rewardedAd = null
                }
            }
        )
    }

    fun showRewardedAd(activity: Activity, onRewardEarned: (rewardItem: RewardItem) -> Unit) {
        rewardedAd?.show(activity) { rewardItem ->
            Timber.tag("RewardedAd")
                .d("User earned reward: ${rewardItem.amount} ${rewardItem.type}")
            onRewardEarned(rewardItem)
        } ?: run {
            Timber.tag("RewardedAd").e("The rewarded ad wasn't ready yet.")
            loadRewardedAd() // Attempt to load a new ad if the current one is null
        }
    }

    fun isAdLoaded(): Boolean =
        rewardedAd != null
}

//AppOpenAd
class AppOpenAdManager @Inject constructor(private val context: Context) {
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
        return showAdOnAppStart && !isFirstLaunch && isAdAvailable() && canShowAdBasedOnInterval()
    }

    fun shouldShowAdOnAppResume(): Boolean {
        return showAdOnAppResume && wasAppInBackground && isAdAvailable() && canShowAdBasedOnInterval()
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
                            appOpenAd = null
                            loadAppOpenAd() // Load next ad
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Timber.tag("AppOpenAd")
                                .d("App open ad failed to show: ${adError.message}")
                            adFailureListener?.invoke(adError.message)
                            appOpenAd = null
                            loadAppOpenAd()
                        }

                        override fun onAdShowedFullScreenContent() {
                            Timber.tag("AppOpenAd").d("App open ad showed")
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
                appOpenAd = null
                onShowAdCompleteListener()
                loadAppOpenAd() // Load next ad
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Timber.tag("AppOpenAd").d("App open ad failed to show: ${adError.message}")
                adFailureListener?.invoke(adError.message)
                appOpenAd = null
                onShowAdCompleteListener()
                loadAppOpenAd()
            }

            override fun onAdShowedFullScreenContent() {
                Timber.tag("AppOpenAd").d("App open ad showed")
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