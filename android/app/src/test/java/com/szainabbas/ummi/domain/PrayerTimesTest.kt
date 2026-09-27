package com.szainabbas.ummi.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Expected times come from praytimes.org's reference implementation (the
 * `praytimes` npm package, method "Jafari", high-latitude rule "NightMiddle"),
 * run once by hand; it is not a dependency of the app.
 */
class PrayerTimesTest {
    private val london = ZoneId.of("Europe/London")

    private fun times(date: String, lat: Double, lng: Double, zone: ZoneId): String {
        val d = PrayerTimes.forDay(LocalDate.parse(date), lat, lng, zone)
        return listOf(d.fajr, d.sunrise, d.dhuhr, d.asr, d.sunset, d.maghrib, d.isha).joinToString(" ")
    }

    @Test
    fun `London in autumn, on summer time`() {
        assertEquals("05:15 06:55 12:51 16:03 18:47 19:08 20:13", times("2026-09-27", 51.5074, -0.1278, london))
    }

    @Test
    fun `London in midwinter`() {
        assertEquals("06:13 08:04 11:59 13:38 15:53 16:19 17:31", times("2026-12-21", 51.5074, -0.1278, london))
    }

    /** The sun never reaches 16° below the horizon, so Fajr and ʿIshāʾ fall back to the middle of the night. */
    @Test
    fun `London at midsummer, when the dawn angle is never reached`() {
        assertEquals("01:02 04:43 13:02 17:25 21:22 21:50 00:05", times("2026-06-21", 51.5074, -0.1278, london))
    }

    @Test
    fun `Birmingham on the day the clocks go forward`() {
        assertEquals("05:08 06:52 13:13 16:40 19:34 19:55 21:04", times("2027-03-28", 52.4862, -1.8904, london))
    }

    @Test
    fun `Karachi`() {
        assertEquals("05:15 06:22 12:23 15:48 18:23 18:37 19:21", times("2026-09-27", 24.8607, 67.0011, ZoneId.of("Asia/Karachi")))
    }

    @Test
    fun `Qom, half an hour off the hour`() {
        assertEquals("04:55 06:09 12:14 15:41 18:19 18:34 19:23", times("2026-03-21", 34.6416, 50.8746, ZoneId.of("Asia/Tehran")))
    }
}
