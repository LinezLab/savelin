package com.pinfold.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun wheelValueAtWrapsInBothDirections() {
        val base = listOf(12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
        assertEquals(12, wheelValueAt(base, 0))
        assertEquals(11, wheelValueAt(base, 11))
        assertEquals(12, wheelValueAt(base, 12))
        assertEquals(11, wheelValueAt(base, -1))
        assertEquals(12, wheelValueAt(base, -12))
        assertEquals(11, wheelValueAt(base, -13))
    }

    @Test
    fun wheelStartIndexLandsInCenterCycleOnSelectedValue() {
        val base = listOf(12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
        val index = wheelStartIndex(base, 9)
        assertEquals(WHEEL_CENTER_CYCLE * base.size + base.indexOf(9), index)
        assertEquals(9, wheelValueAt(base, index))
        assertTrue(index in WHEEL_CENTER_CYCLE * base.size until (WHEEL_CENTER_CYCLE + 1) * base.size)
    }

    @Test
    fun wheelNormalizedIndexPreservesValueAndReturnsToCenterCycle() {
        val base = (0..59).toList()
        val outside = (WHEEL_CENTER_CYCLE - 2) * base.size + 7
        assertEquals(7, wheelValueAt(base, outside))
        val normalized = wheelNormalizedIndex(outside, base.size)
        assertEquals(7, wheelValueAt(base, normalized))
        assertTrue(normalized in WHEEL_CENTER_CYCLE * base.size until (WHEEL_CENTER_CYCLE + 1) * base.size)
        assertEquals(59, wheelValueAt(base, wheelNormalizedIndex(-1, base.size)))
    }

    @Test
    fun wheelRepeatIsEvenAndLarge() {
        assertTrue(WHEEL_REPEAT % 2 == 0)
        assertTrue(WHEEL_REPEAT >= 100)
        assertEquals(WHEEL_REPEAT / 2, WHEEL_CENTER_CYCLE)
    }

    @Test
    fun dateWheelUtcMillisRoundTripsThroughSystemZone() {
        val millis = dateWheelUtcMillis(2026, 12, 24)
        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        assertEquals(LocalDate.of(2026, 12, 24), date)
    }

    @Test
    fun dateWheelUtcMillisClampsDayToLastOfMonth() {
        val leap = Instant.ofEpochMilli(dateWheelUtcMillis(2024, 2, 31))
            .atZone(ZoneId.systemDefault()).toLocalDate()
        assertEquals(LocalDate.of(2024, 2, 29), leap)
        val common = Instant.ofEpochMilli(dateWheelUtcMillis(2025, 2, 31))
            .atZone(ZoneId.systemDefault()).toLocalDate()
        assertEquals(LocalDate.of(2025, 2, 28), common)
    }

    @Test
    fun dateWheelUtcMillisClampsMonthOutOfRange() {
        val high = Instant.ofEpochMilli(dateWheelUtcMillis(2026, 15, 5))
            .atZone(ZoneId.systemDefault()).toLocalDate()
        assertEquals(LocalDate.of(2026, 12, 5), high)
        val low = Instant.ofEpochMilli(dateWheelUtcMillis(2026, 0, 5))
            .atZone(ZoneId.systemDefault()).toLocalDate()
        assertEquals(LocalDate.of(2026, 1, 5), low)
    }

    @Test
    fun dateWheelUtcMillisClampsYearToSaneRange() {
        val high = Instant.ofEpochMilli(dateWheelUtcMillis(10_000, 1, 1))
            .atZone(ZoneId.systemDefault()).toLocalDate()
        assertEquals(LocalDate.of(9999, 1, 1), high)
        val low = Instant.ofEpochMilli(dateWheelUtcMillis(1800, 1, 1))
            .atZone(ZoneId.systemDefault()).toLocalDate()
        assertEquals(LocalDate.of(1900, 1, 1), low)
    }

    @Test
    fun dateWheelMillisCombineWithLocalTimeInSystemZone() {
        val year = 2026
        val month = 12
        val day = 24
        val hour = 7
        val minute = 45
        val expectedTrigger = LocalDateTime.of(year, month, day, hour, minute)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(
            expectedTrigger,
            reminderTriggerAt(dateWheelUtcMillis(year, month, day), hour, minute, expectedTrigger - 1),
        )
    }
}
