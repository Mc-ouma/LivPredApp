package com.soccertips.predictx.repository

import com.soccertips.predictx.data.model.Category
import com.soccertips.predictx.data.model.RootResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreloadRepository @Inject constructor(
    private val firebaseRepository: FirebaseRepository
) {
    // Cache for preloaded data
    private val preloadedData = ConcurrentHashMap<String, RootResponse>()
    private lateinit var predictionRepository: PredictionRepository

    fun setPredictionRepository(predictionRepository: PredictionRepository) {
        if (!::predictionRepository.isInitialized) {
            this.predictionRepository = predictionRepository
        }
    }

    // Start preloading data after categories are loaded
    suspend fun preloadCategoryData() {
        firebaseRepository.getCategories().collect { result ->
            result.fold(
                onSuccess = { categories ->
                    preloadCategories(categories)
                },
                onFailure = {
                    Timber.e(it, "Failed to load categories for preloading")
                }
            )
        }
    }

    private fun preloadCategories(categories: List<Category>) {
        categories.forEach { category ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = predictionRepository.getCategoryData(category.url)
                    preloadedData[category.url] = response
                    Timber.d("Preloaded data for category: ${category.name}")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to preload data for category: ${category.name}")
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

        fun createInstance(firebaseRepository: FirebaseRepository): PreloadRepository {
            return instance ?: synchronized(this) {
                instance ?: PreloadRepository(firebaseRepository).also { instance = it }
            }
        }
    }
}