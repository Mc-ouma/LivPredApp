package com.soccertips.predictx.repository

import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.soccertips.predictx.data.model.Announcement
import java.util.Locale
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
        private const val CATEGORY_AD_STRATEGY_KEY = "category_ad_strategy"
        private const val FETCH_INTERVAL = 1800L // 30 minutes in seconds

        // Ad strategy variants
        const val AD_STRATEGY_REWARDED = "rewarded"
        const val AD_STRATEGY_INTERSTITIAL = "interstitial"
    }

    init {
        val configSettings = remoteConfigSettings { minimumFetchIntervalInSeconds = FETCH_INTERVAL }
        remoteConfig.setConfigSettingsAsync(configSettings)

        // Set default values
        val defaults = mapOf(
            ANNOUNCEMENTS_KEY to "[]",
            CATEGORY_AD_STRATEGY_KEY to AD_STRATEGY_REWARDED // Default to rewarded ads
        )
        remoteConfig.setDefaultsAsync(defaults)
    }

    /**
     * Get the current app language code.
     * Returns the app's locale if set, otherwise the system default.
     */
    private fun getCurrentLanguageCode(): String {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        return if (!appLocales.isEmpty) {
            appLocales.toLanguageTags().split("-").firstOrNull() ?: "en"
        } else {
            Locale.getDefault().language
        }
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

    /**
     * Get the current language code for localization.
     * Can be used by UI components to get localized announcement text.
     */
    fun getLanguageCode(): String = getCurrentLanguageCode()

    /**
     * Get the ad strategy for category unlocking
     * Returns either "rewarded" or "interstitial"
     */
    suspend fun getCategoryAdStrategy(): String {
        return try {
            fetchAndActivate()
            val strategy = remoteConfig.getString(CATEGORY_AD_STRATEGY_KEY)
            Timber.d("Category Ad Strategy: $strategy")
            strategy.ifBlank { AD_STRATEGY_REWARDED }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get category ad strategy, using default")
            AD_STRATEGY_REWARDED
        }
    }
}
