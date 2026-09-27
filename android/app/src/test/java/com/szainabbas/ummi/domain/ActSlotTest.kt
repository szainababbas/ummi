package com.szainabbas.ummi.domain

import com.szainabbas.ummi.TestFiles
import com.szainabbas.ummi.data.model.ActEntry
import com.szainabbas.ummi.data.model.UmmiData
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class ActSlotTest {
    private val data = UmmiData.parse(TestFiles.ummiData)
    private val acts: Map<String, ActEntry> =
        (data.months.values.flatMap { it.acts } + data.baseTasks).associateBy { it.id }

    private fun slot(id: String) = ActSlot.of(acts.getValue(id))

    @Test
    fun `puts dawn, breakfast and empty-stomach acts in the morning`() {
        assertEquals(ActSlot.MORNING, slot("m7b")) // Sūrah al-Anʿām after Fajr
        assertEquals(ActSlot.MORNING, slot("m6d")) // figs and olives for breakfast
        assertEquals(ActSlot.MORNING, slot("m1c")) // dates on an empty stomach
        assertEquals(ActSlot.MORNING, slot("m4d")) // Ṣalātul Layl, though its note says "after ʿIshāʾ"
    }

    @Test
    fun `puts Maghrib, sunset and night acts in the evening`() {
        assertEquals(ActSlot.EVENING, slot("m6c")) // in one rakʿah of Maghrib and ʿIshāʾ
        assertEquals(ActSlot.EVENING, slot("m1f")) // before sunset
        assertEquals(ActSlot.EVENING, slot("m9e")) // on Friday night
    }

    @Test
    fun `puts acts tied to every prayer after the prayers, reading through the accents`() {
        assertEquals(ActSlot.PRAYERS, slot("m4c")) // after each prayer
        assertEquals(ActSlot.PRAYERS, slot("m9c")) // in the Ẓuhr and ʿAṣr prayers
        assertEquals(ActSlot.PRAYERS, slot("m5b")) // at ṣalāh time
        assertEquals(ActSlot.PRAYERS, slot("m4f")) // in one rakʿah of every prayer
        assertEquals(ActSlot.PRAYERS, slot("salah"))
    }

    @Test
    fun `leaves everything else to any time, including "the prayer for comfort"`() {
        assertEquals(ActSlot.ANYTIME, slot("m4a"))
        assertEquals(ActSlot.ANYTIME, slot("m9h"))
        assertEquals(ActSlot.ANYTIME, slot("m7h")) // melon "rather than on an empty stomach"
        assertEquals(ActSlot.ANYTIME, slot("water"))
    }

    @Test
    fun `shows each group beside its prayer time`() {
        val day = PrayerDay(
            LocalTime.of(5, 15), LocalTime.of(6, 55), LocalTime.of(12, 51), LocalTime.of(16, 3),
            LocalTime.of(18, 47), LocalTime.of(19, 8), LocalTime.of(20, 13),
        )
        assertEquals(LocalTime.of(5, 15), ActSlot.MORNING.clock(day))
        assertEquals(LocalTime.of(12, 51), ActSlot.PRAYERS.clock(day))
        assertEquals(LocalTime.of(19, 8), ActSlot.EVENING.clock(day))
        assertEquals(null, ActSlot.ANYTIME.clock(day))
    }
}
