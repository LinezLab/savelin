package com.pinfold.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.pinfold.app.MainActivity
import com.pinfold.app.R
import com.pinfold.app.data.db.PinfoldDatabase
import com.pinfold.app.data.model.ReminderStatus
import com.pinfold.app.data.settings.SettingsRepository
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal fun sampleSizeFor(maxDim: Int, targetPx: Int): Int {
    if (maxDim <= 0 || targetPx <= 0) return 1
    var sample = 1
    while (sample * 2 <= maxDim && maxDim / (sample * 2) >= targetPx) {
        sample *= 2
    }
    return sample.coerceAtLeast(1)
}

internal fun shouldShowUrlImage(showImage: Boolean, path: String?): Boolean =
    showImage && !path.isNullOrBlank()

internal fun useDecoratedAppIcon(urlBitmapPresent: Boolean) = !urlBitmapPresent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val bookmarkId = intent.getLongExtra(ReminderScheduler.EXTRA_BOOKMARK_ID, -1L)
        val reminderId = intent.getLongExtra(ReminderScheduler.EXTRA_REMINDER_ID, -1L)
        if (bookmarkId < 0 || reminderId < 0) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = PinfoldDatabase.get(context)
                val bookmark = db.bookmarkDao().findById(bookmarkId)
                val config = SettingsRepository(context.applicationContext).settings.first()
                if (config.notifications) {
                    val title = if (config.hideDetailsOnLockScreen) "" else bookmark?.title.orEmpty()
                    showNotification(
                        context,
                        title,
                        bookmarkId,
                        bookmark?.url,
                        bookmark?.imagePath,
                        !config.hideDetailsOnLockScreen,
                    )
                }
                db.reminderDao().setStatus(reminderId, ReminderStatus.FIRED)
            } finally {
                pending.finish()
            }
        }
    }

    private fun showNotification(
        context: Context,
        title: String,
        bookmarkId: Long,
        url: String?,
        imagePath: String?,
        showImage: Boolean,
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Sonil reminders", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }
        val open = PendingIntent.getActivity(
            context,
            bookmarkId.toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(ReminderScheduler.EXTRA_BOOKMARK_ID, bookmarkId)
                putExtra("openUrl", url.orEmpty())
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val urlBitmap = if (shouldShowUrlImage(showImage, imagePath)) {
            decodeLargeIconBitmap(imagePath.orEmpty())
        } else {
            null
        }
        val appIcon = launcherBitmap(context)
        val contentText = title.ifBlank { "A saved link is waiting for you" }
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_savelin)
            .setContentTitle("Sonil reminder")
            .setContentText(contentText)
            .setContentIntent(open)
            .setLargeIcon(urlBitmap ?: appIcon)
            .setColor(Color.parseColor("#8401F2"))
            .setAutoCancel(true)
        if (urlBitmap != null) {
            builder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(urlBitmap))
        } else if (useDecoratedAppIcon(urlBitmap != null) && appIcon != null) {
            val collapsed = RemoteViews(context.packageName, R.layout.notification_savelin_content)
            val expanded = RemoteViews(context.packageName, R.layout.notification_savelin_big)
            listOf(collapsed, expanded).forEach { views ->
                views.setImageViewBitmap(R.id.notification_app_icon, appIcon)
                views.setTextViewText(R.id.notification_title, "Sonil reminder")
                views.setTextViewText(R.id.notification_text, contentText)
            }
            builder
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(collapsed)
                .setCustomBigContentView(expanded)
        }
        manager.notify(bookmarkId.toInt(), builder.build())
    }

    private fun decodeLargeIconBitmap(path: String): Bitmap? {
        val file = File(path)
        if (!file.exists() || !file.isFile) return null
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
            if (maxDim <= 0) {
                null
            } else {
                val options = BitmapFactory.Options().apply {
                    inSampleSize = sampleSizeFor(maxDim, LARGE_ICON_TARGET_PX)
                }
                BitmapFactory.decodeFile(path, options)
            }
        }.getOrNull()
    }

    private fun launcherBitmap(context: Context): Bitmap? {
        val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher) ?: return null
        if (drawable is BitmapDrawable) return drawable.bitmap
        val size = (64f * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return bitmap
    }

    private companion object {
        const val CHANNEL_ID = "pinfold_reminders"
        const val LARGE_ICON_TARGET_PX = 256
    }
}
