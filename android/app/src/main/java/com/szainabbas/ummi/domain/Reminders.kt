package com.szainabbas.ummi.domain

import com.szainabbas.ummi.data.AppState
import com.szainabbas.ummi.data.ReminderSettings
import com.szainabbas.ummi.data.Visit
import com.szainabbas.ummi.data.model.ActEntry
import com.szainabbas.ummi.data.model.UmmiData
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** One thing that goes off. Events at the same minute share a notification: see [Reminders.notices]. */
data class ReminderEvent(val at: LocalDateTime, val heading: String, val line: String, val channel: Channel) {
    enum class Channel { DAILY, VISITS }
}

/** A notification as she sees it: a title and one or more lines. */
data class ReminderNotice(val at: LocalDateTime, val title: String, val lines: List<String>, val channel: ReminderEvent.Channel)

/**
 * Works out every reminder for a day, from her settings, the day's acts and
 * the prayer times. No Android here, so the whole of it is tested on the
 * JVM; `notify/` only turns these into alarms and notifications.
 */
object Reminders {
    val VISIT_EVENING_AT: LocalTime = LocalTime.of(20, 0)
    val VISIT_MORNING_AT: LocalTime = LocalTime.of(7, 0)

    fun isOn(act: ActEntry, settings: ReminderSettings): Boolean {
        val anchor = ActTiming.anchor(act)
        if (anchor == Anchor.WATER) return settings.water
        return settings.tasks[act.id] ?: (settings.prayer && anchor.prayerLinked)
    }

    /** Ids among [acts] that follow the prayer-linked switch, for [com.szainabbas.ummi.data.withPrayerLinked]. */
    fun prayerLinkedIds(acts: Collection<ActEntry>): List<String> =
        acts.filter { ActTiming.anchor(it).prayerLinked }.map { it.id }

    /** Every act of every month and the every-day tasks, for resetting choices across months. */
    fun allActs(data: UmmiData): List<ActEntry> = data.months.values.flatMap { it.acts } + data.baseTasks

    /** The acts on Today for [date], in timeline order. */
    fun tasksOn(date: LocalDate, state: AppState, data: UmmiData): List<ActEntry> {
        val due = PregnancyMath.parseDueDate(state.dueDate) ?: return emptyList()
        val week = PregnancyMath.currentWeek(due, date)
        val month = data.months[PregnancyMath.currentMonthNumber(week, data.months).toString()]
        return ActTiming.timeline(DailyPlan.tasksFor(month, data.baseTasks, date))
    }

    /** The bell badge on Today: how many things will go off today. */
    fun activeCount(date: LocalDate, state: AppState, data: UmmiData): Int =
        tasksOn(date, state, data).count { isOn(it, state.reminders) } + if (state.reminders.morning) 1 else 0

    /** The day's reminders apart from visits, in time order. Nothing before a due date is set. */
    fun dayEvents(date: LocalDate, state: AppState, data: UmmiData, pt: DayPrayerTimes): List<ReminderEvent> {
        val due = PregnancyMath.parseDueDate(state.dueDate) ?: return emptyList()
        val settings = state.reminders
        val tasks = tasksOn(date, state, data)
        val events = mutableListOf<ReminderEvent>()
        if (settings.morning) {
            val week = PregnancyMath.currentWeek(due, date)
            val dua = DailyPlan.duaTodayIndex(date, data.duaToday.size)?.let { data.duaToday[it].lbl }
            val things = "${tasks.size} thing${if (tasks.size == 1) "" else "s"} for today"
            events += ReminderEvent(
                date.atTime(ActTiming.MORNING_AT),
                "Today · week $week",
                if (dua == null) "$things." else "$things, and today's dua: $dua.",
                ReminderEvent.Channel.DAILY,
            )
        }
        tasks.filter { isOn(it, settings) }.forEach { act ->
            val anchor = ActTiming.anchor(act)
            ActTiming.times(anchor, pt).forEach { at ->
                val line = if (anchor == Anchor.WATER) "Time for a glass of water." else act.t
                events += ReminderEvent(date.atTime(at.time), at.heading, line, ReminderEvent.Channel.DAILY)
            }
        }
        return events.sortedBy { it.at }
    }

    /** When a visit's reminder goes off, or null for none. "2 hours before" an untimed visit falls back to the morning. */
    fun visitReminderAt(v: Visit): LocalDateTime? {
        val date = Visits.date(v) ?: return null
        val time = runCatching { LocalTime.parse(v.time) }.getOrNull()
        return when (VisitReminder.of(v.reminder)) {
            VisitReminder.NONE -> null
            VisitReminder.EVENING -> date.minusDays(1).atTime(VISIT_EVENING_AT)
            VisitReminder.MORNING -> date.atTime(VISIT_MORNING_AT)
            VisitReminder.TWO_HOURS -> time?.let { date.atTime(it).minusHours(2) } ?: date.atTime(VISIT_MORNING_AT)
        }
    }

