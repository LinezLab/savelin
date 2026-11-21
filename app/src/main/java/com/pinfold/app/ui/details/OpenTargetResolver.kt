package com.pinfold.app.ui.details

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

object OpenTargetResolver {
    private val BROWSERS = setOf(
        "com.android.chrome", "com.google.android.chrome", "org.mozilla.firefox",
        "com.opera.browser", "com.microsoft.emmx", "com.brave.browser",
        "com.android.browser", "com.duckduckgo.mobile.android",
    )

    fun resolve(context: Context, url: String): Resolved {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)
        val matches = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .map { it.activityInfo.packageName }
            .filter { it !in BROWSERS }
        val pkg = matches.firstOrNull()
        val label = pkg?.let { name ->
            runCatching {
                val appInfo = context.packageManager.getApplicationInfo(name, 0)
                context.packageManager.getApplicationLabel(appInfo).toString()
            }.getOrNull()
        }
        val icon = pkg?.let { name ->
            runCatching { context.packageManager.getApplicationIcon(name) }.getOrNull()
        }
        return Resolved(packageName = pkg, label = label, icon = icon?.toImageBitmap())
    }

    data class Resolved(val packageName: String?, val label: String?, val icon: ImageBitmap?)

    private fun Drawable.toImageBitmap(): ImageBitmap? {
        if (this is BitmapDrawable) return bitmap.asImageBitmap()
        val bitmap = Bitmap.createBitmap(intrinsicWidth.coerceAtLeast(1), intrinsicHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        setBounds(0, 0, bitmap.width, bitmap.height)
        draw(Canvas(bitmap))
        return bitmap.asImageBitmap()
    }
}
