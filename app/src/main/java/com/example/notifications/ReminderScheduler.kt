package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.local.AppPrefs
import java.util.Calendar

/**
 * Daily "have you recorded today?" reminder.
 * Uses an inexact alarm (no special permission, kind to the battery) and re-arms itself each day.
 */
object ReminderScheduler {

    const val CHANNEL_ID = "daily_reminder"
    const val ACTION_REMIND = "com.example.clutch.ACTION_DAILY_REMINDER"
    private const val REQUEST_CODE = 4101

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily record reminder",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "A gentle reminder to record eggs, feed and deaths each day"
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    /** Next time the clock shows [minuteOfDay] (today if still ahead, otherwise tomorrow). */
    fun nextTrigger(nowMillis: Long, minuteOfDay: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
            set(Calendar.MINUTE, minuteOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= nowMillis) cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION_REMIND)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Arms (or re-arms) the reminder from saved settings; cancels it if turned off. */
    fun reschedule(context: Context) {
        val prefs = AppPrefs(context)
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = pendingIntent(context)
        alarm.cancel(pi)
        if (!prefs.reminderEnabled) return
        ensureChannel(context)
        val at = nextTrigger(System.currentTimeMillis(), prefs.reminderMinuteOfDay)
        // A 15-minute window lets Android batch the wake-up with others
        alarm.setWindow(AlarmManager.RTC_WAKEUP, at, 15 * 60 * 1000L, pi)
    }
}
