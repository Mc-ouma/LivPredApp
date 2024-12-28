package com.soccertips.predcompose

import android.app.Application
import com.soccertips.predcompose.data.local.AppDatabase

import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class App : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
