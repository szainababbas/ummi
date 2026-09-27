package com.szainabbas.ummi.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalTime

class ClockTest {
    @Test
    fun `reads times the way the handoff writes them`() {
        assertEquals("7:30 am", Clock.label(LocalTime.of(7, 30)))
        assertEquals("12:05 pm", Clock.label(LocalTime.of(12, 5)))
        assertEquals("12:00 am", Clock.label(LocalTime.of(0, 0)))
        assertEquals("9:00 pm", Clock.label(LocalTime.of(21, 0)))
    }

    @Test
    fun `stores times as HH-mm and reads them back`() {
        assertEquals("07:05", Clock.store(LocalTime.of(7, 5)))
        assertEquals(LocalTime.of(7, 5), Clock.parse("07:05"))
        assertNull(Clock.parse("soon"))
        assertNull(Clock.parse(null))
    }
}
