package com.pinfold.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.pinfold.app.ui.glass.RoundedRectangle
import com.pinfold.app.ui.glass.SavelinGlass
import com.pinfold.app.ui.glass.pressable
import com.pinfold.app.ui.glass.theme.Candy
import com.pinfold.app.ui.glass.theme.clear

data class GlassMenuItem(val icon: Int, val label: String, val onClick: () -> Unit, val destructive: Boolean = false)

/** An in-screen menu: the very glass of the nav bar, with neutral tint, a rim and shared press feel. */
@Composable
fun GlassMenuOverlay(items: List<GlassMenuItem>, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val ink = clear.ink
    Box(modifier.fillMaxSize().zIndex(30f)) {
        Box(Modifier.fillMaxSize().clickable(onClick = onDismiss))
        Column(
            Modifier.align(Alignment.BottomEnd).padding(16.dp).fillMaxWidth(0.7f)
                .then(SavelinGlass.modifier(RoundedRectangle(18.dp)))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items.forEach { entry ->
                val tint = if (entry.destructive) Candy.Tomato else ink
                Row(
                    Modifier.fillMaxWidth().height(48.dp)
                        .pressable {
                            entry.onClick()
                            onDismiss()
                        }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcons.Icon(entry.icon, null, tint = tint, size = 20.dp)
                    Spacer(Modifier.size(12.dp))
                    Text(entry.label, color = tint)
                }
            }
        }
    }
}
