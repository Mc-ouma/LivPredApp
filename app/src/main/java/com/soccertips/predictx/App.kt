package com.soccertips.predictx

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.core.content.edit
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import coil.Coil
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.ump.ConsentInformation
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.soccertips.predictx.admob.AppOpenAdManager
import com.soccertips.predictx.notification.NotificationHelper
import com.soccertips.predictx.repository.PredictionRepository
import com.soccertips.predictx.repository.PreloadRepository
import com.soccertips.predictx.util.NetworkTaggingInitializer
import com.soccertips.predictx.util.StartupTimeTracker
import com.soccertips.predictx.util.DevicePerformanceManager
import dagger.hilt.android.HiltAndroidApp
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.guava.await
import timber.log.Timber

@HiltAndroidApp
class App : Application(), Configuration.Provider, Application.ActivityLifecycleCallbacks {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var preloadRepository: PreloadRepository

    @Inject
    lateinit var predictionRepository: PredictionRepository

    @Inject
    lateinit var firebaseRepository: com.soccertips.predictx.repository.FirebaseRepository

    @Inject
    lateinit var tokenRepository: com.soccertips.predictx.notification.TokenRepository

    @Inject
    lateinit var apiConfigProvider: com.soccertips.predictx.repository.ApiConfigProvider

    @Inject
    lateinit var networkTaggingInitializer: NetworkTaggingInitializer

    @Inject
    lateinit var appOpenAdManager: AppOpenAdManager

    @Inject
    lateinit var subscriptionRepository: com.soccertips.predictx.repository.SubscriptionRepository

    @Inject
    lateinit var adStateManager: com.soccertips.predictx.admob.AdStateManager

    @Inject
    lateinit var startupTimeTracker: StartupTimeTracker

    @Inject
    lateinit var devicePerformanceManager: DevicePerformanceManager

    @Inject
    lateinit var firebaseInitializer: com.soccertips.predictx.firebase.FirebaseInitializer

    @Inject
    lateinit var bettingSuccessScheduler:
            com.soccertips.predictx.notification.BettingSuccessScheduler

    @Inject
    lateinit var realTimeResultMonitor: com.soccertips.predictx.notification.RealTimeResultMonitor

    @Inject
    lateinit var dailyReminderAlarmScheduler: com.soccertips.predictx.notification.DailyReminderAlarmScheduler

    @Inject
    lateinit var imageLoader: coil.ImageLoader

    private var currentActivity: Activity? = null

    // Track app foreground status
    private var appInForeground = false

    // Flag to avoid showing ads during initial app startup
    private var isInitialAppStart = true

    // Track when Mobile Ads SDK has been initialized
    //private var isMobileAdsInitialized = false
    private val _isMobileAdsInitialized = MutableStateFlow(false)
    val isMobileAdsInitialized: StateFlow<Boolean> = _isMobileAdsInitialized.asStateFlow()


    // Track if MobileAds initialization is in progress
    private var isMobileAdsInitializing = false

    // Consent management
    private lateinit var consentInformation: ConsentInformation
    private var isConsentInitialized = false
    private var isConsentComplete = false

    // Shared preferences key for first launch check
    private val PREFS_NAME = "app_preferences"
    private val KEY_FIRST_LAUNCH = "is_first_launch"
    private val KEY_APP_INITIALIZED = "is_app_initialized"

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()

        // Record app start time for performance tracking
        StartupTimeTracker.recordAppStart()

        // Initialize device performance manager FIRST to get optimal startup config
        devicePerformanceManager.initialize(this)
        val startupConfig = devicePerformanceManager.getStartupConfig()

        // Log device info for debugging cold start issues
        Timber.i("App starting on ${if (devicePerformanceManager.isLowMemoryDevice()) "low-memory" else "standard"} device")
        if (devicePerformanceManager.isCriticalMemoryDevice()) {
            Timber.w("Device is in critical memory range (1.5-2GB) - applying aggressive optimizations")
        }

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

        // Set custom Coil ImageLoader with optimized caching
        Coil.setImageLoader(imageLoader)

        // Register lifecycle callbacks immediately
        registerActivityLifecycleCallbacks(this)

        // Check app initialization state quickly
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val appInitialized = prefs.getBoolean(KEY_APP_INITIALIZED, false)
        isInitialAppStart = !appInitialized

