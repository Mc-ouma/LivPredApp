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
    lateinit var networkTaggingInitializer: NetworkTaggingInitializer

   override val workManagerConfiguration: Configuration
           get() = Configuration.Builder()
               .setWorkerFactory(workerFactory)
               .build()

    override fun onCreate() {
        super.onCreate()

        // Initialize network tagging early to prevent socket violations
        networkTaggingInitializer.initialize()

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
}
