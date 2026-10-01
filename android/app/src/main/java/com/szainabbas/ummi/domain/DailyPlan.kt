package com.szainabbas.ummi.domain

import com.szainabbas.ummi.data.model.ActEntry
import com.szainabbas.ummi.data.model.MonthEntry
import java.time.LocalDate

/** Which of a month's acts land on a given day, and which dua features that day. */
object DailyPlan {
    /** `ActEntry.days` uses JS `Date.getDay()` numbering: 0 = Sunday … 6 = Saturday. */
    fun jsWeekday(date: LocalDate): Int = date.dayOfWeek.value % 7

    /**
     * The PWA's `actsFor(m, dow)`: every-day acts plus those pinned to this
     * weekday. Pinned acts come first, since the PWA treats them as the ones
     * that lead the day.
     */
    fun actsFor(month: MonthEntry?, date: LocalDate): List<ActEntry> {
        val dow = jsWeekday(date)
        return month?.acts.orEmpty()
            .filter { it.days == null || dow in it.days }
            .sortedByDescending { it.days != null }
    }

    /**
     * The PWA's `tasksToday()`: the month's acts for the day, then the
     * every-day base tasks. A backup's `done` lists ids from both, so the
     * Android list has to show both for ticks to survive a restore.
     */
    fun tasksFor(month: MonthEntry?, baseTasks: List<ActEntry>, date: LocalDate): List<ActEntry> =
        actsFor(month, date) + baseTasks

    /**
     * The "Up next" card: the first task neither done nor put off with "Later".
     * Acts pinned to today's weekday come before every-day ones, so the day's
     * sūrah leads rather than the same adhān item every morning.
     */
    fun upNext(tasks: List<ActEntry>, done: Collection<String>, skipped: Collection<String>): ActEntry? {
        val order = tasks.sortedByDescending { it.days != null }
        return order.firstOrNull { it.id !in done && it.id !in skipped } ?: order.firstOrNull { it.id !in done }
    }

    /**
     * "Later" on [current]: move on to the next open task, and once every open
     * task has been put off, start again from the top (the handoff's "Later
     * cycles to the next open item"). Returns the new set of put-off ids.
     */
    fun later(tasks: List<ActEntry>, done: Collection<String>, skipped: Set<String>, current: ActEntry): Set<String> {
        val putOff = skipped + current.id
        val open = tasks.map { it.id }.filter { it !in done }
        return if (open.all { it in putOff }) emptySet() else putOff
    }

    /** The PWA's `DUA_TODAY[dayIndex() % length]`, where dayIndex is the day of the year. */
    fun duaTodayIndex(date: LocalDate, count: Int): Int? =
        if (count <= 0) null else date.dayOfYear % count
}
