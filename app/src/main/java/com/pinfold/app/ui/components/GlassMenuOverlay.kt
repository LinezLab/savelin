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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kyant.backdrop.floatingLayer
import com.pinfold.app.ui.glass.GlassOverlay
import com.pinfold.app.ui.glass.RoundedRectangle
import com.pinfold.app.ui.glass.SavelinGlass
import com.pinfold.app.ui.glass.pressable
import com.pinfold.app.ui.glass.revealRow
import com.pinfold.app.ui.glass.rememberMenuRow
import com.pinfold.app.ui.glass.rememberSpringAppear
import com.pinfold.app.ui.glass.theme.Candy
import com.pinfold.app.ui.glass.theme.clear

data class GlassMenuItem(val icon: Int, val label: String, val onClick: () -> Unit, val destructive: Boolean = false)

/**
 * An in-screen menu in the global glass vocabulary: it grows out of its trigger on the active Motion
 * spring, its rows arrive one after another, and it folds back into the trigger when dismissed —
 * including a tap-away or Back. Every part is a draw-time layer, never a layout change.
 */
@Composable
fun GlassMenuOverlay(items: List<GlassMenuItem>, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val ink = clear.ink
    GlassOverlay(animatesOut = true) {
        val appear = rememberSpringAppear()
        Box(modifier.fillMaxSize().zIndex(30f)) {
            Box(Modifier.fillMaxSize().clickable(onClick = onDismiss))
            Column(
                Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(16.dp).fillMaxWidth(0.7f)
                    .floatingLayer {
                        val p = appear.value.coerceIn(0f, 1f)
                        val s = 0.6f + 0.4f * p
                        scaleX = s
                        scaleY = s
                        alpha = p
                        transformOrigin = TransformOrigin(1f, 1f)
                    }
                    .then(SavelinGlass.modifier(RoundedRectangle(18.dp)))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items.forEachIndexed { i, entry ->
                    val row = rememberMenuRow(i)
                    val tint = if (entry.destructive) Candy.Tomato else ink
                    Row(
                        Modifier.fillMaxWidth().revealRow(row).height(48.dp)
                            .pressable(onClick = {
                                entry.onClick()
                                onDismiss()
                            })
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
}
