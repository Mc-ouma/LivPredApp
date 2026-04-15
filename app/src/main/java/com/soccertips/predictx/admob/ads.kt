package com.soccertips.predictx.admob

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.soccertips.predictx.R
import com.soccertips.predictx.ui.theme.LocalCardColors
import com.soccertips.predictx.ui.theme.LocalCardElevation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import androidx.core.graphics.toColorInt
import com.google.android.libraries.ads.mobile.sdk.MobileAds.Companion.getInitializationStatus
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.banner.BannerRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdChoicesPlacement
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.VideoOptions
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.google.android.ump.UserMessagingPlatform

// Helper function to check consent status efficiently
private fun canShowAdsWithConsent(activity: Activity): Boolean {
    return try {
        val consentInfo = UserMessagingPlatform.getConsentInformation(activity)
        val canRequest = consentInfo.canRequestAds()
        Timber.d("Consent check: canRequestAds = $canRequest, status = ${consentInfo.consentStatus}")
        canRequest
    } catch (e: Exception) {
        Timber.e("Error checking consent: ${e.message}")
        // Default to false for safety - don't show ads if we can't verify consent
        false
    }
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is android.content.ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
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
        Timber.tag("AdSafety")
            .d("Device on Android ${Build.VERSION.SDK_INT} - monitoring for issues")
    }

    return false
}

// Global singleton to track ad state across different ad types
@Singleton
class AdStateManager @Inject constructor() {
    // Tracks if any full screen ad is currently showing
    private var isAnyFullScreenAdShowing = false

    // Tracks subscription status - when true, all ads are suppressed
    private var _isSubscribed = false

    fun setSubscribed(subscribed: Boolean) {
        _isSubscribed = subscribed
        Timber.tag("AdStateManager").d("Subscription state: $subscribed")
    }

    fun isSubscribed(): Boolean = _isSubscribed

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

    // Check if ads should be shown (not subscribed and device is safe)
    fun shouldShowAds(): Boolean {
        return !_isSubscribed
    }
}

/**
 * Checks if the user is subscribed by reading cached preference.
 * Used by ad composables to skip rendering when user has active subscription.
 */
@Composable
private fun isUserSubscribed(): Boolean {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    return prefs.getBoolean("is_subscribed", false)
}

/**
 * Compact upgrade prompt shown as fallback when banner ads fail to load.
 */
@Composable
fun UpgradePromptBanner(
    modifier: Modifier = Modifier,
    onUpgradeClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onUpgradeClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Star,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.upgrade_banner_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(R.string.upgrade_banner_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onUpgradeClick,
                contentPadding = ButtonDefaults.ContentPadding
            ) {
                Text(
                    text = stringResource(R.string.upgrade_button),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    adUnitId: String = stringResource(R.string.banner_id),
    onUpgradeClick: (() -> Unit)? = null
) {
    if (isUserSubscribed()) return
    val context = LocalContext.current
    val activity = context.findActivity()
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    var adFailed by remember { mutableStateOf(false) }
    var adViewRef by remember { mutableStateOf<AdView?>(null) }

    val adSize = remember(screenWidthDp) {
        AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, screenWidthDp)
    }

    DisposableEffect(Unit) {
        onDispose {
            adViewRef?.destroy()
        }
    }

    if (adFailed && onUpgradeClick != null) {
        UpgradePromptBanner(modifier = modifier, onUpgradeClick = onUpgradeClick)
    } else {
        AndroidView(
            modifier =
                modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.navigationBars
                    ),
            factory = { factoryContext ->
                AdView(factoryContext).apply {
                    adViewRef = this
                    val request = BannerAdRequest.Builder(adUnitId, adSize).build()
                    loadAd(
                        request,
                        object : AdLoadCallback<BannerAd> {
                            override fun onAdLoaded(ad: BannerAd) {
                                activity?.let { registerBannerAd(ad, it) }
                            }

                            override fun onAdFailedToLoad(adError: LoadAdError) {
                                Timber.e("Banner ad failed to load: ${adError.message}")
                                adFailed = true
                            }
                        }
                    )
                }
            },
            update = { view ->
                adViewRef = view
            }
        )
    }
}

