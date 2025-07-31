package com.soccertips.predictx.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
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
class FCMService : FirebaseMessagingService() {

    @Inject
    lateinit var tokenRepository: TokenRepository

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onNewToken(token: String) {
        Timber.d("New FCM token: $token")

        // Save the token to your backend server or repository
        scope.launch {
            tokenRepository.saveToken(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Timber.d("From: ${remoteMessage.from}")

        // Check if message contains a notification payload
        remoteMessage.notification?.let {
            Timber.d("Message Notification Body: ${it.body}")

            // Determine notification type from data
            val notificationType = remoteMessage.data["type"]
            when (notificationType) {
                "all_matches_won" -> {
                    handleBettingSuccessNotification(it.title, it.body, remoteMessage.data)
                }
                else -> {
                    sendNotification(it.title, it.body, remoteMessage.data)
                }
            }
        }

        // Check if message contains a data payload
        if (remoteMessage.data.isNotEmpty()) {
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
                else -> {
                    // Handle existing fixture notifications
                    val fixtureId = remoteMessage.data["fixtureId"]
                    if (fixtureId != null) {
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
        }
    }

    private fun handleBettingSuccessNotification(title: String?, body: String?, data: Map<String, String>) {
        // Extract betting data
        val date = data["date"] ?: ""
        val matchCount = data["match_count"] ?: "0"
        val winCount = data["win_count"] ?: "0"
        val successRate = data["success_rate"] ?: "0"
        val matches = data["matches"] ?: ""
        val summary = data["summary"] ?: ""

        Timber.d("Betting Success - Date: $date, Matches: $matchCount, Wins: $winCount, Rate: $successRate%")

        // Create enhanced intent for betting success
        val intent = Intent(this, MainActivity::class.java).apply {
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

        val pendingIntent = PendingIntent.getActivity(
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
        val channelId = "betting_success_channel"
        val matchCount = data["match_count"] ?: "0"
        val successRate = data["success_rate"] ?: "0"
        val matches = data["matches"] ?: ""
        val summary = data["summary"] ?: ""

        // Create big text style for expanded notification
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText("🎯 Success Rate: $successRate%\n📊 $summary\n\n📋 Match Results:\n$matches")
            .setBigContentTitle(title ?: "🎉 Perfect Betting Day!")
            .setSummaryText("$matchCount matches won")

        // Create action buttons
        val viewDetailsIntent = Intent(this, MainActivity::class.java).apply {
            action = "com.soccertips.predictx.ACTION_VIEW_BETTING_HISTORY"
            putExtra("filter_date", data["date"])
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val viewDetailsPendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt() + 1,
            viewDetailsIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "🎉 Perfect betting day! All $matchCount matches won with $successRate% success rate! 🎯")
        }

        val sharePendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt() + 2,
            Intent.createChooser(shareIntent, "Share Success"),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(bigTextStyle)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setColor(ContextCompat.getColor(this, R.color.success_green)) // Add this color resource
            .addAction(
                R.drawable.ic_visibility, // Add appropriate icon
                "View Details",
                viewDetailsPendingIntent
            )
            .addAction(
                R.drawable.ic_share, // Add appropriate icon
                "Share",
                sharePendingIntent
            )
            .setLights(Color.GREEN, 1000, 1000) // Green light for success
            .setVibrate(longArrayOf(0, 500, 200, 500)) // Custom vibration pattern

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create enhanced notification channel for betting success
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Betting Success Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for successful betting days"
                enableLights(true)
                lightColor = Color.GREEN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    null
                )
            }
            notificationManager.createNotificationChannel(channel)
        }

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
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            action = "com.soccertips.predictx.ACTION_VIEW_MATCH"
            putExtra("fromNotification", true)

            // Add fixture ID if available
            fixtureId?.let {
                putExtra("fixtureId", it)
            }

            // Add any other data from the message
            for ((key, value) in data) {
                putExtra(key, value)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = "fcm_default_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since Android Oreo, notification channels are required
        val channel = NotificationChannel(
            channelId,
            "FCM Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications from Firebase Cloud Messaging"
            enableLights(true)
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)

        // Use a unique ID for each notification
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
