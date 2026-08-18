package com.soccertips.predictx.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.soccertips.predictx.data.model.Category
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import timber.log.Timber

/**
 * Repository that provides categories with a multi-tier fallback strategy:
 * 1. Firebase Realtime Database (with 10s timeout)
 * 2. Firebase Remote Config (cached HTTP, works better on slow networks)
 * 3. SharedPreferences local cache (last successful fetch)
 */
@Singleton
class CategoryRepository @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val remoteConfig: FirebaseRemoteConfig,
    private val sharedPreferences: SharedPreferences,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "CategoryRepository"
        private const val FIREBASE_TIMEOUT_MS = 10_000L
        private const val PREF_CACHED_CATEGORIES = "cached_categories_json"
        private const val PREF_CACHE_TIMESTAMP = "cached_categories_timestamp"
        private const val REMOTE_CONFIG_CATEGORIES_KEY = "categories_json"
    }

    @Volatile
    private var memoryCachedCategories: List<Category>? = null

    /**
     * Provides categories via Stale-While-Revalidate:
     * 1. Immediately emits local cached categories for 0ms instant cold-start rendering
     * 2. Revalidates fresh categories from Firebase / Remote Config in the background
     * 3. Emits updated categories only if fresh data differs from the cached data
     */
    fun getCategories(): Flow<Result<List<Category>>> = flow {
        // Step 1: Immediately emit local cached categories (Stale)
        val cached = getCachedCategories()
        if (cached.isNotEmpty()) {
            Timber.tag(TAG).d("Emitting ${cached.size} categories from local cache (0ms instant render)")
            emit(Result.success(cached))
        }

        // Step 2: Background Revalidation from Firebase Realtime Database
        var freshCategories: List<Category>? = null
        try {
            val result = withTimeout(FIREBASE_TIMEOUT_MS) {
                firebaseRepository.getCategories().first()
            }
            val categories = result.getOrNull()
            if (!categories.isNullOrEmpty()) {
                Timber.tag(TAG).d("Categories revalidated from Firebase Realtime Database")
                freshCategories = categories
            }
        } catch (e: TimeoutCancellationException) {
            Timber.tag(TAG).w("Firebase Realtime Database timed out after ${FIREBASE_TIMEOUT_MS}ms")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Firebase Realtime Database revalidation failed")
        }

        // Step 3: Fallback Revalidation from Firebase Remote Config if Realtime DB didn't return
        if (freshCategories == null) {
            try {
                val categories = fetchFromRemoteConfig()
                if (categories.isNotEmpty()) {
                    Timber.tag(TAG).d("Categories revalidated from Remote Config")
                    freshCategories = categories
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Remote Config fallback revalidation failed")
            }
        }

        // Step 4: Handle fresh categories or fallback
        if (!freshCategories.isNullOrEmpty()) {
            cacheCategories(freshCategories)
            // Emit if no cached data was previously emitted or if data changed
            if (cached.isEmpty() || freshCategories != cached) {
                Timber.tag(TAG).d("Emitting fresh categories (${freshCategories.size} items)")
                emit(Result.success(freshCategories))
            }
        } else if (cached.isEmpty()) {
            // Only emit failure if nothing was previously emitted from cache
            emit(Result.failure(Exception("Unable to load categories from any source")))
        }
    }

    private fun getIconName(resId: Int): String? {
        return when (resId) {
            com.soccertips.predictx.R.drawable.ic_trending_up_24 -> "ic_trending_up_24"
            com.soccertips.predictx.R.drawable.ic_compare_arrows_24 -> "ic_compare_arrows_24"
            com.soccertips.predictx.R.drawable.ic_filter_2_24 -> "ic_filter_2_24"
            com.soccertips.predictx.R.drawable.ic_star_24 -> "ic_star_24"
            com.soccertips.predictx.R.drawable.ic_dashboard_customize_24 -> "ic_dashboard_customize_24"
            com.soccertips.predictx.R.drawable.ic_hourglass_split_24 -> "ic_hourglass_split_24"
            com.soccertips.predictx.R.drawable.ic_house_24 -> "ic_house_24"
            else -> null
        }
    }

    private suspend fun fetchFromRemoteConfig(): List<Category> {
        try {
            remoteConfig.fetchAndActivate().await()
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Remote Config fetch failed, using cached values")
        }

        val json = remoteConfig.getString(REMOTE_CONFIG_CATEGORIES_KEY)
        if (json.isBlank() || json == "[]" || json == "{}") return emptyList()

        val type = object : TypeToken<Map<String, CategoryDto>>() {}.type
        val dtosMap: Map<String, CategoryDto> = gson.fromJson(json, type)
        return dtosMap.values.map { it.toCategory(firebaseRepository) }
    }

    private fun cacheCategories(categories: List<Category>) {
        memoryCachedCategories = categories
        val dtos = categories.map { cat ->
            CategoryDto(
                url = cat.url,
                name = cat.name,
                iconResId = getIconName(cat.iconResId),
                colorHex = cat.colorHex
            )
        }
        val json = gson.toJson(dtos)
        sharedPreferences.edit {
            putString(PREF_CACHED_CATEGORIES, json)
            putLong(PREF_CACHE_TIMESTAMP, System.currentTimeMillis())
        }
    }

    fun getCachedCategories(): List<Category> {
        memoryCachedCategories?.let { if (it.isNotEmpty()) return it }
        val json = sharedPreferences.getString(PREF_CACHED_CATEGORIES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<CategoryDto>>() {}.type
            val dtos: List<CategoryDto> = gson.fromJson(json, type)
            val categories = dtos.map { it.toCategory(firebaseRepository) }
            memoryCachedCategories = categories
            categories
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to parse cached categories")
            emptyList()
        }
    }

    fun getCachedOrSeedCategories(): List<Category> {
        val cached = getCachedCategories()
        if (cached.isNotEmpty()) return cached
        return listOf(
            Category(url = "today.php", name = "Today Tips", iconResId = com.soccertips.predictx.R.drawable.ic_trending_up_24, colorHex = "#4CAF50"),
            Category(url = "sure2.php", name = "Sure 2 Odds", iconResId = com.soccertips.predictx.R.drawable.ic_filter_2_24, colorHex = "#2196F3"),
            Category(url = "daily_bonus.php", name = "Daily Bonus", iconResId = com.soccertips.predictx.R.drawable.ic_star_24, colorHex = "#FF9800")
        )
    }
}

/**
 * DTO matching the Firebase Realtime Database / Remote Config structure.
 * Remote Config uses the same JSON format as Firebase: a map of category keys to objects.
 */
private data class CategoryDto(
    val url: String = "",
    val name: String = "",
    val iconResId: String? = null,
    val colorHex: String? = null,
    val requiresRewardAd: Boolean = false
) {
    fun toCategory(firebaseRepository: FirebaseRepository): Category {
        return Category(
            url = url,
            name = name,
            iconResId = firebaseRepository.getIconResourceId(iconResId),
            colorHex = colorHex
        )
    }
}
