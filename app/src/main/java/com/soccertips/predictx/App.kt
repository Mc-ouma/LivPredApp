package com.soccertips.predictx

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import androidx.core.content.edit
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.soccertips.predictx.admob.AppOpenAdManager
import com.soccertips.predictx.notification.NotificationHelper
import com.soccertips.predictx.repository.PredictionRepository
import com.soccertips.predictx.repository.PreloadRepository
import com.soccertips.predictx.util.NetworkTaggingInitializer
import com.soccertips.predictx.util.StartupTimeTracker
import com.soccertips.predictx.util.StrictModeUtil
import dagger.hilt.android.HiltAndroidApp
import java.io.IOException
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import timber.log.Timber

@HiltAndroidApp
class App : Application(), Configuration.Provider, Application.ActivityLifecycleCallbacks {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var preloadRepository: PreloadRepository

    @Inject lateinit var predictionRepository: PredictionRepository

    @Inject lateinit var firebaseRepository: com.soccertips.predictx.repository.FirebaseRepository

    @Inject lateinit var tokenRepository: com.soccertips.predictx.notification.TokenRepository

    @Inject lateinit var apiConfigProvider: com.soccertips.predictx.repository.ApiConfigProvider

    @Inject lateinit var networkTaggingInitializer: NetworkTaggingInitializer

    @Inject lateinit var appOpenAdManager: AppOpenAdManager

    @Inject lateinit var startupTimeTracker: StartupTimeTracker

    private var currentActivity: Activity? = null

    // Track app foreground status
    private var appInForeground = false

    // Flag to avoid showing ads during initial app startup
    private var isInitialAppStart = true

    // Track when Mobile Ads SDK has been initialized
    private var isMobileAdsInitialized = false

    // Consent management
    private lateinit var consentInformation: ConsentInformation

    // Shared preferences key for first launch check
    private val PREFS_NAME = "app_preferences"
    private val KEY_FIRST_LAUNCH = "is_first_launch"
    private val KEY_APP_INITIALIZED = "is_app_initialized"

    // Flag to ensure the first ad load happens only once.
    private var isFirstAdLoadAttempted = false

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()

        // Record app start time for performance tracking
        StartupTimeTracker.recordAppStart()

        // Only essential initialization on main thread
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize network tagging early to prevent socket violations
        networkTaggingInitializer.initialize()

        // Register lifecycle callbacks immediately
        registerActivityLifecycleCallbacks(this)

        // Check app initialization state quickly
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val appInitialized = prefs.getBoolean(KEY_APP_INITIALIZED, false)
        isInitialAppStart = !appInitialized

        if (!appInitialized) {
            prefs.edit { putBoolean(KEY_APP_INITIALIZED, true) }
        }

        // Defer heavy operations to background threads
        CoroutineScope(Dispatchers.IO).launch { initializeInBackground() }

