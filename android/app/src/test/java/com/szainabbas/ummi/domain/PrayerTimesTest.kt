package com.szainabbas.ummi.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class PrayerTimesTest {
    private val uk = ZoneId.of("Europe/London")

    /**
     * From the praytimes.org Python library (praytimes 2.3.2 on PyPI),
     * method "Jafari" with highLats "AngleBased", for the equinox, both
     * solstices and today. Sunrise, noon and sunset also agree to the minute
     * with adhanpy, a separate implementation (a port of Adhan).
     * Columns: date, Fajr, sunrise, Ẓuhr, ʿAṣr, sunset, Maghrib, ʿIshāʾ.
     */
    private val reference = mapOf(
        "london" to listOf(
            "2026-03-20 04:24 06:03 12:08 15:25 18:14 18:34 19:40",
            "2026-06-21 02:45 04:43 13:02 17:25 21:22 21:50 23:05",
            "2026-09-27 05:15 06:55 12:51 16:03 18:47 19:08 20:13",
            "2026-12-21 06:13 08:04 11:59 13:38 15:53 16:19 17:31",
        ),
        "birmingham" to listOf(
            "2026-03-20 04:28 06:10 12:15 15:32 18:21 18:42 19:49",
            "2026-06-21 02:50 04:45 13:09 17:34 21:34 22:03 23:15",
            "2026-09-27 05:20 07:02 12:59 16:09 18:54 19:15 20:22",
            "2026-12-21 06:22 08:16 12:06 13:40 15:55 16:21 17:35",
        ),
    )

    @Test
    fun `matches praytimes-org's Jafari times to the minute, summer and winter`() {
        for ((key, rows) in reference) {
            for (row in rows) {
                val cols = row.split(" ")
                val t = PrayerTimes.forDay(LocalDate.parse(cols[0]), PrayerTimes.place(key), uk)
                val got = listOf(t.fajr, t.sunrise, t.dhuhr, t.asr, t.sunset, t.maghrib, t.isha).map { it.toString() }
                assertEquals("$key ${cols[0]}", cols.drop(1), got)
            }
        }
    }

    @Test
    fun `puts Fajr a sixteen-sixtieths share of the night before sunrise when the sky never gets dark enough`() {
        // London at midsummer: the sun never reaches 16° below, so Fajr has no angle to find.
        val t = PrayerTimes.forDay(LocalDate.of(2026, 6, 21), PrayerTimes.place("london"), uk)
        val night = ChronoUnit.MINUTES.between(t.sunset, LocalTime.MAX) + 1 + ChronoUnit.MINUTES.between(LocalTime.MIDNIGHT, t.sunrise)
        val before = ChronoUnit.MINUTES.between(t.fajr, t.sunrise)
        assertTrue("Fajr $before minutes before sunrise, night $night", kotlin.math.abs(before - night * 16 / 60) <= 1)
    }

    @Test
    fun `Maghrib comes after sunset and ʿIshāʾ after Maghrib, every day of the year`() {
        var d = LocalDate.of(2026, 1, 1)
        while (d.year == 2026) {
            val t = PrayerTimes.forDay(d, PrayerTimes.place("glasgow"), uk)
            assertTrue("$d", t.fajr < t.sunrise && t.sunrise < t.dhuhr && t.dhuhr < t.asr && t.asr < t.sunset)
            assertTrue("$d", t.sunset < t.maghrib && t.maghrib < t.isha)
            d = d.plusDays(1)
        }
    }

    @Test
    fun `follows the clocks on the day they change`() {
        val before = PrayerTimes.forDay(LocalDate.of(2026, 3, 28), PrayerTimes.place("london"), uk)
        val after = PrayerTimes.forDay(LocalDate.of(2026, 3, 29), PrayerTimes.place("london"), uk)
        val shift = ChronoUnit.MINUTES.between(before.dhuhr, after.dhuhr)
        assertTrue("Ẓuhr moved $shift minutes", shift in 59..61)
    }

    @Test
    fun `an unknown town falls back to London`() {
        assertEquals("London", PrayerTimes.place("atlantis").name)
        assertEquals("London", PrayerTimes.place(null).name)
    }
}
