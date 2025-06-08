package com.soccertips.predictx.repository

import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiConfigProvider @Inject constructor() {
    private val _apiConfig = MutableStateFlow<Map<String, String>>(emptyMap())

    fun updateConfig(config: Map<String, String>) {
        Timber.Forest.d("Updating API config: $config")
        _apiConfig.value = config
    }

    fun getApiKey(): String = _apiConfig.value["API_KEY"]
        ?: throw IllegalStateException("API_KEY not found in config")
    fun getApiHost(): String = _apiConfig.value["API_HOST"]
        ?: throw IllegalStateException("API_HOST not found in config")

}