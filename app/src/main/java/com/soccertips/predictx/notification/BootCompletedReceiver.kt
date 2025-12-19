package com.soccertips.predictx.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

/**
 * BroadcastReceiver that reschedules daily reminder alarms after device reboot.
 *
 * AlarmManager alarms are cleared when the device restarts, so we need to
 * listen for BOOT_COMPLETED to reschedule them.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            Timber.d("Device boot completed - rescheduling daily reminders")

            try {
                val scheduler = DailyReminderAlarmScheduler(context)
                scheduler.scheduleDailyReminders()
                Timber.d("Daily reminders rescheduled after boot")
            } catch (e: Exception) {
                Timber.e(e, "Failed to reschedule daily reminders after boot")
            }
        }
    }
}
