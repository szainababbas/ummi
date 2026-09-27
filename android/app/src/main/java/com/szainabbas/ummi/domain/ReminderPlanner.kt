package com.szainabbas.ummi.domain

import com.szainabbas.ummi.data.AppState
import com.szainabbas.ummi.data.VisitReminder
import com.szainabbas.ummi.data.model.ActEntry
import com.szainabbas.ummi.data.model.UmmiData
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** One notification: when, a stable key (also its notification id), and what it says. */
data class ReminderEvent(
    val at: LocalDateTime,
    val key: String,
    val title: String,
    val text: String,
)

/**
 * Works out which notifications are due and when, from the state alone, so
 * it can be tested without Android. The app schedules one alarm for the next
 * event; when it fires, the events due at that minute are worked out again
 * from the state as it is then (so a task ticked off since is not nagged
 * about) and the following alarm is set.
 */
object ReminderPlanner {
    val MORNING: LocalTime = LocalTime.of(7, 30)
    val WATER_HOURS = listOf(9, 11, 13, 15, 17, 19)

    /** Prayer-linked reminders never come before this, or at all after [LATEST]. */
    val EARLIEST: LocalTime = LocalTime.of(6, 30)
    val LATEST: LocalTime = LocalTime.of(22, 0)
    private const val AFTER_ADHAN_MINUTES = 5L

    private val hm = DateTimeFormatter.ofPattern("h:mm a", Locale.UK)

    /** The prayers a reminder follows, and the parts of the day whose acts it lists. */
    private enum class After(val label: String, val slots: Set<ActSlot>, val time: (PrayerDay) -> LocalTime) {
        FAJR("Fajr", setOf(ActSlot.MORNING, ActSlot.PRAYERS), { it.fajr }),
        ZUHR("Ẓuhr", setOf(ActSlot.PRAYERS), { it.dhuhr }),
        MAGHRIB("Maghrib", setOf(ActSlot.EVENING, ActSlot.PRAYERS), { it.maghrib }),
    }

    /** The daily reminders on [date]: morning summary, prayer-linked acts, water and per-task times. */
    fun eventsOn(date: LocalDate, state: AppState, data: UmmiData, prayers: PrayerDay): List<ReminderEvent> {
        val due = PregnancyMath.parseDueDate(state.dueDate) ?: return emptyList()
        val settings = state.reminders
        val week = PregnancyMath.currentWeek(due, date)
        val month = PregnancyMath.currentMonthNumber(week, data.months)
        val tasks = DailyPlan.tasksFor(data.months[month.toString()], data.baseTasks, date)
        val done = state.done[PregnancyMath.todayKey(date)].orEmpty()
        val open = tasks.filter { it.id !in done }
        val events = mutableListOf<ReminderEvent>()

        if (settings.morning) {
            val dua = DailyPlan.duaTodayIndex(date, data.duaToday.size)?.let { data.duaToday[it].lbl }
            events += ReminderEvent(
                at = date.atTime(MORNING),
                key = "morning:$date",
                title = "Good morning · week $week",
                text = "${tasks.size} things on today's list." + (dua?.let { " Today's dua: $it." } ?: ""),
            )
        }
        if (settings.prayer) {
            for (after in After.entries) {
                val acts = open.filter { ActSlot.of(it) in after.slots }
                if (acts.isEmpty()) continue
                val time = after.time(prayers).plusMinutes(AFTER_ADHAN_MINUTES)
                if (time.isAfter(LATEST)) continue
                events += ReminderEvent(
                    at = date.atTime(maxOf(time, EARLIEST)),
                    key = "prayer:${after.name.lowercase()}:$date",
                    title = "After ${after.label}",
                    text = summary(acts),
                )
            }
        }
        if (settings.water) {
            for (hour in WATER_HOURS) {
                events += ReminderEvent(date.atTime(hour, 0), "water:$date:$hour", "Water", "Time for a glass of water.")
            }
        }
        for ((id, time) in settings.tasks) {
            val act = open.firstOrNull { it.id == id } ?: continue
            val at = runCatching { LocalTime.parse(time) }.getOrNull() ?: continue
            events += ReminderEvent(date.atTime(at), "task:$id:$date", act.t, act.s.ifBlank { "On today's list in Ummi." })
        }
        return events.sortedBy { it.at }
    }

    /** One reminder for each visit that has one set. */
    fun visitEvents(state: AppState): List<ReminderEvent> = state.visits.mapNotNull { visit ->
        val at = Visits.reminderAt(visit) ?: return@mapNotNull null
        val start = Visits.at(visit) ?: return@mapNotNull null
        val day = if (VisitReminder.of(visit.reminder) == VisitReminder.EVENING_BEFORE) "Tomorrow" else "Today"
        val details = listOf(start.format(hm).lowercase(Locale.UK), visit.place, visit.note).filter { it.isNotBlank() }
        ReminderEvent(at, "visit:${visit.id}", "$day: ${visit.title}", details.joinToString(" · "))
    }

    /**
     * Every reminder after [now]: the daily ones for the rest of today and all
     * of tomorrow, and every visit reminder still to come. Soonest first.
     */
    fun upcoming(now: LocalDateTime, state: AppState, data: UmmiData, prayersFor: (LocalDate) -> PrayerDay): List<ReminderEvent> {
        val today = now.toLocalDate()
        val daily = listOf(today, today.plusDays(1)).flatMap { eventsOn(it, state, data, prayersFor(it)) }
        return (daily + visitEvents(state)).filter { it.at.isAfter(now) }.sortedBy { it.at }
    }

    /** The reminders due at [at]'s minute, worked out from the state as it is now. */
    fun dueAt(at: LocalDateTime, state: AppState, data: UmmiData, prayersFor: (LocalDate) -> PrayerDay): List<ReminderEvent> {
        val minute = at.truncatedTo(ChronoUnit.MINUTES)
        val date = minute.toLocalDate()
        return (eventsOn(date, state, data, prayersFor(date)) + visitEvents(state)).filter { it.at == minute }
    }

    /** The count on Today's bell: each daily reminder, each task reminder and each visit reminder to come. */
    fun activeCount(state: AppState, now: LocalDateTime): Int {
        val r = state.reminders
        val daily = listOf(r.morning, r.prayer, r.water).count { it }
        val visits = Visits.upcoming(state.visits, now).count { VisitReminder.of(it.reminder) != VisitReminder.NONE }
        return daily + r.tasks.size + visits
    }

    /** The time a task's bell sets when first tapped: near the part of the day it belongs to. */
    fun defaultTime(act: ActEntry, prayers: PrayerDay?): LocalTime = when (ActSlot.of(act)) {
        ActSlot.MORNING -> MORNING
        ActSlot.PRAYERS -> prayers?.dhuhr?.plusMinutes(10) ?: LocalTime.of(13, 30)
        ActSlot.EVENING -> prayers?.maghrib?.plusMinutes(10) ?: LocalTime.of(19, 0)
        ActSlot.ANYTIME -> LocalTime.of(20, 0)
    }.truncatedTo(ChronoUnit.MINUTES)

    /** "Ṣalawāt 140 times · Rub Khāke Shifāʾ on your stomach and 2 more". */
    private fun summary(acts: List<ActEntry>): String {
        val shown = acts.take(2).joinToString(" · ") { it.t }
        return if (acts.size > 2) "$shown and ${acts.size - 2} more" else shown
    }
}
