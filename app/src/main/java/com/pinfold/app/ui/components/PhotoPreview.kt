package com.pinfold.app.ui.components

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.pinfold.app.network.image.Blurhash
import com.pinfold.app.network.image.ImageStore
import com.pinfold.app.ui.glass.photoLuminance
import com.pinfold.app.ui.glass.theme.clear
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The tiny blurred preview saved with each image, decoded once per card and remembered across
 * recompositions. Cards stored before the preview existed fall back to decoding their blurhash.
 */
@Composable
fun rememberPhotoPreview(imagePath: String?, blurhash: String?): ImageBitmap? {
    var preview by remember(imagePath, blurhash) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(imagePath, blurhash) {
        preview = withContext(Dispatchers.IO) {
            val source = imagePath?.takeIf { it.isNotBlank() }
            val blurFile = source?.let { ImageStore.blurPreviewPathFor(it) }?.takeIf { File(it).exists() }
            val bitmap = if (blurFile != null) {
                runCatching { BitmapFactory.decodeFile(blurFile) }.getOrNull()
            } else {
                blurhash?.takeIf { it.length >= 6 }?.let { Blurhash.decode(it, 48, 48) }
            }
            bitmap?.asImageBitmap()
        }
    }
    return preview
}

/** Content colour flips only when a bright preview would fail contrast; neutral white/ink otherwise. */
@Composable
fun rememberPhotoInk(photo: ImageBitmap?): Color? {
    val luminance = photo?.let { photoLuminance(it) } ?: return null
    return if (luminance < 0.55f) Color.White else clear.ink
}
