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

/** A town the prayer times can be worked out for. */
data class Place(val key: String, val name: String, val lat: Double, val lng: Double)

/** One day's adhān times, to the minute, in the phone's own clock. */
data class DayPrayerTimes(
    val fajr: LocalTime,
    val sunrise: LocalTime,
    val dhuhr: LocalTime,
    val asr: LocalTime,
    val sunset: LocalTime,
    val maghrib: LocalTime,
    val isha: LocalTime,
)

/**
 * Prayer times by the Jaʿfarī (Shia Ithna-ʿAshari, Qum) method: Fajr at
 * 16° below the horizon, Maghrib at 4° (after the redness in the east has
 * gone, not at sunset), ʿIshāʾ at 14°. A port of the praytimes.org code
 * (Hamid Zarrabi-Zadeh), which the tests check it against.
 *
 * In a British summer the sun never gets 16° below the horizon, so there is
 * no true Fajr. The "angle-based" rule then puts Fajr 16/60 of the night
 * before sunrise (and ʿIshāʾ 14/60 after sunset), which is what the
 * praytimes.org and most mosque timetables in the UK use.
 *
 * No location permission: she picks her nearest town from [places].
 */
object PrayerTimes {
    const val FAJR_ANGLE = 16.0
    const val MAGHRIB_ANGLE = 4.0
    const val ISHA_ANGLE = 14.0
    private const val RISE_SET_ANGLE = 0.833

    val places: List<Place> = listOf(
        Place("london", "London", 51.5074, -0.1278),
        Place("birmingham", "Birmingham", 52.4862, -1.8904),
        Place("manchester", "Manchester", 53.4808, -2.2426),
        Place("leeds", "Leeds", 53.8008, -1.5491),
        Place("bradford", "Bradford", 53.7960, -1.7594),
        Place("leicester", "Leicester", 52.6369, -1.1398),
        Place("luton", "Luton", 51.8787, -0.4200),
        Place("bristol", "Bristol", 51.4545, -2.5879),
        Place("cardiff", "Cardiff", 51.4816, -3.1791),
        Place("glasgow", "Glasgow", 55.8642, -4.2518),
        Place("edinburgh", "Edinburgh", 55.9533, -3.1883),
    )

    fun place(key: String?): Place = places.firstOrNull { it.key == key } ?: places.first()

    fun forDay(date: LocalDate, place: Place, zone: ZoneId): DayPrayerTimes {
        // The offset at midday, so a clock-change day uses the clock she'll be living by.
        val tz = zone.rules.getOffset(date.atTime(12, 0)).totalSeconds / 3600.0
        val jDate = julian(date.year, date.monthValue, date.dayOfMonth) - place.lng / (15 * 24.0)
        val lat = place.lat

        fun sunPosition(jd: Double): Pair<Double, Double> {
            val d = jd - 2451545.0
            val g = fixAngle(357.529 + 0.98560028 * d)
            val q = fixAngle(280.459 + 0.98564736 * d)
            val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
            val e = 23.439 - 0.00000036 * d
            val ra = datan2(dcos(e) * dsin(l), dcos(l)) / 15.0
            val eqt = q / 15.0 - fixHour(ra)
            val decl = dasin(dsin(e) * dsin(l))
            return decl to eqt
        }

        fun midDay(time: Double): Double = fixHour(12 - sunPosition(jDate + time).second)

        fun sunAngleTime(angle: Double, time: Double, ccw: Boolean = false): Double {
            val decl = sunPosition(jDate + time).first
            val noon = midDay(time)
            val t = dacos((-dsin(angle) - dsin(decl) * dsin(lat)) / (dcos(decl) * dcos(lat))) / 15.0
            return noon + if (ccw) -t else t
        }

        fun asrTime(factor: Double, time: Double): Double {
            val decl = sunPosition(jDate + time).first
            val angle = -dacot(factor + dtan(abs(lat - decl)))
            return sunAngleTime(angle, time)
        }

        // One pass from the usual first guesses, as praytimes.org does.
        val adjust = tz - place.lng / 15.0
        val sunrise = sunAngleTime(RISE_SET_ANGLE, 6 / 24.0, ccw = true) + adjust
        val sunset = sunAngleTime(RISE_SET_ANGLE, 18 / 24.0) + adjust
        val night = timeDiff(sunset, sunrise)
        val fajr = beforeBase(sunAngleTime(FAJR_ANGLE, 5 / 24.0, ccw = true) + adjust, sunrise, FAJR_ANGLE, night)
        val dhuhr = midDay(12 / 24.0) + adjust
        val asr = asrTime(1.0, 13 / 24.0) + adjust
        val maghrib = afterBase(sunAngleTime(MAGHRIB_ANGLE, 18 / 24.0) + adjust, sunset, MAGHRIB_ANGLE, night)
        val isha = afterBase(sunAngleTime(ISHA_ANGLE, 18 / 24.0) + adjust, sunset, ISHA_ANGLE, night)

        return DayPrayerTimes(
            fajr = toTime(fajr),
            sunrise = toTime(sunrise),
            dhuhr = toTime(dhuhr),
            asr = toTime(asr),
            sunset = toTime(sunset),
            maghrib = toTime(maghrib),
            isha = toTime(isha),
        )
    }

    /** The angle-based high-latitude rule, for times that come before [base] (Fajr before sunrise). */
    private fun beforeBase(time: Double, base: Double, angle: Double, night: Double): Double {
        val portion = angle / 60.0 * night
        return if (time.isNaN() || timeDiff(time, base) > portion) base - portion else time
    }

    /** The same rule for times after [base] (Maghrib and ʿIshāʾ after sunset). */
    private fun afterBase(time: Double, base: Double, angle: Double, night: Double): Double {
        val portion = angle / 60.0 * night
        return if (time.isNaN() || timeDiff(base, time) > portion) base + portion else time
    }

    /** Hours as a double to the nearest minute, as praytimes.org rounds them. */
    private fun toTime(hours: Double): LocalTime {
        val t = fixHour(hours + 0.5 / 60)
        val h = floor(t).toInt()
        val m = floor((t - h) * 60).toInt()
        return LocalTime.of(h % 24, m)
    }

    private fun julian(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) { y -= 1; m += 12 }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun timeDiff(t1: Double, t2: Double) = fixHour(t2 - t1)
    private fun fix(a: Double, mode: Double): Double {
        if (a.isNaN()) return a
        val r = a - mode * floor(a / mode)
        return if (r < 0) r + mode else r
    }
    private fun fixAngle(a: Double) = fix(a, 360.0)
    private fun fixHour(a: Double) = fix(a, 24.0)
    private fun dsin(d: Double) = sin(Math.toRadians(d))
    private fun dcos(d: Double) = cos(Math.toRadians(d))
    private fun dtan(d: Double) = tan(Math.toRadians(d))
    private fun dasin(x: Double) = Math.toDegrees(asin(x))
    private fun dacos(x: Double) = Math.toDegrees(acos(x))
    private fun dacot(x: Double) = Math.toDegrees(atan(1.0 / x))
    private fun datan2(y: Double, x: Double) = Math.toDegrees(atan2(y, x))
}
