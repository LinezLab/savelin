package com.pinfold.app.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RedactionTest {
    @Test
    fun urlKeepsOnlyHost() {
        val out = redact("fetch https://example.com/private/path?token=abc from work")
        assertTrue(out.contains("https://example.com"))
        assertFalse(out.contains("private"))
        assertFalse(out.contains("token=abc"))
    }

    @Test
    fun emailIsRemoved() {
        assertFalse(redact("contact someone@example.com now").contains("someone@example.com"))
    }

    @Test
    fun credentialsAreRemoved() {
        val out = redact("token=abc123 password: hunter2 passphrase=correct horse")
        assertTrue(out.contains("token=<redacted>"))
        assertFalse(out.contains("abc123"))
        assertFalse(out.contains("hunter2"))
    }

    @Test
    fun longTokensAreRemoved() {
        assertFalse(redact("key ABCDEF0123456789abcdef0123456789").contains("ABCDEF0123456789"))
    }

    @Test
    fun ringBufferIsCappedAndRedacted() {
        DiagnosticsLog.clear()
        repeat(250) { DiagnosticsLog.log(LogLevel.INFO, "test", "visit https://site.example/secret/$it") }
        assertEquals(200, DiagnosticsLog.size)
        val text = DiagnosticsLog.format()
        assertTrue(text.contains("https://site.example"))
        assertFalse(text.contains("/secret/"))
        DiagnosticsLog.clear()
    }
}
