package com.soccertips.predictx.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber
import java.util.Properties
import javax.inject.Inject
import javax.inject.Singleton
import java.io.IOException

@Singleton
class ApiConfigProvider @Inject constructor(private val context: Context) {
    private val _apiConfig = MutableStateFlow<Map<String, String>>(emptyMap())

    // Store environment variables
    private val envVars = Properties()

    init {
        // Load environment variables directly from the .env file in assets
        try {
            context.assets.open(".env").use { inputStream ->
                envVars.load(inputStream)
                Timber.d(".env file loaded successfully from assets")
            }
        } catch (e: IOException) {
            Timber.e(e, "Error loading .env file from assets")
        }
    }

    fun updateConfig(config: Map<String, String>) {
        Timber.d("Updating API config")
        _apiConfig.value = config
    }

    fun getApiKey(): String = _apiConfig.value["API_KEY"] ?: run {
        Timber.d("API_KEY not found in config, using value from .env file")
        envVars.getProperty("API_KEY", "")
    }

    fun getApiHost(): String = _apiConfig.value["API_HOST"] ?: run {
        Timber.d("API_HOST not found in config, using value from .env file")
        envVars.getProperty("API_HOST", "")
    }
}