package com.soccertips.predictx.repository

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.soccertips.predictx.data.model.Announcement
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import timber.log.Timber

@Singleton
class RemoteConfigRepository
@Inject
constructor(private val remoteConfig: FirebaseRemoteConfig, private val gson: Gson) {
    companion object {
        private const val ANNOUNCEMENTS_KEY = "app_announcements"
        private const val FETCH_INTERVAL = 1800L // 30 minutes in seconds
    }

    init {
        val configSettings = remoteConfigSettings { minimumFetchIntervalInSeconds = FETCH_INTERVAL }
        remoteConfig.setConfigSettingsAsync(configSettings)

        // Set default values
        val defaults = mapOf(ANNOUNCEMENTS_KEY to "[]")
        remoteConfig.setDefaultsAsync(defaults)
    }

    suspend fun fetchAndActivate(): Boolean {
        return try {
            remoteConfig.fetchAndActivate().await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch remote config")
            false
        }
    }

    fun getAnnouncements(): Flow<List<Announcement>> = flow {
        try {
            // Fetch and activate new values
            fetchAndActivate()

            val announcementsJson = remoteConfig.getString(ANNOUNCEMENTS_KEY)
            Timber.d("Announcements JSON: $announcementsJson")

            if (announcementsJson.isNotBlank()) {
                val type = object : TypeToken<List<Announcement>>() {}.type
                val announcements: List<Announcement> = gson.fromJson(announcementsJson, type)

                // Filter visible announcements and sort by priority
                val visibleAnnouncements =
                        announcements.filter { it.isVisible }.sortedByDescending { it.priority }

                emit(visibleAnnouncements)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse announcements")
            emit(emptyList())
        }
    }
}
