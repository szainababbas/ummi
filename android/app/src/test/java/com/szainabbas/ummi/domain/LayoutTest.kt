package com.szainabbas.ummi.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LayoutTest {
    @Test
    fun `puts āyāt in Qurʾān order, not text order`() {
        val refs = listOf("14:40", "2:255", "25:74", "3:36", "37:100", "3:38", "20:26", "20:25", "94:5")
        assertEquals(
            listOf("2:255", "3:36", "3:38", "14:40", "20:25", "20:26", "25:74", "37:100", "94:5"),
            Layout.ayahOrder(refs),
        )
    }

    @Test
    fun `pads the last row so every chip is the same width`() {
        assertEquals(
            listOf(listOf("a", "b", "c"), listOf("d", null, null)),
            Layout.gridRows(listOf("a", "b", "c", "d"), 3),
        )
        assertEquals(listOf(listOf(1, 2)), Layout.gridRows(listOf(1, 2), 2))
    }

    /** Where item [index]'s centre lands on screen for a given scroll position. */
    private fun centreOnScreen(index: Int, first: Int, offset: Int, item: Int, gap: Int, pad: Int): Int =
        pad - offset + (index - first) * (item + gap) + item / 2

    @Test
    fun `centres the current week in the strip`() {
        val (item, gap, pad, viewport) = listOf(105, 16, 42, 1080)
        for (index in listOf(13, 20, 37)) {
            val (first, offset) = Layout.centredScroll(index, item, gap, pad, viewport)
            assertEquals("week index $index", viewport / 2, centreOnScreen(index, first, offset, item, gap, pad))
        }
    }

    @Test
    fun `leaves the first weeks at the start rather than scrolling past it`() {
        assertEquals(0 to 0, Layout.centredScroll(0, 105, 16, 42, 1080))
        assertEquals(0 to 0, Layout.centredScroll(2, 105, 16, 42, 1080))
    }
}
