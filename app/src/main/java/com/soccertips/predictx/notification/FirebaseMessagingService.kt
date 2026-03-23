package com.soccertips.predictx.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.soccertips.predictx.MainActivity
import com.soccertips.predictx.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class FirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var tokenRepository: TokenRepository
    @Inject
    lateinit var realTimeResultMonitor: RealTimeResultMonitor
    @Inject
    lateinit var bettingSuccessChecker: BettingSuccessChecker

    // Use a SupervisorJob so that failure of one coroutine doesn't cancel others
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onNewToken(token: String) {
        Timber.d("New FCM token: $token")

        // Save the token to your backend server or repository
        serviceScope.launch {
            try {
                tokenRepository.saveToken(token)
                Timber.d("Successfully saved new FCM token")
            } catch (e: Exception) {
                Timber.e(e, "Failed to save new FCM token")
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Timber.d("From: ${remoteMessage.from}")

        // Prefer data payload handling to avoid duplicate notifications/side effects when
        // both notification and data payloads are present.
        when (selectPayloadHandling(remoteMessage.data.isNotEmpty(), remoteMessage.notification != null)) {
            PayloadHandling.DATA -> {
            Timber.d("Message Data: ${remoteMessage.data}")

            val notificationType = remoteMessage.data["type"]

            when (notificationType) {
                "all_matches_won" -> {
                    handleBettingSuccessNotification(
                        remoteMessage.data["title"] ?: "🎉 Perfect Betting Day!",
                        remoteMessage.data["body"] ?: "All matches won today!",
                        remoteMessage.data
                    )
                }

                "match_result_update" -> {
                    // Handle match result updates and trigger real-time monitoring
                    handleMatchResultUpdate(remoteMessage.data)
                    val fixtureId = remoteMessage.data["fixtureId"]
                    if (fixtureId != null) {
                        sendNotification(
                            remoteMessage.data["title"] ?: "Match Result Update",
                            remoteMessage.data["body"] ?: "A match result has been updated",
                            remoteMessage.data
                        )
                    }
                }

                else -> {
                    // Handle existing fixture notifications
                    val fixtureId = remoteMessage.data["fixtureId"]
                    if (fixtureId != null) {
                        // Also trigger real-time monitoring for any fixture updates
                        handleMatchResultUpdate(remoteMessage.data)
                        sendNotification(
                            remoteMessage.data["title"] ?: "New Match Update",
                            remoteMessage.data["body"] ?: "Check out the latest match details",
                            remoteMessage.data
                        )
                    } else {
                        // Handle other types of data messages
                        sendNotification(
                            remoteMessage.data["title"] ?: "New Notification",
                            remoteMessage.data["body"] ?: "You have a new notification",
                            remoteMessage.data
                        )
                    }
                }
            }

            // Data payload handled; return early to avoid duplicate handling.
            return
            }
            PayloadHandling.NOTIFICATION -> {
                // Fallback: handle notification-only payloads
                remoteMessage.notification?.let {
                    Timber.d("Message Notification Body: ${it.body}")

                    sendNotification(it.title, it.body, emptyMap())
                }
            }
            PayloadHandling.NONE -> {
                // Nothing to handle
            }
        }
    }

    internal enum class PayloadHandling {
        DATA,
        NOTIFICATION,
        NONE
    }

    internal fun selectPayloadHandling(
        hasData: Boolean,
        hasNotification: Boolean
    ): PayloadHandling {
        return when {
            hasData -> PayloadHandling.DATA
            hasNotification -> PayloadHandling.NOTIFICATION
            else -> PayloadHandling.NONE
        }
    }

    private fun handleBettingSuccessNotification(
        title: String?,
        body: String?,
        data: Map<String, String>
    ) {
        // Extract betting data
        val date = data["date"] ?: ""
        val categoryUrl = data["category_url"] ?: ""
        val matchCount = data["match_count"] ?: "0"
        val winCount = data["win_count"] ?: "0"
        val successRate = data["success_rate"] ?: "0"
        val matches = data["matches"] ?: ""
        val summary = data["summary"] ?: ""

        Timber.d(
            "Betting Success - Date: $date, Matches: $matchCount, Wins: $winCount, Rate: $successRate%"
        )

        // Mark this date as notified to prevent duplicate notifications from WorkManager fallback
        if (date.isNotEmpty()) {
            bettingSuccessChecker.markDateAsNotifiedFromFcm(date, categoryUrl)
        }

        // Create enhanced intent for betting success
        val intent =
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                action = "com.soccertips.predictx.ACTION_VIEW_BETTING_SUCCESS"
                putExtra("fromNotification", true)
                putExtra("notificationType", "betting_success")

                // Add all betting data
                putExtra("betting_date", date)
                putExtra("match_count", matchCount)
                putExtra("win_count", winCount)
                putExtra("success_rate", successRate)
                putExtra("matches_details", matches)
                putExtra("summary", summary)

                // Add any other data from the message
                for ((key, value) in data) {
                    putExtra(key, value)
                }
            }

        val pendingIntent =
            PendingIntent.getActivity(
                this,
                System.currentTimeMillis().toInt(), // Unique request code
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

        // Create expanded notification for betting success
        createBettingSuccessNotification(title, body, data, pendingIntent)
    }

    private fun createBettingSuccessNotification(
        title: String?,
        body: String?,
        data: Map<String, String>,
        pendingIntent: PendingIntent
    ) {
        val matchCount = data["match_count"] ?: "0"
        val successRate = data["success_rate"] ?: "0"
        val matches = data["matches"] ?: ""
        val summary = data["summary"] ?: ""

        // Create big text style for expanded notification
        val bigTextStyle =
            NotificationCompat.BigTextStyle()
                .bigText(
                    getString(R.string.success_rate_summary, successRate, summary, matches)
                )
                .setBigContentTitle(title ?: getString(R.string.perfect_betting_day_fallback))
                .setSummaryText(getString(R.string.matches_won_summary, matchCount.toIntOrNull() ?: 0))

        // Create action buttons
        val viewDetailsIntent =
            Intent(this, MainActivity::class.java).apply {
                action = "com.soccertips.predictx.ACTION_VIEW_BETTING_HISTORY"
                putExtra("filter_date", data["date"])
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

        val viewDetailsPendingIntent =
            PendingIntent.getActivity(
                this,
                System.currentTimeMillis().toInt() + 1,
                viewDetailsIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

        val shareIntent =
            Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    getString(R.string.share_betting_success, matchCount, successRate)
                )
            }

        val sharePendingIntent =
            PendingIntent.getActivity(
                this,
                System.currentTimeMillis().toInt() + 2,
                Intent.createChooser(shareIntent, getString(R.string.share_success)),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

        val notificationBuilder =
            NotificationCompat.Builder(this, NotificationHelper.BETTING_SUCCESS_CHANNEL_ID)
                .setSmallIcon(R.drawable.launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(bigTextStyle)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setColor(
                    ContextCompat.getColor(this, R.color.success_green)
                ) // Add this color resource
                .addAction(
                    R.drawable.ic_visibility, // Add appropriate icon
                    getString(R.string.view_details),
                    viewDetailsPendingIntent
                )
                .addAction(
                    R.drawable.ic_share, // Add appropriate icon
                    getString(R.string.share),
                    sharePendingIntent
                )
                .setLights(Color.GREEN, 1000, 1000) // Green light for success
                .setVibrate(longArrayOf(0, 500, 200, 500)) // Custom vibration pattern

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Use the centralized channel creation from NotificationHelper
        // Note: Channels should already be created in App.onCreate()
        // But we ensure it's created here as a fallback
        NotificationHelper.createNotificationChannels(this)

        // Use a unique ID for betting success notifications
        val notificationId = "betting_success_${data["date"]}".hashCode()
        notificationManager.notify(notificationId, notificationBuilder.build())

        // Log success for analytics
        Timber.i("Betting success notification shown - Date: ${data["date"]}, Matches: $matchCount")
    }

    // Keep your existing sendNotification method for other notifications
    private fun sendNotification(title: String?, body: String?, data: Map<String, String>) {
        val fixtureId = data["fixtureId"]

        // Create an explicit intent to launch the app's main activity
        val intent =
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                action = "com.soccertips.predictx.ACTION_VIEW_MATCH"
                putExtra("fromNotification", true)

                // Add fixture ID if available
                fixtureId?.let { putExtra("fixtureId", it) }

                // Add any other data from the message
                for ((key, value) in data) {
                    putExtra(key, value)
                }
            }

        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder =
            NotificationCompat.Builder(this, NotificationHelper.FCM_DEFAULT_CHANNEL_ID)
                .setSmallIcon(R.drawable.launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Use the centralized channel creation from NotificationHelper
        // Note: Channels should already be created in App.onCreate()
        // But we ensure it's created here as a fallback
        NotificationHelper.createNotificationChannels(this)

        // Use a unique ID for each notification
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    /** Handle match result updates and trigger real-time monitoring */
    private fun handleMatchResultUpdate(data: Map<String, String>) {
        serviceScope.launch {
            try {
                val fixtureId = data["fixtureId"]
                if (fixtureId != null) {
                    Timber.d("Handling match result update for fixture: $fixtureId")

                    // Trigger real-time monitoring for this specific match
                    realTimeResultMonitor.monitorMatchResult(fixtureId)

                    // Also check if any categories might be completed today
                    realTimeResultMonitor.checkTodayCompletedCategories()

                    Timber.d("Real-time monitoring triggered for fixture $fixtureId")
                } else {
                    // If no specific fixture ID, check all today's completed categories
                    realTimeResultMonitor.checkTodayCompletedCategories()
                    Timber.d("Real-time monitoring triggered for all today's categories")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to handle match result update")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel all running coroutines to prevent memory leaks
        serviceJob.cancel()
        Timber.d("FCMService destroyed, all coroutines cancelled")
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Cancel coroutines when task is removed
        serviceJob.cancel()
        Timber.d("FCMService task removed, coroutines cancelled")
    }

    /**
     * Called when the system is running low on memory and actively running processes should trim
     * their memory usage.
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            TRIM_MEMORY_BACKGROUND, TRIM_MEMORY_UI_HIDDEN -> {
                // Cancel non-essential operations when memory is low
                Timber.d("Memory trim requested (level: $level), considering resource cleanup")


            }

        }
    }
}
