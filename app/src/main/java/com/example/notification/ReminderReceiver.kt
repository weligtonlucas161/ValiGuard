package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_MONDAY_CHECK = "com.example.notification.ACTION_MONDAY_CHECK"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_MONDAY_CHECK || intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            MondayReminderScheduler.showMondayNotification(context)
            // Schedule next week
            MondayReminderScheduler.scheduleMondayReminder(context, 7, 0)
        }
    }
}