/**
 * Collapsible Banner Ad that starts expanded and can be collapsed by the user.
 * The banner will automatically expand when the ad loads and collapse when closed.
 *
 * @param modifier Modifier for the composable
 * @param adUnitId The AdMob ad unit ID for the collapsible banner
 * @param collapsiblePosition Position of the collapsible banner - "bottom" or "top"
 */
@Composable
fun CollapsibleBannerAdView(
    modifier: Modifier = Modifier,
    adUnitId: String = stringResource(R.string.banner_id),
    collapsiblePosition: String = "bottom",
    onUpgradeClick: (() -> Unit)? = null
) {
    if (isUserSubscribed()) return
    val context = LocalContext.current
    val activity = context.findActivity()

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    var adFailed by remember { mutableStateOf(false) }
    var adViewRef by remember { mutableStateOf<AdView?>(null) }

    val adSize = remember(screenWidthDp) {
        AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, screenWidthDp)
    }

    DisposableEffect(Unit) {
        onDispose {
            adViewRef?.destroy()
        }
    }

    when {
        adFailed && onUpgradeClick != null -> {
            UpgradePromptBanner(modifier = modifier, onUpgradeClick = onUpgradeClick)
        }

        else -> {
            AndroidView(
                modifier = modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                factory = { factoryContext ->
                    AdView(factoryContext).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        adViewRef = this
                        val extras = Bundle().apply {
                            putString("collapsible", collapsiblePosition)
                        }
                        val request = BannerAdRequest.Builder(adUnitId, adSize)
                            .setGoogleExtrasBundle(extras)
                            .build()

                        loadAd(
                            request,
                            object : AdLoadCallback<BannerAd> {
                                override fun onAdLoaded(ad: BannerAd) {
                                    Timber.d("Collapsible banner loaded. isCollapsible=${ad.isCollapsible()}")
                                    activity?.let { registerBannerAd(ad, it) }
                                }

                                override fun onAdFailedToLoad(adError: LoadAdError) {
                                    Timber.e("Collapsible banner failed: ${adError.message}")
                                    adFailed = true
                                }
                            }
                        )
                    }
                },
                update = { view ->
                    adViewRef = view
                }
            )
        }
    }
}

@Composable
fun InlineBannerAdView(
    modifier: Modifier = Modifier,
    adUnitId: String = stringResource(R.string.banner_id),
    onUpgradeClick: (() -> Unit)? = null
) {
    if (isUserSubscribed()) return
    val context = LocalContext.current
    val activity = context.findActivity()

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    var adFailed by remember { mutableStateOf(false) }
    var adViewRef by remember { mutableStateOf<AdView?>(null) }

    val adSize = remember(screenWidthDp) {
        AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(context, screenWidthDp)
    }

    DisposableEffect(Unit) {
        onDispose {
            adViewRef?.destroy()
        }
    }

    if (adFailed && onUpgradeClick != null) {
        UpgradePromptBanner(modifier = modifier, onUpgradeClick = onUpgradeClick)
    } else {
        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { factoryContext ->
                AdView(factoryContext).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    adViewRef = this

                    val request = BannerAdRequest.Builder(adUnitId, adSize).build()

                    loadAd(
                        request,
                        object : AdLoadCallback<BannerAd> {
                            override fun onAdLoaded(ad: BannerAd) {
                                Timber.d("Inline banner loaded")

                                ad.adEventCallback = object : BannerAdEventCallback {
                                    override fun onAdImpression() {
                                        Timber.d("Inline banner recorded an impression")
                                    }

                                    override fun onAdClicked() {
                                        Timber.d("Inline banner clicked")
                                    }

                                    override fun onAdShowedFullScreenContent() {
                                        Timber.d("Inline banner showed full screen content")
                                    }

                                    override fun onAdDismissedFullScreenContent() {
                                        Timber.d("Inline banner dismissed full screen content")
                                    }

                                    override fun onAdFailedToShowFullScreenContent(
                                        fullScreenContentError: FullScreenContentError
                                    ) {
                                        Timber.w("Inline banner failed to show: $fullScreenContentError")
                                    }
                                }

                                activity?.let { registerBannerAd(ad, it) }
                            }

                            override fun onAdFailedToLoad(adError: LoadAdError) {
                                Timber.e("Inline banner failed to load: ${adError.message}")
                                adFailed = true
                            }
                        }
                    )
                }
            },
            update = { view ->
                adViewRef = view
            }
        )
    }
}

