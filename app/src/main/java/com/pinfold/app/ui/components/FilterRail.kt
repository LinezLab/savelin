package com.pinfold.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.pinfold.app.ui.glass.Capsule as GlassCapsule
import com.pinfold.app.ui.glass.GlassComponent
import com.pinfold.app.ui.glass.MotionSettings
import com.pinfold.app.ui.glass.SavelinGlass
import com.pinfold.app.ui.glass.theme.Candy
import com.pinfold.app.ui.glass.theme.clear
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilterRail(
    important: Boolean,
    due: Boolean,
    dueCount: Int,
    visible: Boolean,
    onImportant: () -> Unit,
    onDue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val slide by animateFloatAsState(if (visible) 0f else 28f, MotionSettings.style.let { androidx.compose.animation.core.spring(0.9f, 420f) }, label = "filterRailSlide")
    val opacity by animateFloatAsState(if (visible) 1f else 0f, androidx.compose.animation.core.tween(220), label = "filterRailFade")
    var tooltip by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(tooltip) {
        if (tooltip != null) { delay(1400); tooltip = null }
    }
    Box(
        modifier.fillMaxSize().navigationBarsPadding().padding(end = 16.dp, bottom = 86.dp),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Box(
            Modifier
                .width(60.dp)
                .height(116.dp)
                .graphicsLayer { alpha = opacity; translationX = slide }
                .then(SavelinGlass.modifierFor(GlassComponent.NAVIGATION, GlassCapsule))
                .clip(GlassCapsule),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RailSlot(
                    icon = AppIcons.Important,
                    name = "Important",
                    selected = important,
                    count = 0,
                    onToggle = onImportant,
                    onTooltip = { tooltip = it },
                )
                RailSlot(
                    icon = AppIcons.Reminder,
                    name = "Due",
                    selected = due,
                    count = dueCount,
                    onToggle = onDue,
                    onTooltip = { tooltip = it },
                )
            }
            val tip = tooltip
            if (tip != null) {
                Box(
                    Modifier.align(Alignment.TopCenter).offset(y = (-34).dp)
                        .background(clear.frost, androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(tip, color = clear.onGlass, style = androidx.compose.material3.MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RailSlot(
    icon: Int,
    name: String,
    selected: Boolean,
    count: Int,
    onToggle: () -> Unit,
    onTooltip: (String) -> Unit,
) {
    val accent = if (selected) Candy.Sky else SavelinGlass.ink
    val dark = clear.dark
    val rim = clear.rim
    val label = if (count > 0) "$name ($count)" else name
    Box(
        Modifier
            .size(width = 52.dp, height = 52.dp)
            .drawBehind {
                if (selected) {
                    drawRoundRect(
                        color = Color.White.copy(alpha = if (dark) 0.12f else 0.26f),
                        cornerRadius = CornerRadius(size.minDimension / 2),
                    )
                    drawRoundRect(
                        color = rim.copy(alpha = 0.5f),
                        cornerRadius = CornerRadius(size.minDimension / 2),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }
            }
            .combinedClickable(onClick = onToggle, onLongClick = { onTooltip(label) })
            .semantics {
                contentDescription = label
                stateDescription = if (selected) "On" else "Off"
                role = Role.Checkbox
            },
        contentAlignment = Alignment.Center,
    ) {
        AppIcons.Icon(icon, null, tint = accent, size = 22.dp)
        if (count > 0) {
            Box(Modifier.align(Alignment.TopEnd).padding(top = 5.dp, end = 5.dp)) {
                Text("$count", color = accent, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
            }
        }
    }
}
