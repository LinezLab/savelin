package com.pinfold.app.ui.glass

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

private val PhotoSaturation = ColorMatrix().apply { setToSaturation(1.18f) }

/**
 * Draws the card's own (already strongly blurred) preview through a clipped chip shape. The crop and
 * centre offset match the hero picture, a slight saturation lift and a 4–10 % neutral tint keep the
 * chip legible, and a top highlight plus the caller's 1 dp rim give it a glass edge. No accent, no
 * Material primary/surfaceVariant fill is used anywhere here.
 */
fun Modifier.photoThrough(
    photo: ImageBitmap?,
    shape: Shape,
    dark: Boolean,
): Modifier {
    if (photo == null) return this
    val filter = ColorFilter.colorMatrix(PhotoSaturation)
    val neutral = if (dark) Color.Black else Color.White
    return this.clip(shape).drawBehind {
        val iw = photo.width.coerceAtLeast(1)
        val ih = photo.height.coerceAtLeast(1)
        val scale = maxOf(size.width / iw, size.height / ih)
        val dw = iw * scale
        val dh = ih * scale
        drawImage(
            image = photo,
            dstOffset = IntOffset(((size.width - dw) / 2f).roundToInt(), ((size.height - dh) / 2f).roundToInt()),
            dstSize = IntSize(dw.roundToInt().coerceAtLeast(1), dh.roundToInt().coerceAtLeast(1)),
            colorFilter = filter,
        )
        // brightness lift + neutral wash, never a candy tint
        drawRect(neutral.copy(alpha = if (dark) 0.06f else 0.08f))
        drawRect(
            brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)),
            topLeft = Offset.Zero,
            size = Size(size.width, size.height * 0.45f),
        )
    }
}

/** Average luminance of a decoded preview (sampled to 8×8), used only to flip content colour for contrast. */
fun photoLuminance(photo: ImageBitmap?): Float? {
    if (photo == null) return null
    return runCatching {
        val small = android.graphics.Bitmap.createScaledBitmap(photo.asAndroidBitmap(), 8, 8, false)
        val pixels = IntArray(64)
        small.getPixels(pixels, 0, 8, 0, 0, 8, 8)
        var total = 0f
        for (p in pixels) {
            total += 0.2126f * android.graphics.Color.red(p) + 0.7152f * android.graphics.Color.green(p) + 0.0722f * android.graphics.Color.blue(p)
        }
        total / (64f * 255f)
    }.getOrNull()
}
