package com.szainabbas.ummi.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.szainabbas.ummi.data.AppState
import com.szainabbas.ummi.data.model.UmmiData
import com.szainabbas.ummi.domain.PrayerTimes
import com.szainabbas.ummi.domain.ReminderEvent
import com.szainabbas.ummi.domain.Reminders
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Keeps exactly one alarm set: for the next thing that should go off. When
 * it fires, [ReminderReceiver] shows what is due and calls [reschedule]
 * again, so the chain carries on. Changing anything in the app, opening it,
 * restarting the phone or changing its clock also calls [reschedule].
 *
 * Exact alarms when Android allows them; otherwise an inexact one, which a
 * sleeping phone may hold back by a few minutes. The Reminders screen says so.
 */
object ReminderScheduler {
    const val ACTION_FIRE = "com.szainabbas.ummi.REMINDER"
    const val EXTRA_AT = "at"
    private const val REQUEST_CODE = 1

    /** Today and tomorrow's reminders, and every visit reminder from today on. */
    fun events(state: AppState, data: UmmiData, from: LocalDate, zone: ZoneId): List<ReminderEvent> {
        val place = PrayerTimes.place(state.reminders.place)
        return Reminders.events(from, 2, state, data) { PrayerTimes.forDay(it, place, zone) }
    }

    fun reschedule(context: Context, state: AppState, data: UmmiData, now: ZonedDateTime = ZonedDateTime.now()) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val next = Reminders.nextAt(events(state, data, now.toLocalDate(), now.zone), now.toLocalDateTime())
        if (next == null) {
            alarms.cancel(pendingIntent(context, null))
            return
        }
        val millis = next.atZone(now.zone).toInstant().toEpochMilli()
        val intent = pendingIntent(context, millis)
        if (canBeExact(context)) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, intent)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, intent)
        }
    }

    /** Below Android 12 exact alarms need no permission; from 12 on she may have to allow "Alarms and reminders". */
    fun canBeExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() ?: false
    }

    /** One request code, so each new alarm replaces the last. Extras don't count towards that match. */
    private fun pendingIntent(context: Context, atMillis: Long?): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION_FIRE)
        if (atMillis != null) intent.putExtra(EXTRA_AT, atMillis)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
