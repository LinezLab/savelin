package com.pinfold.app.ui.settings

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pinfold.app.ui.components.AppIcons
import com.pinfold.app.ui.components.GlassChoicePopUp
import com.pinfold.app.ui.components.rememberIosFlingBehavior
import com.pinfold.app.ui.components.rememberIosOverscrollEffect
import com.pinfold.app.ui.glass.GlassBackButton
import com.pinfold.app.ui.glass.GlassSlider
import com.pinfold.app.ui.glass.GlassToggle
import com.pinfold.app.ui.glass.pressable
import com.pinfold.app.ui.glass.theme.Candy
import com.pinfold.app.ui.glass.theme.clear

internal fun displayOption(raw: String): String =
    raw.replace('_', ' ').replaceFirstChar { it.uppercase() }

@Composable
fun SettingsScaffold(
    title: String,
    onBack: () -> Unit,
    topInsets: WindowInsets = WindowInsets.statusBars.union(WindowInsets.displayCutout),
    flingBehavior: FlingBehavior = rememberIosFlingBehavior(),
    overscrollEffect: OverscrollEffect? = rememberIosOverscrollEffect(),
    content: @Composable () -> Unit,
) {
    val scrollState = rememberScrollState()
    Box(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().windowInsetsPadding(topInsets)
                .verticalScroll(scrollState, overscrollEffect = overscrollEffect, flingBehavior = flingBehavior)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassBackButton(onBack = onBack)
                Spacer(Modifier.size(14.dp))
                Text(title, style = MaterialTheme.typography.headlineLarge, color = clear.onGlass)
            }
            Spacer(Modifier.heightIn(min = 20.dp))
            content()
            Spacer(Modifier.heightIn(min = 160.dp))
        }
    }
}

@Composable
fun SettingsSection(header: String, content: @Composable () -> Unit) {
    Spacer(Modifier.heightIn(min = 28.dp))
    Text(
        header.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
        color = clear.onGlassSoft,
        modifier = Modifier.padding(start = 6.dp, bottom = 10.dp),
    )
    content()
}

/** A taller plain card with breathing room on all sides, so 200 % text and RTL stay readable. */
@Composable
fun SettingsCard(
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
    verticalArrangement: Arrangement.Vertical? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier.fillMaxWidth().padding(vertical = 6.dp)
            .clip(shape)
            .background(if (clear.dark) Color(0xFF121216) else Color(0xFFF4F4F8))
            .border(1.dp, clear.line, shape)
            .padding(contentPadding),
        verticalArrangement = verticalArrangement ?: Arrangement.Top,
    ) { content() }
}

@Composable
fun SettingsRow(
    icon: Int,
    tileColor: Color,
    title: String,
    subtitle: String? = null,
    trailing: @Composable () -> Unit = { Chevron() },
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = if (subtitle == null) 64.dp else 80.dp)
            .pressable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcons.Icon(icon, null, tint = clear.onGlassSoft, size = 20.dp)
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = clear.onGlass, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = clear.onGlassSoft, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.size(10.dp))
        trailing()
    }
}

/** A row that opens the shared in-screen glass pop-up with every option and its check mark. */
@Composable
fun SettingsChoiceRow(
    icon: Int,
    tileColor: Color,
    title: String,
    subtitle: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    SettingsRow(icon = icon, tileColor = tileColor, title = title, subtitle = displayOption(subtitle), onClick = { open = true })
    if (open) {
        GlassChoicePopUp(
            title = title,
            options = options,
            selected = subtitle,
            onSelect = { onSelect(it); open = false },
            onDismiss = { open = false },
        )
    }
}

@Composable
fun Chevron() {
    AppIcons.Icon(AppIcons.Expand, null, tint = clear.onGlassSoft, size = 18.dp)
}

@Composable
fun GlassSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    GlassToggle(checked = checked, onChange = onChange)
}

@Composable
fun ChoiceTile(
    label: String,
    selected: Boolean,
    preview: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.sizeIn(minWidth = 116.dp, minHeight = 134.dp, maxWidth = 116.dp, maxHeight = 134.dp)
            .clip(shape)
            .background(if (clear.dark) Color(0xFF121216) else Color(0xFFF4F4F8))
            .border(1.dp, clear.line, shape)
            .pressable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.height(82.dp).fillMaxWidth()) { preview() }
        Spacer(Modifier.size(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
            color = clear.onGlass,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (selected) {
            AppIcons.Icon(AppIcons.Selected, "Selected", tint = clear.onGlass, size = 16.dp)
        }
    }
}

@Composable
fun ExpandableRow(title: String, icon: Int, expanded: Boolean, onToggle: () -> Unit) {
    SettingsRow(icon = icon, tileColor = Candy.Grape, title = title, onClick = onToggle, trailing = {
        AppIcons.Icon(AppIcons.Expand, null, tint = clear.onGlassSoft, size = 18.dp)
    })
}

@Composable
fun FineSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "$label: ${(value * 100).toInt() / 100f}",
            style = MaterialTheme.typography.bodyMedium,
            color = clear.onGlass,
        )
        GlassSlider(
            value = value.coerceIn(range),
            onChange = onChange,
            range = range,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
