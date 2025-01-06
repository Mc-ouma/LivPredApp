package com.soccertips.predcompose

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.soccertips.predcompose.data.local.AppDatabase
import com.soccertips.predcompose.data.local.entities.FavoriteItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class CheckDueItemsWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val database by lazy { AppDatabase.getDatabase(context) }

    override suspend fun doWork(): Result {
        val currentTime = inputData.getString("currentTime") ?: return Result.failure()
        val currentDate = inputData.getString("currentDate") ?: return Result.failure()

        val dueItems = getDueItems(currentTime, currentDate)
        if (dueItems.isNotEmpty()) {
            dueItems.forEach { item ->
                sendNotification(item)
            }
        }
        return Result.success()
    }
    private fun createPendingIntent(item: FavoriteItem): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra("fixtureId", item.fixtureId) // Pass the fixtureId as an extra
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        return PendingIntent.getActivity(
            applicationContext,
            item.fixtureId.toInt(), // Use fixtureId as the request code to ensure unique PendingIntents
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private suspend fun getDueItems(currentTime: String, currentDate: String): List<FavoriteItem> {
        return withContext(Dispatchers.IO) {
            database.favoriteDao().getDueItem(currentTime, currentDate)
        }
    }

    private fun sendNotification(item: FavoriteItem) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the PendingIntent
        val pendingIntent = createPendingIntent(item)

        // Create a notification channel (required for Android 8.0+)
        val channelId = "due_items_channel"
        val channelName = "Due Items"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, channelName, importance)
        notificationManager.createNotificationChannel(channel)

        // Build the notification
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Item Due")
            .setContentText("${item.homeTeam} vs ${item.awayTeam} is due now!")
            .setSmallIcon(R.drawable.outline_add_circle_outline_24)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Don't forget to check the fixture."))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // Show the notification
        notificationManager.notify(item.fixtureId.toInt(), notification)
    }
}


