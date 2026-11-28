package com.pinfold.app.ui.components

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateTo
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
import kotlin.math.abs

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

/** A frame's scroll request hit a container bound when the container took less than asked. */
internal fun flingHitBound(delta: Float, consumed: Float): Boolean = abs(delta - consumed) > 0.5f

internal class IosSpringFlingBehavior(private val reduceMotion: Boolean) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        val target = iosFlingDistance(initialVelocity)
        if (target == 0f) return 0f
        var applied = 0f
        var leftover = initialVelocity
        AnimationState(initialValue = 0f, initialVelocity = initialVelocity).animateTo(
            targetValue = target,
            animationSpec = iosSpringSpec(reduceMotion),
        ) {
            val delta = value - applied
            if (delta != 0f) {
                val consumed = scrollBy(delta)
                applied += consumed
                leftover = this.velocity
                if (flingHitBound(delta, consumed)) cancelAnimation()
            } else {
                leftover = this.velocity
            }
        }
        return leftover
    }
}

@Composable
internal fun rememberIosFlingBehavior(reduceMotion: Boolean = MotionSettings.reduceMotion): FlingBehavior {
    val plainFling = ScrollableDefaults.flingBehavior()
    return remember(reduceMotion) {
        if (reduceMotion) plainFling else IosSpringFlingBehavior(reduceMotion)
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
