package com.pinfold.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pinfold.app.ui.glass.Capsule
import com.pinfold.app.ui.glass.GlassCircle
import com.pinfold.app.ui.glass.GlassSettings
import com.pinfold.app.ui.glass.glass
import com.pinfold.app.ui.glass.theme.clear

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
    val style = GlassSettings.style
    val pillAlpha by animateFloatAsState(if (visible) 1f else 0f, label = "pillAlpha")
    val settingsAlpha by animateFloatAsState(if (visible && !searchActive) 1f else 0f, label = "settingsAlpha")
    val glassModifier = Modifier.glass(
        Capsule,
        colors.glass,
        blur = style.blur.dp.coerceAtMost(8.dp),
        lensHeight = 24.dp.coerceAtMost(style.refractionHeight.dp * 2f),
        lensAmount = 40.dp.coerceAtMost(style.refractionAmount.dp * 2f),
    )

    BoxWithConstraints(modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 14.dp)) {
        Box(
            Modifier.align(Alignment.BottomStart).alpha(pillAlpha)
                .width(if (searchActive) maxWidth else 124.dp)
                .height(60.dp)
                .then(glassModifier)
                .clip(Capsule)
                .padding(6.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (searchActive) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.ink),
                        decorationBox = { inner ->
                            if (query.isEmpty()) Text("Search bookmarks", color = colors.inkSoft)
                            inner()
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                    GlassCircle(onClick = onSearchToggle, onLongClick = onSearchToggle, size = 46.dp, label = "Close search") {
                        AppIcons.Icon(AppIcons.Close, "Close search", tint = colors.ink)
                    }
                }
            } else {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp)) {
                        GlassCircle(onClick = onSearchToggle, onLongClick = onSearchToggle, size = 48.dp, label = "Search") {
                            AppIcons.Icon(AppIcons.Search, "Search", tint = colors.ink)
                        }
                    }
                    Box(Modifier.size(56.dp)) {
                        GlassCircle(onClick = onAdd, onLongClick = onAddLong, size = 48.dp, label = "Add link") {
                            AppIcons.Icon(AppIcons.Add, "Add link", tint = colors.ink)
                        }
                    }
                }
            }
        }
        Box(Modifier.align(Alignment.BottomEnd).alpha(settingsAlpha).size(60.dp).then(glassModifier).clip(Capsule)) {
            GlassCircle(onClick = onSettings, onLongClick = onSettingsLong, size = 60.dp, label = "Settings") {
                AppIcons.Icon(AppIcons.Settings, "Settings", tint = colors.ink)
            }
        }
    }
}
