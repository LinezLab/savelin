package com.pinfold.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.pinfold.app.data.model.Reminder

object ReminderScheduler {
    const val EXTRA_BOOKMARK_ID = "bookmarkId"
    const val EXTRA_REMINDER_ID = "reminderId"
    const val ACTION_REMIND_LATER = "com.pinfold.app.REMIND_LATER"
    const val EXTRA_SNOOZE_MINUTES = "snoozeMinutes"
    const val LATER_MINUTES = 10

    fun schedule(context: Context, reminder: Reminder) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_BOOKMARK_ID, reminder.bookmarkId)
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pending = PendingIntent.getBroadcast(context, reminder.id.toInt(), intent, flags)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarm.canScheduleExactAlarms()) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerAt, pending)
        } else {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerAt, pending)
        }
    }

    fun cancel(context: Context, reminderId: Long) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        alarm.cancel(PendingIntent.getBroadcast(context, reminderId.toInt(), intent, flags))
    }
}
