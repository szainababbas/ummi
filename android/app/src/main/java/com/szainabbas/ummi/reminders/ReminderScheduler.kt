package com.szainabbas.ummi.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.szainabbas.ummi.data.AppState
import com.szainabbas.ummi.data.Place
import com.szainabbas.ummi.data.model.UmmiData
import com.szainabbas.ummi.domain.PrayerDay
import com.szainabbas.ummi.domain.PrayerTimes
import com.szainabbas.ummi.domain.ReminderPlanner
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Keeps one alarm set, for the next reminder [ReminderPlanner] finds. Called
 * whenever the state changes, when the app starts, after a reboot or a clock
 * change, and by [ReminderReceiver] each time an alarm goes off.
 *
 * The alarm is inexact (`setAndAllowWhileIdle`): exact alarms need a special
 * permission Android keeps for alarm-clock apps, and a gentle reminder a few
 * minutes late is fine.
 */
object ReminderScheduler {
    fun prayersFor(state: AppState, zone: ZoneId = ZoneId.systemDefault()): (LocalDate) -> PrayerDay {
        val place = state.place ?: Place.DEFAULT
        return { date -> PrayerTimes.forDay(date, place.lat, place.lng, zone) }
    }

    fun reschedule(context: Context, state: AppState, data: UmmiData, now: LocalDateTime = LocalDateTime.now()) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val zone = ZoneId.systemDefault()
        val next = ReminderPlanner.upcoming(now, state, data, prayersFor(state, zone)).firstOrNull()
        val millis = next?.at?.atZone(zone)?.toInstant()?.toEpochMilli()
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_FIRE)
            .putExtra(ReminderReceiver.EXTRA_AT, millis ?: 0L)
        val pending = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (millis == null) {
            alarms.cancel(pending)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
        }
    }
}
