package com.pinfold.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.dp
import com.pinfold.app.ui.glass.BarGlyphButton
import com.pinfold.app.ui.glass.Capsule
import com.pinfold.app.ui.glass.Feel
import com.pinfold.app.ui.glass.LocalHaptics
import com.pinfold.app.ui.glass.GlassComponent
import com.pinfold.app.ui.glass.MotionSettings
import com.pinfold.app.ui.glass.SavelinGlass
import com.pinfold.app.ui.glass.SpringMotion
import com.pinfold.app.ui.glass.pressable
import com.pinfold.app.ui.glass.theme.clear
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * The dock: one glass container. Search, Plus and every glyph inside are bare icons; their pressed
 * and selected state is a shared neutral bubble drawn inside the container, never a second glass
 * surface. The Settings circle is a separate floating object with its own single container.
 */
@Composable
fun FloatingPillNav(
    searchActive: Boolean,
    query: String,
    visible: Boolean,
    onSearchToggle: () -> Unit,
    onQueryChange: (String) -> Unit,
    onAdd: () -> Unit,
    onAddLong: () -> Unit,
    onSettings: () -> Unit,
    onSettingsLong: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = clear
    val morph by animateFloatAsState(if (searchActive) 1f else 0f, SpringMotion.searchMorph(), label = "searchMorph")
    val pillAlpha by animateFloatAsState(if (visible) 1f else 0f, SpringMotion.popOpen(), label = "pillAlpha")
    val settingsAlpha by animateFloatAsState(if (visible && !searchActive) 1f else 0f, SpringMotion.popOpen(), label = "settingsAlpha")

    BoxWithConstraints(modifier.fillMaxWidth().wrapContentHeight().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 14.dp)) {
        val shape = RoundedCornerShape(lerp(30.dp, 20.dp, morph))
        Box(
            Modifier.align(Alignment.BottomStart).alpha(pillAlpha).zIndex(10f)
                .width(lerp(124.dp, maxWidth, morph))
                .height(60.dp)
                .then(SavelinGlass.modifierFor(GlassComponent.NAVIGATION, shape))
                .clip(shape)
                .padding(6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.fillMaxSize()) {
                val inactive = remember { MutableInteractionSource() }
                if (searchActive) {
                    ButtonsRow(
                        Modifier.fillMaxSize().graphicsLayer {
                            val a = (1f - morph).coerceIn(0f, 1f)
                            alpha = a
                            val s = 0.92f + 0.08f * a
                            scaleX = s; scaleY = s
                        }.clickable(inactive, null) {},
                        onSearchToggle, onAdd, onAddLong, colors.onGlass,
                    )
                    SearchFieldRow(
                        Modifier.fillMaxSize().graphicsLayer {
                            val a = morph.coerceIn(0f, 1f)
                            alpha = a
                            val s = 0.96f + 0.04f * a
                            scaleX = s; scaleY = s
                        },
                        query, onQueryChange, onSearchToggle, true, colors.onGlass, colors.onGlassSoft,
                    )
                } else {
                    SearchFieldRow(
                        Modifier.fillMaxSize().graphicsLayer { alpha = morph.coerceIn(0f, 1f) }.clickable(inactive, null) {},
                        query, onQueryChange, onSearchToggle, false, colors.onGlass, colors.onGlassSoft,
                    )
                    ButtonsRow(
                        Modifier.fillMaxSize().graphicsLayer {
                            val a = (1f - morph).coerceIn(0f, 1f)
                            alpha = a
                            val s = 0.92f + 0.08f * a
                            scaleX = s; scaleY = s
                        },
                        onSearchToggle, onAdd, onAddLong, colors.onGlass,
                    )
                }
            }
        }
        Box(
            Modifier.align(Alignment.BottomEnd).alpha(settingsAlpha).size(60.dp)
                .then(SavelinGlass.modifierFor(GlassComponent.NAVIGATION, Capsule))
                .clip(Capsule),
            contentAlignment = Alignment.Center,
        ) {
            NavGlyph(AppIcons.Settings, "Settings", onSettings, onSettingsLong, colors.onGlass, 60.dp, enabled = !searchActive)
        }
    }
}

