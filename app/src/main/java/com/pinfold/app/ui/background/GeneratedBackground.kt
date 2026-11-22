package com.pinfold.app.ui.background

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

private val CREAM = Color(0xFFFFF4DF)
private val LIGHT = Color(0xFF7FB6FF)
private val MID = Color(0xFF2455D8)
private val DEEP = Color(0xFF0B1B6B)
private val SHADOW = Color(0xFF05061A)

@Composable
fun GeneratedBackground(modifier: Modifier = Modifier, loopMs: Int = 40_000, animate: Boolean = true) {
    val transition = rememberInfiniteTransition(label = "background")
    val t by if (animate) {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(loopMs, easing = LinearEasing), RepeatMode.Restart),
            label = "loop",
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0f) }
    }

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(Brush.linearGradient(listOf(SHADOW, DEEP), start = Offset(w, 0f), end = Offset(0f, h)))

        val origin = Offset(0f, h)
        val layers = 5
        for (i in 0 until layers) {
            val phase = i.toFloat() / layers
            val wave = sin(2 * Math.PI * (t + phase)).toFloat()
            val angle = Math.PI / 4 * (phase * 2f) + wave * 0.12f
            val reach = maxOf(w, h) * (0.65f + 0.28f * phase + 0.06f * cos(2 * Math.PI * (t + phase)).toFloat())
            val centre = origin + Offset(cos(angle).toFloat() * reach * 0.28f, -sin(angle).toFloat() * reach * 0.28f)
            val ramp = listOf(
                CREAM.copy(alpha = 0.92f - phase * 0.12f),
                LIGHT.copy(alpha = 0.85f),
                MID.copy(alpha = 0.9f),
                DEEP.copy(alpha = 0.95f),
                Color.Transparent,
            )
            drawRect(
                brush = Brush.radialGradient(colors = ramp, center = centre, radius = reach),
                blendMode = BlendMode.SrcOver,
            )
        }

        drawRect(
            Brush.radialGradient(
                colors = listOf(Color.Transparent, SHADOW.copy(alpha = 0.85f)),
                center = Offset(w * 0.8f, h * 0.15f),
                radius = maxOf(w, h) * 0.95f,
            ),
        )
    }
}
