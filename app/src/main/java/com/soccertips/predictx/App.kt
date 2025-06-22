package com.soccertips.predictx

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.android.gms.ads.MobileAds
import com.soccertips.predictx.admob.AppOpenAdManager
import com.soccertips.predictx.notification.NotificationHelper
import com.soccertips.predictx.repository.PredictionRepository
import com.soccertips.predictx.repository.PreloadRepository
import com.soccertips.predictx.util.NetworkTaggingInitializer
import com.soccertips.predictx.util.StrictModeUtil
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import androidx.core.content.edit

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
    lateinit var apiConfigProvider: com.soccertips.predictx.repository.ApiConfigProvider

    @Inject
    lateinit var networkTaggingInitializer: NetworkTaggingInitializer

    @Inject
    lateinit var appOpenAdManager: AppOpenAdManager
    private var currentActivity: Activity? = null

    // Track app foreground status
    private var appInForeground = false

    // Flag to avoid showing ads during initial app startup
    private var isInitialAppStart = true

    // Track when Mobile Ads SDK has been initialized
    private var isMobileAdsInitialized = false

    // Shared preferences key for first launch check
    private val PREFS_NAME = "app_preferences"
    private val KEY_FIRST_LAUNCH = "is_first_launch"
    private val KEY_APP_INITIALIZED = "is_app_initialized"

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Initialize network tagging early to prevent socket violations
        networkTaggingInitializer.initialize()

        initApiConfig()

        preloadRepository.setPredictionRepository(predictionRepository)

        NotificationHelper.createNotificationChannels(this)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            StrictModeUtil.enableStrictModeForIntentViolations()

            // Add StrictMode policy for edge-to-edge issues
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                StrictMode.setVmPolicy(
                    StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
                        .detectAll()
                        .penaltyLog()
                        .build()
                )
            }
        }

        // Check if the app has been initialized before
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val appInitialized = prefs.getBoolean(KEY_APP_INITIALIZED, false)

        if (!appInitialized) {
            // First time app initialization - mark it
            prefs.edit {
                putBoolean(KEY_APP_INITIALIZED, true)
            }
            // Keep isInitialAppStart as true to avoid showing ads
        } else {
            // App has been initialized before, we can potentially show ads sooner
            isInitialAppStart = false
            Timber.d("App has been initialized before, ready for ads")
        }
        // Initialize Mobile Ads SYNCHRONOUSLY on main thread first
        MobileAds.initialize(this) { initializationStatus ->
            Timber.d("MobileAds initialized with status: $initializationStatus")

            // Setup app open ad manager
            setupAppOpenAdManager()

            // Mark Mobile Ads as initialized
            isMobileAdsInitialized = true

            // If this is not the first launch and app was previously initialized, allow ads
            if (!isFirstLaunch() && !appInitialized) {
                isInitialAppStart = false
                Timber.d("AppOpenAdManager: Ready for ads after MobileAds initialization")
            }

            Timber.d("AppOpenAdManager: Final state - ads initialized=$isMobileAdsInitialized, initialAppStart=$isInitialAppStart")
        }

        CoroutineScope(Dispatchers.IO).launch {
            preloadRepository.preloadCategoryData()
        }
        registerActivityLifecycleCallbacks(this)
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
    }

    private fun initApiConfig() {
        CoroutineScope(Dispatchers.IO).launch {
            Timber.d("Starting to fetch API config from Firebase...")
            firebaseRepository.getApiConfig().collect { result ->
                result.onSuccess { configMap ->
                    Timber.d("API config successfully fetched from Firebase: $configMap")
                    // Log the specific keys we're looking for
                    Timber.d("API_KEY value from Firebase: ${configMap["API_KEY"]}")
                    Timber.d("API_HOST value from Firebase: ${configMap["API_HOST"]}")

                    apiConfigProvider.updateConfig(configMap)
                    Timber.d("ApiConfigProvider updated - API Key: ${apiConfigProvider.getApiKey()}, Host: ${apiConfigProvider.getApiHost()}")
                }.onFailure { error ->
                    Timber.e(error, "Failed to fetch API config from Firebase")
                }
            }
        }
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?
    ) {
        currentActivity = activity
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }

    override fun onActivityPaused(activity: Activity) {
        // Not needed but must be implemented
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity

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
                Timber.d("AppOpenAdManager: Not showing ad on resume (initialization state: ads initialized=${isMobileAdsInitialized}, initialAppStart=${isInitialAppStart})")
            }
        }
    }

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle
    ) {
        // No specific action needed but must be implemented
    }

    override fun onActivityStarted(activity: Activity) {
        val isFirstLaunchCheck = isFirstLaunch()

        // Set current activity
        currentActivity = activity

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
            Timber.d("AppOpenAdManager: Not showing app open ad on start (initialization state: ads initialized=${isMobileAdsInitialized}, initialAppStart=${isInitialAppStart}, firstLaunch=${isFirstLaunchCheck})")
        }
    }

    override fun onActivityStopped(activity: Activity) {
        // When app is stopped, mark it as backgrounded
        if (activity.isFinishing) {
            appInForeground = false
            appOpenAdManager.onAppBackgrounded()
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
            appOpenAdManager.showAdIfAvailable(activity) {
                Timber.d("AppOpenAdManager: App open ad shown or dismissed")
                // Any post-ad display actions can go here
            }
        } else {
            Timber.d("AppOpenAdManager: No app open ad available to show")
            // Ensure we have an ad ready for next time
            appOpenAdManager.loadAppOpenAd()
        }
    }
}
