package com.szainabbas.ummi.domain

import com.szainabbas.ummi.TestFiles
import com.szainabbas.ummi.data.model.MonthEntry
import com.szainabbas.ummi.data.model.UmmiData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class PregnancyMathTest {
    private val today = LocalDate.of(2026, 9, 26)
    private fun dueIn(days: Long) = today.plusDays(days)
    private val months = UmmiData.parse(TestFiles.ummiData).months

    @Test
    fun `is week 40 on the due date`() {
        assertEquals(40, PregnancyMath.currentWeek(dueIn(0), today))
    }

    /** The PWA's floor((280 - daysToDue) / 7), so a week turns over on the same day in both apps. */
    @Test
    fun `counts back a week for every seven days to go`() {
        assertEquals(39, PregnancyMath.currentWeek(dueIn(1), today))
        assertEquals(39, PregnancyMath.currentWeek(dueIn(7), today))
        assertEquals(38, PregnancyMath.currentWeek(dueIn(8), today))
        assertEquals(25, PregnancyMath.currentWeek(dueIn(105), today))
        assertEquals(24, PregnancyMath.currentWeek(dueIn(106), today))
    }

    @Test
    fun `keeps counting past the due date, up to week 41`() {
        assertEquals(40, PregnancyMath.currentWeek(dueIn(-6), today))
        assertEquals(41, PregnancyMath.currentWeek(dueIn(-7), today))
        assertEquals(41, PregnancyMath.currentWeek(dueIn(-30), today))
    }

    @Test
    fun `never goes below week 4, where the content starts`() {
        assertEquals(4, PregnancyMath.currentWeek(dueIn(280), today))
        assertEquals(4, PregnancyMath.currentWeek(dueIn(400), today))
        assertEquals(4, PregnancyMath.currentWeek(null, today))
    }

    @Test
    fun `counts days to go, never below zero`() {
        assertEquals(105, PregnancyMath.daysToGo(dueIn(105), today))
        assertEquals(0, PregnancyMath.daysToGo(dueIn(-3), today))
        assertNull(PregnancyMath.daysToGo(null, today))
    }

    @Test
    fun `puts weeks 13, 14, 27 and 28 in the same trimesters as the web app`() {
        assertEquals(1, PregnancyMath.trimester(4))
        assertEquals(1, PregnancyMath.trimester(13))
        assertEquals(2, PregnancyMath.trimester(14))
        assertEquals(2, PregnancyMath.trimester(27))
        assertEquals(3, PregnancyMath.trimester(28))
        assertEquals(3, PregnancyMath.trimester(41))
    }

    @Test
    fun `maps weeks to the guide's months at every boundary`() {
        assertEquals(1, PregnancyMath.currentMonthNumber(4, months))
        assertEquals(2, PregnancyMath.currentMonthNumber(5, months))
        assertEquals(6, PregnancyMath.currentMonthNumber(24, months))
        assertEquals(7, PregnancyMath.currentMonthNumber(25, months))
        assertEquals(9, PregnancyMath.currentMonthNumber(33, months))
        assertEquals(9, PregnancyMath.currentMonthNumber(41, months))
    }

    @Test
    fun `falls back like the web app when no month holds the week`() {
        val sparse = mapOf("1" to MonthEntry(from = 5, to = 8, note = "", acts = emptyList()))
        assertEquals(1, PregnancyMath.currentMonthNumber(0, sparse))
        assertEquals(9, PregnancyMath.currentMonthNumber(3, sparse))
        assertEquals(9, PregnancyMath.currentMonthNumber(50, sparse))
    }

    @Test
    fun `reads the due date the way the backup stores it`() {
        assertEquals(LocalDate.of(2027, 1, 9), PregnancyMath.parseDueDate("2027-01-09"))
        assertNull(PregnancyMath.parseDueDate("09/01/2027"))
        assertNull(PregnancyMath.parseDueDate(""))
        assertNull(PregnancyMath.parseDueDate(null))
    }

    @Test
    fun `formats dates in English whatever the phone's language`() {
        assertEquals("9 Jan", PregnancyMath.shortDate(LocalDate.of(2027, 1, 9)))
        assertEquals("2026-09-26", PregnancyMath.todayKey(today))
    }

    @Test
    fun `reads the date picker's UTC midnight as that calendar day`() {
        assertEquals(LocalDate.of(2026, 12, 15), PregnancyMath.fromPickerMillis(1797292800000))
        assertEquals(1797292800000, PregnancyMath.toPickerMillis(LocalDate.of(2026, 12, 15)))
    }

    /** Converting through the phone's zone instead of UTC is off by a day on one side of the date line. */
    @Test
    fun `keeps the picked day in every time zone`() {
        val original = java.util.TimeZone.getDefault()
        try {
            for (zone in listOf("Pacific/Kiritimati", "America/Los_Angeles", "Europe/London", "Asia/Karachi")) {
                java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(zone))
                val due = LocalDate.of(2026, 12, 15)
                assertEquals(zone, due, PregnancyMath.fromPickerMillis(PregnancyMath.toPickerMillis(due)))
                assertEquals(zone, due, PregnancyMath.fromPickerMillis(1797292800000))
            }
        } finally {
            java.util.TimeZone.setDefault(original)
        }
    }

    @Test
    fun `writes the due date out in full`() {
        assertEquals("15 December 2026", PregnancyMath.longDate(LocalDate.of(2026, 12, 15)))
    }

    @Test
    fun `picks a or an for the size sentence`() {
        assertEquals("About the size of a poppy seed", PregnancyMath.sizeSentence("Poppy seed"))
        assertEquals("About the size of an apple", PregnancyMath.sizeSentence("Apple"))
        assertEquals("About the size of an ear of corn", PregnancyMath.sizeSentence("Ear of corn"))
    }
}
