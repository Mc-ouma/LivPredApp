package com.soccertips.predictx.repository

import com.soccertips.predictx.data.model.Category
import com.soccertips.predictx.data.model.RootResponse
import com.soccertips.predictx.util.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

sealed class PreloadNetworkState {
    object Available : PreloadNetworkState()
    object Unavailable : PreloadNetworkState()
    object Loading : PreloadNetworkState()
    object Done : PreloadNetworkState()
}

@Singleton
class PreloadRepository @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val categoryRepository: dagger.Lazy<CategoryRepository>,
    private val networkUtils: NetworkUtils
) {
    // Cache for preloaded data
    private val preloadedData = ConcurrentHashMap<String, RootResponse>()
    private lateinit var predictionRepository: PredictionRepository

    // Network state flow to be observed by UI components
    private val _networkState = MutableStateFlow<PreloadNetworkState>(PreloadNetworkState.Available)
    val networkState: StateFlow<PreloadNetworkState> = _networkState

    fun setPredictionRepository(predictionRepository: PredictionRepository) {
        if (!::predictionRepository.isInitialized) {
            this.predictionRepository = predictionRepository
        }
    }

    // Start preloading data immediately without waiting for Firebase network roundtrip
    suspend fun preloadCategoryData() {
        if (!networkUtils.isNetworkAvailable()) {
            Timber.d("Network is not available. Skipping preload.")
            _networkState.value = PreloadNetworkState.Unavailable
            return
        }
        _networkState.value = PreloadNetworkState.Loading

        // Fast Path (0ms): Read local cached categories or default seed endpoints immediately
        val immediateCategories = try {
            categoryRepository.get().getCachedOrSeedCategories()
        } catch (e: Exception) {
            Timber.w(e, "Error reading cached categories for preloading")
            emptyList()
        }

        if (immediateCategories.isNotEmpty()) {
            preloadCategories(immediateCategories)
        }

        // Secondary Background Path: Revalidate with Firebase and preload any new categories
        CoroutineScope(Dispatchers.IO).launch {
            try {
                firebaseRepository.getCategories().collect { result ->
                    result.fold(
                        onSuccess = { freshCategories ->
                            val uncollectedCategories = freshCategories.filter {
                                it.url.isNotBlank() && !preloadedData.containsKey(it.url)
                            }
                            if (uncollectedCategories.isNotEmpty()) {
                                preloadCategories(uncollectedCategories)
                            }
                        },
                        onFailure = {
                            Timber.w(it, "Firebase categories query failed during background preloading")
                        }
                    )
                }
            } catch (e: Exception) {
                Timber.w(e, "Background Firebase preloading collector error")
            }
        }
    }

    fun preloadCategories(categories: List<Category>) {
        if (!networkUtils.isNetworkAvailable() || categories.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            if (!::predictionRepository.isInitialized) {
                Timber.w("PredictionRepository not yet initialized in PreloadRepository")
                return@launch
            }

            val semaphore = Semaphore(3)

            // Priority 1: Fetch the first/primary category (e.g. Today Tips) immediately
            val primaryCategory = categories.firstOrNull { it.url.isNotBlank() }
            if (primaryCategory != null) {
                launch {
                    try {
                        if (!preloadedData.containsKey(primaryCategory.url)) {
                            val response = predictionRepository.getCategoryData(primaryCategory.url)
                            preloadedData[primaryCategory.url] = response
                            Timber.d("Priority preloaded data for: ${primaryCategory.name} (${primaryCategory.url})")
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed priority preload for category: ${primaryCategory.name}")
                    }
                }
            }

            // Priority 2: Concurrently preload remaining categories with bounded concurrency
            val remainingCategories = categories.filter { it != primaryCategory && it.url.isNotBlank() }
            var completedCount = 0
            val totalCount = remainingCategories.size

            if (totalCount == 0) {
                _networkState.value = PreloadNetworkState.Done
                return@launch
            }

            remainingCategories.forEach { category ->
                launch {
                    semaphore.withPermit {
                        try {
                            if (!preloadedData.containsKey(category.url)) {
                                val response = predictionRepository.getCategoryData(category.url)
                                preloadedData[category.url] = response
                                Timber.d("Preloaded data for category: ${category.name}")
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to preload data for category: ${category.name}")
                            if (!networkUtils.isNetworkAvailable()) {
                                _networkState.value = PreloadNetworkState.Unavailable
                            }
                        } finally {
                            completedCount++
                            if (completedCount == totalCount) {
                                _networkState.value = PreloadNetworkState.Done
                                Timber.d("All categories preloaded successfully")
                            }
                        }
                    }
                }
            }
        }
    }

    fun getPreloadedData(endpoint: String): RootResponse? {
        return preloadedData[endpoint]
    }

    fun putPreloadedData(endpoint: String, data: RootResponse) {
        preloadedData[endpoint] = data
    }

    companion object {
        @Volatile
        private var instance: PreloadRepository? = null

        fun getInstance(): PreloadRepository {
            return instance ?: throw IllegalStateException("PreloadRepository not initialized")
        }

        fun createInstance(
            firebaseRepository: FirebaseRepository,
            categoryRepository: dagger.Lazy<CategoryRepository>,
            networkUtils: NetworkUtils
        ): PreloadRepository {
            return instance ?: synchronized(this) {
                instance ?: PreloadRepository(firebaseRepository, categoryRepository, networkUtils).also {
                    instance = it
                }
            }
        }
    }
}