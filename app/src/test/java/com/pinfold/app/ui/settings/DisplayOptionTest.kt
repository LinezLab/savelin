package com.pinfold.app.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayOptionTest {
    @Test
    fun replacesUnderscoresWithSpacesAndUppercasesFirstCharacter() {
        assertEquals("DATA SAVER", displayOption("DATA_SAVER"))
        assertEquals("GLASS STRIP", displayOption("GLASS_STRIP"))
        assertEquals("IMAGE ONLY", displayOption("IMAGE_ONLY"))
        assertEquals("NATIVE", displayOption("NATIVE"))
        assertEquals("Already nice", displayOption("already_nice"))
    }

    @Test
    fun preservesOtherCharactersAndEmptyInput() {
        assertEquals("", displayOption(""))
        assertEquals("7", displayOption("7"))
        assertEquals("QUALITY 2", displayOption("QUALITY_2"))
        assertEquals("AUTO", displayOption("AUTO"))
    }
}
