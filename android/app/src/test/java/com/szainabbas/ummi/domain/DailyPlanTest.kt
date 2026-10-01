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

    private val abc = listOf(act("a", null), act("b", null), act("c", null))

    @Test
    fun `puts the first open task up next`() {
        assertEquals("a", DailyPlan.upNext(abc, done = emptySet(), skipped = emptySet())?.id)
        assertEquals("b", DailyPlan.upNext(abc, done = setOf("a"), skipped = emptySet())?.id)
        assertNull(DailyPlan.upNext(abc, done = setOf("a", "b", "c"), skipped = emptySet()))
    }

    @Test
    fun `puts today's pinned act up next ahead of every-day ones, wherever it sits in the list`() {
        val day = listOf(act("adhan", null), act("salah", null), act("surah", listOf(4)))
        assertEquals("surah", DailyPlan.upNext(day, done = emptySet(), skipped = emptySet())?.id)
        assertEquals("adhan", DailyPlan.upNext(day, done = setOf("surah"), skipped = emptySet())?.id)
        assertEquals("adhan", DailyPlan.upNext(day, done = emptySet(), skipped = setOf("surah"))?.id)
    }

    /** Presses "Later" [times] times and records what was up next before each press. */
    private fun pressLater(times: Int, done: Set<String>): List<String> {
        var skipped = emptySet<String>()
        return List(times) {
            val next = DailyPlan.upNext(abc, done, skipped)!!
            skipped = DailyPlan.later(abc, done, skipped, next)
            next.id
        }
    }

    @Test
    fun `later moves on through the open tasks and keeps wrapping round`() {
        assertEquals(listOf("a", "b", "c", "a", "b", "c", "a"), pressLater(7, done = emptySet()))
    }

    @Test
    fun `later skips over tasks already done, and still wraps round`() {
        assertEquals(listOf("a", "c", "a", "c", "a"), pressLater(5, done = setOf("b")))
    }

    @Test
    fun `later on the only open task leaves it up next`() {
        val done = setOf("a", "b")
        val only = DailyPlan.upNext(abc, done, emptySet())!!
        assertEquals("c", DailyPlan.upNext(abc, done, DailyPlan.later(abc, done, emptySet(), only))?.id)
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
