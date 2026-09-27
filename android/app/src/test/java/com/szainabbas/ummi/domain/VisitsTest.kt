package com.szainabbas.ummi.domain

import com.szainabbas.ummi.data.Visit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class VisitsTest {
    private val today = LocalDate.of(2026, 9, 27) // a Sunday

    private fun v(id: String, date: String, time: String = "", title: String = id, type: String = "other", reminder: String = "none") =
        Visit(id = id, date = date, time = time, title = title, type = type, reminder = reminder)

    private val visits = listOf(
        v("later", "2026-10-20"),
        v("lastWeek", "2026-09-20", "10:00"),
        v("todayUntimed", "2026-09-27"),
        v("todayPm", "2026-09-27", "14:30"),
        v("todayAm", "2026-09-27", "09:10"),
        v("longAgo", "2026-08-01"),
        v("broken", "not a date"),
    )

    @Test
    fun `lists what's coming up soonest first, keeping today's visits all day`() {
        assertEquals(
            listOf("todayAm", "todayPm", "todayUntimed", "later"),
            Visits.upcoming(visits, today).map { it.id },
        )
        assertEquals("todayAm", Visits.next(visits, today)?.id)
    }

    @Test
    fun `lists past visits most recent first`() {
        assertEquals(listOf("lastWeek", "longAgo"), Visits.past(visits, today).map { it.id })
    }

    @Test
    fun `has no next visit when there is nothing ahead`() {
        assertNull(Visits.next(listOf(v("old", "2026-01-01")), today))
        assertNull(Visits.next(emptyList(), today))
    }

    @Test
    fun `says how far off a visit is`() {
        assertEquals("TODAY", Visits.whenLabel(v("a", "2026-09-27"), today))
        assertEquals("TOMORROW", Visits.whenLabel(v("a", "2026-09-28"), today))
        assertEquals("IN 3 DAYS", Visits.whenLabel(v("a", "2026-09-30"), today))
        assertEquals("IN 13 DAYS", Visits.whenLabel(v("a", "2026-10-10"), today))
        assertEquals("IN 2 WEEKS", Visits.whenLabel(v("a", "2026-10-11"), today))
    }

    @Test
    fun `writes times the UK way`() {
        assertEquals("9:10 am", Visits.timeLabel(v("a", "2026-10-06", "09:10")))
        assertEquals("2:30 pm", Visits.timeLabel(v("a", "2026-10-06", "14:30")))
        assertEquals("", Visits.timeLabel(v("a", "2026-10-06")))
        assertEquals("Tuesday 6 October · 9:10 am", Visits.longWhen(v("a", "2026-10-06", "09:10")))
        assertEquals("Tuesday 6 October", Visits.longWhen(v("a", "2026-10-06")))
    }

    @Test
    fun `leaves out of the subtitle whatever isn't set`() {
        assertEquals("Scan · 9:10 am · Evening before", Visits.meta(v("a", "2026-10-06", "09:10", type = "scan", reminder = "evening")))
        assertEquals("Blood test", Visits.meta(v("a", "2026-10-06", type = "blood")))
        assertEquals("Other", Visits.meta(v("a", "2026-10-06", type = "something new")))
    }

    @Test
    fun `labels the chip on Today by how near the visit is`() {
        assertEquals("Today 9:10 · Glucose", Visits.chipLabel(v("a", "2026-09-27", "09:10", "Glucose"), today))
        assertEquals("Tomorrow · Midwife", Visits.chipLabel(v("a", "2026-09-28", title = "Midwife"), today))
        assertEquals("Tue 14:30 · Scan", Visits.chipLabel(v("a", "2026-09-29", "14:30", "Scan"), today))
        assertEquals("6 Oct · Scan", Visits.chipLabel(v("a", "2026-10-06", title = "Scan"), today))
    }

    @Test
    fun `builds the date tile`() {
        assertEquals("OCT", Visits.tileMonth(v("a", "2026-10-06")))
        assertEquals("6", Visits.tileDay(v("a", "2026-10-06")))
    }

    @Test
    fun `needs a title and a date before it can save`() {
        assertTrue(Visits.canSave("Scan", today))
        assertFalse(Visits.canSave("   ", today))
        assertFalse(Visits.canSave("Scan", null))
    }

    @Test
    fun `reads unknown keys as other and no reminder`() {
        assertEquals(VisitType.OTHER, VisitType.of("x"))
        assertEquals(VisitType.SCAN, VisitType.of("scan"))
        assertEquals(VisitReminder.NONE, VisitReminder.of("x"))
        assertEquals(VisitReminder.TWO_HOURS, VisitReminder.of("2h"))
    }

    @Test
    fun `works out when each kind of reminder goes off`() {
        val v = Visit(id = "a", date = "2026-10-06", time = "09:10", title = "Scan", reminder = "evening")
        assertEquals(LocalDateTime.of(2026, 10, 5, 20, 0), Visits.reminderAt(v))
        assertEquals(LocalDateTime.of(2026, 10, 6, 8, 0), Visits.reminderAt(v.copy(reminder = "morning")))
        assertEquals(LocalDateTime.of(2026, 10, 6, 7, 10), Visits.reminderAt(v.copy(reminder = "2h")))
        // "2 hours before" needs a time; without one it falls back to the morning of.
        assertEquals(LocalDateTime.of(2026, 10, 6, 8, 0), Visits.reminderAt(v.copy(reminder = "2h", time = "")))
        assertEquals(null, Visits.reminderAt(v.copy(reminder = "none")))
        assertEquals(null, Visits.reminderAt(v.copy(date = "soon")))
    }
}
