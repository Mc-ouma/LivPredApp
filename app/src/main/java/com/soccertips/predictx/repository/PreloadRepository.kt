package com.soccertips.predictx.repository

import com.soccertips.predictx.data.model.Category
import com.soccertips.predictx.data.model.RootResponse
import com.soccertips.predictx.util.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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

    // Start preloading data after categories are loaded
    suspend fun preloadCategoryData() {
        if (!networkUtils.isNetworkAvailable()) {
            Timber.e("Network is not available. Cannot preload data.")
            _networkState.value = PreloadNetworkState.Unavailable
            return
        }
        _networkState.value = PreloadNetworkState.Loading
        firebaseRepository.getCategories().collect { result ->
            result.fold(
                onSuccess = { categories ->
                    preloadCategories(categories)
                },
                onFailure = {
                    Timber.e(it, "Failed to load categories for preloading")
                    _networkState.value = PreloadNetworkState.Unavailable
                }
            )
        }
    }

    private fun preloadCategories(categories: List<Category>) {
        if (!networkUtils.isNetworkAvailable()) {
            Timber.e("Network connection lost, cancelling preloading")
            _networkState.value = PreloadNetworkState.Unavailable
            return
        }

        var completedCount = 0
        val totalCount = categories.size

        categories.forEach { category ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = predictionRepository.getCategoryData(category.url)
                    preloadedData[category.url] = response
                    Timber.d("Preloaded data for category: ${category.name}")

                    completedCount++
                    if (completedCount == totalCount) {
                        _networkState.value = PreloadNetworkState.Done
                        Timber.d("All categories preloaded successfully")
                    }

                } catch (e: Exception) {
                    Timber.e(e, "Failed to preload data for category: ${category.name}")
                    if (!networkUtils.isNetworkAvailable()) {
                        Timber.e("Network connection lost during preloading")
                        _networkState.value = PreloadNetworkState.Unavailable
                    } else {
                        _networkState.value = PreloadNetworkState.Available
                    }

                }
            }
        }
    }

    fun getPreloadedData(endpoint: String): RootResponse? {
        return preloadedData[endpoint]
    }

    companion object {
        @Volatile
        private var instance: PreloadRepository? = null

        fun getInstance(): PreloadRepository {
            return instance ?: throw IllegalStateException("PreloadRepository not initialized")
        }

        fun createInstance(
            firebaseRepository: FirebaseRepository,
            networkUtils: NetworkUtils
        ): PreloadRepository {
            return instance ?: synchronized(this) {
                instance ?: PreloadRepository(firebaseRepository, networkUtils).also {
                    instance = it
                }
            }
        }
    }
}