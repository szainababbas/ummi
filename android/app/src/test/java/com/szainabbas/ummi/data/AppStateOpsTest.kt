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

    private val scan = Visit(id = "v1", date = "2026-10-06", time = "09:10", title = "  Anomaly scan ", type = "scan")

    @Test
    fun `adds a visit, tidying the title and a blank note`() {
        val state = AppState().withVisitSaved(scan.copy(note = "   "))
        assertEquals(listOf(scan.copy(title = "Anomaly scan")), state.visits)
    }

    @Test
    fun `saving a visit with the same id edits it in place`() {
        val other = Visit(id = "v2", date = "2026-10-20", title = "Midwife", type = "midwife")
        val state = AppState().withVisitSaved(scan).withVisitSaved(other)
            .withVisitSaved(scan.copy(title = "Growth scan", note = "full bladder"))
        assertEquals(listOf("Growth scan", "Midwife"), state.visits.map { it.title })
        assertEquals("full bladder", state.visits[0].note)
    }

    @Test
    fun `deletes only the visit with that id`() {
        val other = Visit(id = "v2", date = "2026-10-20", title = "Midwife")
        val state = AppState().withVisitSaved(scan).withVisitSaved(other).withVisitDeleted("v1")
        assertEquals(listOf("v2"), state.visits.map { it.id })
    }

    @Test
    fun `adds names to the top, tidied, and skips blanks and repeats in any case`() {
        val state = AppState().withNameAdded("Maryam").withNameAdded("  zahra   batool ").withNameAdded("   ")
            .withNameAdded("MARYAM")
        assertEquals(listOf("zahra batool", "Maryam"), state.names.map { it.name })
        assertEquals(false, state.names[0].fav)
    }

    @Test
    fun `hearts, notes and removes only the name it is given`() {
        val start = AppState().withNameAdded("Maryam").withNameAdded("Ali")
        val hearted = start.withNameFavouriteToggled("Maryam")
        assertEquals(listOf(false, true), hearted.names.map { it.fav })
        assertEquals(listOf(false, false), hearted.withNameFavouriteToggled("Maryam").names.map { it.fav })
        val noted = start.withNameNote("Ali", "  After Imam ʿAlī (as) ")
        assertEquals(listOf("After Imam ʿAlī (as)", null), noted.names.map { it.note })
        assertEquals(null, noted.withNameNote("Ali", "  ").names[0].note)
        assertEquals(listOf("Maryam"), start.withNameRemoved("Ali").names.map { it.name })
    }

    @Test
    fun `undoing a removal puts the name back where it was`() {
        val start = AppState().withNameAdded("C").withNameAdded("B").withNameAdded("A") // A, B, C
        val b = start.names[1].copy(fav = true)
        val removed = start.withNameRemoved("B")
        assertEquals(listOf("A", "B", "C"), removed.withNameRestored(b, 1).names.map { it.name })
        assertEquals(true, removed.withNameRestored(b, 1).names[1].fav)
        assertSame(start, start.withNameRestored(b, 1)) // already there
    }

    @Test
    fun `a bell on one act is her choice for that act, and water's bell is the water switch`() {
        val state = AppState().withTaskReminder("m6d", true).withTaskReminder("m4c", false).withTaskReminder("water", true)
        assertEquals(mapOf("m6d" to true, "m4c" to false), state.reminders.tasks)
        assertEquals(true, state.reminders.water)
    }

    @Test
    fun `the prayer-linked switch clears her choices for prayer-linked acts only`() {
        val state = AppState().withTaskReminder("m4c", false).withTaskReminder("m6d", true)
            .withPrayerLinked(true, listOf("m4c", "m6a"))
        assertEquals(true, state.reminders.prayer)
        assertEquals(mapOf("m6d" to true), state.reminders.tasks)
    }

    @Test
    fun `the every-day switches change only the one asked for`() {
        val state = AppState().withDailyReminders(morning = true).withDailyReminders(water = true).withReminderPlace("leeds")
        assertEquals(ReminderSettings(morning = true, prayer = false, water = true, place = "leeds"), state.reminders)
    }
}
