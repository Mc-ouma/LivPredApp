package com.soccertips.predcompose

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import com.soccertips.predcompose.data.local.entities.FavoriteItem
import com.soccertips.predcompose.util.RescheduleWorker
import timber.log.Timber

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val fixtureId = intent.getStringExtra("fixtureId") ?: return
        val favoriteItem = FavoriteItem(
            fixtureId = fixtureId,
            homeTeam = intent.getStringExtra("homeTeam") ?: return,
            awayTeam = intent.getStringExtra("awayTeam") ?: return,
            mDate = intent.getStringExtra("mDate") ?: return,
            mTime = intent.getStringExtra("mTime") ?: return,
            hLogoPath = intent.getStringExtra("hLogoPath") ?: return,
            aLogoPath = intent.getStringExtra("aLogoPath") ?: return,
            league = intent.getStringExtra("league") ?: return,
            mStatus = intent.getStringExtra("mStatus") ?: return,
            outcome = intent.getStringExtra("outcome") ?: return,
            pick = intent.getStringExtra("pick") ?: return,
            color = intent.getIntExtra("color", 0),
            leagueLogo = intent.getStringExtra("leagueLogo") ?: return,
        )

        sendNotification(item = favoriteItem, context)
    }

    private fun sendNotification(
        item: FavoriteItem,
        context: Context,
    ) {
        val notificationManager =
            NotificationManagerCompat.from(context)

        val channelId = "due_items_channel"
        val channelName = "Due Items"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(channelId, channelName, importance)
        notificationManager.createNotificationChannel(channel)

        // Load the team logos
        val homeTeamLogo = Glide.with(context)
            .asBitmap()
            .load(item.hLogoPath)
            .submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
            .get()

        val awayTeamLogo = Glide.with(context)
            .asBitmap()
            .load(item.aLogoPath)
            .submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
            .get()

        // Build the notification
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Fixture Reminder")
            .setContentText("${item.homeTeam} vs ${item.awayTeam} is starting soon!")
            .setSmallIcon(R.drawable.outline_add_circle_outline_24)
            .setLargeIcon(homeTeamLogo)
            .setAutoCancel(true)
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(awayTeamLogo) // Set the away team logo in the big picture style
                    .bigLargeIcon(null as Bitmap?) // Hide the large icon in the big picture style
                    .setSummaryText("Don't miss the match between ${item.homeTeam} and ${item.awayTeam} at ${item.mTime} on ${item.mDate}.")
            )
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    item.fixtureId.toInt(), // Use fixtureId as the request code to ensure unique PendingIntents
                    Intent(context, MainActivity::class.java).apply {
                        putExtra("fixtureId", item.fixtureId) // Pass the fixtureId as an extra
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        // Show the notification
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Timber.e("Notification permission not granted")
            return
        }
        notificationManager.notify(
            item.fixtureId.toInt(), notification
        )
    }
}
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val workRequest = OneTimeWorkRequestBuilder<RescheduleWorker>().build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}



