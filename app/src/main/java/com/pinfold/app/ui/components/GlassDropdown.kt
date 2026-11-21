package com.pinfold.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pinfold.app.ui.glass.GlassChip
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
    androidx.compose.foundation.layout.Box(modifier) {
        GlassChip(
            onClick = { expanded = true },
            height = 40.dp,
            contentPadding = PaddingValues(horizontal = 14.dp),
            label = label,
        ) {
            Text("$label: $selected", color = clear.ink)
            AppIcons.Icon(AppIcons.Expand, null, tint = clear.inkSoft, size = 16.dp)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelect(option); expanded = false },
                )
            }
        }
    }
}
