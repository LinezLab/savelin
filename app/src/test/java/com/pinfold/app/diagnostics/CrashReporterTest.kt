package com.pinfold.app.diagnostics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReporterTest {
    @Test
    fun reportCapturesFullMessageCauseChainFramesAndContext() {
        val inner = IllegalStateException("inner secret token=abcdef0123456789abcdef0123456789")
        inner.stackTrace = arrayOf(
            StackTraceElement("rv", "k", "Inner.kt", 41),
            StackTraceElement("qv0", "<init>", "Unknown Source", 0),
        )
        val error = IllegalArgumentException("Key HomeGridKey.Bookmark(id=7) was used multiple times", inner)
        error.stackTrace = arrayOf(
            StackTraceElement("ve2", "b", "SaveableStateProvider.kt", 12),
            StackTraceElement("zc2", "i", "LazyLayoutItemContentFactory.kt", 88),
        )
        val events = listOf(LogEvent(1_700_000_000_000L, LogLevel.INFO, "UI/Home", "Open details"))

        val report = CrashReporter.buildReport(
            error,
            "main",
            metadata = "App version: 1.6.1 (code 161)\nABI: arm64-v8a\n",
            timestampMillis = 1_700_000_000_000L,
            events = events,
        )

        assertTrue(report.startsWith("========== SAVELIN FULL DIAGNOSTICS =========="))
        assertTrue(report.trimEnd().endsWith("========== END SAVELIN DIAGNOSTICS =========="))
        assertTrue(report.contains("Timestamp: 2023-11-14 22:13:20.000 +0000"))
        assertTrue(report.contains("Thread: main"))
        assertTrue(report.contains("App version: 1.6.1 (code 161)"))
        assertTrue(report.contains("ABI: arm64-v8a"))
        assertTrue(report.contains("Type: java.lang.IllegalArgumentException"))
        assertTrue(report.contains("Message: Key HomeGridKey.Bookmark(id=7) was used multiple times"))
        assertTrue(report.contains("at ve2.b(SaveableStateProvider.kt:12)"))
        assertTrue(report.contains("at zc2.i(LazyLayoutItemContentFactory.kt:88)"))
        assertTrue(report.contains("at rv.k(Inner.kt:41)"))
        assertTrue(report.contains("Caused by:"))
        assertTrue(report.contains("[0] java.lang.IllegalArgumentException"))
        assertTrue(report.contains("[1] java.lang.IllegalStateException"))
        assertTrue(report.contains("UI/Home: Open details"))
        assertFalse(report.contains("abcdef0123456789abcdef0123456789"))
    }

    @Test
    fun reportRedactsUrlsEmailsAndPassphrasesInMessages() {
        val error = IllegalArgumentException(
            "visit https://example.com/private/x?token=abc mail dev@example.com passphrase=hunter2"
        )
        val report = CrashReporter.buildReport(error, "main")

        assertTrue(report.contains("https://example.com"))
        assertFalse(report.contains("/private/"))
        assertFalse(report.contains("dev@example.com"))
        assertFalse(report.contains("hunter2"))
        assertFalse(report.contains("token=abc"))
        assertTrue(report.contains("<redacted>"))
        assertTrue(report.contains("<email>"))
    }

    @Test
    fun reportKeepsEveryFrameNotJustTheFirstEight() {
        val frames = (0 until 24).map { StackTraceElement("frame$it", "call", "File$it.kt", it + 1) }.toTypedArray()
        val error = IllegalStateException("boom")
        error.stackTrace = frames

        val report = CrashReporter.buildReport(error, "worker")

        assertTrue(report.contains("Thread: worker"))
        for (i in 0 until 24) {
            assertTrue("missing frame $i", report.contains("at frame$i.call(File$i.kt:${i + 1})"))
        }
    }
}
