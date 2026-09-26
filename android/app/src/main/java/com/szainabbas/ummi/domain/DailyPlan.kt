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

    /** The PWA's `DUA_TODAY[dayIndex() % length]`, where dayIndex is the day of the year. */
    fun duaTodayIndex(date: LocalDate, count: Int): Int? =
        if (count <= 0) null else date.dayOfYear % count
}
