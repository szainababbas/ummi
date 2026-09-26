package com.szainabbas.ummi.domain

import com.szainabbas.ummi.data.model.MonthEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
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

    /** The PWA's `trimesterShort`: "1st trimester" / "2nd trimester" / "3rd trimester". */
    fun trimesterShort(week: Int): String = when (trimester(week)) {
        1 -> "1st trimester"
        2 -> "2nd trimester"
        else -> "3rd trimester"
    }

    /** Journey's week subtitle; "this week" only when the week shown is the current one. */
    fun weekSubtitle(week: Int, currentWeek: Int): String =
        trimesterShort(week) + if (week == currentWeek) " · this week" else ""

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

    /**
     * Material 3's DatePicker speaks in milliseconds at UTC midnight. Converting
     * through the phone's own time zone instead can land on the day before.
     */
    fun toPickerMillis(date: LocalDate): Long = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun fromPickerMillis(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

    /** e.g. "15 December 2026" for the due date on More and the start screen. */
    fun longDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.UK))

    /** e.g. "9 Jan" for the "Due ≈ 9 Jan" meta line. */
    fun shortDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("d MMM", java.util.Locale.UK))
}
