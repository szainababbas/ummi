package com.szainabbas.ummi.domain

import com.szainabbas.ummi.data.model.MonthEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

/**
 * Same maths as the PWA (`index.html`'s week/month helpers): due date is
 * week 40, so the current week counts back from days-until-due. Clamped to
 * the 4–41 range the WEEKS content actually covers.
 */
object PregnancyMath {
    private val isoDate = DateTimeFormatter.ISO_LOCAL_DATE

    fun parseDueDate(dueDate: String?): LocalDate? =
        dueDate?.let { runCatching { LocalDate.parse(it, isoDate) }.getOrNull() }

    fun currentWeek(dueDate: LocalDate?, today: LocalDate = LocalDate.now()): Int {
        if (dueDate == null) return 4
        val daysUntilDue = ChronoUnit.DAYS.between(today, dueDate).toInt()
        val week = 40 - ceil(daysUntilDue / 7.0).toInt()
        return week.coerceIn(4, 41)
    }

    fun daysToGo(dueDate: LocalDate?, today: LocalDate = LocalDate.now()): Int? {
        if (dueDate == null) return null
        return ChronoUnit.DAYS.between(today, dueDate).toInt().coerceAtLeast(0)
    }

    fun trimester(week: Int): Int = when {
        week <= 13 -> 1
        week <= 27 -> 2
        else -> 3
    }

    /** Mirrors the PWA's `monthOf(w)`: the month whose range holds the week, else 1 below every range and 9 above. */
    fun currentMonthNumber(week: Int, months: Map<String, MonthEntry>): Int {
        for (m in 1..9) {
            val range = months[m.toString()] ?: continue
            if (week in range.from..range.to) return m
        }
        return if (week < 1) 1 else 9
    }

    /** The PWA's `sizeSentence`: "About the size of a poppy seed" / "an apple". */
    fun sizeSentence(size: String): String {
        val article = if (size.firstOrNull()?.lowercaseChar() in setOf('a', 'e', 'i', 'o', 'u')) "an" else "a"
        return "About the size of $article ${size.lowercase()}"
    }

    fun todayKey(today: LocalDate = LocalDate.now()): String = today.format(isoDate)

    /** e.g. "9 Jan" for the "Due ≈ 9 Jan" meta line. */
    fun shortDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("d MMM", java.util.Locale.UK))
}
