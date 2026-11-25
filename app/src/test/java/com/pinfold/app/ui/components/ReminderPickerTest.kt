package com.pinfold.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class ReminderPickerTest {
    private val now = LocalDateTime.of(2026, 11, 25, 10, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun utcMidnight(day: LocalDate): Long =
        day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private fun expected(day: LocalDate, hour: Int, minute: Int): Long {
        val local = Instant.ofEpochMilli(utcMidnight(day)).atZone(ZoneId.systemDefault()).toLocalDate()
        return LocalDateTime.of(local, LocalTime.of(hour, minute)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    @Test
    fun selectedDateCombinesWithLocalTime() {
        val day = LocalDate.of(2026, 12, 1)
        assertEquals(expected(day, 9, 30), reminderTriggerAt(utcMidnight(day), 9, 30, now))
    }

    @Test
    fun pastTriggerIsRejected() {
        assertNull(reminderTriggerAt(utcMidnight(LocalDate.of(2026, 11, 20)), 9, 0, now))
    }

    @Test
    fun missingDateIsRejected() {
        assertNull(reminderTriggerAt(null, 9, 0, now))
    }

    @Test
    fun twelveHourWheelConvertsToTwentyFourHourClock() {
        assertEquals(0, hourIn24(12, false))
        assertEquals(9, hourIn24(9, false))
        assertEquals(12, hourIn24(12, true))
        assertEquals(21, hourIn24(9, true))
    }

    @Test
    fun outOfRangeClockValuesAreClamped() {
        val day = LocalDate.of(2026, 12, 1)
        assertEquals(expected(day, 23, 59), reminderTriggerAt(utcMidnight(day), 99, 99, now))
    }
}
