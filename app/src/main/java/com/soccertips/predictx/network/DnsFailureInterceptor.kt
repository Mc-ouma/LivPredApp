package com.soccertips.predictx.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import okhttp3.Dns
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import timber.log.Timber
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A custom DNS resolver and interceptor that handles hostname resolution failures gracefully.
 * This helps prevent app crashes when the API endpoint can't be resolved and provides fallback options.
 */
@Singleton
class DnsFailureInterceptor @Inject constructor(private val context: Context) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        try {
            // Attempt to proceed with the request normally
            return chain.proceed(request)
        } catch (e: UnknownHostException) {
            // DNS resolution failure
            Timber.e(e, "DNS resolution failed for host: ${request.url.host}")

            // Check if we have a network connection at all
            val isConnected = isNetworkAvailable(context)
            val errorMessage = if (isConnected) {
                "Unable to fetch games. This could be temporary - please try again later."
            } else {
                "No internet connection. Please check your network settings and try again."
            }

            // Return a fake response instead of crashing
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(504) // Gateway Timeout
                .message(errorMessage)
                .body(errorMessage.toResponseBody(null))
                .build()
        } catch (e: IOException) {
            // Handle other IO exceptions
            Timber.e(e, "Network error while connecting to: ${request.url.host}")

            val errorMessage = "Network error. Please check your connection and try again."
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(503) // Service Unavailable
                .message(errorMessage)
                .body(errorMessage.toResponseBody(null))
                .build()
        }
    }

    /**
     * Check if the device has an active network connection
     */
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Custom DNS that falls back to a cached IP if the hostname resolution fails
     */
    class FallbackDns(private val delegate: Dns = Dns.SYSTEM) : Dns {
        // Known IPs for common API hosts - could be expanded or stored in a persistent cache
        private val knownHosts = mutableMapOf<String, List<InetAddress>>(
            // You could populate this with known IPs for your API endpoints
        )

        @Throws(UnknownHostException::class)
        override fun lookup(hostname: String): List<InetAddress> {
            return try {
                // First try normal DNS resolution
                val addresses = delegate.lookup(hostname)

                // Cache the result for future use
                if (addresses.isNotEmpty()) {
                    knownHosts[hostname] = addresses
                }

                addresses
            } catch (e: UnknownHostException) {
                // If DNS resolution fails, try to use cached IP if available
                knownHosts[hostname]?.let {
                    Timber.d("Using cached IP for $hostname")
                    return it
                }

                // If no cached IP, rethrow the exception
                throw e
            }
        }
    }
}
