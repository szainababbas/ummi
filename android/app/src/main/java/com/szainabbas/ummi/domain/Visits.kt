package com.szainabbas.ummi.domain

import com.szainabbas.ummi.data.Visit
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class VisitType(val key: String, val label: String) {
    MIDWIFE("midwife", "Midwife"),
    SCAN("scan", "Scan"),
    BLOOD("blood", "Blood test"),
    GP("gp", "GP"),
    CONSULTANT("consultant", "Consultant"),
    OTHER("other", "Other");

    companion object {
        fun of(key: String): VisitType = entries.firstOrNull { it.key == key } ?: OTHER
    }
}

enum class VisitReminder(val key: String, val label: String) {
    NONE("none", "None"),
    EVENING("evening", "Evening before"),
    MORNING("morning", "Morning of"),
    TWO_HOURS("2h", "2 hours before");

    companion object {
        fun of(key: String): VisitReminder = entries.firstOrNull { it.key == key } ?: NONE
    }
}

/**
 * Sorting and wording for the Visits screen and the next-visit chip on
 * Today. A visit stays under "Coming up" for the whole of its day, so one
 * this morning doesn't vanish into "Past" while she's still in the waiting room.
 */
object Visits {
    private val uk = Locale.UK

    fun date(v: Visit): LocalDate? = runCatching { LocalDate.parse(v.date) }.getOrNull()

    private fun time(v: Visit): LocalTime? = runCatching { LocalTime.parse(v.time) }.getOrNull()

    /** Soonest first; on the same day, timed visits in order and untimed ones last. */
    fun upcoming(visits: List<Visit>, today: LocalDate): List<Visit> =
        visits.filter { d -> date(d)?.let { !it.isBefore(today) } ?: false }
            .sortedWith(compareBy<Visit>({ date(it) }, { time(it) ?: LocalTime.MAX }))

    /** Most recent first. */
    fun past(visits: List<Visit>, today: LocalDate): List<Visit> =
        visits.filter { d -> date(d)?.isBefore(today) ?: false }
            .sortedWith(compareByDescending<Visit> { date(it) }.thenByDescending { time(it) ?: LocalTime.MIN })

    fun next(visits: List<Visit>, today: LocalDate): Visit? = upcoming(visits, today).firstOrNull()

    /** The hero eyebrow: "TODAY", "TOMORROW", "IN 3 DAYS", "IN 2 WEEKS". */
    fun whenLabel(v: Visit, today: LocalDate): String {
        val days = date(v)?.let { ChronoUnit.DAYS.between(today, it) } ?: return ""
        return when {
            days <= 0L -> "TODAY"
            days == 1L -> "TOMORROW"
            days < 14 -> "IN $days DAYS"
            else -> "IN ${days / 7} WEEKS"
        }
    }

    /** "9:10 am", or "" when no time is set. */
    fun timeLabel(v: Visit): String = time(v)?.let {
        it.format(DateTimeFormatter.ofPattern("h:mm a", uk)).lowercase(uk)
    } ?: ""

    /** "Tuesday 6 October · 9:10 am" */
    fun longWhen(v: Visit): String {
        val d = date(v)?.format(DateTimeFormatter.ofPattern("EEEE d MMMM", uk)) ?: v.date
        return listOf(d, timeLabel(v)).filter { it.isNotEmpty() }.joinToString(" · ")
    }

    /** The row subtitle: "Scan · 9:10 am · Evening before", leaving out what isn't set. */
    fun meta(v: Visit): String {
        val reminder = VisitReminder.of(v.reminder).takeIf { it != VisitReminder.NONE }?.label
        return listOfNotNull(VisitType.of(v.type).label, timeLabel(v).ifEmpty { null }, reminder).joinToString(" · ")
    }

    /** The date tile: "OCT" over "6". */
    fun tileMonth(v: Visit): String = date(v)?.format(DateTimeFormatter.ofPattern("MMM", uk))?.uppercase(uk) ?: ""

    fun tileDay(v: Visit): String = date(v)?.dayOfMonth?.toString() ?: ""

    /**
     * The chip on Today: "Today 9:10 · Glucose test", "Tue 9:10 · …" within
     * the week, "6 Oct · …" further out.
     */
    fun chipLabel(v: Visit, today: LocalDate): String {
        val d = date(v) ?: return v.title
        val days = ChronoUnit.DAYS.between(today, d)
        val day = when {
            days <= 0L -> "Today"
            days == 1L -> "Tomorrow"
            days < 7 -> d.format(DateTimeFormatter.ofPattern("EEE", uk))
            else -> d.format(DateTimeFormatter.ofPattern("d MMM", uk))
        }
        val t = time(v)?.format(DateTimeFormatter.ofPattern("H:mm", uk))
        return listOfNotNull(day, t).joinToString(" ") + " · " + v.title
    }

    /**
     * When the visit's reminder goes off, or null for none. "Evening before"
     * is 8 pm the day before and "Morning of" 8 am; "2 hours before" needs a
     * time, so a visit without one gets the morning-of reminder instead.
     */
    fun reminderAt(v: Visit): LocalDateTime? {
        val d = date(v) ?: return null
        return when (VisitReminder.of(v.reminder)) {
            VisitReminder.NONE -> null
            VisitReminder.EVENING -> d.minusDays(1).atTime(20, 0)
            VisitReminder.MORNING -> d.atTime(8, 0)
            VisitReminder.TWO_HOURS -> time(v)?.let { d.atTime(it).minusHours(2) } ?: d.atTime(8, 0)
        }
    }

    fun canSave(title: String, date: LocalDate?): Boolean = title.isNotBlank() && date != null

    /** A new visit's id; only has to differ from the others on this phone. */
    fun newId(nowMillis: Long): String = "v$nowMillis"
}
