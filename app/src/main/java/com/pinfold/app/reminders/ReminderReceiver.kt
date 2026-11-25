package com.pinfold.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapDrawable
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.pinfold.app.MainActivity
import com.pinfold.app.R
import com.pinfold.app.data.db.PinfoldDatabase
import com.pinfold.app.data.model.ReminderStatus
import com.pinfold.app.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
                    showNotification(context, title, bookmarkId, bookmark?.url)
                }
                db.reminderDao().setStatus(reminderId, ReminderStatus.FIRED)
            } finally {
                pending.finish()
            }
        }
    }

    private fun showNotification(context: Context, title: String, bookmarkId: Long, url: String?) {
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
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_savelin)
            .setContentTitle("Sonil reminder")
            .setContentText(title.ifBlank { "A saved link is waiting for you" })
            .setContentIntent(open)
            .setLargeIcon(launcherBitmap(context))
            .setColor(Color.parseColor("#8401F2"))
            .setAutoCancel(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) notification.colorized = true
        manager.notify(bookmarkId.toInt(), notification)
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
    }
}
