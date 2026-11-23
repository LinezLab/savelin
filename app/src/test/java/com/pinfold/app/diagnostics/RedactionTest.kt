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
    fun urlUserInfoAndPathAreDropped() {
        val out = redact("https://user:pass@example.com:8443/secret/path")
        assertTrue(out.contains("https://example.com:8443"))
        assertFalse(out.contains("user:pass"))
        assertFalse(out.contains("secret"))
    }

    @Test
    fun emailIsRemoved() {
        assertFalse(redact("contact someone@example.com now").contains("someone@example.com"))
        assertTrue(redact("contact someone@example.com now").contains("<email>"))
    }

    @Test
    fun credentialsAreRemoved() {
        val out = redact("token=abc123 password: hunter2 passphrase=correct api_key=xyz")
        assertTrue(out.contains("token=<redacted>"))
        assertTrue(out.contains("passphrase=<redacted>"))
        assertFalse(out.contains("abc123"))
        assertFalse(out.contains("hunter2"))
        assertFalse(out.contains("correct"))
        assertFalse(out.contains("xyz"))
    }

    @Test
    fun bearerAndLongTokensAreRemoved() {
        assertFalse(redact("Authorization: Bearer abc.def.ghi").contains("abc.def.ghi"))
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

    @Test
    fun snapshotLastReturnsNewestEventsOnly() {
        DiagnosticsLog.clear()
        repeat(5) { DiagnosticsLog.uiAction("Home", "action$it") }
        val last = DiagnosticsLog.snapshotLast(2)
        assertEquals(2, last.size)
        assertEquals("action3", last[0].message)
        assertEquals("action4", last[1].message)
        assertEquals("UI/Home", last[1].tag)
        DiagnosticsLog.clear()
    }
}
