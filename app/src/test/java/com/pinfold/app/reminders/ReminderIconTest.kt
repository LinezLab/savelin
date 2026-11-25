package com.pinfold.app.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderIconTest {
    @Test
    fun smallImagesUseFullResolution() {
        assertEquals(1, sampleSizeFor(1, 256))
        assertEquals(1, sampleSizeFor(128, 256))
        assertEquals(1, sampleSizeFor(256, 256))
    }

    @Test
    fun largerImagesAreDownsampledByPowersOfTwo() {
        assertEquals(2, sampleSizeFor(512, 256))
        assertEquals(4, sampleSizeFor(1080, 256))
        assertEquals(8, sampleSizeFor(2048, 256))
    }

    @Test
    fun customTargetAndInvalidDimensionsAreClamped() {
        assertEquals(8, sampleSizeFor(1024, 128))
        assertEquals(1, sampleSizeFor(0, 256))
        assertEquals(1, sampleSizeFor(-10, 256))
        assertEquals(1, sampleSizeFor(1024, 0))
        assertEquals(1, sampleSizeFor(1024, -1))
    }

    @Test
    fun urlImageIsShownOnlyWhenAllowedAndPathIsPresent() {
        assertTrue(shouldShowUrlImage(true, "/data/user/0/com.pinfold.app/files/image.webp"))
        assertTrue(shouldShowUrlImage(true, "relative/image.png"))
    }

    @Test
    fun hiddenDetailsSuppressUrlImage() {
        assertFalse(shouldShowUrlImage(false, "/data/user/0/com.pinfold.app/files/image.webp"))
    }

    @Test
    fun blankOrMissingPathSuppressUrlImage() {
        assertFalse(shouldShowUrlImage(true, null))
        assertFalse(shouldShowUrlImage(true, ""))
        assertFalse(shouldShowUrlImage(true, "   "))
    }
}
