package com.example

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppPrefs
import com.example.notifications.ReminderReceiver
import com.example.notifications.ReminderScheduler
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReminderNotificationTest {

    private lateinit var app: Application
    private val notifications get() = app.getSystemService(NotificationManager::class.java)
    private val alarms get() = app.getSystemService(AlarmManager::class.java)

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        AppPrefs(app).apply {
            reminderEnabled = true
            reminderMinuteOfDay = 18 * 60
        }
    }

    @After
    fun tearDown() = ShadowAlarmManager.setCanScheduleExactAlarms(false)

    @Test
    fun `reminder channel pops up and replaces the old quiet one`() {
        notifications.createNotificationChannel(NotificationChannel("daily_reminder", "Old", NotificationManager.IMPORTANCE_DEFAULT))

        ReminderScheduler.ensureChannel(app)

        assertNull("old quiet channel removed", notifications.getNotificationChannel("daily_reminder"))
        val channel = notifications.getNotificationChannel(ReminderScheduler.CHANNEL_ID)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
        assertEquals(Notification.VISIBILITY_PUBLIC, channel.lockscreenVisibility)
    }

    @Test
    fun `alarm rings at the exact minute and wakes a dozing phone`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)

        ReminderScheduler.reschedule(app)

        val alarm = shadowOf(alarms).nextScheduledAlarm
        assertNotNull(alarm)
        assertTrue(alarm!!.isAllowWhileIdle)
        assertEquals(0L, alarm.windowLengthMs)
        assertEquals(AlarmManager.RTC_WAKEUP, alarm.type)
    }

    @Test
    fun `without exact permission it still fires while dozing`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)

        ReminderScheduler.reschedule(app)

        val alarm = shadowOf(alarms).nextScheduledAlarm
        assertNotNull(alarm)
        assertTrue(alarm!!.isAllowWhileIdle)
    }

    @Test
    fun `test reminder is high priority, public and has Record now`() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        ReminderReceiver.showTest(app)

        val shown = shadowOf(notifications).allNotifications.single()
        assertEquals(ReminderScheduler.CHANNEL_ID, shown.channelId)
        assertEquals(NotificationCompat.PRIORITY_HIGH, shown.priority)
        assertEquals(Notification.VISIBILITY_PUBLIC, shown.visibility)
        assertEquals("Record now", shown.actions.single().title.toString())
    }
}
