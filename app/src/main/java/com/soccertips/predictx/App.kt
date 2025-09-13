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

    @Inject
    lateinit var bettingSuccessScheduler:
            com.soccertips.predictx.notification.BettingSuccessScheduler

    @Inject lateinit var realTimeResultMonitor: com.soccertips.predictx.notification.RealTimeResultMonitor

    private var currentActivity: Activity? = null

    // Track app foreground status
    private var appInForeground = false

    // Flag to avoid showing ads during initial app startup
    private var isInitialAppStart = true

    // Track when Mobile Ads SDK has been initialized
    private var isMobileAdsInitialized = false

    // Track if MobileAds initialization is in progress
    private var isMobileAdsInitializing = false

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

        // Set system property to help with ViewConfiguration issues
        try {
            System.setProperty("android.os.strictmode.checkContextForConfiguration", "false")
        } catch (e: Exception) {
            Timber.w("Could not set ViewConfiguration system property: ${e.message}")
        }

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

        // Don't initialize Mobile Ads here - wait for activity context
        // MobileAds will be initialized in onActivityCreated with proper Activity context
    }

    private suspend fun initializeInBackground() {
        withContext(Dispatchers.IO) {
            // Initialize non-critical components in background
            NotificationHelper.createNotificationChannels(this@App)

            // Set up prediction repository dependency
            preloadRepository.setPredictionRepository(predictionRepository)

            // Initialize API config
            initApiConfig()

            // Initialize betting success checking system
            bettingSuccessScheduler.initialize()

            // Initialize real-time result monitoring system
            realTimeResultMonitor.startMonitoring()
            Timber.d("Real-time result monitoring initialized")

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
        // Prevent multiple initialization attempts
        if (isMobileAdsInitialized || isMobileAdsInitializing) {
            Timber.d("MobileAds already initialized or initializing, skipping")
            return
        }

        // Ensure we have an activity context and run on Main thread
        val activity = currentActivity
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            Timber.w("Cannot initialize MobileAds - no valid activity context available")
            return
        }

        withContext(Dispatchers.Main) {
            try {
                isMobileAdsInitializing = true
                Timber.d(
                        "Initializing MobileAds with activity context: ${activity.javaClass.simpleName}"
                )

                // Additional validation before calling MobileAds.initialize
                if (!activity.hasWindowFocus()) {
                    Timber.w(
                            "Activity does not have window focus, delaying MobileAds initialization"
                    )
                    delay(1000)

                    // Double-check activity is still valid
                    if (activity.isFinishing || activity.isDestroyed) {
                        Timber.w("Activity became invalid during delay, aborting MobileAds init")
                        isMobileAdsInitializing = false
                        return@withContext
                    }
                }

                // Use application context for initialization but ensure activity context for ad
                // operations
                try {
                    withTimeout(5000) { // Add timeout to prevent hanging
                        MobileAds.initialize(activity) { initializationStatus ->
                            Timber.d("MobileAds initialized with status: $initializationStatus")

                            // Setup app open ad manager on the main thread after initialization
                            // but only if activity is still valid
                            if (!activity.isFinishing && !activity.isDestroyed) {
                                setupAppOpenAdManager()
                            } else {
                                Timber.w(
                                        "Activity invalid after MobileAds init, skipping ad manager setup"
                                )
                            }

                            // Mark Mobile Ads as initialized
                            isMobileAdsInitialized = true
                            isMobileAdsInitializing = false

                            // If this is not the first launch, allow ads
                            if (!isFirstLaunch()) {
                                isInitialAppStart = false
                                Timber.d(
                                        "AppOpenAdManager: Ready for ads after MobileAds initialization"
                                )
                            }

                            Timber.d(
                                    "AppOpenAdManager: Final state - ads initialized=$isMobileAdsInitialized, initialAppStart=$isInitialAppStart"
                            )

                            // Load first ad with additional delay to ensure everything is stable
                            CoroutineScope(Dispatchers.Main).launch {
                                delay(2000) // Additional delay before loading first ad

                                // Re-check currentActivity to ensure we have the most recent one
                                val currentValidActivity = currentActivity
                                if (currentValidActivity != null &&
                                                !currentValidActivity.isFinishing &&
                                                !currentValidActivity.isDestroyed &&
                                                !isFirstAdLoadAttempted
                                ) {

                                    Timber.d("Loading first App Open ad after initialization delay")
                                    try {
                                        // Ensure ad manager has the latest activity context
                                        appOpenAdManager.setActivityContext(currentValidActivity)
                                        appOpenAdManager.loadAppOpenAd()
                                        isFirstAdLoadAttempted = true
                                    } catch (e: Exception) {
                                        Timber.e("Error loading first app open ad: ${e.message}")
                                        if (e.message?.contains("ViewConfiguration") == true ||
                                                        e.message?.contains("WindowManager") ==
                                                                true ||
                                                        e.message?.contains("visual Context") ==
                                                                true
                                        ) {

                                            Timber.e(
                                                    "Context error during ad loading - will retry later"
                                            )
                                            // Retry after additional delay
                                            CoroutineScope(Dispatchers.Main).launch {
                                                delay(5000)
                                                val retryActivity = currentActivity
                                                if (retryActivity != null &&
                                                                !retryActivity.isFinishing &&
                                                                !retryActivity.isDestroyed
                                                ) {
                                                    try {
                                                        // Update context again before retry
                                                        appOpenAdManager.setActivityContext(
                                                                retryActivity
                                                        )
                                                        appOpenAdManager.loadAppOpenAd()
                                                        isFirstAdLoadAttempted = true
                                                    } catch (retryError: Exception) {
                                                        Timber.e(
                                                                "Retry failed: ${retryError.message}"
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (timeoutEx: TimeoutCancellationException) {
                    Timber.e("MobileAds initialization timed out")
                    isMobileAdsInitializing = false
                }
            } catch (e: Exception) {
                Timber.e("Error initializing MobileAds: ${e.message}")
                isMobileAdsInitializing = false

                // Special handling for context-related errors
                if (e.message?.contains("ViewConfiguration") == true ||
                                e.message?.contains("context") == true ||
                                e.message?.contains("WindowManager") == true ||
                                e.message?.contains("visual Context") == true
                ) {

                    Timber.e("Context error detected - will retry with longer delay")
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(5000) // Wait 5 seconds before retry
                        if (currentActivity != null &&
                                        !currentActivity!!.isFinishing &&
                                        !currentActivity!!.isDestroyed
                        ) {
                            Timber.d(
                                    "Retrying MobileAds initialization after ViewConfiguration error"
                            )
                            initializeMobileAds()
                        }
                    }
                }
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

        // Set the current activity context but don't load ad yet
        // Ad loading will be handled separately with proper delays
        currentActivity?.let { activity ->
            appOpenAdManager.setActivityContext(activity)
            Timber.d("AppOpenAdManager setup complete with activity context")
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
                // Don't return here - still try to initialize ads if possible
            }

            // Consent has been gathered or there was an error.
            // The Mobile Ads SDK can be initialized or will now use the updated consent.
            val canRequestAds =
                    try {
                        consentInformation.canRequestAds()
                    } catch (e: Exception) {
                        Timber.e("Error checking consent status: ${e.message}")
                        false
                    }

            Timber.d("Consent process completed. Can request ads: $canRequestAds")

            // Initialize MobileAds if not already done and consent allows it
            if (!isMobileAdsInitialized && !isMobileAdsInitializing && canRequestAds) {
                CoroutineScope(Dispatchers.Main).launch {
                    // Add delay before initializing after consent
                    delay(1000)

                    // Double-check activity is still valid
                    if (currentActivity != null &&
                                    !currentActivity!!.isFinishing &&
                                    !currentActivity!!.isDestroyed
                    ) {
                        initializeMobileAds()
                    } else {
                        Timber.w(
                                "Activity no longer valid after consent delay, skipping MobileAds init"
                        )
                    }
                }
            }
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        currentActivity = activity
        try {
            FirebaseCrashlytics.getInstance().log("onActivityCreated: ${activity.javaClass.name}")
        } catch (_: Exception) {}

        // Initialize consent first
        initializeConsent(activity)

        // Don't initialize MobileAds immediately - wait for activity to be fully ready
        // This will be handled in onActivityResumed after a delay
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
            FirebaseCrashlytics.getInstance().log("onActivityResumed: ${activity.javaClass.name}")
        } catch (_: Exception) {}

        // Initialize MobileAds with delay after activity is fully resumed and stable
        if (!isMobileAdsInitialized && !isMobileAdsInitializing) {
            CoroutineScope(Dispatchers.Main).launch {
                // Wait for activity to be fully stable before initializing ads
                delay(1500) // Give the activity time to fully load and stabilize

                // Double-check that we still have an active activity
                if (currentActivity != null && !activity.isFinishing && !activity.isDestroyed) {
                    Timber.d("Initializing MobileAds after activity stabilization delay")
                    initializeMobileAds()
                } else {
                    Timber.w(
                            "Activity no longer valid after stabilization delay, skipping MobileAds init"
                    )
                }
            }
        }

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
