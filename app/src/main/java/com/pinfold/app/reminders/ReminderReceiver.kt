package com.pinfold.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
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
                    showNotification(context, title)
                }
                db.reminderDao().setStatus(reminderId, ReminderStatus.FIRED)
            } finally {
                pending.finish()
            }
        }
    }

    private fun showNotification(context: Context, title: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Pinfold reminder")
            .setContentText(title.ifBlank { "A saved link is waiting for you" })
            .setAutoCancel(true)
            .build()
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private companion object {
        const val CHANNEL_ID = "pinfold_reminders"
    }
}