/**
 * Native Ad View Composable for displaying native ads in lists.
 * Styled to match the ItemCard design for a seamless user experience.
 *
 * @param modifier Modifier for the composable
 * @param adUnitId The AdMob ad unit ID for the native ad
 */
@Composable
fun NativeAdItem(
    modifier: Modifier = Modifier,
    adUnitId: String = stringResource(R.string.native_id)
) {
    if (isUserSubscribed()) return
    val nativeTypes = remember { listOf(NativeAd.NativeAdType.NATIVE) }
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    // Load native ad
    DisposableEffect(adUnitId) {
        val request = NativeAdRequest.Builder(adUnitId, nativeTypes)
            .setAdChoicesPlacement(AdChoicesPlacement.TOP_RIGHT)
            .setMediaAspectRatio(NativeAd.NativeMediaAspectRatio.LANDSCAPE)
            .setVideoOptions(
                VideoOptions.Builder()
                    .setStartMuted(true)
                    .setClickToExpandRequested(true)
                    .build()
            )
            .build()

        NativeAdLoader.load(
            request,
            object : NativeAdLoaderCallback {
                override fun onNativeAdLoaded(ad: NativeAd) {
                    nativeAd?.destroy()
                    nativeAd = ad.apply {
                        adEventCallback =
                            object :
                                com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback {
                                override fun onAdImpression() {
                                    Timber.d("Native ad impression recorded")
                                }

                                override fun onAdClicked() {
                                    Timber.d("Native ad clicked")
                                }
                            }
                    }
                    Timber.d("Native ad loaded successfully")
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Timber.e("Native ad failed to load: ${adError.message}")
                    nativeAd = null
                }
            }
        )

        onDispose {
            nativeAd?.destroy()
            nativeAd = null
        }
    }

    // Only show the ad view when the ad is loaded
    if (nativeAd != null) {
        NativeAdContent(
            nativeAd = nativeAd!!,
            modifier = modifier
        )
    }
}

/**
 * Compose-based native ad content styled to match ItemCard design.
 */
@Composable
private fun NativeAdContent(
    nativeAd: NativeAd,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cardColors = LocalCardColors.current
    val cardElevation = LocalCardElevation.current

    // Get Material theme colors to pass to Android Views
    val primaryContainerColor =
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f).toArgb()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f).toArgb()
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val secondaryContainerColor =
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f).toArgb()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = cardColors,
        elevation = cardElevation,
        shape = MaterialTheme.shapes.medium
    ) {
        // We need AndroidView to wrap content in NativeAdView for proper ad tracking
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                NativeAdView(ctx)
            },
            update = { nativeAdView ->
                // Clear previous views
                nativeAdView.removeAllViews()

                // Create and add the content view styled like ItemCard
                val contentView = createItemCardStyleNativeAd(
                    context = context,
                    nativeAd = nativeAd,
                    nativeAdView = nativeAdView,
                    primaryContainerColor = primaryContainerColor,
                    onSurfaceColor = onSurfaceColor,
                    onSurfaceVariantColor = onSurfaceVariantColor,
                    primaryColor = primaryColor,
                    secondaryContainerColor = secondaryContainerColor
                )
                nativeAdView.addView(contentView)
            }
        )
    }
}

/**
 * Creates native ad content view styled to match ItemCard design.
 */
