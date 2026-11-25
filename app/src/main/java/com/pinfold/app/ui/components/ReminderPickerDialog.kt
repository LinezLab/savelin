package com.pinfold.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pinfold.app.ui.glass.SavelinGlassDialog
import com.pinfold.app.ui.glass.theme.ClearType
import com.pinfold.app.ui.glass.theme.clear
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

internal fun hourIn24(hour12: Int, pm: Boolean): Int =
    (hour12.coerceIn(1, 12) % 12) + if (pm) 12 else 0

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
    var hour12 by remember { mutableStateOf(9) }
    var minute by remember { mutableStateOf(0) }
    var pm by remember { mutableStateOf(false) }
    val contentInk = ink ?: clear.hero
    val trigger = reminderTriggerAt(dateState.selectedDateMillis, hourIn24(hour12, pm), minute)
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
            DatePicker(
                state = dateState,
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    containerColor = Color.Transparent,
                    titleContentColor = contentInk,
                    headlineContentColor = contentInk,
                    weekdayContentColor = contentInk.copy(alpha = 0.72f),
                    navigationContentColor = contentInk,
                    yearContentColor = contentInk,
                    currentYearContentColor = contentInk,
                    dayContentColor = contentInk,
                    selectedDayContentColor = contentInk,
                    selectedDayContainerColor = contentInk.copy(alpha = 0.22f),
                    todayContentColor = contentInk,
                    todayDateBorderColor = contentInk.copy(alpha = 0.6f),
                    dividerColor = contentInk.copy(alpha = 0.2f),
                ),
            )
        } else {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Wheel(
                    values = listOf(12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
                    selected = hour12,
                    label = { it.toString() },
                    onSelect = { hour12 = it },
                    ink = contentInk,
                    modifier = Modifier.weight(1f),
                )
                Wheel(
                    values = (0..59).toList(),
                    selected = minute,
                    label = { it.toString().padStart(2, '0') },
                    onSelect = { minute = it },
                    ink = contentInk,
                    modifier = Modifier.weight(1f),
                )
                Wheel(
                    values = listOf(false, true),
                    selected = pm,
                    label = { if (it) "PM" else "AM" },
                    onSelect = { pm = it },
                    ink = contentInk,
                    modifier = Modifier.weight(1f),
                )
            }
            if (trigger == null) Text("Choose a future time", color = contentInk)
        }
    }
}

@Composable
private fun <T> Wheel(
    values: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    ink: Color,
    modifier: Modifier = Modifier,
    rowHeight: Dp = 44.dp,
) {
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val rowPx = with(LocalDensity.current) { rowHeight.toPx() }
    val centerOffset = (rowPx * 2f).toInt()
    val centered by remember(values, rowPx) {
        derivedStateOf {
            (state.firstVisibleItemIndex + (state.firstVisibleItemScrollOffset / rowPx).roundToInt())
                .coerceIn(0, values.lastIndex)
        }
    }
    LaunchedEffect(Unit) {
        state.scrollToItem(values.indexOf(selected).coerceAtLeast(0), -centerOffset)
    }
    LaunchedEffect(centered) {
        val value = values[centered]
        if (value != selected) onSelect(value)
    }
    LaunchedEffect(Unit) {
        var wasScrolling = false
        snapshotFlow { state.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling ->
                if (wasScrolling && !scrolling) {
                    val index = (state.firstVisibleItemIndex +
                        (state.firstVisibleItemScrollOffset / rowPx).roundToInt())
                        .coerceIn(0, values.lastIndex)
                    val aligned = if (index == 0) {
                        state.firstVisibleItemScrollOffset == 0
                    } else {
                        abs(state.firstVisibleItemScrollOffset - rowPx) <= 2f
                    }
                    if (!aligned) state.animateScrollToItem(index, -centerOffset)
                }
                wasScrolling = scrolling
            }
    }
    Box(modifier.height(rowHeight * 5)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                Modifier.fillMaxWidth().height(rowHeight)
                    .background(ink.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            )
        }
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = rowHeight * 2),
        ) {
            items(values.size) { index ->
                val value = values[index]
                val isSelected = value == selected
                Box(
                    Modifier.fillMaxWidth().height(rowHeight).clickable {
                        onSelect(value)
                        scope.launch { state.animateScrollToItem(index, -centerOffset) }
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label(value),
                        color = if (isSelected) ink else ink.copy(alpha = 0.55f),
                        style = if (isSelected) ClearType.BodyStrong else ClearType.Body,
                    )
                }
            }
        }
    }
}
