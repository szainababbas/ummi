package com.szainabbas.ummi.domain

import com.szainabbas.ummi.data.Visit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class VisitsTest {
    private val now = LocalDateTime.of(2026, 9, 26, 12, 0)
    private val ogtt = Visit("a", "2026-09-29", "09:10", "Glucose test (OGTT)", type = "Blood test", reminder = "evening")
    private val midwife = Visit("b", "2026-10-16", "10:30", "Midwife check · 28 weeks")
    private val scan = Visit("c", "2026-08-28", "09:00", "Anomaly scan · 20 weeks", type = "Scan")
    private val thisMorning = Visit("d", "2026-09-26", "11:59", "Earlier today")
    private val visits = listOf(midwife, scan, ogtt, thisMorning)

    @Test
    fun `lists visits to come soonest first, and past ones latest first`() {
        assertEquals(listOf("a", "b"), Visits.upcoming(visits, now).map { it.id })
        assertEquals(listOf("d", "c"), Visits.past(visits, now).map { it.id })
        assertEquals("a", Visits.next(visits, now)?.id)
    }

    @Test
    fun `keeps a visit coming up until its start time`() {
        val atNoon = thisMorning.copy(time = "12:00")
        assertEquals(listOf(atNoon), Visits.upcoming(listOf(atNoon), now))
    }

    @Test
    fun `skips a visit with a date it cannot read`() {
        assertEquals(emptyList<Visit>(), Visits.upcoming(listOf(ogtt.copy(date = "soon")), now))
        assertEquals(emptyList<Visit>(), Visits.past(listOf(ogtt.copy(date = "soon")), now))
    }

    @Test
    fun `works out each kind of reminder`() {
        assertEquals(LocalDateTime.of(2026, 9, 28, 20, 0), Visits.reminderAt(ogtt))
        assertEquals(LocalDateTime.of(2026, 9, 29, 8, 0), Visits.reminderAt(ogtt.copy(reminder = "morning")))
        assertEquals(LocalDateTime.of(2026, 9, 29, 7, 10), Visits.reminderAt(ogtt.copy(reminder = "2h")))
        assertNull(Visits.reminderAt(midwife))
    }

    @Test
    fun `counts down in days`() {
        val today = LocalDate.of(2026, 9, 26)
        assertEquals("Next · in 3 days", Visits.countdown(ogtt, today))
        assertEquals("Next · tomorrow", Visits.countdown(ogtt, today.plusDays(2)))
        assertEquals("Next · today", Visits.countdown(ogtt, today.plusDays(3)))
    }

    @Test
    fun `labels a visit the way the handoff does`() {
        assertEquals("Tuesday 29 September, 9:10", Visits.longWhen(ogtt))
        assertEquals("Tue 9:10 · Glucose test (OGTT)", Visits.chip(ogtt))
        assertEquals("Blood test · 9:10 · reminder evening before", Visits.meta(ogtt))
        assertEquals("Midwife · 10:30", Visits.meta(midwife))
        assertEquals("Remind me the evening before · 8:00 pm", Visits.reminderLine(ogtt))
        assertEquals("Sep" to "29", Visits.tile(ogtt))
    }

    @Test
    fun `words the snackbars`() {
        assertEquals("Reminder set · Mon 8:00 pm", Visits.reminderSetMessage(ogtt))
        assertEquals("Reminder set · Tue 7:10 am", Visits.reminderSetMessage(ogtt.copy(reminder = "2h")))
        assertEquals("Reminder off", Visits.reminderSetMessage(midwife))
        assertEquals("Visit added · reminder evening before", Visits.savedMessage(ogtt, isNew = true))
        assertEquals("Visit saved", Visits.savedMessage(midwife, isNew = false))
    }
}
