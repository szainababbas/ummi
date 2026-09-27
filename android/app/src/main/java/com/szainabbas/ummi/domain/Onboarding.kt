package com.szainabbas.ummi.domain

import java.time.LocalDate

/**
 * The four-step welcome: welcome, due date, name, reminders. Kept as plain
 * values so the screen only draws it and the tests can walk through it.
 */
data class Onboarding(
    val step: Int = 0,
    val byWeek: Boolean = false,
    val due: LocalDate? = null,
    val week: Int = 12,
    val name: String = "",
    val morning: Boolean = true,
    val prayer: Boolean = true,
    val water: Boolean = false,
) {
    val canGoBack: Boolean get() = step > 0

    /** The due date she ends up with: the one she picked, or one worked out from her week. */
    fun dueDate(today: LocalDate): LocalDate? = if (byWeek) dueFromWeek(week, today) else due

    fun weekNow(today: LocalDate): Int? = dueDate(today)?.let { PregnancyMath.currentWeek(it, today) }

    /** "That's week 24, 2nd trimester. Welcome." Null until there is a date to go on. */
    fun summary(today: LocalDate): String? =
        weekNow(today)?.let { "That's week $it, ${PregnancyMath.trimesterShort(it)}. Welcome." }

    /** Step 2 needs a date before it lets her on; the rest always can. */
    fun canContinue(today: LocalDate): Boolean = step != 1 || dueDate(today) != null

    val cta: String get() = when (step) {
        0 -> "Begin the journey"
        1 -> "Continue"
        2 -> if (name.isBlank()) "Skip" else "Continue"
        else -> "Allow notifications"
    }

    fun next(): Onboarding = copy(step = (step + 1).coerceAtMost(LAST))
    fun back(): Onboarding = copy(step = (step - 1).coerceAtLeast(0))
    fun weekDown(): Onboarding = copy(week = (week - 1).coerceAtLeast(MIN_WEEK))
    fun weekUp(): Onboarding = copy(week = (week + 1).coerceAtMost(MAX_WEEK))

    companion object {
        const val LAST = 3
        const val MIN_WEEK = 4
        const val MAX_WEEK = 41

        /**
         * The PWA's `deriveDueFromWeek`: week 40 is the due date, so it is
         * (40 - week) weeks from today, counted in calendar days.
         */
        fun dueFromWeek(week: Int, today: LocalDate): LocalDate = today.plusDays((40L - week) * 7)
    }
}
