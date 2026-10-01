package com.szainabbas.ummi.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class DestinationsTest {
    @Test
    fun `the bottom bar leaves Visits off but keeps its route`() {
        assertEquals(
            listOf(UmmiDestination.Today, UmmiDestination.Journey, UmmiDestination.Duas, UmmiDestination.More),
            UmmiDestination.bottomBar,
        )
        assertEquals("visits", UmmiDestination.Visits.route)
    }
}
