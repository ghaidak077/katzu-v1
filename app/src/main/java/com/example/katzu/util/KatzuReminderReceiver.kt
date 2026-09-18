package com.example.katzu.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives scheduled learning alarm triggers, issues a sassy Katzu reminder notification,
 * and schedules the subsequent notification according to the chosen preset.
 */
class KatzuReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        AppLogger.i("KatzuReminderReceiver", "Alarm trigger received! Showing notification...")

        NotificationHelper.showReminderNotification(context)

        // Reschedule for next day or next interval
        val preset = intent?.getStringExtra("frequency_preset") ?: "regular"
        val hour = intent?.getIntExtra("hour", 20) ?: 20
        val minute = intent?.getIntExtra("minute", 0) ?: 0

        NotificationHelper.scheduleReminder(context, preset, hour, minute)
    }
}
