package com.soccertips.predictx.util

import android.net.TrafficStats
import android.os.Build
import timber.log.Timber
import java.lang.reflect.Method
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class NetworkTaggingInitializer @Inject constructor() {

    companion object {
        private const val APP_SOCKET_TAG = 0xF00D // Unique tag for this app's traffic
        private const val FIREBASE_SOCKET_TAG = 0xF1FE // Tag for Firebase traffic
        private const val ANALYTICS_SOCKET_TAG = 0xABA1 // Tag for Analytics traffic
    }

    /**
     * Initialize network traffic tagging using API-appropriate methods
     */
    fun initialize() {
        try {
            // For newer Android versions, use the thread-level tagging approach
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                initializeThreadTagging()
            } else {
                Timber.d("Network tagging not available on this API level")
            }
        } catch (e: Exception) {
            Timber.w(e, "Network tagging initialization failed, continuing without tagging")
        }
    }

    private fun initializeThreadTagging() {
        try {
            // Set a default thread tag for the main thread
            TrafficStats.setThreadStatsTag(APP_SOCKET_TAG)

            // Create a thread local to handle per-thread tagging
            setupThreadLocalTagging()

            Timber.d("Thread-based network tagging initialized successfully")
        } catch (e: Exception) {
            Timber.w(e, "Failed to initialize thread-based network tagging")
        }
    }

    private fun setupThreadLocalTagging() {
        // Set up a thread-local approach for tagging network requests
        // This is safer than trying to override system-level socket tagging
        val threadLocal = ThreadLocal<Int>()

        // Set default tag for current thread
        threadLocal.set(APP_SOCKET_TAG)

        // For background threads that might make network calls, we can set appropriate tags
        // This approach is more compatible across different Android versions
    }

    /**
     * Tag network traffic for Firebase operations
     */
    fun tagFirebaseTraffic() {
        try {
            TrafficStats.setThreadStatsTag(FIREBASE_SOCKET_TAG)
        } catch (e: Exception) {
            Timber.w(e, "Failed to tag Firebase traffic")
        }
    }

         private fun createSocketTagger(socketTaggerClass: Class<*>, setThreadStatsTagMethod: Method): Any {
             // Create a proxy that implements the SocketTagger class
             return java.lang.reflect.Proxy.newProxyInstance(
                 socketTaggerClass.classLoader,
                 arrayOf(socketTaggerClass)
             ) { _, method, args ->
                 if (method.name == "tag" && args?.size == 1 && args[0] is Socket?) {
                     // Our custom tagging logic when tag() is called
                     val stackTrace = Thread.currentThread().stackTrace
                     val tag = determineTagFromStackTrace(stackTrace)

                     // Set the thread tag before the socket gets tagged
                     setThreadStatsTagMethod.invoke(null, tag)

                     // The original tag method returns void/Unit
                     return@newProxyInstance null
                 }
                 // For any other method calls, handle accordingly
                 null
             }
         }

         private fun determineTagFromStackTrace(stackTrace: Array<StackTraceElement>): Int {
             // Check stack trace to identify the source of the socket connection
             for (element in stackTrace) {
                 when {
                     element.className.contains("firebase") -> return FIREBASE_SOCKET_TAG
                     element.className.contains("datatransport") -> return ANALYTICS_SOCKET_TAG
                     element.className.contains("crash") -> return ANALYTICS_SOCKET_TAG
                 }
             }
             // Default to the app tag for unknown sources
             return APP_SOCKET_TAG
         }
     }