package com.szainabbas.ummi.domain

import com.szainabbas.ummi.TestFiles
import com.szainabbas.ummi.data.model.UmmiData
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class ActTimingTest {
    private val data = UmmiData.parse(TestFiles.ummiData)
    private val acts = (data.months.values.flatMap { it.acts } + data.baseTasks).associateBy { it.id }

    private fun anchorOf(id: String) = ActTiming.anchor(acts.getValue(id))

    /** Every rule, each on an act from the guide that needs it. */
    @Test
    fun `reads when an act happens from its own wording`() {
        val expected = mapOf(
            "m7b" to Anchor.FAJR, // "over almonds after Fajr"
            "m8b" to Anchor.FAJR, // "ten times after Fajr"
            "m9c" to Anchor.DHUHR, // "In the Ẓuhr and ʿAṣr prayers", not every prayer
            "m6c" to Anchor.MAGHRIB, // "in one rakʿah of Maghrib and ʿIshāʾ"
            "m9e" to Anchor.MAGHRIB, // "Thursday evening after Maghrib"
            "m1f" to Anchor.BEFORE_SUNSET,
            "m4d" to Anchor.NIGHT, // Ṣalātul Layl, whose note mentions Fajr only as the end of its time
            "m1a" to Anchor.PRAYERS, // "before every prayer"
            "m4c" to Anchor.PRAYERS, // "after each prayer"
            "m4g" to Anchor.PRAYERS, // "After the daily prayers"
            "m7e" to Anchor.PRAYERS, // "after your prayers"
            "m5b" to Anchor.PRAYERS, // "at ṣalāh time"
            "m3b" to Anchor.PRAYERS, // only its note says "Before each prayer"
            "salah" to Anchor.PRAYERS,
            "m7h" to Anchor.ANYTIME, // melon "after food rather than on an empty stomach"
            "m1c" to Anchor.MORNING, // dates "on an empty stomach"
            "m1d" to Anchor.MORNING, // "in the morning"
            "m6d" to Anchor.MORNING, // "after breakfast"
            "water" to Anchor.WATER,
            "walk" to Anchor.ANYTIME,
            "m2c" to Anchor.ANYTIME, // the long ṣalawāt in month 2 names no time
            "m4j" to Anchor.ANYTIME,
        )
        for ((id, anchor) in expected) assertEquals(id, anchor, anchorOf(id))
    }

    @Test
    fun `puts Today in order, morning to any time, keeping the guide's order within each part`() {
        val ids = listOf("walk", "m6c", "m6a", "m6d", "salah", "m6b").map { acts.getValue(it) }
        assertEquals(listOf("m6d", "m6a", "salah", "m6c", "walk", "m6b"), ActTiming.timeline(ids).map { it.id })
    }

    private val pt = DayPrayerTimes(
        fajr = LocalTime.of(5, 15), sunrise = LocalTime.of(6, 55), dhuhr = LocalTime.of(12, 51),
        asr = LocalTime.of(16, 3), sunset = LocalTime.of(18, 47), maghrib = LocalTime.of(19, 8), isha = LocalTime.of(20, 13),
    )

    @Test
    fun `rings a few minutes after the adhān, three times a day for every-prayer acts`() {
        assertEquals(
            listOf(AnchorTime(LocalTime.of(5, 20), "After Fajr"), AnchorTime(LocalTime.of(12, 56), "After Ẓuhr"), AnchorTime(LocalTime.of(19, 13), "After Maghrib")),
            ActTiming.times(Anchor.PRAYERS, pt),
        )
        assertEquals(listOf(AnchorTime(LocalTime.of(12, 56), "After Ẓuhr")), ActTiming.times(Anchor.DHUHR, pt))
        assertEquals(listOf(AnchorTime(LocalTime.of(18, 17), "Before sunset")), ActTiming.times(Anchor.BEFORE_SUNSET, pt))
        assertEquals(LocalTime.of(7, 30), ActTiming.times(Anchor.MORNING, pt).single().time)
        assertEquals(LocalTime.of(16, 0), ActTiming.times(Anchor.ANYTIME, pt).single().time)
        assertEquals(listOf(9, 11, 13, 15, 17, 19), ActTiming.times(Anchor.WATER, pt).map { it.time.hour })
    }

    @Test
    fun `the night reminder waits until after ʿIshāʾ in summer`() {
        assertEquals(LocalTime.of(21, 30), ActTiming.times(Anchor.NIGHT, pt).single().time)
        val summer = pt.copy(isha = LocalTime.of(23, 5))
        assertEquals(LocalTime.of(23, 20), ActTiming.times(Anchor.NIGHT, summer).single().time)
    }

    @Test
    fun `labels each task with when its reminder comes`() {
        assertEquals("7:30 am", ActTiming.whenLabel(Anchor.MORNING, pt))
        assertEquals("After Fajr · 5:20 am", ActTiming.whenLabel(Anchor.FAJR, pt))
        assertEquals("After each prayer", ActTiming.whenLabel(Anchor.PRAYERS, pt))
        assertEquals("After Maghrib · 7:13 pm", ActTiming.whenLabel(Anchor.MAGHRIB, pt))
        assertEquals("4:00 pm", ActTiming.whenLabel(Anchor.ANYTIME, pt))
        assertEquals("Every 2 hours, 9 am to 7 pm", ActTiming.whenLabel(Anchor.WATER, pt))
    }

    @Test
    fun `writes clock times the way the handoff does`() {
        assertEquals("12:05 am", ActTiming.clock(LocalTime.of(0, 5)))
        assertEquals("12:00 pm", ActTiming.clock(LocalTime.NOON))
        assertEquals("9:00 pm", ActTiming.clock(LocalTime.of(21, 0)))
    }
}
