package com.soccertips.predcompose.di

import android.content.Context
import androidx.work.WorkManager
import com.soccertips.predcompose.util.WorkManagerWrapper
import com.soccertips.predcompose.util.WorkManagerWrapperImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WorkManagerModule {

    @Provides
    @Singleton
    fun provideWorkManager(context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideWorkManagerWrapper(workManager: WorkManager): WorkManagerWrapper {
        return WorkManagerWrapperImpl(workManager)
    }
}