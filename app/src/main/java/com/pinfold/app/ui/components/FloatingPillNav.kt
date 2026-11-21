package com.pinfold.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
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
import com.pinfold.app.ui.glass.theme.Candy
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
    val alpha by animateFloatAsState(if (visible) 1f else 0f, label = "navAlpha")

    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .height(64.dp)
            .alpha(alpha)
            .glass(
                Capsule,
                colors.glass,
                blur = style.blur.dp,
                lensHeight = style.refractionHeight.dp,
                lensAmount = style.refractionAmount.dp,
            )
            .clip(Capsule)
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (searchActive) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { androidx.compose.material3.Text("Search bookmarks", color = colors.inkSoft) },
                    singleLine = true,
                    modifier = Modifier.fillMaxSize(),
                )
                GlassCircle(onClick = onSearchToggle, onLongClick = onSearchToggle, size = 46.dp, label = "Close search") {
                    AppIcons.Icon(AppIcons.Close, "Close search", tint = colors.ink)
                }
            }
        } else {
            Row(
                Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
            ) {
                Box(Modifier.fillMaxWidth(0.33f)) {
                    GlassCircle(
                        onClick = onSearchToggle,
                        onLongClick = onSearchToggle,
                        size = 48.dp,
                        label = "Search",
                    ) { AppIcons.Icon(AppIcons.Search, "Search", tint = colors.ink) }
                }
                Box(Modifier.fillMaxWidth(0.33f)) {
                    GlassCircle(
                        onClick = onAdd,
                        onLongClick = onAddLong,
                        size = 48.dp,
                        label = "Add link",
                    ) { AppIcons.Icon(AppIcons.Add, "Add link", tint = colors.ink) }
                }
                Box(Modifier.fillMaxWidth(0.33f)) {
                    GlassCircle(
                        onClick = onSettings,
                        onLongClick = onSettingsLong,
                        size = 48.dp,
                        label = "Settings",
                    ) { AppIcons.Icon(AppIcons.Settings, "Settings", tint = colors.ink) }
                }
            }
        }
    }
}
