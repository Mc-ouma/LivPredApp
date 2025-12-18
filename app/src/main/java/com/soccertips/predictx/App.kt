package com.soccertips.predictx

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.core.content.edit
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.gms.ads.MobileAds
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
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
    lateinit var dailyReminderScheduler: com.soccertips.predictx.notification.DailyReminderScheduler

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
    private var isConsentInitialized = false
    private var isConsentComplete = false

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
     */
    private fun cleanupOldWorkManagerJobs() {
        try {
            Timber.d("WorkManager: Starting cleanup of old jobs")

            val workManager = WorkManager.getInstance(this@App)

            var cancelledCount = 0

            // Cancel all work by tags for match notifications (old format)
            workManager.cancelAllWorkByTag("match_notification")

            // Get and cancel finished manual betting check jobs
            workManager.getWorkInfosByTag("manual_betting_check").get()?.forEach { workInfo ->
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
            workManager.getWorkInfosByTag("end_of_day_check").get()?.forEach { workInfo ->
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
        } catch (e: Exception) {
            Timber.e(e, "WorkManager: Error during cleanup")
        }
    }

    private suspend fun initializeInBackground(startupConfig: DevicePerformanceManager.StartupConfig) {
        withContext(Dispatchers.IO) {
            val isLowMemory = devicePerformanceManager.isLowMemoryDevice()

            // On low-memory devices, delay all initialization to prioritize UI rendering
            if (isLowMemory) {
                delay(500) // Give UI thread a head start
            }

            // Clean up old/stale WorkManager jobs first (lightweight, always do this)
            cleanupOldWorkManagerJobs()

            // Initialize non-critical components in background
            // On low-memory devices, defer this
            if (!startupConfig.skipNonEssentialInit) {
                NotificationHelper.createNotificationChannels(this@App)
            } else {
                // Defer notification channel creation on low-memory devices
                delay(startupConfig.deferFirebaseMs)
                NotificationHelper.createNotificationChannels(this@App)
            }

            // Set up prediction repository dependency
            preloadRepository.setPredictionRepository(predictionRepository)

            // Initialize API config - this is essential, do it early
            initApiConfig()

            // On low-memory devices, defer non-essential schedulers
            if (!isLowMemory) {
                // Initialize betting success checking system
                bettingSuccessScheduler.initialize()

                // Initialize real-time result monitoring system
                realTimeResultMonitor.startMonitoring()
                Timber.d("Real-time result monitoring initialized")

                // Schedule daily reminder notifications
                dailyReminderScheduler.scheduleDailyReminder()
                Timber.d("Daily reminder scheduler initialized")
            } else {
                // Defer these initializations on low-memory devices
                delay(startupConfig.deferFirebaseMs)
                bettingSuccessScheduler.initialize()
                realTimeResultMonitor.startMonitoring()
                Timber.d("Real-time result monitoring initialized (deferred for low-memory device)")
                dailyReminderScheduler.scheduleDailyReminder()
                Timber.d("Daily reminder scheduler initialized (deferred for low-memory device)")
            }

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
        if (isMobileAdsInitialized || isMobileAdsInitializing) {
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
                // Reduce retry count on low-memory devices to avoid blocking
                val maxChecks = if (isLowMemory) 3 else 5
                var checks = 0
                while ((!activity.hasWindowFocus() ||
                            activity.window?.decorView?.isAttachedToWindow != true) && checks < maxChecks
                ) {
                    Timber.w(
                        "Activity not ready for MobileAds init (focus=${activity.hasWindowFocus()}, attached=${activity.window?.decorView?.isAttachedToWindow}). Retrying..."
                    )
                    delay(if (isLowMemory) 500 else 300)
                    checks++

                    if (activity.isFinishing || activity.isDestroyed) {
                        Timber.w(
                            "Activity became invalid during readiness wait, aborting MobileAds init"
                        )
                        isMobileAdsInitializing = false
                        return@withContext
                    }
                }

                if (!activity.hasWindowFocus() ||
                    activity.window?.decorView?.isAttachedToWindow != true
                ) {
                    Timber.w("Activity still not ready for MobileAds init, scheduling retry")
                    isMobileAdsInitializing = false
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(1000)
                        initializeMobileAds()
                    }
                    return@withContext
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
        if (!isMobileAdsInitialized && !isMobileAdsInitializing && isConsentComplete) {
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
                "Consent: Not initializing MobileAds yet (initialized=$isMobileAdsInitialized, initializing=$isMobileAdsInitializing, consentComplete=$isConsentComplete)"
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

        // Initialize consent only once (first activity creation)
        if (!isConsentInitialized) {
            isConsentInitialized = true
            Timber.d(
                "Consent: Initializing for first time with activity: ${activity.javaClass.simpleName}"
            )
            initializeConsent(activity)
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
            devicePerformanceManager.logPerformanceInfo()
        }

        // Update AppOpenAdManager with current activity context
        appOpenAdManager.setActivityContext(activity)
        try {
            FirebaseCrashlytics.getInstance().log("onActivityResumed: ${activity.javaClass.name}")
        } catch (_: Exception) {
        }

        // Initialize MobileAds with delay after activity is fully resumed and stable
        // Only if consent is complete and MobileAds is not already initialized
        if (!isMobileAdsInitialized && !isMobileAdsInitializing && isConsentComplete) {
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
                "MobileAds init check: initialized=$isMobileAdsInitialized, initializing=$isMobileAdsInitializing, consentComplete=$isConsentComplete"
            )
        }

        // Mark app as in foreground
        if (!appInForeground) {
            appInForeground = true
            appOpenAdManager.onAppForegrounded()

            // Check if ads can be shown - skip on low-memory devices during initial start
            val isLowMemory = devicePerformanceManager.isLowMemoryDevice()
            if (isMobileAdsInitialized && !isInitialAppStart && !(isLowMemory && isInitialAppStart)) {
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
                    "AppOpenAdManager: Not showing ad on resume (initialization state: ads initialized=${isMobileAdsInitialized}, initialAppStart=${isInitialAppStart}, lowMemory=$isLowMemory)"
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
