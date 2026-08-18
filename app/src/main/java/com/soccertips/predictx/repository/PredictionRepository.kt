package com.soccertips.predictx.repository

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.gson.Gson
import com.soccertips.predictx.data.model.RootResponse
import com.soccertips.predictx.network.ApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.IOException
import retrofit2.HttpException
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.pow

@Singleton
class PredictionRepository @Inject constructor(
    private val apiService: ApiService,
    private val preloadRepository: Lazy<PreloadRepository>,
    @ApplicationContext private val context: Context,
    private val apiConfigProvider: ApiConfigProvider
) {
    private val gson = Gson()
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Firebase database reference
    private val database = Firebase.database.reference

    // In-memory cache for API / preloaded responses (15 minutes fresh TTL)
    private val memoryCache = ConcurrentHashMap<String, Pair<Long, RootResponse>>()
    private val memoryCacheTtlMs = 15 * 60 * 1000L // 15 minutes

    // Cache for storing fallback responses to avoid excessive Firebase reads (30 minutes TTL)
    private val fallbackCache = ConcurrentHashMap<String, Pair<Long, RootResponse>>()
    private val fallbackCacheExpirationMs = 30 * 60 * 1000L // 30 minutes

    // In-flight request deduplication map to prevent redundant concurrent network calls
    private val inFlightRequests = ConcurrentHashMap<String, Deferred<RootResponse>>()
    private val requestMutex = Mutex()

    private val diskPrefs = context.getSharedPreferences("prediction_disk_cache", Context.MODE_PRIVATE)

    /**
     * Stale-While-Revalidate Flow:
     * 1. Emits cached/stale data immediately if available (0ms cold start)
     * 2. Revalidates fresh data from the network/Firebase in the background
     * 3. Emits fresh data once retrieved
     */
    fun getCategoryDataFlow(url: String): Flow<RootResponse> = flow {
        // Step 1: Emit stale/cached data immediately if available (0ms load)
        val cachedResponse = getCachedData(url)
        if (cachedResponse != null) {
            Timber.d("Stale-While-Revalidate: Emitting cached data immediately for $url")
            emit(cachedResponse)
        }

        // Step 2: Fetch fresh data from network or Firebase fallback
        try {
            val freshResponse = fetchCategoryDataDirect(url)
            // Emit fresh data if no stale data was emitted or if the fresh data is updated
            if (cachedResponse == null || freshResponse != cachedResponse) {
                Timber.d("Stale-While-Revalidate: Emitting fresh data for $url")
                emit(freshResponse)
            }
        } catch (e: Exception) {
            if (cachedResponse == null) {
                Timber.e(e, "Stale-While-Revalidate: Network fetch failed and no cache available for $url")
                throw e
            } else {
                Timber.w(e, "Stale-While-Revalidate: Fresh fetch failed for $url, keeping cached data")
            }
        }
    }

    suspend fun getCategoryData(url: String, forceRefresh: Boolean = false): RootResponse {
        if (!forceRefresh) {
            // Check preloaded, memory, or disk cache
            val cached = getCachedData(url)
            if (cached != null) {
                val cachedTimestamp = memoryCache[url]?.first
                if (cachedTimestamp != null && System.currentTimeMillis() - cachedTimestamp < memoryCacheTtlMs) {
                    Timber.d("Returning fresh in-memory data for $url")
                    return cached
                } else if (preloadRepository.value.getPreloadedData(url) != null) {
                    Timber.d("Returning preloaded data for $url")
                    return cached
                }
            }
        }

        // Fetch fresh data with in-flight deduplication
        return fetchCategoryDataDirect(url)
    }

    /**
     * Helper to get any available cached data (preloaded, memory, fallback, or persistent disk snapshot).
     */
    fun getCachedData(url: String): RootResponse? {
        preloadRepository.value.getPreloadedData(url)?.let { return it }
        memoryCache[url]?.let { (timestamp, response) ->
            if (System.currentTimeMillis() - timestamp < memoryCacheTtlMs * 2) {
                return response
            }
        }
        fallbackCache[url]?.let { (timestamp, response) ->
            if (System.currentTimeMillis() - timestamp < fallbackCacheExpirationMs) {
                return response
            }
        }
        getFromDiskCache(url)?.let { return it }
        return null
    }

    private fun saveToCaches(url: String, response: RootResponse) {
        val now = System.currentTimeMillis()
        memoryCache[url] = now to response
        fallbackCache[url] = now to response
        preloadRepository.value.putPreloadedData(url, response)
        saveToDiskCache(url, response)
    }

    private fun saveToDiskCache(url: String, response: RootResponse) {
        try {
            val key = "cache_" + url.hashCode().toString()
            val json = gson.toJson(response)
            diskPrefs.edit().putString(key, json).putLong("${key}_ts", System.currentTimeMillis()).apply()
        } catch (e: Exception) {
            Timber.w(e, "Failed to save prediction response to disk cache for $url")
        }
    }

    private fun getFromDiskCache(url: String): RootResponse? {
        return try {
            val key = "cache_" + url.hashCode().toString()
            val json = diskPrefs.getString(key, null) ?: return null
            val response = gson.fromJson(json, RootResponse::class.java)
            if (response != null && response.serverResponse.isNotEmpty()) {
                response
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to read prediction response from disk cache for $url")
            null
        }
    }

    /**
     * Executes the network fetch with in-flight request deduplication so multiple
     * concurrent callers for the same URL share a single HTTP call.
     */
    private suspend fun fetchCategoryDataDirect(url: String): RootResponse {
        val deferred = requestMutex.withLock {
            inFlightRequests.getOrPut(url) {
                repositoryScope.async {
                    try {
                        executeFetch(url)
                    } finally {
                        inFlightRequests.remove(url)
                    }
                }
            }
        }
        return deferred.await()
    }

    private suspend fun executeFetch(url: String): RootResponse {
        try {
            val response = apiService.getServerResponses(url)
            saveToCaches(url, response)
            Timber.d("Direct API call successful and cached for $url")
            return response
        } catch (e: HttpException) {
            if (e.code() == 403 || e.code() == 404) {
                Timber.w("Received HTTP ${e.code()} from API, trying Firebase fallback for $url")
                return tryFirebaseFallback(url, e)
            } else {
                return retryApiCallWithBackoff(url, e)
            }
        } catch (e: IOException) {
            return retryApiCallWithBackoff(url, e)
        } catch (e: Exception) {
            Timber.w(e, "Error fetching from API for $url, attempting fallback")
            return tryFirebaseFallback(url, e)
        }
    }

    private suspend fun retryApiCallWithBackoff(url: String, initialException: Exception): RootResponse {
        // Fast single retry after 300ms
        try {
            kotlinx.coroutines.delay(300L)
            val response = apiService.getServerResponses(url)
            saveToCaches(url, response)
            Timber.d("API fast retry successful for $url")
            return response
        } catch (e: Exception) {
            Timber.w(e, "Fast retry failed for $url, attempting Firebase / disk fallback")
        }

        return tryFirebaseFallback(url, initialException)
    }

    private suspend fun tryFirebaseFallback(url: String, exception: Exception): RootResponse {
        try {
            val response = fetchFromFirebase(url)
            if (response != null && response.serverResponse.isNotEmpty()) {
                saveToCaches(url, response)
                Timber.d("Firebase fallback successful for $url")
                return response
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in Firebase fallback for $url")
        }

        // Last-resort resilience: Check persistent disk snapshot before failing
        val diskSnapshot = getFromDiskCache(url)
        if (diskSnapshot != null && diskSnapshot.serverResponse.isNotEmpty()) {
            Timber.d("Serving last-known-good disk snapshot for $url")
            memoryCache[url] = System.currentTimeMillis() to diskSnapshot
            return diskSnapshot
        }

        throw exception
    }

    private suspend fun fetchFromFirebase(url: String): RootResponse? = withContext(Dispatchers.IO) {
        val categoryKey = getCategoryKeyFromUrl(url)

        return@withContext suspendCancellableCoroutine { continuation ->
            var isResumed = false

            try {
                Timber.d("Fetching data from Firebase for category: $categoryKey")
                val predictionRef = database.child("predictions").child(categoryKey).child("current").child("data")

                predictionRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        synchronized(this) {
                            if (isResumed || !continuation.isActive) return
                            isResumed = true
                        }

                        if (snapshot.exists()) {
                            try {
                                val dataJson = gson.toJson(snapshot.value)
                                val response = gson.fromJson(dataJson, RootResponse::class.java)

                                if (response != null && response.serverResponse.isNotEmpty()) {
                                    Timber.d("Successfully fetched fallback data from Firebase for $url")
                                    continuation.resume(response)
                                } else {
                                    Timber.e("Firebase data exists but could not be parsed for $url")
                                    continuation.resume(null)
                                }
                            } catch (e: Exception) {
                                Timber.e(e, "Error parsing Firebase data for $url")
                                continuation.resume(null)
                            }
                        } else {
                            Timber.e("No data found in Firebase for category $categoryKey")
                            continuation.resume(null)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        synchronized(this) {
                            if (isResumed || !continuation.isActive) return
                            isResumed = true
                        }

                        Timber.e("Firebase error: ${error.message}")
                        continuation.resumeWithException(error.toException())
                    }
                })

                // Fast 5-second timeout for Firebase fallback
                val timeoutRunnable = Runnable {
                    synchronized(this) {
                        if (isResumed || !continuation.isActive) return@Runnable
                        isResumed = true
                    }

                    Timber.e("Firebase read timed out for $url")
                    continuation.resume(null)
                }

                val handler = android.os.Handler(context.mainLooper)
                handler.postDelayed(timeoutRunnable, TimeUnit.SECONDS.toMillis(5))

                continuation.invokeOnCancellation {
                    handler.removeCallbacks(timeoutRunnable)
                }

            } catch (e: Exception) {
                synchronized(this) {
                    if (isResumed || !continuation.isActive) return@suspendCancellableCoroutine
                    isResumed = true
                }

                Timber.e(e, "Error initializing Firebase fallback")
                continuation.resumeWithException(e)
            }
        }
    }

    /**
     * Instantly extracts the category slug from the URL without a network query.
     * Examples:
     * - "https://api.scorecastapp.com/storage/json/json_betofday.json" -> "betofday"
     * - "today.php" -> "today"
     * - "sure2.php" -> "sure2"
     * - "daily_bonus.php" -> "daily_bonus"
     */
    private fun getCategoryKeyFromUrl(url: String): String {
        val cleanSlug = url.substringAfterLast("/")
            .substringBefore("?")
            .removePrefix("json_")
            .removeSuffix(".json")
            .removeSuffix(".php")
            .trim()

        return if (cleanSlug.isNotBlank()) cleanSlug else "today"
    }
}