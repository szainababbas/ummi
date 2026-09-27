package com.szainabbas.ummi.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.szainabbas.ummi.data.AppStateRepository
import com.szainabbas.ummi.data.UmmiDataRepository
import com.szainabbas.ummi.domain.Reminders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZonedDateTime

/**
 * Wakes for the alarm [ReminderScheduler] set, and for the phone restarting
 * or its clock or time zone changing (all of which clear or skew alarms).
 * Shows whatever is due, then sets the next alarm.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext
                val state = AppStateRepository(app).state.first()
                val data = UmmiDataRepository.load(app)
                val now = ZonedDateTime.now()
                val atMillis = intent.getLongExtra(ReminderScheduler.EXTRA_AT, -1L)
                if (intent.action == ReminderScheduler.ACTION_FIRE && atMillis > 0) {
                    val scheduledFor = Instant.ofEpochMilli(atMillis).atZone(now.zone).toLocalDateTime()
                    val events = ReminderScheduler.events(state, data, scheduledFor.toLocalDate(), now.zone)
                    Notifier.post(app, Reminders.notices(Reminders.dueNow(events, scheduledFor, now.toLocalDateTime())))
                }
                ReminderScheduler.reschedule(app, state, data, now)
            } finally {
                pending.finish()
            }
        }
    }
}
