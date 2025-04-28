package com.soccertips.predictx

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.soccertips.predictx.notification.NotificationHelper
import com.soccertips.predictx.repository.PredictionRepository
import com.soccertips.predictx.repository.PreloadRepository
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

   override val workManagerConfiguration: Configuration
           get() = Configuration.Builder()
               .setWorkerFactory(workerFactory)
               .build()

    override fun onCreate() {
        super.onCreate()

        preloadRepository.setPredictionRepository(predictionRepository)

        NotificationHelper.createNotificationChannels(this)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        CoroutineScope(Dispatchers.IO).launch {
            preloadRepository.preloadCategoryData()
        }
    }


}
