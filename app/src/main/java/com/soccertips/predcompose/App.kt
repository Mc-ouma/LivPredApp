package com.soccertips.predcompose

import android.app.Application
import com.soccertips.predcompose.notification.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        NotificationHelper.createNotificationChannels(this)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

}
