package com.pinfold.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.floatingLayer
import com.pinfold.app.ui.glass.GlassPillButton
import com.pinfold.app.ui.glass.GlassPopover
import com.pinfold.app.ui.glass.RoundedRectangle
import com.pinfold.app.ui.glass.SpringMotion
import com.pinfold.app.ui.glass.pressable
import com.pinfold.app.ui.glass.revealRow
import com.pinfold.app.ui.glass.rememberRowReveal
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
    var closing by remember { mutableStateOf(false) }
    val appear = remember { Animatable(0f) }
    val open = { closing = false; expanded = true }
    val close = { if (expanded) closing = true }
    LaunchedEffect(expanded, closing) {
        val target = if (expanded && !closing) 1f else 0f
        appear.animateTo(target, if (target == 1f) SpringMotion.popOpen() else SpringMotion.popClose())
        if (target == 0f && expanded) {
            expanded = false
            closing = false
        }
    }
    Box(modifier) {
        GlassPillButton(onClick = { if (expanded) close() else open() }, height = 40.dp, label = label) {
            Text("$label: $selected", color = clear.onGlass)
            AppIcons.Icon(AppIcons.Expand, null, tint = clear.onGlassSoft, size = 16.dp)
        }
        if (expanded) {
            Box(Modifier.matchParentSize().clickable { close() })
            GlassPopover(
                Modifier.align(Alignment.TopStart).padding(top = 46.dp).fillMaxWidth().padding(6.dp)
                    .floatingLayer {
                        val p = appear.value.coerceIn(0f, 1f)
                        val s = 0.7f + 0.3f * p
                        scaleX = s
                        scaleY = s
                        alpha = p
                        transformOrigin = TransformOrigin(0f, 0f)
                    },
                shape = RoundedRectangle(18.dp),
            ) {
                options.forEachIndexed { i, option ->
                    val row = rememberRowReveal(appear, i)
                    Row(
                        Modifier.fillMaxWidth().revealRow(row).height(44.dp)
                            .pressable(onClick = {
                                onSelect(option)
                                close()
                            })
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(option, color = clear.onGlass, modifier = Modifier.weight(1f))
                        if (option == selected) {
                            AppIcons.Icon(AppIcons.Selected, "Selected", tint = clear.onGlass, size = 16.dp)
                        }
                    }
                }
            }
        }
    }
}
