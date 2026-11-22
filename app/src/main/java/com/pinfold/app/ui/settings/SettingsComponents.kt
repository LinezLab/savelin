package com.pinfold.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pinfold.app.ui.components.AppIcons
import com.pinfold.app.ui.glass.GlassBackButton
import com.pinfold.app.ui.glass.RoundedRectangle
import com.pinfold.app.ui.glass.SavelinGlass
import com.pinfold.app.ui.glass.theme.Candy
import com.pinfold.app.ui.glass.theme.clear

@Composable
fun SettingsScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
            .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassBackButton(onBack = onBack)
            Spacer(Modifier.size(12.dp))
            Text(title, style = MaterialTheme.typography.headlineLarge, color = clear.ink)
        }
        Spacer(Modifier.height(16.dp))
        content()
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
fun SettingsSection(header: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(24.dp))
    Text(
        header.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
        color = clear.inkSoft,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
    content()
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().then(SavelinGlass.modifier(RoundedRectangle(22.dp)))
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) { content() }
}

@Composable
fun SettingsRow(
    icon: Int,
    tileColor: Color,
    title: String,
    subtitle: String? = null,
    trailing: @Composable () -> Unit = { Chevron() },
    onClick: () -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().height(if (subtitle == null) 56.dp else 64.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(32.dp).then(SavelinGlass.modifier(RoundedCornerShape(9.dp))),
            contentAlignment = Alignment.Center,
        ) { AppIcons.Icon(icon, null, tint = clear.ink, size = 18.dp) }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.fillMaxWidth(0.78f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = clear.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = clear.inkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        trailing()
    }
}

@Composable
fun Chevron() {
    AppIcons.Icon(AppIcons.Expand, null, tint = clear.inkSoft, size = 18.dp)
}

@Composable
fun GlassSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        colors = androidx.compose.material3.SwitchDefaults.colors(
            checkedTrackColor = clear.glass,
            uncheckedTrackColor = clear.glass,
            checkedThumbColor = clear.ink,
            uncheckedThumbColor = clear.inkSoft,
            checkedBorderColor = Candy.Mint,
            uncheckedBorderColor = clear.line,
        ),
    )
}

@Composable
fun ChoiceTile(
    label: String,
    selected: Boolean,
    preview: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.size(width = 96.dp, height = 112.dp)
            .then(SavelinGlass.modifier(RoundedCornerShape(16.dp)))
            .border(if (selected) 2.dp else 1.dp, if (selected) Candy.Sky else clear.rim, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.height(64.dp).fillMaxWidth()) { preview() }
        Spacer(Modifier.size(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
            color = clear.ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (selected) {
            AppIcons.Icon(AppIcons.Selected, "Selected", tint = Candy.Sky, size = 14.dp)
        }
    }
}

@Composable
fun ExpandableRow(title: String, icon: Int, expanded: Boolean, onToggle: () -> Unit) {
    SettingsRow(icon = icon, tileColor = Candy.Grape, title = title, onClick = onToggle, trailing = {
        AppIcons.Icon(AppIcons.Expand, null, tint = clear.inkSoft, size = 18.dp)
    })
}

@Composable
fun FineSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Text(
        "$label: ${(value * 100).toInt() / 100f}",
        style = MaterialTheme.typography.bodyMedium,
        color = clear.ink,
    )
    androidx.compose.material3.Slider(
        value = value.coerceIn(range),
        onValueChange = onChange,
        valueRange = range,
        modifier = Modifier.fillMaxWidth(),
    )
}
