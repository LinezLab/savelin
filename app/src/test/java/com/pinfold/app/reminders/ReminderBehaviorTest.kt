package com.pinfold.app.reminders

import com.pinfold.app.data.model.ReminderStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderBehaviorTest {
    @Test
    fun offAndInvalidRepeatSettingsDisableRepeat() {
        assertEquals(0, repeatMinutesFromSetting("OFF"))
        assertEquals(0, repeatMinutesFromSetting("off"))
        assertEquals(0, repeatMinutesFromSetting("junk"))
        assertEquals(0, repeatMinutesFromSetting("-5"))
    }

    @Test
    fun numericRepeatSettingsAreParsed() {
        assertEquals(5, repeatMinutesFromSetting("5"))
        assertEquals(15, repeatMinutesFromSetting("15"))
        assertEquals(30, repeatMinutesFromSetting("30"))
        assertEquals(60, repeatMinutesFromSetting("60"))
    }

    @Test
    fun nextTriggerAtAddsMinutesAndClampsOverflow() {
        assertEquals(1_360_000L, nextTriggerAt(1_000_000L, 6))
        assertEquals(1_900_000L, nextTriggerAt(1_000_000L, 15))
        assertEquals(1_000_000L, nextTriggerAt(1_000_000L, 0))
        assertEquals(1_000_000L, nextTriggerAt(1_000_000L, -3))
        assertEquals(Long.MAX_VALUE, nextTriggerAt(Long.MAX_VALUE - 100, 5))
    }

    @Test
    fun onlyFiredRemindersWithPositiveIntervalAreRescheduled() {
        assertTrue(shouldReschedule(ReminderStatus.FIRED, 5))
        assertFalse(shouldReschedule(ReminderStatus.FIRED, 0))
        assertFalse(shouldReschedule(ReminderStatus.PENDING, 5))
        assertFalse(shouldReschedule(ReminderStatus.SNOOZED, 10))
        assertFalse(shouldReschedule(ReminderStatus.DONE, 5))
    }

    @Test
    fun remindLaterMinutesFallBackToDefault() {
        assertEquals(5, snoozeMinutesOrDefault(5))
        assertEquals(ReminderScheduler.LATER_MINUTES, snoozeMinutesOrDefault(0))
        assertEquals(ReminderScheduler.LATER_MINUTES, snoozeMinutesOrDefault(-1))
    }
}
