package com.soccertips.predcompose

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class App : Application() {
   /* override val workManagerConfiguration: Configuration by lazy {
        Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    }*/

    override fun onCreate() {
        super.onCreate()
        //WorkManager.initialize(this, workManagerConfiguration)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }


}
