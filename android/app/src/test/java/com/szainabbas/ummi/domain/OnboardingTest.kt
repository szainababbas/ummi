package com.szainabbas.ummi.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class OnboardingTest {
    private val today = LocalDate.of(2026, 9, 27)

    @Test
    fun `walks forward through four steps and back, and no further either way`() {
        var o = Onboarding()
        assertFalse(o.canGoBack)
        repeat(5) { o = o.next() }
        assertEquals(3, o.step)
        assertTrue(o.canGoBack)
        repeat(5) { o = o.back() }
        assertEquals(0, o.step)
    }

    @Test
    fun `the button says what it will do`() {
        assertEquals("Begin the journey", Onboarding(step = 0).cta)
        assertEquals("Continue", Onboarding(step = 1).cta)
        assertEquals("Skip", Onboarding(step = 2, name = "  ").cta)
        assertEquals("Continue", Onboarding(step = 2, name = "Aimen").cta)
        assertEquals("Allow notifications", Onboarding(step = 3).cta)
    }

    @Test
    fun `the due date step waits for a date, but not in week mode`() {
        assertFalse(Onboarding(step = 1).canContinue(today))
        assertNull(Onboarding(step = 1).summary(today))
        assertTrue(Onboarding(step = 1, due = LocalDate.of(2026, 11, 20)).canContinue(today))
        assertTrue(Onboarding(step = 1, byWeek = true).canContinue(today))
        assertTrue(Onboarding(step = 2).canContinue(today))
    }

    @Test
    fun `sums up the week and trimester from either a date or a week`() {
        assertEquals("That's week 32, 3rd trimester. Welcome.", Onboarding(due = LocalDate.of(2026, 11, 20)).summary(today))
        assertEquals("That's week 24, 2nd trimester. Welcome.", Onboarding(byWeek = true, week = 24).summary(today))
    }

    /** The web app's deriveDueFromWeek had the week-14 case a day early in a UK summer; this is the fixed answer. */
    @Test
    fun `works the due date out from her week in calendar days, as the web app does`() {
        assertEquals(LocalDate.of(2027, 3, 28), Onboarding.dueFromWeek(14, today))
        assertEquals(today, Onboarding.dueFromWeek(40, today))
        assertEquals(14, Onboarding(byWeek = true, week = 14).weekNow(today))
    }

    @Test
    fun `the week stepper stays between 4 and 41`() {
        var o = Onboarding(week = 5)
        repeat(3) { o = o.weekDown() }
        assertEquals(4, o.week)
        o = Onboarding(week = 40)
        repeat(3) { o = o.weekUp() }
        assertEquals(41, o.week)
    }

    @Test
    fun `starts with the morning summary and prayer-linked acts ticked, and water not`() {
        val o = Onboarding()
        assertEquals(listOf(true, true, false), listOf(o.morning, o.prayer, o.water))
    }
}
