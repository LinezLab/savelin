package com.pinfold.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.dp
import com.pinfold.app.ui.glass.Capsule
import com.pinfold.app.ui.glass.GlassCircle
import com.pinfold.app.ui.glass.SpringMotion
import com.pinfold.app.ui.glass.SavelinGlass
import com.pinfold.app.ui.glass.theme.clear

/**
 * The dock: the Search button morphs into the search field on the active Motion spring — width, shape
 * and a content crossfade all share one value, and closing reverses the very same animation.
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

    BoxWithConstraints(modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 14.dp)) {
        val shape = RoundedCornerShape(lerp(30.dp, 20.dp, morph))
        Box(
            Modifier.align(Alignment.BottomStart).alpha(pillAlpha)
                .width(lerp(124.dp, maxWidth, morph))
                .height(60.dp)
                .then(SavelinGlass.modifier(shape))
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
                            scaleX = s
                            scaleY = s
                        }.clickable(inactive, null) {},
                        onSearchToggle, onAdd, onAddLong, colors.onGlass,
                    )
                    SearchFieldRow(
                        Modifier.fillMaxSize().graphicsLayer {
                            val a = morph.coerceIn(0f, 1f)
                            alpha = a
                            val s = 0.96f + 0.04f * a
                            scaleX = s
                            scaleY = s
                        },
                        query, onQueryChange, onSearchToggle, true, colors.onGlass, colors.onGlassSoft,
                    )
                } else {
                    SearchFieldRow(
                        Modifier.fillMaxSize().graphicsLayer {
                            val a = morph.coerceIn(0f, 1f)
                            alpha = a
                        }.clickable(inactive, null) {},
                        query, onQueryChange, onSearchToggle, false, colors.onGlass, colors.onGlassSoft,
                    )
                    ButtonsRow(
                        Modifier.fillMaxSize().graphicsLayer {
                            val a = (1f - morph).coerceIn(0f, 1f)
                            alpha = a
                            val s = 0.92f + 0.08f * a
                            scaleX = s
                            scaleY = s
                        },
                        onSearchToggle, onAdd, onAddLong, colors.onGlass,
                    )
                }
            }
        }
        Box(Modifier.align(Alignment.BottomEnd).alpha(settingsAlpha).size(60.dp).then(SavelinGlass.modifier(Capsule)).clip(Capsule)) {
            GlassCircle(onClick = onSettings, onLongClick = onSettingsLong, size = 60.dp, label = "Settings") {
                AppIcons.Icon(AppIcons.Settings, "Settings", tint = colors.onGlass)
            }
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
            GlassCircle(onClick = onSearchToggle, onLongClick = onSearchToggle, size = 48.dp, label = "Search") {
                AppIcons.Icon(AppIcons.Search, "Search", tint = ink)
            }
        }
        Box(Modifier.size(56.dp)) {
            GlassCircle(onClick = onAdd, onLongClick = onAddLong, size = 48.dp, label = "Add link") {
                AppIcons.Icon(AppIcons.Add, "Add link", tint = ink)
            }
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
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
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
            GlassCircle(onClick = { onQueryChange("") }, onLongClick = { onQueryChange("") }, size = 38.dp, label = "Clear search") {
                AppIcons.Icon(AppIcons.Close, "Clear search", tint = inkSoft, size = 16.dp)
            }
        }
        GlassCircle(onClick = { if (enabled) onSearchToggle() }, onLongClick = { if (enabled) onSearchToggle() }, size = 46.dp, label = "Close search") {
            AppIcons.Icon(AppIcons.Close, "Close search", tint = ink)
        }
    }
}
