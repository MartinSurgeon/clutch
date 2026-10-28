package com.example.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.ClutchDatabase
import com.example.ui.currentIsoDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires once a day at the farmer's chosen time. Only notifies when a pen is still missing
 * today's record — no nagging after the work is done. Also re-arms after reboot, time change
 * or when the farmer allows exact alarms.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ACTION_REMIND) {
            ReminderScheduler.reschedule(context)
            return
        }

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val missing = ClutchDatabase.get(context).clutchDao().penNamesMissingRecord(currentIsoDate())
                if (missing.isNotEmpty()) show(context, missing)
            } finally {
                ReminderScheduler.reschedule(context)
                pending.finish()
            }
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 4102

        /** Lets the farmer see and hear exactly what the daily reminder looks like. */
        fun showTest(context: Context) = show(context, emptyList())

        private fun show(context: Context, pens: List<String>) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return

            ReminderScheduler.ensureChannel(context)
            val open = Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_RECORD, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val tap = PendingIntent.getActivity(
                context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val which = when (pens.size) {
                0 -> "This is how your daily reminder will look."
                1 -> "${pens[0]} is waiting."
                2 -> "${pens[0]} and ${pens[1]} are waiting."
                else -> "${pens.size} pens are waiting."
            }
            val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Have you recorded today's eggs?")
                .setContentText("$which It takes about a minute.")
                .setStyle(NotificationCompat.BigTextStyle().bigText("$which Record eggs, feed and deaths — it takes about a minute."))
                .setContentIntent(tap)
                .addAction(R.drawable.ic_notification, "Record now", tap)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                // Android 7 and older: pop up with sound; Android 8+ takes this from the channel
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }
}
