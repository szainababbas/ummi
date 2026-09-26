package com.szainabbas.ummi.domain

import com.szainabbas.ummi.data.model.ActEntry
import com.szainabbas.ummi.data.model.MonthEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DailyPlanTest {
    private val sunday = LocalDate.of(2026, 9, 27)
    private val monday = LocalDate.of(2026, 9, 28)
    private val tuesday = LocalDate.of(2026, 9, 29)
    private val saturday = LocalDate.of(2026, 10, 3)

    private fun act(id: String, days: List<Int>?) = ActEntry(id = id, cat = "Food", t = id, days = days)

    private val month = MonthEntry(
        from = 1, to = 4, note = "",
        acts = listOf(act("daily1", null), act("sun", listOf(0)), act("monThu", listOf(1, 4)), act("daily2", null)),
    )

    @Test
    fun `numbers weekdays the way the content does, Sunday as 0`() {
        assertEquals(0, DailyPlan.jsWeekday(sunday))
        assertEquals(1, DailyPlan.jsWeekday(monday))
        assertEquals(6, DailyPlan.jsWeekday(saturday))
    }

    @Test
    fun `hands out every-day acts plus the ones pinned to today, pinned first`() {
        assertEquals(listOf("sun", "daily1", "daily2"), DailyPlan.actsFor(month, sunday).map { it.id })
        assertEquals(listOf("monThu", "daily1", "daily2"), DailyPlan.actsFor(month, monday).map { it.id })
        assertEquals(listOf("daily1", "daily2"), DailyPlan.actsFor(month, tuesday).map { it.id })
    }

    @Test
    fun `adds the every-day tasks after the month's acts`() {
        val base = listOf(act("salah", null), act("water", null))
        assertEquals(listOf("sun", "daily1", "daily2", "salah", "water"), DailyPlan.tasksFor(month, base, sunday).map { it.id })
        assertEquals(listOf("salah", "water"), DailyPlan.tasksFor(null, base, sunday).map { it.id })
    }

    @Test
    fun `has nothing to hand out without a month`() {
        assertEquals(emptyList<ActEntry>(), DailyPlan.actsFor(null, sunday))
    }

    @Test
    fun `rotates today's dua by day of the year, as the web app does`() {
        assertEquals(1, DailyPlan.duaTodayIndex(LocalDate.of(2026, 1, 1), 8))
        assertEquals(365 % 8, DailyPlan.duaTodayIndex(LocalDate.of(2026, 12, 31), 8))
        assertEquals(270 % 8, DailyPlan.duaTodayIndex(sunday, 8))
        assertNull(DailyPlan.duaTodayIndex(sunday, 0))
    }
}
