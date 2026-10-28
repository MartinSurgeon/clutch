package com.example.notifications

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.example.data.local.AppPrefs
import java.util.Calendar

/**
 * Daily "have you recorded today?" reminder.
 * Pops up on screen (high-importance channel, shown on the lock screen) and fires at the chosen
 * minute even when the phone is dozing. Re-arms itself each day.
 */
object ReminderScheduler {

    // Android never lets an app raise an existing channel's importance, so the louder
    // channel needs a new id; the old quiet one is removed.
    const val CHANNEL_ID = "daily_reminder_alert"
    private const val OLD_CHANNEL_ID = "daily_reminder"
    const val ACTION_REMIND = "com.example.clutch.ACTION_DAILY_REMINDER"
    private const val REQUEST_CODE = 4101

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.deleteNotificationChannel(OLD_CHANNEL_ID)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily record reminder",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pops up at your chosen time if today's eggs, feed and deaths are not recorded yet"
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    /** False when the farmer has blocked Clutch's notifications or switched the reminder channel off. */
    fun canNotify(context: Context): Boolean {
        val compat = NotificationManagerCompat.from(context)
        if (!compat.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = context.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL_ID)
            if (channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE) return false
        }
        return true
    }

    /** Android 12+ may need the farmer's OK ("Alarms & reminders") before an alarm can ring at the exact minute. */
    fun canRingOnTime(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
    }

    /** Opens the "Alarms & reminders" switch for Clutch (Android 12+). */
    fun exactAlarmSettingsIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
        // Exact and allowed while dozing, so it rings at the chosen minute even if the phone sat
        // untouched all afternoon. Without the Android 12+ permission it still fires while dozing,
        // just up to a few minutes late.
        val exact = canRingOnTime(context) &&
            runCatching { alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi) }.isSuccess
        if (!exact) alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }
}