@Composable
private fun ButtonsRow(
    modifier: Modifier,
    onSearchToggle: () -> Unit,
    onAdd: () -> Unit,
    onAddLong: () -> Unit,
    ink: Color,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(56.dp)) {
            NavGlyph(AppIcons.Search, "Search", onSearchToggle, onSearchToggle, ink, 48.dp)
        }
        Box(Modifier.size(56.dp)) {
            NavGlyph(AppIcons.Add, "Add link", onAdd, onAddLong, ink, 48.dp)
        }
    }
}

@Composable
private fun SearchFieldRow(
    modifier: Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchToggle: () -> Unit,
    enabled: Boolean,
    ink: Color,
    inkSoft: Color,
) {
    Row(modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = ink),
            cursorBrush = SolidColor(ink),
            decorationBox = { inner ->
                if (query.isEmpty()) Text("Search bookmarks", color = inkSoft)
                inner()
            },
            modifier = Modifier.weight(1f),
        )
        if (enabled && query.isNotEmpty()) {
            NavGlyph(AppIcons.Close, "Clear search", { onQueryChange("") }, { onQueryChange("") }, inkSoft, 38.dp, 16.dp, consumeTouches = true)
        }
        NavGlyph(AppIcons.Close, "Close search", { if (enabled) onSearchToggle() }, { if (enabled) onSearchToggle() }, ink, 46.dp, consumeTouches = true)
    }
}

/** A bare glyph inside a container: its press state is a shared neutral bubble drawn in-place. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NavGlyph(
    icon: Int,
    label: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    ink: Color,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    iconSize: androidx.compose.ui.unit.Dp = 22.dp,
    enabled: Boolean = true,
    consumeTouches: Boolean = false,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val haptics = LocalHaptics.current
    val dark = clear.dark
    Box(
        Modifier.size(size)
            .drawBehind {
                drawRoundRect(
                    color = Color.White.copy(alpha = if (pressed) {
                        if (dark) 0.20f else 0.34f
                    } else if (dark) 0.08f else 0.14f),
                    cornerRadius = CornerRadius(minOf(this.size.width, this.size.height) / 2),
                )
            }
            .graphicsLayer { val s = if (pressed) 0.9f else 1f; scaleX = s; scaleY = s }
            .combinedClickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = { haptics?.play(Feel.Tap); onClick() },
                onLongClick = onLongClick?.let { l -> { haptics?.play(Feel.Bloom); l() } },
            )
            .then(if (consumeTouches) Modifier.pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            } else Modifier)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        AppIcons.Icon(icon, null, tint = ink, size = iconSize)
    }
}

/** The multi-select quick actions, floated just above the Settings circle. */
@Composable
fun MultiSelectActionCapsule(
    count: Int,
    onDelete: () -> Unit,
    onRefetch: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = clear.onGlass
    Row(
        modifier
            .then(SavelinGlass.modifierFor(GlassComponent.NAVIGATION, Capsule))
            .clip(Capsule)
            .clickable(remember { MutableInteractionSource() }, null) {}
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            "$count selected",
            color = ink,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        BarGlyphButton(onClick = onDelete, label = "Delete selected", size = 40.dp) {
            AppIcons.Icon(AppIcons.Delete, null, tint = ink, size = 19.dp)
        }
        BarGlyphButton(onClick = onRefetch, label = "Refresh selected", size = 40.dp) {
            AppIcons.Icon(AppIcons.Refetch, null, tint = ink, size = 19.dp)
        }
        BarGlyphButton(onClick = onCopy, label = "Copy selected links", size = 40.dp) {
            AppIcons.Icon(AppIcons.Copy, null, tint = ink, size = 19.dp)
        }
        BarGlyphButton(onClick = onShare, label = "Share selected links", size = 40.dp) {
            AppIcons.Icon(AppIcons.Share, null, tint = ink, size = 19.dp)
        }
    }
}
