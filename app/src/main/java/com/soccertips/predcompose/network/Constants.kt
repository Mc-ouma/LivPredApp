package com.soccertips.predcompose.network

object Constants {
    const val BASE_URL: String = System.getenv("BASE_URL") ?: "https://v3.football.api-sports.io/"
    const val API_KEY: String = System.getenv("API_KEY") ?: ""
    const val API_HOST: String = System.getenv("API_HOST") ?: "v3.football.api-sports.io"
    const val CACHE_MAX_AGE_SHORT = 60 * 60 // 1 hour in seconds
    const val CACHE_MAX_AGE_LONG = 2 * 60 * 60 // 2 hours in seconds
    const val CACHE_MAX_AGE_VERY_LONG = 24 * 60 * 60 // 24 hours in seconds
}
