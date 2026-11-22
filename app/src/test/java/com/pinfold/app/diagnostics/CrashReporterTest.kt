package com.pinfold.app.diagnostics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReporterTest {
    @Test
    fun reportKeepsClassesAndObfuscatedFramesButNotMessages() {
        val frames = arrayOf(
            StackTraceElement("rv", "k", "Unknown Source", 0),
            StackTraceElement("qv0", "<init>", "Unknown Source", 0),
        )
        val cause = IllegalStateException("secret inner message")
        cause.stackTrace = frames
        val error = IllegalStateException("secret message")
        error.stackTrace = frames
        error.initCause(cause)

        val report = CrashReporter.buildReport(error, "main")

        assertTrue(report.startsWith("Uncaught java.lang.IllegalStateException on main"))
        assertTrue(report.contains("at rv.k"))
        assertTrue(report.contains("at qv0.<init>"))
        assertFalse(report.contains("secret"))
    }
}
