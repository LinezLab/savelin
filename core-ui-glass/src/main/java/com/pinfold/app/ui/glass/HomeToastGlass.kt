package com.pinfold.app.ui.glass

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.pinfold.app.ui.glass.theme.clear

@Composable
fun Modifier.homeToastGlass(
    shape: Shape = Capsule,
    wash: Color = clear.glass,
    luminance: GlassLuminance? = null,
): Modifier = this.glass(
    shape = shape,
    wash = wash,
    blur = 12.dp,
    lensHeight = 12.dp,
    lensAmount = 24.dp,
    luminance = luminance,
)
