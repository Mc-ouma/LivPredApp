package com.soccertips.predictx.repository

import android.content.Context
import com.google.gson.Gson
import com.soccertips.predictx.data.model.RootResponse
import com.soccertips.predictx.network.ApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PredictionRepository @Inject constructor(
    private val apiService: ApiService,
    private val preloadRepository: Lazy<PreloadRepository>,
    @ApplicationContext private val context: Context,
    private val apiConfigProvider: ApiConfigProvider
) {
    private val gson = Gson()

    suspend fun getCategoryData(url: String): RootResponse {
        // Check if the URL is a valid endpoint in preloaded data
        preloadRepository.value.getPreloadedData(url)?.let {
            return it
        }

        // Use standard API call - no WebView fallback
        return apiService.getServerResponses(url)
    }
}