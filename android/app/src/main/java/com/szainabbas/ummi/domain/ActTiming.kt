package com.szainabbas.ummi.domain

import com.szainabbas.ummi.data.model.ActEntry
import java.time.LocalTime

/** The part of the day Today groups an act under. */
enum class DayPart(val label: String) {
    MORNING("Morning"),
    PRAYERS("With each prayer"),
    EVENING("Maghrib & ʿIshāʾ"),
    ANYTIME("Any time today"),
}

/**
 * When in the day an act belongs, and so when its reminder goes off.
 * Prayer-linked anchors follow the adhān; the rest sit at a fixed time.
 */
enum class Anchor(val part: DayPart, val prayerLinked: Boolean) {
    FAJR(DayPart.MORNING, true),
    MORNING(DayPart.MORNING, false),
    PRAYERS(DayPart.PRAYERS, true),
    /** Adhān and iqāmah, said before praying: rings at the adhān, not after it. */
    BEFORE_PRAYERS(DayPart.PRAYERS, true),
    DHUHR(DayPart.PRAYERS, true),
    BEFORE_SUNSET(DayPart.EVENING, false),
    MAGHRIB(DayPart.EVENING, true),
    NIGHT(DayPart.EVENING, false),
    WATER(DayPart.ANYTIME, false),
    ANYTIME(DayPart.ANYTIME, false),
}

/** One time an act's reminder goes off, with the heading its notification carries. */
data class AnchorTime(val time: LocalTime, val heading: String)

object ActTiming {
    /** Minutes after the adhān that a prayer-linked reminder comes, so it lands after she has prayed. */
    const val AFTER_ADHAN_MINUTES = 5L
    val MORNING_AT: LocalTime = LocalTime.of(7, 30)
    val ANYTIME_AT: LocalTime = LocalTime.of(16, 0)
    val NIGHT_EARLIEST: LocalTime = LocalTime.of(21, 30)
    val WATER_HOURS = listOf(9, 11, 13, 15, 17, 19)

    /**
     * Reads the act's own wording: the guide says "after Fajr", "after every
     * prayer", "on an empty stomach". The acts carry no separate time field,
     * so a reworded act can move group; the tests pin every rule to a real act.
     * Order matters: "after food rather than on an empty stomach" (the melon)
     * is not a morning act, and the Ẓuhr and Maghrib acts name one prayer only.
     */
    fun anchor(act: ActEntry): Anchor {
        if (act.id == "salah") return Anchor.PRAYERS
        if (act.id == "water") return Anchor.WATER
        val title = act.t.lowercase()
        val all = (act.t + " " + act.s).lowercase()
        return when {
            "after fajr" in all -> Anchor.FAJR
            "ẓuhr" in all -> Anchor.DHUHR
            "maghrib" in all -> Anchor.MAGHRIB
            "before sunset" in all -> Anchor.BEFORE_SUNSET
            "ṣalātul layl" in title -> Anchor.NIGHT
            BEFORE_PRAYER_WORDS.any { it in all } -> Anchor.BEFORE_PRAYERS
            PRAYER_WORDS.any { it in all } -> Anchor.PRAYERS
            "after your food" in all || "after food" in all -> Anchor.ANYTIME
            MORNING_WORDS.any { it in all } -> Anchor.MORNING
            else -> Anchor.ANYTIME
        }
    }

    private val BEFORE_PRAYER_WORDS = listOf("before every prayer", "before each prayer", "ṣalāh time")
    private val PRAYER_WORDS = listOf("every prayer", "each prayer", "daily prayers", "your prayers")
    private val MORNING_WORDS = listOf("empty stomach", "morning", "breakfast")

    /** Today's list in the handoff's order: morning, after the prayers, evening, any time. Stable within a group. */
    fun timeline(tasks: List<ActEntry>): List<ActEntry> = tasks.sortedBy { anchor(it).part.ordinal }

    /** When the act's reminder goes off on a day with these prayer times. Water has several; PRAYERS has three. */
    fun times(anchor: Anchor, pt: DayPrayerTimes): List<AnchorTime> {
        fun after(t: LocalTime, name: String) = AnchorTime(t.plusMinutes(AFTER_ADHAN_MINUTES), "After $name")
        return when (anchor) {
            Anchor.FAJR -> listOf(after(pt.fajr, "Fajr"))
            Anchor.MORNING -> listOf(AnchorTime(MORNING_AT, "This morning"))
            // Shia practice joins Ẓuhr with ʿAṣr and Maghrib with ʿIshāʾ, so three times a day.
            Anchor.PRAYERS -> listOf(after(pt.fajr, "Fajr"), after(pt.dhuhr, "Ẓuhr"), after(pt.maghrib, "Maghrib"))
            Anchor.BEFORE_PRAYERS -> listOf(AnchorTime(pt.fajr, "Fajr time"), AnchorTime(pt.dhuhr, "Ẓuhr time"), AnchorTime(pt.maghrib, "Maghrib time"))
            Anchor.DHUHR -> listOf(after(pt.dhuhr, "Ẓuhr"))
            Anchor.BEFORE_SUNSET -> listOf(AnchorTime(pt.sunset.minusMinutes(30), "Before sunset"))
            Anchor.MAGHRIB -> listOf(after(pt.maghrib, "Maghrib"))
            Anchor.NIGHT -> listOf(AnchorTime(maxOf(NIGHT_EARLIEST, pt.isha.plusMinutes(15)), "Tonight"))
            Anchor.WATER -> WATER_HOURS.map { AnchorTime(LocalTime.of(it, 0), "Water") }
            Anchor.ANYTIME -> listOf(AnchorTime(ANYTIME_AT, "For today"))
        }
    }

    /** The small line under a task on Today and in Reminders: "7:30 am", "After Fajr · 5:20 am", "After each prayer". */
    fun whenLabel(anchor: Anchor, pt: DayPrayerTimes): String {
        val times = times(anchor, pt)
        return when (anchor) {
            Anchor.PRAYERS -> "After each prayer"
            Anchor.BEFORE_PRAYERS -> "Before each prayer"
            Anchor.WATER -> "Every 2 hours, 9 am to 7 pm"
            Anchor.MORNING, Anchor.ANYTIME, Anchor.NIGHT -> clock(times.first().time)
            else -> times.first().heading + " · " + clock(times.first().time)
        }
    }

    /** The Reminders screen's narrow time column: "7:30 am", "5:20 am", "Each prayer", "Every 2 hours". */
    fun shortWhen(anchor: Anchor, pt: DayPrayerTimes): String = when (anchor) {
        Anchor.PRAYERS, Anchor.BEFORE_PRAYERS -> "Each prayer"
        Anchor.WATER -> "Every 2 hours"
        else -> clock(times(anchor, pt).first().time)
    }

    /** "7:30 am", "12:05 pm". */
    fun clock(t: LocalTime): String {
        val h = t.hour % 12
        return "${if (h == 0) 12 else h}:${t.minute.toString().padStart(2, '0')} ${if (t.hour < 12) "am" else "pm"}"
    }
}
