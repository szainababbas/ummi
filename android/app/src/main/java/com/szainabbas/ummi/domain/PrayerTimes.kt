package com.szainabbas.ummi.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/** One day's prayer times, local to the zone they were worked out in. */
data class PrayerDay(
    val fajr: LocalTime,
    val sunrise: LocalTime,
    val dhuhr: LocalTime,
    val asr: LocalTime,
    val sunset: LocalTime,
    val maghrib: LocalTime,
    val isha: LocalTime,
)

/**
 * Prayer times by the Shia Ithna-ʿAshari (Jaʿfari) convention of the Leva
 * Institute, Qum: Fajr at 16° below the horizon, Maghrib once the sun is 4°
 * below it (the redness gone from the east), ʿIshāʾ at 14°, and ʿAṣr when a
 * shadow equals its object.
 *
 * The sun's position uses the standard low-precision formulas from the US
 * Naval Observatory, as most prayer-time software does; the tests hold the
 * results to within a minute of an established implementation.
 *
 * In a British summer the sun never gets 16° below the horizon, so Fajr and
 * ʿIshāʾ are undefined; as is common, they are then capped at half the night
 * from sunrise and sunset.
 */
object PrayerTimes {
    private const val FAJR_ANGLE = 16.0
    private const val MAGHRIB_ANGLE = 4.0
    private const val ISHA_ANGLE = 14.0
    private const val RISE_SET_ANGLE = 0.833

    fun forDay(date: LocalDate, lat: Double, lng: Double, zone: ZoneId): PrayerDay {
        val offsetHours = zone.rules.getOffset(date.atTime(12, 0)).totalSeconds / 3600.0
        val calc = Calc(julian(date) - lng / 360.0, lat)

        // One pass from rough guesses, as the reference implementation does.
        val fajr = calc.sunAngleTime(FAJR_ANGLE, 5 / 24.0, ccw = true)
        val sunrise = calc.sunAngleTime(RISE_SET_ANGLE, 6 / 24.0, ccw = true)
        val dhuhr = calc.midDay(12 / 24.0)
        val asr = calc.asrTime(1.0, 13 / 24.0)
        val sunset = calc.sunAngleTime(RISE_SET_ANGLE, 18 / 24.0, ccw = false)
        val maghrib = calc.sunAngleTime(MAGHRIB_ANGLE, 18 / 24.0, ccw = false)
        val isha = calc.sunAngleTime(ISHA_ANGLE, 18 / 24.0, ccw = false)

        val shift = offsetHours - lng / 15.0
        val rise = sunrise + shift
        val set = sunset + shift
        val night = fixHour(rise - set)
        return PrayerDay(
            fajr = clock(capToNight(fajr + shift, rise, night, before = true)),
            sunrise = clock(rise),
            dhuhr = clock(dhuhr + shift),
            asr = clock(asr + shift),
            sunset = clock(set),
            maghrib = clock(capToNight(maghrib + shift, set, night, before = false)),
            isha = clock(capToNight(isha + shift, set, night, before = false)),
        )
    }

    /**
     * The "middle of the night" rule for nights when the sun never gets far
     * enough below the horizon: half the night before sunrise, or after sunset.
     * (The reference also caps a real time lying beyond that point, which only
     * happens at latitudes this app's places don't reach.)
     */
    private fun capToNight(time: Double, base: Double, night: Double, before: Boolean): Double =
        if (time.isNaN()) base + if (before) -night / 2 else night / 2 else time

    /** Hours to the nearest minute, as a clock time. */
    private fun clock(hours: Double): LocalTime {
        val h = fixHour(hours + 0.5 / 60)
        val whole = floor(h).toInt()
        return LocalTime.of(whole, floor((h - whole) * 60).toInt())
    }

    private fun julian(date: LocalDate): Double {
        var y = date.year
        var m = date.monthValue
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + date.dayOfMonth + b - 1524.5
    }

    private class Calc(val jDate: Double, val lat: Double) {
        /** Declination and equation of time for a moment [t] (a fraction of the day). */
        fun sun(t: Double): Pair<Double, Double> {
            val d = jDate + t - 2451545.0
            val g = fixAngle(357.529 + 0.98560028 * d)
            val q = fixAngle(280.459 + 0.98564736 * d)
            val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
            val e = 23.439 - 0.00000036 * d
            val ra = datan2(dcos(e) * dsin(l), dcos(l)) / 15
            val declination = dasin(dsin(e) * dsin(l))
            val equation = q / 15 - fixHour(ra)
            return declination to equation
        }

        fun midDay(t: Double): Double = fixHour(12 - sun(t).second)

        fun sunAngleTime(angle: Double, t: Double, ccw: Boolean): Double {
            val decl = sun(t).first
            val noon = midDay(t)
            val span = dacos((-dsin(angle) - dsin(decl) * dsin(lat)) / (dcos(decl) * dcos(lat))) / 15
            return noon + if (ccw) -span else span
        }

        fun asrTime(factor: Double, t: Double): Double {
            val decl = sun(t).first
            val angle = -darccot(factor + dtan(abs(lat - decl)))
            return sunAngleTime(angle, t, ccw = false)
        }
    }

    private fun fixAngle(a: Double) = fix(a, 360.0)
    private fun fixHour(h: Double) = fix(h, 24.0)
    private fun fix(x: Double, mod: Double): Double {
        val r = x - mod * floor(x / mod)
        return if (r < 0) r + mod else r
    }

    private fun rad(d: Double) = d * Math.PI / 180
    private fun deg(r: Double) = r * 180 / Math.PI
    private fun dsin(d: Double) = sin(rad(d))
    private fun dcos(d: Double) = cos(rad(d))
    private fun dtan(d: Double) = tan(rad(d))
    private fun dasin(x: Double) = deg(asin(x))
    private fun dacos(x: Double) = deg(acos(x))
    private fun datan2(y: Double, x: Double) = deg(atan2(y, x))
    private fun darccot(x: Double) = deg(atan(1 / x))
}
