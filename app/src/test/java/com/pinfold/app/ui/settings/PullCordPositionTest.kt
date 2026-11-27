package com.pinfold.app.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class PullCordPositionTest {
    @Test
    fun mapsEachNamedPositionToItsAnchor() {
        assertEquals(0.18f, pullCordAcross("LEFT"), 0.0001f)
        assertEquals(0.5f, pullCordAcross("CENTER"), 0.0001f)
        assertEquals(0.62f, pullCordAcross("RIGHT"), 0.0001f)
    }

    @Test
    fun unknownPositionFallsBackToRightAnchor() {
        assertEquals(0.62f, pullCordAcross(""), 0.0001f)
        assertEquals(0.62f, pullCordAcross("SOMETHING"), 0.0001f)
    }
}
