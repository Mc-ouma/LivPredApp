package com.soccertips.predictx.repository

import com.soccertips.predictx.data.model.RootResponse
import com.soccertips.predictx.network.ApiService
import com.soccertips.predictx.network.Constants
import javax.inject.Inject

class PredictionRepository
@Inject
constructor(
    private val apiService: ApiService,
) {
    suspend fun getCategoryData(endpoint: String, usesAlternativeUrl: Boolean= false): RootResponse {
        val baseUrl = if(usesAlternativeUrl) {
            Constants.DAILY_BONUS_BASE_URL
        }else {  Constants.DEFAULT_BASE_URL
        }
        val url = baseUrl + endpoint
        return apiService.getServerResponses(url)
    }



}
