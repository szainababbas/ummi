package com.szainabbas.ummi.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class AppStateOpsTest {
    private val day1 = "2026-09-26"
    private val day2 = "2026-09-27"

    @Test
    fun `ticking a task twice unticks it`() {
        val once = AppState().withTaskToggled("m4a", day1)
        assertEquals(listOf("m4a"), once.done[day1])
        assertEquals(emptyList<String>(), once.withTaskToggled("m4a", day1).done[day1])
    }

    @Test
    fun `keeps each day's ticks separate`() {
        val state = AppState().withTaskToggled("m4a", day1).withTaskToggled("salah", day2)
        assertEquals(listOf("m4a"), state.done[day1])
        assertEquals(listOf("salah"), state.done[day2])
    }

    @Test
    fun `marking the dua recited toggles only that day`() {
        val state = AppState().withDuaRecitedToggled(day1)
        assertEquals(true, state.duaDone[day1])
        assertEquals(null, state.duaDone[day2])
        assertEquals(false, state.withDuaRecitedToggled(day1).duaDone[day1])
    }

    @Test
    fun `adds reflections newest first, trimmed`() {
        val state = AppState().withJournalEntry("first", day1).withJournalEntry("  second  ", day2)
        assertEquals(listOf(JournalEntry(day2, "second"), JournalEntry(day1, "first")), state.journal)
    }

    @Test
    fun `ignores a blank reflection`() {
        val state = AppState(dueDate = "2026-11-20")
        assertSame(state, state.withJournalEntry("   ", day1))
    }

    @Test
    fun `adds names to the top, trimmed, and skips blanks and repeats`() {
        val state = AppState().withNameAdded("Ali").withNameAdded("  Maryam ").withNameAdded(" ").withNameAdded("maryam")
        assertEquals(listOf("Maryam", "Ali"), state.names.map { it.name })
    }

    @Test
    fun `hearts, notes and removes one name without touching the others`() {
        val state = AppState().withNameAdded("Ali").withNameAdded("Maryam")
            .withNameFavouriteToggled("Ali")
            .withNameNote("Maryam", "  After Sayyidah Maryam (as) ")
        assertEquals(listOf(false, true), state.names.map { it.favourite })
        assertEquals(listOf("After Sayyidah Maryam (as)", ""), state.names.map { it.note })
        assertEquals(false, state.withNameFavouriteToggled("Ali").names[1].favourite)
        assertEquals(listOf("Ali"), state.withNameRemoved("Maryam").names.map { it.name })
    }

    private val scan = Visit(id = "v1", date = "2026-10-16", time = "10:30", title = " Dating scan ", type = "Scan")

    @Test
    fun `saving a visit adds it, and saving it again replaces it`() {
        val added = AppState().withVisitSaved(scan)
        assertEquals(listOf("Dating scan"), added.visits.map { it.title })
        val edited = added.withVisitSaved(scan.copy(title = "Anomaly scan", place = " Clinic "))
        assertEquals(1, edited.visits.size)
        assertEquals("Anomaly scan", edited.visits[0].title)
        assertEquals("Clinic", edited.visits[0].place)
    }

    @Test
    fun `changes a visit's reminder and removes a visit`() {
        val state = AppState().withVisitSaved(scan).withVisitSaved(scan.copy(id = "v2"))
        assertEquals("evening", state.withVisitReminder("v2", VisitReminder.EVENING_BEFORE).visits[1].reminder)
        assertEquals("none", state.withVisitReminder("v2", VisitReminder.EVENING_BEFORE).visits[0].reminder)
        assertEquals(listOf("v2"), state.withVisitRemoved("v1").visits.map { it.id })
    }

    @Test
    fun `reads an unknown visit reminder as none`() {
        assertEquals(VisitReminder.NONE, VisitReminder.of("fortnightly"))
        assertEquals(VisitReminder.TWO_HOURS, VisitReminder.of("2h"))
    }

    @Test
    fun `sets and clears a task's reminder time`() {
        val on = AppState().withTaskReminder("m4a", "07:30")
        assertEquals(mapOf("m4a" to "07:30"), on.reminders.tasks)
        assertEquals(emptyMap<String, String>(), on.withTaskReminder("m4a", null).reminders.tasks)
    }
}
