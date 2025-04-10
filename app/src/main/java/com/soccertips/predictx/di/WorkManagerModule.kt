package com.soccertips.predictx.di

import android.content.Context
import androidx.work.WorkManager
import com.soccertips.predictx.util.WorkManagerWrapper
import com.soccertips.predictx.util.WorkManagerWrapperImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WorkManagerModule {
    @Binds
    abstract fun bindWorkManagerWrapper(
        workManagerWrapperImpl: WorkManagerWrapperImpl
    ): WorkManagerWrapper

    companion object {
        @Provides
        @Singleton
        fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
            return WorkManager.getInstance(context)
        }
    }

}