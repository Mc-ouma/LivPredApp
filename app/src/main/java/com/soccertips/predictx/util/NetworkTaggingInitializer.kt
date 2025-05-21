package com.soccertips.predictx.util

     import android.net.TrafficStats
     import timber.log.Timber
     import java.lang.reflect.Method
     import java.net.Socket
     import javax.inject.Inject
     import javax.inject.Singleton

/**
      * Initializer that globally tags socket connections to prevent StrictMode violations.
      * This works by replacing Android's default socket tagger with our custom implementation.
      */
     @Singleton
     class NetworkTaggingInitializer @Inject constructor() {

         companion object {
             private const val APP_SOCKET_TAG = 0xF00D // Unique tag for this app's traffic
             private const val FIREBASE_SOCKET_TAG = 0xF1FE // Tag for Firebase traffic
             private const val ANALYTICS_SOCKET_TAG = 0xABA1 // Tag for Analytics traffic
         }

         /**
          * Initialize global socket tagging by replacing the system SocketTagger
          */
         fun initialize() {
             try {
                 // Get the setThreadStatsTag method from TrafficStats
                 val setThreadStatsTagMethod = TrafficStats::class.java.getMethod(
                     "setThreadStatsTag", Int::class.javaPrimitiveType)

                 // Get the SocketTagger class via reflection (it's internal)
                 val socketTaggerClass = Class.forName("android.net.TrafficStats\$SocketTagger")

                 // Get the setSocketTagger method via reflection
                 val setSocketTaggerMethod = TrafficStats::class.java.getMethod(
                     "setSocketTagger", socketTaggerClass)

                 // Create our custom SocketTagger
                 val customTagger = createSocketTagger(socketTaggerClass, setThreadStatsTagMethod)

                 // Set our custom tagger
                 setSocketTaggerMethod.invoke(null, customTagger)

                 Timber.d("Custom socket tagger installed successfully")
             } catch (e: Exception) {
                 Timber.e(e, "Failed to install custom socket tagger: ${e.message}")
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