    fun visitEvent(v: Visit): ReminderEvent? {
        val at = visitReminderAt(v) ?: return null
        val date = Visits.date(v) ?: return null
        val heading = when {
            at.toLocalDate().isBefore(date) -> "Tomorrow: ${v.title}"
            VisitReminder.of(v.reminder) == VisitReminder.TWO_HOURS && v.time.isNotEmpty() -> "In 2 hours: ${v.title}"
            else -> "Today: ${v.title}"
        }
        val line = listOf(Visits.timeLabel(v), v.note.orEmpty().trim()).filter { it.isNotEmpty() }
            .ifEmpty { listOf(VisitType.of(v.type).label) }.joinToString(" · ")
        return ReminderEvent(at, heading, line, ReminderEvent.Channel.VISITS)
    }

    /**
     * The daily reminders from the start of [from]'s day through the end of
     * the next [days] - 1 days, and every visit reminder from [from] on, however
     * far ahead (a scan in three weeks still needs its alarm set), in time
     * order. [prayerTimes] gives a day's adhān times (the caller knows her town
     * and time zone).
     */
    fun events(
        from: LocalDate,
        days: Int,
        state: AppState,
        data: UmmiData,
        prayerTimes: (LocalDate) -> DayPrayerTimes,
    ): List<ReminderEvent> {
        if (state.dueDate == null) return emptyList()
        val daily = (0 until days).flatMap { i -> from.plusDays(i.toLong()).let { dayEvents(it, state, data, prayerTimes(it)) } }
        val visits = state.visits.mapNotNull(::visitEvent).filter { !it.at.toLocalDate().isBefore(from) }
        return (daily + visits).sortedBy { it.at }
    }

    /** The first moment after [after] that something goes off. */
    fun nextAt(events: List<ReminderEvent>, after: LocalDateTime): LocalDateTime? =
        events.map { it.at }.filter { it.isAfter(after) }.minOrNull()

    /**
     * What to show when an alarm set for [scheduledFor] fires at [now]: that
     * minute's events and any that came due while the alarm was held back
     * (Android can delay one while the phone sleeps).
     */
    fun dueNow(events: List<ReminderEvent>, scheduledFor: LocalDateTime, now: LocalDateTime): List<ReminderEvent> {
        val until = maxOf(now, scheduledFor)
        return events.filter { !it.at.isBefore(scheduledFor) && !it.at.isAfter(until) }
    }

    /**
     * One notification per minute and channel. Its title is the first
     * event's heading; its lines are every event's line (after Ẓuhr, three
     * acts give three lines under one "After Ẓuhr").
     */
    fun notices(events: List<ReminderEvent>): List<ReminderNotice> =
        events.groupBy { it.at to it.channel }.map { (key, group) ->
            val (at, channel) = key
            val title = group.first().heading
            // A visit's heading carries its title, so a second visit that minute adds it to its line.
            val lines = group.mapIndexed { i, e -> if (i > 0 && e.heading != title && channel == ReminderEvent.Channel.VISITS) "${e.heading} · ${e.line}" else e.line }
            ReminderNotice(at, title, lines, channel)
        }.sortedBy { it.at }

    /** A row on the Reminders screen: the act, the short time for its left column, and whether it is on. */
    data class TaskRow(val act: ActEntry, val time: String, val on: Boolean)

    fun todayRows(date: LocalDate, state: AppState, data: UmmiData, pt: DayPrayerTimes): List<TaskRow> =
        tasksOn(date, state, data).map { TaskRow(it, ActTiming.shortWhen(ActTiming.anchor(it), pt), isOn(it, state.reminders)) }

    /** Upcoming visits that have a reminder set, soonest first, for the Reminders screen. */
    fun visitsWithReminders(visits: List<Visit>, today: LocalDate): List<Visit> =
        Visits.upcoming(visits, today).filter { visitReminderAt(it) != null }

    /** The line under the reminder chips on the Add visit form. */
    fun visitHint(reminder: VisitReminder, time: LocalTime?): String = when (reminder) {
        VisitReminder.NONE -> "No reminder for this one."
        VisitReminder.EVENING -> "At ${ActTiming.clock(VISIT_EVENING_AT)} the day before."
        VisitReminder.MORNING -> "At ${ActTiming.clock(VISIT_MORNING_AT)} on the day."
        VisitReminder.TWO_HOURS ->
            if (time == null) "Add a time, or it comes at ${ActTiming.clock(VISIT_MORNING_AT)} on the day."
            else "At ${ActTiming.clock(time.minusHours(2))}, two hours before."
    }
}
