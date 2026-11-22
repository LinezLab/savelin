package com.pinfold.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pinfold.app.ui.glass.GlassPillButton
import com.pinfold.app.ui.glass.GlassPopover
import com.pinfold.app.ui.glass.RoundedRectangle
import com.pinfold.app.ui.glass.pressable
import com.pinfold.app.ui.glass.theme.clear

@Composable
fun GlassDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        GlassPillButton(onClick = { expanded = !expanded }, height = 40.dp, label = label) {
            Text("$label: $selected", color = clear.ink)
            AppIcons.Icon(AppIcons.Expand, null, tint = clear.inkSoft, size = 16.dp)
        }
        if (expanded) {
            Box(Modifier.matchParentSize().clickable { expanded = false })
            GlassPopover(
                Modifier.align(Alignment.TopStart).padding(top = 46.dp).fillMaxWidth().padding(6.dp),
                shape = RoundedRectangle(18.dp),
            ) {
                options.forEach { option ->
                    Row(
                        Modifier.fillMaxWidth().height(44.dp)
                            .pressable {
                                onSelect(option)
                                expanded = false
                            }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(option, color = clear.ink, modifier = Modifier.weight(1f))
                        if (option == selected) {
                            AppIcons.Icon(AppIcons.Selected, "Selected", tint = clear.ink, size = 16.dp)
                        }
                    }
                }
            }
        }
    }
}
