package com.pinfold.app.ui.home

import com.pinfold.app.data.model.FetchStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MissingImagePolicyTest {
    private val exists: (String) -> Boolean = { it == "/images/ok.webp" }

    @Test
    fun blankPathIsAlwaysMissing() {
        assertTrue(MissingImagePolicy.isMissing(null, FetchStatus.OK, exists))
        assertTrue(MissingImagePolicy.isMissing("", FetchStatus.PENDING, exists))
        assertTrue(MissingImagePolicy.isMissing("   ", FetchStatus.PARTIAL, exists))
    }

    @Test
    fun missingFileIsMissingEvenWhenStatusIsOk() {
        assertTrue(MissingImagePolicy.isMissing("/images/deleted.webp", FetchStatus.OK, exists))
    }

    @Test
    fun nonOkStatusIsMissingEvenWhenFileExists() {
        assertTrue(MissingImagePolicy.isMissing("/images/ok.webp", FetchStatus.PARTIAL, exists))
        assertTrue(MissingImagePolicy.isMissing("/images/ok.webp", FetchStatus.PENDING, exists))
    }

    @Test
    fun okStatusWithExistingFileIsNotMissing() {
        assertFalse(MissingImagePolicy.isMissing("/images/ok.webp", FetchStatus.OK, exists))
    }
}
