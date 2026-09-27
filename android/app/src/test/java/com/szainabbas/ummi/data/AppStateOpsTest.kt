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
}
