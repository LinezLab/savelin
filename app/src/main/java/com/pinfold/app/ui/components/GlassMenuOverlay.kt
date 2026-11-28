package com.pinfold.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kyant.backdrop.floatingLayer
import com.pinfold.app.ui.glass.Feel
import com.pinfold.app.ui.glass.homeToastGlass
import com.pinfold.app.ui.glass.GlassOverlay
import com.pinfold.app.ui.glass.RoundedRectangle
import com.pinfold.app.ui.glass.SavelinGlass
import com.pinfold.app.ui.glass.revealRow
import com.pinfold.app.ui.glass.rememberMenuRow
import com.pinfold.app.ui.glass.rememberSpringAppear
import com.pinfold.app.ui.glass.theme.clear

data class GlassMenuItem(val icon: Int, val label: String, val onClick: () -> Unit, val destructive: Boolean = false)

/**
 * A menu in the same glass vocabulary as a button: one live-glass container in the overlay host, a
 * small title header and rows that are themselves buttons with a shared press bubble. No Close button
 * and no opaque panel; a tap outside, Back or a row all dismiss it.
 */
@Composable
fun GlassMenuOverlay(
    items: List<GlassMenuItem>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    ink: Color? = null,
) {
    val resolvedInk = ink ?: SavelinGlass.ink
    GlassOverlay(animatesOut = true) {
        BackHandler(onBack = onDismiss)
        val appear = rememberSpringAppear()
        Box(modifier.fillMaxSize().zIndex(30f)) {
            Box(Modifier.fillMaxSize().clickable(onClick = onDismiss))
            Column(
                Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(16.dp).fillMaxWidth(0.78f)
                    .floatingLayer {
                        val p = appear.value.coerceIn(0f, 1f)
                        val s = 0.6f + 0.4f * p
                        scaleX = s
                        scaleY = s
                        alpha = p
                        transformOrigin = TransformOrigin(1f, 1f)
                    }
                    .homeToastGlass(RoundedRectangle(24.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (title != null) {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodySmall,
                        color = resolvedInk.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
                items.forEachIndexed { i, entry ->
                    MenuRow(entry, resolvedInk, i, onDismiss)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MenuRow(entry: GlassMenuItem, resolvedInk: Color, index: Int, onDismiss: () -> Unit) {
    val row = rememberMenuRow(index)
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val haptics = com.pinfold.app.ui.glass.LocalHaptics.current
    val dark = clear.dark
    val tint = resolvedInk
    Row(
        Modifier.fillMaxWidth().revealRow(row).heightIn(min = 48.dp)
            .drawBehind {
                if (pressed) {
                    drawRoundRect(
                        color = Color.White.copy(alpha = if (dark) 0.12f else 0.24f),
                        cornerRadius = CornerRadius(18.dp.toPx()),
                    )
                }
            }
            .combinedClickable(
                interactionSource = source,
                indication = null,
                role = Role.Button,
                onClick = {
                    haptics?.play(Feel.Tap)
                    entry.onClick()
                    onDismiss()
                },
            )
            .semantics { contentDescription = entry.label }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(32.dp).drawBehind {
                drawRoundRect(
                    color = Color.White.copy(alpha = if (dark) 0.10f else 0.18f),
                    cornerRadius = CornerRadius(10.dp.toPx()),
                )
            },
            contentAlignment = Alignment.Center,
        ) {
            AppIcons.Icon(entry.icon, null, tint = tint, size = 18.dp)
        }
        Spacer(Modifier.size(12.dp))
        Text(entry.label, color = tint, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
