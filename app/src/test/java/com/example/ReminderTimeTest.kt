package com.example

import com.example.notifications.ReminderScheduler
import com.example.ui.formatMinuteOfDay
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class ReminderTimeTest {

    private fun at(hour: Int, minute: Int): Long = Calendar.getInstance().apply {
        set(2026, Calendar.OCTOBER, 27, hour, minute, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Test
    fun `reminder fires later today when the time is still ahead`() {
        assertEquals(at(18, 0), ReminderScheduler.nextTrigger(at(9, 30), 18 * 60))
    }

    @Test
    fun `reminder moves to tomorrow once the time has passed`() {
        assertEquals(at(18, 0) + 24 * 60 * 60 * 1000L, ReminderScheduler.nextTrigger(at(18, 0), 18 * 60))
    }

    @Test
    fun `times read like a clock`() {
        assertEquals("6:00 PM", formatMinuteOfDay(18 * 60))
        assertEquals("12:05 AM", formatMinuteOfDay(5))
        assertEquals("12:30 PM", formatMinuteOfDay(12 * 60 + 30))
    }
}
