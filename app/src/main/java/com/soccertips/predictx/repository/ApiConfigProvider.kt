package com.soccertips.predictx.repository

import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiConfigProvider @Inject constructor() {
    private val _apiConfig = MutableStateFlow<Map<String, String>>(emptyMap())

    // Default values for API configuration
    private val defaultApiKey = ""
    private val defaultApiHost = ""

    fun updateConfig(config: Map<String, String>) {
        Timber.d("Updating API config: $config")
        _apiConfig.value = config
    }

    /**
     * Get API key or return default value instead of throwing an exception
     */
    fun getApiKey(): String = _apiConfig.value["API_KEY"] ?: defaultApiKey

    /**
     * Get API host or return default value instead of throwing an exception
     */
    fun getApiHost(): String = _apiConfig.value["API_HOST"] ?: defaultApiHost
}
