package com.pinfold.app.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.pinfold.app.ui.glass.SavelinGlassDialog
import com.pinfold.app.ui.glass.theme.clear
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

internal fun reminderTriggerAt(
    selectedDateUtcMillis: Long?,
    hour: Int,
    minute: Int,
    now: Long = System.currentTimeMillis(),
): Long? {
    val dateMillis = selectedDateUtcMillis ?: return null
    val date = Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val trigger = LocalDateTime.of(date, LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)))
        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    return if (trigger > now) trigger else null
}

@Composable
internal fun ReminderPickerDialog(
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit,
    ink: Color? = clear.hero,
) {
    var step by remember { mutableStateOf(1) }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis() + 86_400_000L)
    val timeState = rememberTimePickerState(initialHour = 9, initialMinute = 0, is24Hour = true)
    val trigger = reminderTriggerAt(dateState.selectedDateMillis, timeState.hour, timeState.minute)
    SavelinGlassDialog(
        onDismiss = onDismiss,
        title = if (step == 1) "Reminder date" else "Reminder time",
        confirm = if (step == 1) "Next" else "Set reminder",
        onConfirm = {
            if (step == 1) step = 2
            else if (trigger != null) {
                onPick(trigger)
                onDismiss()
            }
        },
        confirmEnabled = step == 1 || trigger != null,
        dismiss = "Cancel",
        ink = ink,
    ) {
        if (step == 1) {
            DatePicker(state = dateState, showModeToggle = false)
        } else {
            TimePicker(state = timeState)
            if (trigger == null) Text("Choose a future time", color = ink ?: clear.hero)
        }
    }
}