        // Start Mobile Ads initialization on IO thread, setup on Main
        CoroutineScope(Dispatchers.IO).launch { initializeMobileAds() }
    }

    private suspend fun initializeInBackground() {
        withContext(Dispatchers.IO) {
            // Initialize non-critical components in background
            NotificationHelper.createNotificationChannels(this@App)

            // Set up prediction repository dependency
            preloadRepository.setPredictionRepository(predictionRepository)

            // Initialize API config
            initApiConfig()

            // Initialize Firebase messaging with delay
            delay(1000) // Let UI start first
            initFirebaseMessaging()

            // StrictMode for debug builds
            if (BuildConfig.DEBUG) {
                withContext(Dispatchers.Main) {
                    StrictModeUtil.enableStrictModeForIntentViolations()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        StrictMode.setVmPolicy(
                                StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
                                        .detectAll()
                                        .penaltyLog()
                                        .build()
                        )
                    }
                }
            }

            // Delay preloading until app UI is ready
            delay(2000)
            preloadRepository.preloadCategoryData()
        }
    }

    private suspend fun initializeMobileAds() {
        withContext(Dispatchers.IO) {
            MobileAds.initialize(this@App) { initializationStatus ->
                Timber.d("MobileAds initialized with status: $initializationStatus")

                // Setup app open ad manager on the main thread after initialization
                CoroutineScope(Dispatchers.Main).launch {
                    setupAppOpenAdManager()
                    // DO NOT load ad here. It will be loaded when an activity is available.
                }

                // Mark Mobile Ads as initialized
                isMobileAdsInitialized = true

                // If this is not the first launch, allow ads
                if (!isFirstLaunch()) {
                    isInitialAppStart = false
                    Timber.d("AppOpenAdManager: Ready for ads after MobileAds initialization")
                }

                Timber.d(
                        "AppOpenAdManager: Final state - ads initialized=$isMobileAdsInitialized, initialAppStart=$isInitialAppStart"
                )
            }
        }
    }

    private fun setupAppOpenAdManager() {
        // Set up impression listener for analytics
        appOpenAdManager.setAdImpressionListener {
            Timber.d("AppOpenAd impression recorded for analytics")
            // Here you could add code to record the impression in your analytics system
        }

        // Set up failure listener for analytics
        appOpenAdManager.setAdFailureListener { errorMessage ->
            Timber.e("AppOpenAd failed: $errorMessage")
            // Here you could add code to record the failure in your analytics system
        }

        // Configure AppOpenAdManager to use activity context for ad loading
        appOpenAdManager.useActivityContextForAdLoading(true)

        // Set the current activity context if available and trigger first load if not attempted
        currentActivity?.let { activity ->
            appOpenAdManager.setActivityContext(activity)
            if (!isFirstAdLoadAttempted) {
                Timber.d("Triggering initial App Open ad load after MobileAds init")
                appOpenAdManager.loadAppOpenAd()
                isFirstAdLoadAttempted = true
            }
        }
    }

    private fun initApiConfig() {
        // First set default values in case Firebase fails
        val defaultConfig =
                mapOf(
                        "API_KEY" to BuildConfig.DEFAULT_API_KEY,
                        "API_HOST" to BuildConfig.DEFAULT_API_HOST
                )
        // Set default values immediately to prevent crashes
        apiConfigProvider.updateConfig(defaultConfig)
        Timber.d("Set default API config: $defaultConfig")

        // Then try to fetch from Firebase
        CoroutineScope(Dispatchers.IO).launch {
            Timber.d("Starting to fetch API config from Firebase...")
            firebaseRepository.getApiConfig().collect { result ->
                result
                        .onSuccess { configMap ->
                            Timber.d("API config successfully fetched from Firebase: $configMap")
                            // Log the specific keys we're looking for
                            Timber.d("API_KEY value from Firebase: ${configMap["API_KEY"]}")
                            Timber.d("API_HOST value from Firebase: ${configMap["API_HOST"]}")

                            apiConfigProvider.updateConfig(configMap)
                            Timber.d(
                                    "ApiConfigProvider updated - API Key: ${apiConfigProvider.getApiKey()}, Host: ${apiConfigProvider.getApiHost()}"
                            )
                        }
                        .onFailure { error ->
                            Timber.e(error, "Failed to fetch API config from Firebase")
                            // We already have default values set, so no need to handle failure
                            // specifically
                        }
            }
        }
    }

    /**
     * Initializes and requests consent information. This should be called from an Activity context,
     * e.g., in onActivityStarted.
     */
    private fun initializeConsent(activity: Activity) {
        val paramsBuilder = com.google.android.ump.ConsentRequestParameters.Builder()

        if (BuildConfig.DEBUG) {
            val debugSettings =
                    com.google.android.ump.ConsentDebugSettings.Builder(this)
                            .addTestDeviceHashedId("AF635FCF25F0A2F4F2631DE103049E7D")
                            .build()
            paramsBuilder.setConsentDebugSettings(debugSettings)
        }

        val params = paramsBuilder.build()

        consentInformation =
                com.google.android.ump.UserMessagingPlatform.getConsentInformation(this)
        consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                {
                    // Consent information updated.
                    // Load and show the form if required.
                    loadAndShowConsentFormIfRequired(activity)
                },
                { requestError ->
                    Timber.e("Failed to request consent info update: ${requestError.message}")
                }
        )
    }

    /** Loads and shows the consent form if it's required. */
    private fun loadAndShowConsentFormIfRequired(activity: Activity) {
        com.google.android.ump.UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                loadAndShowError ->
            if (loadAndShowError != null) {
                Timber.e("Failed to load or show consent form: ${loadAndShowError.message}")
                return@loadAndShowConsentFormIfRequired
            }

            // Consent has been gathered.
            // The Mobile Ads SDK can be initialized or will now use the updated consent.
            Timber.d("Consent gathered. Can request ads: ${consentInformation.canRequestAds()}")
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        currentActivity = activity
        try {
            FirebaseCrashlytics.getInstance()
                    .log("onActivityCreated: ${'$'}{activity.javaClass.name}")
        } catch (_: Exception) {}
        initializeConsent(activity)

        // Load the first App Open Ad once an activity is available.
        if (isMobileAdsInitialized && !isFirstAdLoadAttempted) {
            Timber.d("Activity created, attempting to load first App Open Ad.")
            appOpenAdManager.loadAppOpenAd()
            isFirstAdLoadAttempted = true
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
        try {
            FirebaseCrashlytics.getInstance()
                    .log("onActivityDestroyed: ${'$'}{activity.javaClass.name}")
        } catch (_: Exception) {}
    }

    override fun onActivityPaused(activity: Activity) {
        // Not needed but must be implemented
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity

        // Record first frame for performance tracking (first activity resume)
        if (isInitialAppStart) {
            startupTimeTracker.recordFirstFrameRendered()
        }

        // Update AppOpenAdManager with current activity context
        appOpenAdManager.setActivityContext(activity)
        try {
            FirebaseCrashlytics.getInstance()
                    .log("onActivityResumed: ${'$'}{activity.javaClass.name}")
        } catch (_: Exception) {}

        // Mark app as in foreground
        if (!appInForeground) {
            appInForeground = true
            appOpenAdManager.onAppForegrounded()

            // Check if ads can be shown
            if (isMobileAdsInitialized && !isInitialAppStart) {
                Timber.d("AppOpenAdManager: Checking if ad can be shown on resume")
                // Check if the app is eligible to show ad on app resume
                if (appOpenAdManager.shouldShowAdOnAppResume()) {
                    Timber.d("AppOpenAdManager: Showing ad on resume")
                    showAppOpenAd(activity)
                } else {
                    Timber.d("AppOpenAdManager: Not showing ad on resume (not eligible)")
                }
            } else {
                Timber.d(
                        "AppOpenAdManager: Not showing ad on resume (initialization state: ads initialized=${isMobileAdsInitialized}, initialAppStart=${isInitialAppStart})"
                )
            }
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // No specific action needed but must be implemented
    }

    override fun onActivityStarted(activity: Activity) {
        val isFirstLaunchCheck = isFirstLaunch()

        // Set current activity
        currentActivity = activity
        try {
            FirebaseCrashlytics.getInstance()
                    .log(
                            "onActivityStarted: firstLaunch=${'$'}isFirstLaunchCheck activity=${'$'}{activity.javaClass.name}"
                    )
        } catch (_: Exception) {}

        // Determine if we should show app open ad on start
        if (isMobileAdsInitialized && !isInitialAppStart && !isFirstLaunchCheck) {
            Timber.d("AppOpenAdManager: Checking if ad can be shown on activity start")
            if (appOpenAdManager.shouldShowAdOnAppStart(isFirstLaunchCheck)) {
                Timber.d("AppOpenAdManager: Attempting to show app open ad on activity start")
                showAppOpenAd(activity)
            } else {
                Timber.d("AppOpenAdManager: Not showing app open ad on start (not eligible)")
            }
        } else {
            Timber.d(
                    "AppOpenAdManager: Not showing app open ad on start (initialization state: ads initialized=${isMobileAdsInitialized}, initialAppStart=${isInitialAppStart}, firstLaunch=${isFirstLaunchCheck})"
            )
        }
    }

    override fun onActivityStopped(activity: Activity) {
        // When app is stopped, mark it as backgrounded
        if (activity.isFinishing) {
            appInForeground = false
            appOpenAdManager.onAppBackgrounded()
        }
        try {
            FirebaseCrashlytics.getInstance()
                    .log(
                            "onActivityStopped: finishing=${'$'}{activity.isFinishing} activity=${'$'}{activity.javaClass.name}"
                    )
        } catch (_: Exception) {}
    }

    private fun isFirstLaunch(): Boolean {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val isFirstLaunch = sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true)

        if (isFirstLaunch) {
            sharedPreferences.edit { putBoolean(KEY_FIRST_LAUNCH, false) }
            Timber.d("App is launching for the first time")
            return true
        }
        return false
    }

    private fun showAppOpenAd(activity: Activity) {
        if (appOpenAdManager.isAdAvailable()) {
            Timber.d("AppOpenAdManager: Showing app open ad")
            try {
                FirebaseCrashlytics.getInstance()
                        .log(
                                "showAppOpenAd: available=true activity=${'$'}{activity.javaClass.name}"
                        )
            } catch (_: Exception) {}
            appOpenAdManager.showAdIfAvailable(activity) {
                Timber.d("AppOpenAdManager: App open ad shown or dismissed")
                try {
                    FirebaseCrashlytics.getInstance().log("showAppOpenAd: onShowAdComplete")
                } catch (_: Exception) {}
                // Any post-ad display actions can go here
            }
        } else {
            Timber.d("AppOpenAdManager: No app open ad available to show")
            try {
                FirebaseCrashlytics.getInstance()
                        .log("showAppOpenAd: available=false -> loadAppOpenAd")
            } catch (_: Exception) {}
            // Ensure we have an ad ready for next time
            appOpenAdManager.loadAppOpenAd()
        }
    }

    /** Initialize Firebase Cloud Messaging and request a new token with retry logic */
    private fun initFirebaseMessaging() {
        // Enable FCM auto init
        com.google.firebase.messaging.FirebaseMessaging.getInstance().isAutoInitEnabled = true

        // Try to get the token with retry logic
        requestFcmTokenWithRetry()
    }

    /** Request FCM token with exponential backoff retry */
    private fun requestFcmTokenWithRetry(attempt: Int = 0, maxAttempts: Int = 5) {
        if (attempt >= maxAttempts) {
            Timber.e("Failed to get FCM token after $maxAttempts attempts")
            // Generate a placeholder token to allow the app to continue working
            generatePlaceholderToken()
            return
        }

        // Calculate exponential backoff delay (0s, 2s, 4s, 8s, 16s)
        val delayMillis = if (attempt == 0) 0L else (1L shl attempt) * 1000

        Timber.d(
                "Attempting to get FCM token (attempt ${attempt + 1}/$maxAttempts), delay: $delayMillis ms"
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                delay(delayMillis) // Wait before retry with exponential backoff

                // Use withTimeout to avoid waiting too long
                withTimeout(20_000) {
                    com.google.firebase.messaging.FirebaseMessaging.getInstance()
                            .token
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    // Got the token successfully
                                    val token = task.result
                                    Timber.d("FCM Token retrieved successfully: $token")

                                    // Save the token to repository
                                    CoroutineScope(Dispatchers.IO).launch {
                                        tokenRepository.saveToken(token)
                                    }
                                } else {
                                    val exception = task.exception
                                    if (exception is IOException &&
                                                    exception.message?.contains(
                                                            "SERVICE_NOT_AVAILABLE"
                                                    ) == true
                                    ) {
                                        Timber.w(
                                                exception,
                                                "FCM service not available (attempt ${attempt + 1}/$maxAttempts), will retry..."
                                        )
                                        // Retry with increased attempt counter
                                        requestFcmTokenWithRetry(attempt + 1, maxAttempts)
                                    } else {
                                        Timber.e(exception, "Failed to get FCM token with error")
                                        // For other errors, try at least one more time
                                        if (attempt == 0) {
                                            requestFcmTokenWithRetry(maxAttempts - 1, maxAttempts)
                                        } else {
                                            // Generate a placeholder after exhausting retries
                                            generatePlaceholderToken()
                                        }
                                    }
                                }
                            }
                }
            } catch (e: TimeoutCancellationException) {
                Timber.w(e, "FCM token request timed out (attempt ${attempt + 1}/$maxAttempts)")
                // Retry with increased attempt counter
                requestFcmTokenWithRetry(attempt + 1, maxAttempts)
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error during FCM token retrieval")
                if (attempt < maxAttempts - 1) {
                    requestFcmTokenWithRetry(attempt + 1, maxAttempts)
                } else {
                    generatePlaceholderToken()
                }
            }
        }
    }

    /**
     * Generate a placeholder token when Firebase service is unavailable This allows the app to
     * continue functioning without FCM
     */
    private fun generatePlaceholderToken() {
        Timber.w("Generating placeholder FCM token due to service unavailability")
        val placeholderToken = "placeholder-${UUID.randomUUID()}"

        CoroutineScope(Dispatchers.IO).launch {
            tokenRepository.saveToken(placeholderToken, isPlaceholder = true)

            // Schedule a retry after some time (15 minutes)
            delay(15 * 60 * 1000) // 15 minutes in milliseconds
            requestFcmTokenWithRetry()
        }
    }
}
