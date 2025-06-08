package com.soccertips.predictx

import android.app.Application
import android.os.Build
import android.os.StrictMode
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
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

@HiltAndroidApp
class App : Application(), Configuration.Provider {


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

        CoroutineScope(Dispatchers.IO).launch {
            preloadRepository.preloadCategoryData()
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
}