        if (!appInitialized) {
            prefs.edit { putBoolean(KEY_APP_INITIALIZED, true) }
        }

        // Defer heavy operations to background threads with device-aware delays
        CoroutineScope(Dispatchers.IO).launch { initializeInBackground(startupConfig) }

        // Don't initialize Mobile Ads here - wait for activity context
        // MobileAds will be initialized in onActivityCreated with proper Activity context
    }

    /**
     * Cleans up old and stale WorkManager jobs to prevent the 100-job limit from being exceeded.
     * This method cancels finished/failed jobs and removes old notification jobs.
     * Made fully async to avoid blocking startup.
     */
    private suspend fun cleanupOldWorkManagerJobs() = withContext(Dispatchers.IO) {
        try {
            Timber.d("WorkManager: Starting async cleanup of old jobs")

            val workManager = WorkManager.getInstance(this@App)

            var cancelledCount = 0

            // Cancel all work by tags for match notifications (old format)
            workManager.cancelAllWorkByTag("match_notification")

            // Get and cancel finished manual betting check jobs - use async with timeout
            try {
                workManager.getWorkInfosByTag("manual_betting_check").await().forEach { workInfo ->
                    if (workInfo.state == WorkInfo.State.SUCCEEDED ||
                        workInfo.state == WorkInfo.State.FAILED ||
                        workInfo.state == WorkInfo.State.CANCELLED
                    ) {
                        try {
                            workManager.cancelWorkById(workInfo.id)
                            cancelledCount++
                        } catch (e: Exception) {
                            Timber.w(
                                "Failed to cancel manual betting work ${workInfo.id}: ${e.message}"
                            )
                        }
                    }
                }

                // Get and cancel finished end-of-day check jobs
                workManager.getWorkInfosByTag("end_of_day_check").await().forEach { workInfo ->
                    if (workInfo.state == WorkInfo.State.SUCCEEDED ||
                        workInfo.state == WorkInfo.State.FAILED ||
                        workInfo.state == WorkInfo.State.CANCELLED
                    ) {
                        try {
                            workManager.cancelWorkById(workInfo.id)
                            cancelledCount++
                        } catch (e: Exception) {
                            Timber.w("Failed to cancel end-of-day work ${workInfo.id}: ${e.message}")
                        }
                    }
                }

                // Prune completed work from the database
                workManager.pruneWork()

                Timber.d("WorkManager: Cleanup completed - cancelled $cancelledCount old jobs")
            } catch (tagError: Exception) {
                Timber.w("WorkManager: Error getting work info by tag: ${tagError.message}")
            }
        } catch (e: Exception) {
            Timber.e(e, "WorkManager: Error during cleanup")
        }
    }

    private suspend fun initializeInBackground(startupConfig: DevicePerformanceManager.StartupConfig) {
        withContext(Dispatchers.IO) {
            val isLowMemory = devicePerformanceManager.isLowMemoryDevice()

            // CRITICAL: On ALL devices, give UI thread priority during startup to prevent ANR
            // Longer delay on low-memory devices
            val startupDelay = if (isLowMemory) 1000L else 500L
            delay(startupDelay)

            // Initialize subscription status early so ads are suppressed for subscribers
            initializeSubscription()

            // Clean up old/stale WorkManager jobs first (lightweight, always do this)
            // This is now async and won't block
            cleanupOldWorkManagerJobs()

            // Initialize non-critical components in background with staggered delays
            // to avoid resource contention
            delay(300)

            if (!startupConfig.skipNonEssentialInit) {
                NotificationHelper.createNotificationChannels(this@App)
            } else {
                // Defer notification channel creation on low-memory devices
                delay(startupConfig.deferFirebaseMs)
                NotificationHelper.createNotificationChannels(this@App)
            }

            // Set up prediction repository dependency - lightweight operation
            delay(200)
            preloadRepository.setPredictionRepository(predictionRepository)

            // Initialize API config - this is essential but can wait for UI
            delay(300)
            initApiConfig()

            // On ALL devices, defer non-essential schedulers to after UI is stable
            // Use longer delays than before to ensure UI responsiveness
            val schedulerDelay = if (isLowMemory) 2000L else 1500L
            delay(schedulerDelay)

            // Initialize betting success checking system
            bettingSuccessScheduler.initialize()
            delay(200)

            // Initialize real-time result monitoring system
            realTimeResultMonitor.startMonitoring()
            Timber.d("Real-time result monitoring initialized${if (isLowMemory) " (deferred for low-memory device)" else ""}")
            delay(200)

            // Schedule daily reminder notifications using AlarmManager (exact timing)
            dailyReminderAlarmScheduler.scheduleDailyReminders()
            Timber.d("Daily reminder alarm scheduler initialized${if (isLowMemory) " (deferred for low-memory device)" else ""}")

            // Initialize Firebase messaging with device-aware delay
            delay(startupConfig.deferFirebaseMs)
            initFirebaseMessaging()

            // Preloading - skip aggressive preloading on low-memory devices
            if (!startupConfig.skipAggressivePreloading) {
                delay(startupConfig.deferPreloadingMs)
                preloadRepository.preloadCategoryData()
            } else {
                Timber.d("Skipping aggressive preloading on low-memory device")
                // On low-memory devices, only preload when user accesses categories
            }
        }
    }

    private suspend fun initializeMobileAds() {
        // Prevent multiple initialization attempts
        if (isMobileAdsInitialized.value || isMobileAdsInitializing) {
            Timber.d("MobileAds already initialized or initializing, skipping")
            return
        }

        // On low-memory devices, add extra delay before initializing ads
        val isLowMemory = devicePerformanceManager.isLowMemoryDevice()
        if (isLowMemory) {
            val adDelay = devicePerformanceManager.getStartupConfig().deferAdInitializationMs
            Timber.d("Low-memory device detected, deferring MobileAds init by ${adDelay}ms")
            delay(adDelay)
        }

        if (currentActivity == null) {
            Timber.w("Cannot initialize MobileAds yet - no activity context available")
            return
        }

        withContext(Dispatchers.IO) {
            try {
                isMobileAdsInitializing = true

                val initConfig =
                    InitializationConfig.Builder("ca-app-pub-8504414839434291~8215753517")
                        .build()

                MobileAds.initialize(applicationContext, initConfig) { initializationStatus ->
                    Timber.d("MobileAds initialized with status: $initializationStatus")

                    _isMobileAdsInitialized.value = true
                    isMobileAdsInitializing = false

                    setupAppOpenAdManager()

                    if (!isFirstLaunch()) {
                        isInitialAppStart = false
                        Timber.d("AppOpenAdManager: Ready for ads after MobileAds initialization")
                    }

                    maybePreloadAppOpenAd()

                }
            } catch (e: Exception) {
                Timber.e("Error initializing MobileAds: ${e.message}")
                isMobileAdsInitializing = false
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

        // Keep the manager aligned with the current activity; loading is triggered separately.
        currentActivity?.let { activity ->
            appOpenAdManager.setActivityContext(activity)
            Timber.d("AppOpenAdManager setup complete with activity context")
        }
    }

    private fun maybePreloadAppOpenAd() {
        val activity = currentActivity
        if (!isMobileAdsInitialized.value || activity == null || activity.isFinishing || activity.isDestroyed) {
            return
        }

        appOpenAdManager.setActivityContext(activity)
        appOpenAdManager.loadAppOpenAd()
    }

    private fun initializeSubscription() {
        // Sync cached subscription status to AdStateManager immediately
        val cachedStatus = subscriptionRepository.isSubscribedSync()
        adStateManager.setSubscribed(cachedStatus)
        Timber.d("Subscription status synced from cache: $cachedStatus")

        // Initialize billing client to verify/refresh subscription status
        subscriptionRepository.initialize()

        // Observe changes and keep AdStateManager in sync
        CoroutineScope(Dispatchers.Main).launch {
            subscriptionRepository.isSubscribed.collect { isSubscribed ->
                adStateManager.setSubscribed(isSubscribed)
                subscriptionRepository.cacheSubscriptionStatus(isSubscribed)
                Timber.d("Subscription status updated: $isSubscribed")
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
                        Timber.d("API config successfully fetched from Firebase")

                        apiConfigProvider.updateConfig(configMap)
                        Timber.d("ApiConfigProvider updated")
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
     * e.g., in onActivityCreated for the first activity only.
     */
    private fun initializeConsent(activity: Activity) {
        // Build consent request parameters
        val paramsBuilder = com.google.android.ump.ConsentRequestParameters.Builder()

        // Configure debug settings for testing (only in debug builds)
        if (BuildConfig.DEBUG) {
            val debugSettings =
                com.google.android.ump.ConsentDebugSettings.Builder(activity)
                    // Set debug geography to test GDPR consent in EEA
                    .setDebugGeography(
                        com.google.android.ump.ConsentDebugSettings.DebugGeography
                            .DEBUG_GEOGRAPHY_EEA
                    )
                    // Add test device IDs if needed
                    .addTestDeviceHashedId("B3EEABB8EE11C2BE770B684D95219ECB")
                    .build()

            paramsBuilder.setConsentDebugSettings(debugSettings)
            Timber.d("Consent: Debug mode enabled with EEA geography")
        }

        // Set tag for under age of consent if applicable
        // paramsBuilder.setTagForUnderAgeOfConsent(false)

        val params = paramsBuilder.build()

        // Get consent information instance
        consentInformation =
            com.google.android.ump.UserMessagingPlatform.getConsentInformation(activity)

        Timber.d("Consent: Requesting consent info update...")
        Timber.d("Consent: Current status = ${consentInformation.consentStatus}")

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                // Consent information updated successfully
                Timber.d("Consent: Info update successful")
                Timber.d("Consent: Status = ${consentInformation.consentStatus}")
                Timber.d("Consent: Can request ads = ${consentInformation.canRequestAds()}")
                Timber.d(
                    "Consent: Form available = ${consentInformation.isConsentFormAvailable}"
                )

                // Log privacy options requirement (useful for CMP verification)
                try {
                    val privacyOptionsRequired =
                        consentInformation.privacyOptionsRequirementStatus
                    Timber.d("Consent: Privacy options required = $privacyOptionsRequired")
                } catch (e: Exception) {
                    Timber.w(
                        "Consent: Could not check privacy options requirement: ${e.message}"
                    )
                }

                // Check if consent form is available and show if required
                if (consentInformation.isConsentFormAvailable) {
                    Timber.d("Consent: Form is available - showing if required")
                    loadAndShowConsentFormIfRequired(activity)
                } else {
                    Timber.d("Consent: Form is NOT available")
                    Timber.i("Consent: This means either:")
                    Timber.i("  1. User is not in a region requiring consent (non-EEA)")
                    Timber.i("  2. Funding Choices message not configured in AdMob console")
                    Timber.i("  3. Consent already obtained in previous session")

                    // No form needed, mark as complete and proceed
                    isConsentComplete = true
                    initializeMobileAdsIfReady()
                }
            },
            { requestError ->
                // Handle consent update error
                Timber.e("Consent: Failed to request info update: ${requestError.message}")
                Timber.e("Consent: Error code = ${requestError.errorCode}")

                // Specific error handling based on error code
                when (requestError.errorCode) {
                    1 -> Timber.e("Consent: INTERNAL_ERROR - Retry may help")
                    2 -> Timber.e("Consent: INTERNET_ERROR - Check network connection")
                    3 -> Timber.e("Consent: INVALID_OPERATION - Check AdMob configuration")
                    4 -> Timber.e("Consent: TIME_OUT - Network too slow")
                    else -> Timber.e("Consent: Unknown error code")
                }

                Timber.w("Consent: ⚠️ If error persists, verify:")
                Timber.w("  1. Funding Choices message is published in AdMob console")
                Timber.w("  2. App is correctly linked to AdMob account")
                Timber.w("  3. Internet connection is stable")

                // Mark consent as complete even on error to not block the app
                // In production, consider your privacy policy requirements
                isConsentComplete = true

                // Try to initialize ads anyway (they won't show in regions requiring consent)
                initializeMobileAdsIfReady()
            }
        )
    }

    /** Loads and shows the consent form if it's required. */
    private fun loadAndShowConsentFormIfRequired(activity: Activity) {
        Timber.d("Consent: Loading and showing form if required...")

        com.google.android.ump.UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { loadAndShowError ->
            if (loadAndShowError != null) {
                Timber.e("Consent: Failed to load or show form: ${loadAndShowError.message}")
                Timber.e("Consent: Error code = ${loadAndShowError.errorCode}")
            } else {
                Timber.d("Consent: Form flow completed successfully")
            }

            // Consent has been gathered or there was an error, or form was not required.
            // Check if we can now request ads
            val canRequestAds =
                try {
                    consentInformation.canRequestAds()
                } catch (e: Exception) {
                    Timber.e("Consent: Error checking canRequestAds: ${e.message}")
                    // Default to false for safety in case of error
                    false
                }

            val consentStatus =
                try {
                    consentInformation.consentStatus
                } catch (e: Exception) {
                    Timber.e("Consent: Error checking consent status: ${e.message}")
                    com.google.android.ump.ConsentInformation.ConsentStatus.UNKNOWN
                }

            Timber.d("Consent: Process completed")
            Timber.d("Consent: Status = $consentStatus")
            Timber.d("Consent: Can request ads = $canRequestAds")

            // Mark consent as complete
            isConsentComplete = true

            // Log consent decision for debugging
            when (consentStatus) {
                com.google.android.ump.ConsentInformation.ConsentStatus.OBTAINED -> {
                    Timber.d("Consent: User has provided consent")
                    try {
                        FirebaseCrashlytics.getInstance().log("Consent: OBTAINED")
                    } catch (_: Exception) {
                    }
                }

                com.google.android.ump.ConsentInformation.ConsentStatus.REQUIRED -> {
                    Timber.w("Consent: Consent is still required but form was dismissed")
                    try {
                        FirebaseCrashlytics.getInstance().log("Consent: REQUIRED (form dismissed)")
                    } catch (_: Exception) {
                    }
                }

                com.google.android.ump.ConsentInformation.ConsentStatus.NOT_REQUIRED -> {
                    Timber.d("Consent: Not required for this user")
                    try {
                        FirebaseCrashlytics.getInstance().log("Consent: NOT_REQUIRED")
                    } catch (_: Exception) {
                    }
                }

                else -> {
                    Timber.w("Consent: Unknown status")
                }
            }

            // Initialize MobileAds if consent allows and it's not already initialized
            if (canRequestAds) {
                initializeMobileAdsIfReady()
            } else {
                Timber.w("Consent: Cannot request ads - consent not granted or still required")
                // Don't initialize MobileAds - wait for user to provide consent
                // You might want to show a message to the user explaining why ads aren't showing
            }
        }
    }

    /** Helper function to initialize MobileAds only if all conditions are met */
    private fun initializeMobileAdsIfReady() {
        if (!isMobileAdsInitialized.value && !isMobileAdsInitializing && isConsentComplete) {
            CoroutineScope(Dispatchers.Main).launch {
                // Add delay before initializing after consent
                delay(1000)

                // Double-check activity is still valid
                val activity = currentActivity
                if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                    Timber.d("Consent: Proceeding with MobileAds initialization")
                    initializeMobileAds()
                } else {
                    Timber.w("Consent: Activity no longer valid, skipping MobileAds init")
                }
            }
        } else {
            Timber.d(
                "Consent: Not initializing MobileAds yet (initialized=${isMobileAdsInitialized.value}, initializing=$isMobileAdsInitializing, consentComplete=$isConsentComplete)"
            )
        }
    }

    /**
     * Reset consent information - useful for testing or if user wants to change consent Call this
     * from your settings/privacy screen
     */
    fun resetConsent(activity: Activity) {
        Timber.d("Consent: Resetting consent information")
        try {
            if (::consentInformation.isInitialized) {
                consentInformation.reset()
            }
            isConsentInitialized = false
            isConsentComplete = false

            // Re-initialize consent
            initializeConsent(activity)

            Timber.d("Consent: Reset successful")
            try {
                FirebaseCrashlytics.getInstance().log("Consent: Manual reset by user")
            } catch (_: Exception) {
            }
        } catch (e: Exception) {
            Timber.e("Consent: Error during reset: ${e.message}")
        }
    }

    /** Check if user can request ads (has provided consent or consent not required) */
    fun canShowAds(): Boolean {
        return try {
            if (::consentInformation.isInitialized) {
                consentInformation.canRequestAds()
            } else {
                false
            }
        } catch (e: Exception) {
            Timber.e("Error checking ad permission: ${e.message}")
            false
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        currentActivity = activity
        try {
            FirebaseCrashlytics.getInstance().log("onActivityCreated: ${activity.javaClass.name}")
        } catch (_: Exception) {
        }

        // Defer consent initialization to avoid blocking startup
        // This is a non-essential operation that can wait until the app is fully rendered
        if (!isConsentInitialized) {
            isConsentInitialized = true
            Timber.d(
                "Consent: Scheduling deferred initialization with activity: ${activity.javaClass.simpleName}"
            )
            // Defer consent initialization to after startup completes
            CoroutineScope(Dispatchers.Main).launch {
                // Wait for UI to be fully rendered and stable
                val deferDelay = if (devicePerformanceManager.isLowMemoryDevice()) 2500L else 1500L
                delay(deferDelay)
                // Double-check activity is still valid
                if (currentActivity != null && !activity.isFinishing && !activity.isDestroyed) {
                    Timber.d("Consent: Starting deferred initialization")
                    initializeConsent(activity)
                } else {
                    Timber.w("Consent: Activity no longer valid, skipping initialization")
                    isConsentInitialized = false // Allow retry on next activity
                }
            }
        } else {
            Timber.d("Consent: Already initialized, skipping")
        }

        // Don't initialize MobileAds immediately - wait for consent completion
        // This will be handled in loadAndShowConsentFormIfRequired callback
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
        try {
            FirebaseCrashlytics.getInstance()
                .log("onActivityDestroyed: ${'$'}{activity.javaClass.name}")
        } catch (_: Exception) {
        }
    }

    override fun onActivityPaused(activity: Activity) {
        // Not needed but must be implemented
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity

        // Record first frame for performance tracking (first activity resume)
        if (isInitialAppStart) {
            startupTimeTracker.recordFirstFrameRendered()
            // Defer performance logging to avoid blocking during critical startup
            CoroutineScope(Dispatchers.Default).launch {
                delay(500)
                devicePerformanceManager.logPerformanceInfo()
            }
        }

        // Update AppOpenAdManager with current activity context
        appOpenAdManager.setActivityContext(activity)
        try {
            FirebaseCrashlytics.getInstance().log("onActivityResumed: ${activity.javaClass.name}")
        } catch (_: Exception) {
        }

        // Initialize MobileAds with delay after activity is fully resumed and stable
        // Only if consent is complete and MobileAds is not already initialized
        if (!isMobileAdsInitialized.value && !isMobileAdsInitializing && isConsentComplete) {
            CoroutineScope(Dispatchers.Main).launch {
                // Device-aware delay - longer on low-memory devices
                val stabilizationDelay = if (devicePerformanceManager.isLowMemoryDevice()) {
                    devicePerformanceManager.getStartupConfig().deferAdInitializationMs
                } else {
                    1500L
                }
                delay(stabilizationDelay)

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
        } else {
            Timber.d(
                "MobileAds init check: initialized=${isMobileAdsInitialized.value}, initializing=$isMobileAdsInitializing, consentComplete=$isConsentComplete"
            )
        }

        if (isMobileAdsInitialized.value) {
            maybePreloadAppOpenAd()
        }

        // Mark app as in foreground
        if (!appInForeground) {
            appInForeground = true
            appOpenAdManager.onAppForegrounded()

            // Check if ads can be shown - skip on low-memory devices during initial start
            val isLowMemory = devicePerformanceManager.isLowMemoryDevice()
            if (isMobileAdsInitialized.value && !isInitialAppStart && !(isLowMemory && isInitialAppStart)) {
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
                    "AppOpenAdManager: Not showing ad on resume (initialization state: ads initialized=${isMobileAdsInitialized.value}, initialAppStart=${isInitialAppStart}, lowMemory=$isLowMemory)"
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
        } catch (_: Exception) {
        }

        // Determine if we should show app open ad on start
        if (isMobileAdsInitialized.value && !isInitialAppStart && !isFirstLaunchCheck) {
            Timber.d("AppOpenAdManager: Checking if ad can be shown on activity start")
            if (appOpenAdManager.shouldShowAdOnAppStart(isFirstLaunchCheck)) {
                Timber.d("AppOpenAdManager: Attempting to show app open ad on activity start")
                showAppOpenAd(activity)
            } else {
                Timber.d("AppOpenAdManager: Not showing app open ad on start (not eligible)")
            }
        } else {
            Timber.d(
                "AppOpenAdManager: Not showing app open ad on start (initialization state: ads initialized=${isMobileAdsInitialized.value}, initialAppStart=${isInitialAppStart}, firstLaunch=${isFirstLaunchCheck})"
            )
        }
    }

    override fun onActivityStopped(activity: Activity) {
        // This app currently uses a single-activity flow, so stopped means backgrounded for ad purposes.
        if (currentActivity == activity) {
            appInForeground = false
            appOpenAdManager.onAppBackgrounded()
        }
        try {
            FirebaseCrashlytics.getInstance()
                .log(
                    "onActivityStopped: finishing=${'$'}{activity.isFinishing} activity=${'$'}{activity.javaClass.name}"
                )
        } catch (_: Exception) {
        }
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
            } catch (_: Exception) {
            }
            appOpenAdManager.showAdIfAvailable(activity) {
                Timber.d("AppOpenAdManager: App open ad shown or dismissed")
                try {
                    FirebaseCrashlytics.getInstance().log("showAppOpenAd: onShowAdComplete")
                } catch (_: Exception) {
                }
                // Any post-ad display actions can go here
            }
        } else {
            Timber.d("AppOpenAdManager: No app open ad available to show")
            try {
                FirebaseCrashlytics.getInstance()
                    .log("showAppOpenAd: available=false -> loadAppOpenAd")
            } catch (_: Exception) {
            }
            // Ensure we have an ad ready for next time
            appOpenAdManager.loadAppOpenAd()
        }
    }

    /** Initialize Firebase Cloud Messaging and request a new token with retry logic */
    private fun initFirebaseMessaging() {
        // First, ensure Firebase is properly initialized
        if (!firebaseInitializer.initializeFirebase(this)) {
            Timber.e("Firebase initialization failed, cannot initialize FCM")
            generatePlaceholderToken()
            return
        }

        // Enable FCM auto init
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().isAutoInitEnabled = true
            Timber.d("FCM auto-init enabled successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to enable FCM auto-init")
            generatePlaceholderToken()
            return
        }

        // Try to get the token with retry logic
        requestFcmTokenWithRetry()

        // Ensure topic subscriptions for existing users (runs regardless of token refresh)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                tokenRepository.subscribeToDefaultTopics()
                Timber.d("FCM topic subscriptions ensured")
            } catch (e: Exception) {
                Timber.e(e, "Failed to ensure FCM topic subscriptions")
            }
        }
    }

    /** Request FCM token with exponential backoff retry */
    private fun requestFcmTokenWithRetry(attempt: Int = 0, maxAttempts: Int = 3) {
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

                                // Save the token to repository and subscribe to topics
                                CoroutineScope(Dispatchers.IO).launch {
                                    tokenRepository.saveToken(token)
                                    // Subscribe to FCM topics for server-side notifications
                                    tokenRepository.subscribeToDefaultTopics()
                                }
                            } else {
                                val exception = task.exception
                                when {
                                    exception is IOException &&
                                            exception.message?.contains(
                                                "SERVICE_NOT_AVAILABLE"
                                            ) == true -> {
                                        Timber.w(
                                            exception,
                                            "FCM service not available (attempt ${attempt + 1}/$maxAttempts), will retry..."
                                        )
                                        // Retry with increased attempt counter
                                        requestFcmTokenWithRetry(attempt + 1, maxAttempts)
                                    }

                                    exception?.message?.contains("AUTHENTICATION_FAILED") ==
                                            true -> {
                                        Timber.e(
                                            exception,
                                            "FCM authentication failed - check Firebase configuration"
                                        )

                                        // Don't retry on authentication failures, generate
                                        // placeholder immediately
                                        generatePlaceholderToken()
                                    }

                                    exception?.message?.contains("ExecutionException") ==
                                            true &&
                                            exception.message?.contains(
                                                "AUTHENTICATION_FAILED"
                                            ) == true -> {
                                        Timber.e(
                                            exception,
                                            "FCM authentication failed (wrapped in ExecutionException) - check Firebase configuration"
                                        )

                                        // Don't retry on authentication failures, generate
                                        // placeholder immediately
                                        generatePlaceholderToken()
                                    }

                                    else -> {
                                        Timber.e(
                                            exception,
                                            "Failed to get FCM token with error ${exception?.message}"
                                        )
                                        // For other errors, try at least one more time
                                        if (attempt == 0) {
                                            requestFcmTokenWithRetry(attempt + 1, maxAttempts)
                                        } else {
                                            // Generate a placeholder after exhausting retries
                                            generatePlaceholderToken()
                                        }
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
