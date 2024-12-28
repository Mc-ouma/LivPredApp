package com.soccertips.predcompose.repository

import com.soccertips.predcompose.data.model.RootResponse
import com.soccertips.predcompose.network.ApiService
import javax.inject.Inject

class PredictionRepository
@Inject
constructor(
    private val apiService: ApiService,
) {
    suspend fun getCategoryData(endpoint: String): RootResponse =
        apiService.getServerResponses(endpoint)


}
