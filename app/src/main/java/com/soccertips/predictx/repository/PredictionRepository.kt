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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okio.IOException
import retrofit2.HttpException
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.pow

class PredictionRepository @Inject constructor(
    private val apiService: ApiService,
    private val preloadRepository: Lazy<PreloadRepository>,
    @ApplicationContext private val context: Context,
    private val apiConfigProvider: ApiConfigProvider
) {
    private val gson = Gson()

    // Firebase database reference
    private val database = Firebase.database.reference

    // Cache for storing fallback responses to avoid excessive Firebase reads
    private val fallbackCache = mutableMapOf<String, Pair<Long, RootResponse>>()
    private val fallbackCacheExpirationMs = 30 * 60 * 1000 // 30 minutes

    suspend fun getCategoryData(url: String): RootResponse {
        // Check if the URL is a valid endpoint in preloaded data
        preloadRepository.value.getPreloadedData(url)?.let {
            Timber.d("Returning preloaded data for $url")
            return it
        }

        // Check if we have a recent fallback response cached
        val cachedFallback = fallbackCache[url]
        if (cachedFallback != null) {
            val (timestamp, response) = cachedFallback
            if (System.currentTimeMillis() - timestamp < fallbackCacheExpirationMs) {
                Timber.d("Returning cached fallback data for $url")
                return response
            } else {
                // Remove expired cache entry
                fallbackCache.remove(url)
            }
        }

        // Try direct API call first - no automatic fallback
        try {
            val response = apiService.getServerResponses(url)
            return response
        } catch (e: HttpException) {
            // Only trigger fallback for specific HTTP errors from OkHttp
            if (e.code() == 403) {
                Timber.w("Received 403 Forbidden from API, trying Firebase fallback")
                return tryFirebaseFallback(url, e)
            } else {
                // For other HTTP errors, retry with exponential backoff
                return retryApiCallWithBackoff(url, e)
            }
        } catch (e: IOException) {
            // For network errors, retry with exponential backoff
            return retryApiCallWithBackoff(url, e)
        } catch (e: Exception) {
            // For unexpected errors, log and rethrow
            Timber.e(e, "Unexpected error fetching data from API")
            throw e
        }
    }

    private suspend fun retryApiCallWithBackoff(url: String, initialException: Exception, maxRetries: Int = 2): RootResponse {
        var retryCount = 0
        var lastException = initialException

        // Retry with exponential backoff
        while (retryCount < maxRetries) {
            retryCount++
            try {
                // Calculate exponential backoff delay
                val delayMs = 1000L * (2.0.pow(retryCount.toDouble())).toLong()
                Timber.d("Retrying API call after $delayMs ms (attempt $retryCount of $maxRetries)")
                kotlinx.coroutines.delay(delayMs)

                // Try API call again
                return apiService.getServerResponses(url)
            } catch (e: HttpException) {
                lastException = e
                // If we encounter a 403, immediately go to fallback
                if (e.code() == 403) {
                    Timber.w("Received 403 Forbidden from API during retry, trying Firebase fallback")
                    return tryFirebaseFallback(url, e)
                }
                // Otherwise continue with retries
            } catch (e: IOException) {
                lastException = e
                // Continue with retries for network errors
            }
        }

        // If we've exhausted all retries, try fallback as last resort
        Timber.d("All retries failed, trying Firebase fallback as last resort")
        return tryFirebaseFallback(url, lastException)
    }

    private suspend fun tryFirebaseFallback(url: String, exception: Exception): RootResponse {
        try {
            val response = fetchFromFirebase(url)
            if (response != null) {
                // Cache the successful fallback response
                fallbackCache[url] = System.currentTimeMillis() to response
                Timber.d("Firebase fallback successful for $url")
                return response
            } else {
                Timber.e("Firebase fallback returned null for $url")
                throw exception
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in Firebase fallback")
            throw exception
        }
    }

    private suspend fun fetchFromFirebase(url: String): RootResponse? = withContext(Dispatchers.IO) {
        // First get the category key before creating the suspendCancellableCoroutine
        val categoryKey = getCategoryKeyFromUrl(url)

        if (categoryKey == null) {
            Timber.e("Could not determine category key from URL: $url")
            return@withContext null
        }

        return@withContext suspendCancellableCoroutine { continuation ->
            var isResumed = false // Flag to track if continuation has been resumed

            try {
                Timber.d("Fetching data from Firebase for category: $categoryKey")

                // Path to the latest prediction data for this category
                val predictionRef = database.child("predictions").child(categoryKey).child("current").child("data")

                predictionRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        synchronized(this) {
                            if (isResumed || !continuation.isActive) return
                            isResumed = true
                        }

                        if (snapshot.exists()) {
                            try {
                                // Convert the Firebase snapshot to JSON string
                                val dataJson = gson.toJson(snapshot.value)

                                // Parse the JSON into our RootResponse model
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

                // Set timeout for Firebase read
                val timeoutRunnable = Runnable {
                    synchronized(this) {
                        if (isResumed || !continuation.isActive) return@Runnable
                        isResumed = true
                    }

                    Timber.e("Firebase read timed out for $url")
                    continuation.resume(null)
                }

                val handler = android.os.Handler(context.mainLooper)
                handler.postDelayed(timeoutRunnable, TimeUnit.SECONDS.toMillis(15))

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
     * Extract the category key from the URL by matching it with Firebase categories
     */
    private suspend fun getCategoryKeyFromUrl(url: String): String? = withContext(Dispatchers.IO) {
        return@withContext suspendCancellableCoroutine { continuation ->
            var isResumed = false // Flag to track if continuation has been resumed

            // Get the categories reference
            val categoriesRef = database.child("categories")

            categoriesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    synchronized(this) {
                        if (isResumed || !continuation.isActive) return
                        isResumed = true
                    }

                    if (snapshot.exists()) {
                        for (categorySnapshot in snapshot.children) {
                            val categoryUrl = categorySnapshot.child("url").getValue(String::class.java)
                            if (categoryUrl == url) {
                                val categoryKey = categorySnapshot.key
                                Timber.d("Found category key $categoryKey for URL: $url")
                                continuation.resume(categoryKey)
                                return
                            }
                        }
                        // If we get here, we didn't find a matching URL
                        Timber.e("No matching category found in Firebase for URL: $url")
                        continuation.resume(null)
                    } else {
                        Timber.e("No categories found in Firebase")
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
        }
    }
}