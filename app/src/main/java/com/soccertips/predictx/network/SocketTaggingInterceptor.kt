package com.soccertips.predictx.network

import android.net.TrafficStats
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject

/**
 * OkHttp Interceptor that tags socket connections to avoid StrictMode violations.
 *
 * This interceptor sets a traffic stats tag before each request and clears it afterward,
 * ensuring that all network traffic is properly attributed in Android's network stats.
 */
class SocketTaggingInterceptor @Inject constructor() : Interceptor {

    companion object {
        // Using a constant tag for all app network traffic
        // You can use different tags for different types of requests if needed
        private const val SOCKET_TAG = 0xF00D // A unique tag for this app's traffic
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        // Set the traffic stats tag for this thread before making the request
        TrafficStats.setThreadStatsTag(SOCKET_TAG)

        return try {
            // Proceed with the request while the socket is tagged
            chain.proceed(chain.request())
        } finally {
            // Always reset the tag when done, regardless of success or exception
            TrafficStats.clearThreadStatsTag()
        }
    }
}
