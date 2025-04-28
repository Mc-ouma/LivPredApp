package com.soccertips.predictx.util

import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.startup.Initializer
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class WorkManagerInitializer : Initializer<WorkManager> {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var configuration: Configuration

    override fun create(@ApplicationContext context: Context): WorkManager {
        WorkManager.Companion.initialize(context, configuration)
        return WorkManager.Companion.getInstance(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}