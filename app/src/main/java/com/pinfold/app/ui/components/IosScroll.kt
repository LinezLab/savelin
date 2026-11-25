package com.pinfold.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pinfold.app.ui.glass.MotionSettings

internal const val SCROLL_BOUNCE_STIFFNESS = 300f
internal const val SCROLL_BOUNCE_DAMPING = 0.86f
internal const val SCROLL_REDUCED_STIFFNESS = 900f
internal const val SCROLL_FLING_DECAY_PER_SEC = 4.5f
internal const val SCROLL_FLING_MAX_PX = 12000f

internal fun <T> iosSpringSpec(reduceMotion: Boolean): SpringSpec<T> = spring(
    dampingRatio = if (reduceMotion) 1f else SCROLL_BOUNCE_DAMPING,
    stiffness = if (reduceMotion) SCROLL_REDUCED_STIFFNESS else SCROLL_BOUNCE_STIFFNESS,
)

internal fun iosFlingDistance(initialVelocity: Float): Float =
    (initialVelocity / SCROLL_FLING_DECAY_PER_SEC).coerceIn(-SCROLL_FLING_MAX_PX, SCROLL_FLING_MAX_PX)

private class IosSpringFlingBehavior : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        val target = iosFlingDistance(initialVelocity)
        if (target == 0f) return 0f
        var applied = 0f
        Animatable(0f).animateTo(
            targetValue = target,
            animationSpec = iosSpringSpec(reduceMotion = false),
            initialVelocity = initialVelocity,
        ) {
            val wanted = value - applied
            applied = value
            if (wanted != 0f) this@performFling.scrollBy(wanted)
        }
        return 0f
    }
}

@Composable
internal fun rememberIosFlingBehavior(reduceMotion: Boolean = MotionSettings.reduceMotion): FlingBehavior {
    val plainFling = ScrollableDefaults.flingBehavior()
    return remember(reduceMotion, plainFling) {
        if (reduceMotion) plainFling else IosSpringFlingBehavior()
    }
}

@Composable
internal fun rememberIosOverscrollEffect(reduceMotion: Boolean = MotionSettings.reduceMotion): OverscrollEffect? {
    val effect = rememberOverscrollEffect()
    return if (reduceMotion) null else effect
}

@Composable
internal fun Modifier.iosVerticalScroll(
    state: ScrollState,
    reduceMotion: Boolean = MotionSettings.reduceMotion,
): Modifier = verticalScroll(
    state = state,
    overscrollEffect = rememberIosOverscrollEffect(reduceMotion),
    flingBehavior = rememberIosFlingBehavior(reduceMotion),
)

@Composable
internal fun Modifier.iosHorizontalScroll(
    state: ScrollState,
    reduceMotion: Boolean = MotionSettings.reduceMotion,
): Modifier = horizontalScroll(
    state = state,
    overscrollEffect = rememberIosOverscrollEffect(reduceMotion),
    flingBehavior = rememberIosFlingBehavior(reduceMotion),
)
