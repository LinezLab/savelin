package com.pinfold.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.floatingLayer
import com.pinfold.app.ui.glass.GlassOverlay
import com.pinfold.app.ui.glass.GlassPillButton
import com.pinfold.app.ui.glass.RoundedRectangle
import com.pinfold.app.ui.glass.SavelinGlass
import com.pinfold.app.ui.glass.pressable
import com.pinfold.app.ui.glass.revealRow
import com.pinfold.app.ui.glass.rememberMenuRow
import com.pinfold.app.ui.glass.rememberSpringAppear
import com.pinfold.app.ui.settings.displayOption

/**
 * A setting that opens an in-screen glass pop-up with EVERY option and a check on the current one.
 * There is no Android dropdown: the pop-up is a live glass surface that springs out of the row and
 * folds back on a tap-away, on the same Motion vocabulary as the nav pill and every other menu.
 */
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
        GlassPillButton(onClick = { expanded = true }, height = 48.dp, label = label) {
            Text("$label: ${displayOption(selected)}", color = SavelinGlass.ink)
            AppIcons.Icon(AppIcons.Expand, null, tint = SavelinGlass.inkSoft, size = 16.dp)
        }
        if (expanded) {
            GlassChoicePopUp(
                title = label,
                options = options,
                selected = selected,
                onSelect = { onSelect(it); expanded = false },
                onDismiss = { expanded = false },
            )
        }
    }
}

/** The shared in-screen pop-up behind every choice in Settings. All options stay visible. */
@Composable
fun GlassChoicePopUp(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    GlassOverlay(animatesOut = true) {
        BackHandler(onBack = onDismiss)
        val appear = rememberSpringAppear()
        Box(Modifier.fillMaxSize().padding(16.dp)) {
            Box(Modifier.fillMaxSize().clickable(onClick = onDismiss))
            Column(
                Modifier.heightIn(max = 420.dp).iosVerticalScroll(rememberScrollState())
                    .align(Alignment.BottomCenter).fillMaxWidth()
                    .floatingLayer {
                        val p = appear.value.coerceIn(0f, 1f)
                        val s = 0.76f + 0.24f * p
                        scaleX = s
                        scaleY = s
                        alpha = p
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .then(SavelinGlass.popupModifier(RoundedRectangle(22.dp)))
                    .padding(10.dp),
            ) {
                Text(
                    title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = SavelinGlass.inkSoft,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
                options.forEachIndexed { i, option ->
                    val row = rememberMenuRow(i)
                    Row(
                        Modifier.fillMaxWidth().height(52.dp).revealRow(row)
                            .pressable(onClick = { onSelect(option) })
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(displayOption(option), color = SavelinGlass.ink, modifier = Modifier.weight(1f))
                        if (option == selected) {
                            AppIcons.Icon(AppIcons.Selected, "Selected", tint = SavelinGlass.ink, size = 20.dp)
                        }
                    }
                }
            }
        }
    }
}
