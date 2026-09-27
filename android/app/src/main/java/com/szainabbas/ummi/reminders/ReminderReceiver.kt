package com.szainabbas.ummi.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.szainabbas.ummi.data.AppStateRepository
import com.szainabbas.ummi.data.UmmiDataRepository
import com.szainabbas.ummi.domain.ReminderPlanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Shows the reminders due when an alarm goes off, then sets the next alarm.
 * Also re-arms after a reboot, an app update or a clock or time-zone change,
 * all of which clear or shift alarms.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val state = AppStateRepository(app).state.first()
                val data = UmmiDataRepository.load(app)
                if (intent.action == ACTION_FIRE) {
                    val zone = ZoneId.systemDefault()
                    val at = LocalDateTime.ofInstant(Instant.ofEpochMilli(intent.getLongExtra(EXTRA_AT, 0L)), zone)
                    ReminderPlanner.dueAt(at, state, data, ReminderScheduler.prayersFor(state, zone))
                        .forEach { ReminderNotifier.show(app, it) }
                }
                ReminderScheduler.reschedule(app, state, data)
            } finally {
                result.finish()
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.szainabbas.ummi.REMINDER"
        const val EXTRA_AT = "at"
    }
}