@SuppressLint("UseKtx", "SetTextI18n")
private fun createItemCardStyleNativeAd(
    context: Context,
    nativeAd: NativeAd,
    nativeAdView: NativeAdView,
    primaryContainerColor: Int,
    onSurfaceColor: Int,
    onSurfaceVariantColor: Int,
    primaryColor: Int,
    secondaryContainerColor: Int
): View {
    val density = context.resources.displayMetrics.density
    fun Int.dp() = (this * density).toInt()
    fun Float.dp() = (this * density)

    // Main container (matches ItemCard Column structure)
    val mainLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    // === HEADER (matches MatchHeader style) ===
    val headerLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        setBackgroundColor(primaryContainerColor)
        setPadding(12.dp(), 8.dp(), 12.dp(), 8.dp())
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    // Ad icon in header (like league logo)
    val iconView = ImageView(context).apply {
        layoutParams = LinearLayout.LayoutParams(24.dp(), 24.dp()).apply {
            marginEnd = 8.dp()
        }
        scaleType = ImageView.ScaleType.CENTER_CROP
    }
    nativeAd.icon?.drawable?.let {
        iconView.setImageDrawable(it)
        headerLayout.addView(iconView)
        nativeAdView.iconView = iconView
    }

    // Advertiser name (like league name)
    val advertiserView = TextView(context).apply {
        text = nativeAd.advertiser ?: "Sponsored"
        textSize = 14f
        setTextColor(onSurfaceColor)
        setTypeface(typeface, Typeface.BOLD)
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )
    }
    headerLayout.addView(advertiserView)
    nativeAd.advertiser?.let { nativeAdView.advertiserView = advertiserView }

    // "Ad" badge (positioned like date in MatchHeader)
    val adBadge = TextView(context).apply {
        text = "Ad"
        textSize = 10f
        setTextColor(Color.WHITE)
        setPadding(6.dp(), 2.dp(), 6.dp(), 2.dp())
        setBackgroundColor(primaryColor)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = 8.dp()
        }
    }
    headerLayout.addView(adBadge)

    mainLayout.addView(headerLayout)

    // === CONTENT SECTION (matches TeamsRow style) ===
    val contentLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(12.dp(), 12.dp(), 12.dp(), 12.dp())
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    // Headline (main title - like team names importance)
    val headlineView = TextView(context).apply {
        text = nativeAd.headline
        textSize = 16f
        setTextColor(onSurfaceColor)
        setTypeface(typeface, Typeface.BOLD)
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }
    contentLayout.addView(headlineView)
    nativeAdView.headlineView = headlineView

    // Body text (description)
    nativeAd.body?.let { body ->
        val bodyView = TextView(context).apply {
            text = body
            textSize = 13f
            setTextColor(onSurfaceVariantColor)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 4.dp()
            }
        }
        contentLayout.addView(bodyView)
        nativeAdView.bodyView = bodyView
    }

    // Star rating
    nativeAd.starRating?.let { rating ->
        val ratingContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 4.dp()
            }
        }

        val intRating = rating.toInt().coerceIn(0, 5)
        val ratingText = TextView(context).apply {
            text = "★".repeat(intRating) + "☆".repeat(5 - intRating)
            textSize = 12f
            setTextColor("#FFC107".toColorInt())
        }
        ratingContainer.addView(ratingText)

        val ratingValue = TextView(context).apply {
            text = " (${String.format("%.1f", rating)})"
            textSize = 12f
            setTextColor(onSurfaceVariantColor)
        }
        ratingContainer.addView(ratingValue)

        contentLayout.addView(ratingContainer)
        nativeAdView.starRatingView = ratingContainer
    }

    mainLayout.addView(contentLayout)

    // === MEDIA VIEW (for images and videos) ===
    val mediaContent = nativeAd.mediaContent
    val mediaView = MediaView(context)
    val aspectRatio = mediaContent.aspectRatio
    val calculatedHeight = if (aspectRatio > 0) {
        ((context.resources.displayMetrics.widthPixels - 24.dp()) / aspectRatio).toInt()
            .coerceIn(100.dp(), 250.dp()) // Min/max bounds
    } else {
        180.dp()
    }

    mediaView.layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        calculatedHeight
    ).apply {
        setMargins(12.dp(), 0, 12.dp(), 12.dp())
    }
    mediaView.imageScaleType = ImageView.ScaleType.FIT_CENTER // Better for varied content
    mediaView.mediaContent = mediaContent
    mainLayout.addView(mediaView)
    nativeAdView.registerNativeAd(nativeAd, mediaView)

    // === FOOTER (matches MatchStatusRow style) ===
    nativeAd.callToAction?.let { cta ->
        val footerLayout = FrameLayout(context).apply {
            setBackgroundColor(secondaryContainerColor)
            setPadding(12.dp(), 8.dp(), 12.dp(), 8.dp())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val ctaButton = TextView(context).apply {
            text = cta.uppercase()
            textSize = 14f
            setTextColor(primaryColor)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }
        footerLayout.addView(ctaButton)
        nativeAdView.callToActionView = ctaButton

        mainLayout.addView(footerLayout)
    }

    return mainLayout
}
/*

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

    // Track last load attempt time for rate limiting
    private var lastLoadAttemptTime = 0L
    private val minLoadIntervalMs = 5000L // Minimum 5 seconds between load attempts

    // Method to set the current activity context
    fun setActivityContext(activity: Activity?) {
        val previousActivity = currentActivityContext
        currentActivityContext = activity

        // Only trigger load if this is a new valid activity and we don't have an ad
        if (activity != null && activity != previousActivity &&
            interstitialAd == null && !isAdLoading
        ) {
            Timber.tag("InterstitialAd").d("New activity context set, triggering ad load")
            loadInterstitialAd()
        }
    }

    // Method to configure whether to use activity context
    fun useActivityContextForAdLoading(enable: Boolean) {
        useActivityContextForLoading = enable
    }

    // Method to request ad loading if needed (called after dismissal or failure)
    fun loadAdIfNeeded() {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastLoad = currentTime - lastLoadAttemptTime

        // Rate limit load attempts to avoid excessive requests
        if (timeSinceLastLoad < minLoadIntervalMs && lastLoadAttemptTime > 0) {
            Timber.tag("InterstitialAd")
                .d("Rate limiting: ${timeSinceLastLoad}ms since last load, waiting...")
            return
        }

        if (interstitialAd == null && !isAdLoading && currentActivityContext != null) {
            loadInterstitialAd()
        }
    }

    // Force load an ad, bypassing rate limiting (use sparingly)
    fun forceLoadAd() {
        if (interstitialAd == null && !isAdLoading && currentActivityContext != null) {
            Timber.tag("InterstitialAd").d("Force loading ad (bypassing rate limit)")
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
            _isAdReady.value = true // Ensure state is synced
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
        lastLoadAttemptTime = System.currentTimeMillis()
        Timber.tag("InterstitialAd").d("Starting to load interstitial ad...")
        try {
            FirebaseCrashlytics.getInstance().log("Interstitial: load start")
        } catch (_: Exception) {
        }

        InterstitialAd.load(
            AdRequest.Builder(adUnitId).build(),
            object : AdLoadCallback<InterstitialAd> {
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

                    ad.adEventCallback = object : InterstitialAdEventCallback {
                        override fun onAdImpression() {
                            Timber.tag("InterstitialAd").d("Ad recorded an impression")
                        }

                        override fun onAdClicked() {
                            Timber.tag("InterstitialAd").d("Ad was clicked")
                        }
                    }
                    */
