package com.szainabbas.ummi.domain

import com.szainabbas.ummi.data.Visit
import com.szainabbas.ummi.data.VisitReminder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Sorting, labelling and reminder times for the Visits screen and its notifications. */
object Visits {
    /** The handoff's visit types, in the order the chips show them. */
    val TYPES = listOf("Midwife", "Scan", "Blood test", "GP", "Consultant", "Other")

    private val hm = DateTimeFormatter.ofPattern("H:mm", Locale.UK)

    fun at(visit: Visit): LocalDateTime? = runCatching {
        LocalDateTime.of(LocalDate.parse(visit.date), LocalTime.parse(visit.time))
    }.getOrNull()

    /** Still to come, soonest first. A visit stays "coming up" until its start time has passed. */
    fun upcoming(visits: List<Visit>, now: LocalDateTime): List<Visit> =
        visits.filter { at(it)?.let { t -> !t.isBefore(now) } ?: false }.sortedBy { at(it) }

    /** Already happened, most recent first. */
    fun past(visits: List<Visit>, now: LocalDateTime): List<Visit> =
        visits.filter { at(it)?.isBefore(now) ?: false }.sortedByDescending { at(it) }

    fun next(visits: List<Visit>, now: LocalDateTime): Visit? = upcoming(visits, now).firstOrNull()

    /** When to remind her, or null for no reminder. */
    fun reminderAt(visit: Visit): LocalDateTime? {
        val start = at(visit) ?: return null
        return when (VisitReminder.of(visit.reminder)) {
            VisitReminder.NONE -> null
            VisitReminder.EVENING_BEFORE -> start.toLocalDate().minusDays(1).atTime(20, 0)
            VisitReminder.MORNING_OF -> start.toLocalDate().atTime(8, 0)
            VisitReminder.TWO_HOURS -> start.minusHours(2)
        }
    }

    /** The hero's eyebrow: "Next · today", "Next · tomorrow", "Next · in 3 days". */
    fun countdown(visit: Visit, today: LocalDate): String {
        val date = runCatching { LocalDate.parse(visit.date) }.getOrNull() ?: return "Next"
        return when (val days = ChronoUnit.DAYS.between(today, date)) {
            0L -> "Next · today"
            1L -> "Next · tomorrow"
            else -> "Next · in $days days"
        }
    }

    /** "Tuesday 29 September, 9:10" for the hero. */
    fun longWhen(visit: Visit): String {
        val t = at(visit) ?: return visit.date
        return t.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.UK)) + ", " + t.format(hm)
    }

    /** "Tue 9:10 · Glucose test" for the chip on Today. */
    fun chip(visit: Visit): String {
        val t = at(visit) ?: return visit.title
        return t.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)) + " " + t.format(hm) + " · " + visit.title
    }

    /** "Midwife · 10:30 · reminder evening before" for a Coming up row. */
    fun meta(visit: Visit): String {
        val time = at(visit)?.format(hm) ?: visit.time
        val reminder = VisitReminder.of(visit.reminder)
        return visit.type + " · " + time + if (reminder == VisitReminder.NONE) "" else " · reminder " + reminder.label.lowercase()
    }

    /** The hero's reminder row: "Remind me the evening before · 8:00 pm". */
    fun reminderLine(visit: Visit): String = when (VisitReminder.of(visit.reminder)) {
        VisitReminder.NONE -> "No reminder"
        VisitReminder.EVENING_BEFORE -> "Remind me the evening before · 8:00 pm"
        VisitReminder.MORNING_OF -> "Remind me the morning of · 8:00 am"
        VisitReminder.TWO_HOURS -> "Remind me 2 hours before"
    }

    /** "Oct" and "16" for the date tile. English, not UK: the UK short form of September is "Sept". */
    fun tile(visit: Visit): Pair<String, String> {
        val date = runCatching { LocalDate.parse(visit.date) }.getOrNull() ?: return "" to ""
        return date.format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)) to date.dayOfMonth.toString()
    }

    /** The snackbar after turning a reminder on: "Reminder set · Mon 8:00 pm". */
    fun reminderSetMessage(visit: Visit): String {
        val at = reminderAt(visit) ?: return "Reminder off"
        return "Reminder set · " + at.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)) + " " + Clock.label(at.toLocalTime())
    }

    /** The snackbar after saving: "Visit added · reminder evening before". */
    fun savedMessage(visit: Visit, isNew: Boolean): String {
        val reminder = VisitReminder.of(visit.reminder)
        return (if (isNew) "Visit added" else "Visit saved") +
            if (reminder == VisitReminder.NONE) "" else " · reminder " + reminder.label.lowercase()
    }
}
