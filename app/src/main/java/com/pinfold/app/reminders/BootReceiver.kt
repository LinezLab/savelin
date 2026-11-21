package com.pinfold.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pinfold.app.data.db.PinfoldDatabase
import com.pinfold.app.data.model.ReminderStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = PinfoldDatabase.get(context)
                db.reminderDao().due(ReminderStatus.PENDING, Long.MAX_VALUE).forEach {
                    ReminderScheduler.schedule(context, it)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
