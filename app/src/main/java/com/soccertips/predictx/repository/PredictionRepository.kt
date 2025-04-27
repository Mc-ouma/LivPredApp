package com.soccertips.predictx.repository

import com.soccertips.predictx.data.model.RootResponse
import com.soccertips.predictx.network.ApiService
import javax.inject.Inject

class PredictionRepository
@Inject
constructor(
    private val apiService: ApiService,
    private val preloadRepository: Lazy<PreloadRepository>
) {
    suspend fun getCategoryData(url: String): RootResponse {
        /// Check if the URL is a valid endpoint
        preloadRepository.value.getPreloadedData(url)?.let {
            return it
        }

        /// If not found in preloaded data, fetch from API
        return apiService.getServerResponses(url)
    }


}
