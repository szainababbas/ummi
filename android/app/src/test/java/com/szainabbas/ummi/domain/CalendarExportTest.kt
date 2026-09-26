package com.szainabbas.ummi.domain

import com.szainabbas.ummi.TestFiles
import com.szainabbas.ummi.data.model.ActEntry
import com.szainabbas.ummi.data.model.UmmiData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.TimeZone

class CalendarExportTest {
    private val months = UmmiData.parse(TestFiles.ummiData).months

    /** tests/run.js checks the web app's buildICS against the same file. */
    @Test
    fun `writes the same calendar file as the web app, byte for byte`() {
        val expected = TestFiles.read("../../tests/fixtures/calendar-2026-12-15.ics")
        val actual = CalendarExport.build(LocalDate.of(2026, 12, 15), months, today = LocalDate.of(2026, 9, 26))
        if (expected != actual) {
            val at = expected.zip(actual).indexOfFirst { (a, b) -> a != b }.let { if (it < 0) minOf(expected.length, actual.length) else it }
            throw AssertionError(
                "calendar differs at char $at:\nweb:     " + expected.substring(maxOf(0, at - 80), minOf(expected.length, at + 80)) +
                    "\nandroid: " + actual.substring(maxOf(0, at - 80), minOf(actual.length, at + 80)),
            )
        }
    }

    /** The web app's 24-hour stepping duplicates a date across the October clock change for a summer due date. */
    @Test
    fun `has exactly one event per day, across a clock change`() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/London"))
            val due = LocalDate.of(2027, 7, 15)
            val ics = CalendarExport.build(due, months, today = LocalDate.of(2026, 9, 26))
            val starts = Regex("""DTSTART;VALUE=DATE:(\d{8})""").findAll(ics).map { it.groupValues[1] }.toList()
            assertEquals("a date appears twice", starts.size, starts.toSet().size)
            val expected = (28..280).map { due.minusDays(280).plusDays(it.toLong()) }
                .map { it.toString().replace("-", "") }
            assertEquals(expected, starts)
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test
    fun `leaves short lines alone`() {
        assertEquals("SUMMARY:Short", CalendarExport.fold("SUMMARY:Short"))
    }

    @Test
    fun `folds long lines at 75 bytes, then 74 after the leading space`() {
        val folded = CalendarExport.fold("D".repeat(200))
        val parts = folded.split("\r\n")
        assertEquals(listOf(75, 75, 52), parts.map { it.toByteArray(Charsets.UTF_8).size })
        assertTrue(parts.drop(1).all { it.startsWith(" ") })
        assertEquals("D".repeat(200), parts.first() + parts.drop(1).joinToString("") { it.drop(1) })
    }

    @Test
    fun `never cuts through an Arabic or accented character`() {
        val line = "DESCRIPTION:" + "رَبِّ هَبْ لِي مِنَ الصَّالِحِينَ ".repeat(4) + "Ṣalawāt ".repeat(10)
        val parts = CalendarExport.fold(line).split("\r\n")
        parts.forEach { assertTrue("over 75 bytes: $it", it.toByteArray(Charsets.UTF_8).size <= 75) }
        parts.forEach { assertTrue("broken character in: $it", !it.contains('�')) }
        assertEquals(line, parts.first() + parts.drop(1).joinToString("") { it.drop(1) })
    }

    @Test
    fun `escapes the characters the format reserves`() {
        assertEquals("a\\,b\\;c\\\\d\\ne", CalendarExport.escape("a,b;c\\d\ne"))
    }

    @Test
    fun `leads with a weekday-pinned act, and otherwise rotates the every-day ones`() {
        fun act(id: String, days: List<Int>?) = ActEntry(id = id, cat = "Food", t = id, days = days)
        val daily = listOf(act("x", null), act("y", null))
        assertEquals("x", CalendarExport.featuredAct(daily, 30).id)
        assertEquals("y", CalendarExport.featuredAct(daily, 31).id)
        assertEquals("pin", CalendarExport.featuredAct(daily + act("pin", listOf(3)), 31).id)
    }
}