/*ad.fullScreenContentCallback =
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

                            override fun onAdFailedToShowFullScreenContent(error: LoadAdError) {
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
                        }*//*

                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Timber.tag("InterstitialAd")
                        .e("Failed to load ad: ${error.message} (code: ${error.code})")
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
        // Skip ads for subscribers
        if (adStateManager.isSubscribed()) {
            Timber.tag("InterstitialAd").d("User is subscribed, skipping interstitial ad")
            onAdDismissed()
            return
        }

        Timber.tag("InterstitialAd").d("showInterstitialAdWithCallback called")
        Timber.tag("InterstitialAd").d("Activity: ${activity.javaClass.simpleName}")
        Timber.tag("InterstitialAd").d("Activity isFinishing: ${activity.isFinishing}")
        Timber.tag("InterstitialAd").d("Activity isDestroyed: ${activity.isDestroyed}")
        Timber.tag("InterstitialAd")
            .d("Ad state manager showing: ${adStateManager.isFullScreenAdShowing()}")
        Timber.tag("InterstitialAd").d("Interstitial ad object: $interstitialAd")

        // CRITICAL: Check if device is safe for full screen ads
        if (!adStateManager.isSafeForFullScreenAds()) {
            Timber.tag("InterstitialAd")
                .w("Skipping ad on problematic device: ${Build.MANUFACTURER} ${Build.MODEL}")
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
            val initStatus = getInitializationStatus()
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

        ad.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdImpression() {
                Timber.tag("InterstitialAd").d("Ad recorded an impression")
            }

            override fun onAdClicked() {
                Timber.tag("InterstitialAd").d("Ad was clicked")
            }

            override fun onAdDismissedFullScreenContent() {
                Timber.tag("InterstitialAd").d("Ad dismissed full screen content (from adEventCallback)")

            }
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

                override fun onAdFailedToShowFullScreenContent(error: LoadAdError) {
                    Timber.tag("InterstitialAd")
                        .e("Failed to show ad: ${error.message} (code: ${error.code})")
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
*/
@Singleton
class InterstitialAdManager
@Inject
constructor(
    private val applicationContext: Context,
    private val adStateManager: AdStateManager
) {

    private var interstitialAd: InterstitialAd? = null

    private val adUnitId: String by lazy {
        applicationContext.getString(R.string.interstitial_id)
    }

    private val _isAdReady = MutableStateFlow(false)
    val isAdReady: StateFlow<Boolean> = _isAdReady.asStateFlow()

    private var currentActivityContext: Activity? = null

    private var isAdLoading = false
    private var retryAttempt = 0
    private val maxRetries = 5

    private var lastLoadAttemptTime = 0L
    private val minLoadIntervalMs = 5000L


    fun setActivityContext(activity: Activity?) {
        val previous = currentActivityContext
        currentActivityContext = activity

        if (activity != null && activity != previous &&
            interstitialAd == null && !isAdLoading
        ) {
            Timber.tag("InterstitialAd").d("New activity → load trigger")
            loadAdIfNeeded()
        }
    }

    fun loadAdIfNeeded() {
        val now = System.currentTimeMillis()
        val diff = now - lastLoadAttemptTime

        if (diff < minLoadIntervalMs && lastLoadAttemptTime > 0) {
            Timber.tag("InterstitialAd").d("Rate limited ($diff ms)")
            return
        }

        if (interstitialAd == null && !isAdLoading) {
            loadInterstitialAd()
        }
    }

    fun forceLoadAd() {
        val app = applicationContext as? com.soccertips.predictx.App
        if (app?.isMobileAdsInitialized?.value != true) {
            Timber.tag("InterstitialAd").w("MobileAds not initialized yet, skipping force load")
            return
        }

        if (interstitialAd == null && !isAdLoading) {
            Timber.tag("InterstitialAd").d("Force loading ad")
            loadInterstitialAd()
        }
    }

    private fun loadInterstitialAd() {
        if (isAdLoading) return
        if (interstitialAd != null) {
            _isAdReady.value = true
            return
        }

        val app = applicationContext as? com.soccertips.predictx.App
        if (app?.isMobileAdsInitialized?.value != true) {
            Timber.tag("InterstitialAd").w("MobileAds not initialized yet, skipping load")
            return
        }

        isAdLoading = true
        lastLoadAttemptTime = System.currentTimeMillis()

        Timber.tag("InterstitialAd").d("Loading interstitial...")
        val request = AdRequest.Builder(adUnitId).build()

        InterstitialAd.load(
            request,
            object : AdLoadCallback<InterstitialAd> {

                override fun onAdLoaded(ad: InterstitialAd) {
                    Timber.tag("InterstitialAd").d("Ad loaded")

                    interstitialAd = ad
                    isAdLoading = false
                    retryAttempt = 0
                    _isAdReady.value = true

                    attachCallbacks(ad)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Timber.tag("InterstitialAd")
                        .e("Load failed: ${error.message} (${error.code})")

                    interstitialAd = null
                    isAdLoading = false
                    _isAdReady.value = false

                    retryWithBackoff()
                }
            }
        )
    }

    // ✅ PURE Next-Gen callback system
    private fun attachCallbacks(ad: InterstitialAd) {
        ad.adEventCallback = object : InterstitialAdEventCallback {

            override fun onAdImpression() {
                Timber.tag("InterstitialAd").d("Impression")
            }

            override fun onAdClicked() {
                Timber.tag("InterstitialAd").d("Clicked")
            }

            override fun onAdShowedFullScreenContent() {
                Timber.tag("InterstitialAd").d("Ad showed")
                adStateManager.setFullScreenAdShowing(true)
            }

            override fun onAdDismissedFullScreenContent() {
                Timber.tag("InterstitialAd").d("Ad dismissed")

                adStateManager.setFullScreenAdShowing(false)

                interstitialAd = null
                _isAdReady.value = false
                retryAttempt = 0

                loadAdIfNeeded()
            }

            override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                Timber.tag("InterstitialAd")
                    .e("Show failed: ${error.message}")

                adStateManager.setFullScreenAdShowing(false)

                interstitialAd = null
                _isAdReady.value = false

                loadAdIfNeeded()
            }
        }
    }

    fun showInterstitialAdWithCallback(
        activity: Activity,
        onAdDismissed: () -> Unit
    ) {

        if (adStateManager.isSubscribed()) {
            onAdDismissed()
            return
        }

        if (!adStateManager.isSafeForFullScreenAds()) {
            onAdDismissed()
            return
        }

        if (activity.isFinishing || activity.isDestroyed) {
            onAdDismissed()
            return
        }

        if (!canShowAdsWithConsent(activity)) {
            onAdDismissed()
            return
        }

        if (adStateManager.isFullScreenAdShowing()) {
            onAdDismissed()
            return
        }

        val ad = interstitialAd
        if (ad == null) {
            loadAdIfNeeded()
            onAdDismissed()
            return
        }

        try {
            // 🎯 Wrap callback WITHOUT breaking base lifecycle
            val baseCallback = ad.adEventCallback

            ad.adEventCallback = object : InterstitialAdEventCallback {

                override fun onAdShowedFullScreenContent() {
                    baseCallback?.onAdShowedFullScreenContent()
                }

                override fun onAdDismissedFullScreenContent() {
                    baseCallback?.onAdDismissedFullScreenContent()
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                    baseCallback?.onAdFailedToShowFullScreenContent(error)
                    onAdDismissed()
                }

                override fun onAdImpression() {
                    baseCallback?.onAdImpression()
                }

                override fun onAdClicked() {
                    baseCallback?.onAdClicked()
                }
            }

            Timber.tag("InterstitialAd").d("Showing ad")
            ad.show(activity)

        } catch (e: Exception) {
            Timber.tag("InterstitialAd")
                .e("Show exception: ${e.message}")

            adStateManager.setFullScreenAdShowing(false)

            interstitialAd = null
            _isAdReady.value = false

            onAdDismissed()
            loadAdIfNeeded()
        }
    }

    private fun retryWithBackoff() {
        retryAttempt++

        if (retryAttempt > maxRetries) return

        val delay = (2.0.pow(retryAttempt)).toLong() * 1000

        Handler(Looper.getMainLooper()).postDelayed({
            loadAdIfNeeded()
        }, delay)
    }

    fun isCurrentlyLoading(): Boolean = isAdLoading
}
