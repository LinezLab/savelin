package com.pinfold.app.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IssuesLogTest {
    @Test
    fun issueRecordsWhatAndWhyWithHostOnly() {
        DiagnosticsLog.clear()
        DiagnosticsLog.issue("Preview", "fetch", "SocketTimeout host=https://site.example/private/path?token=abc")
        val issue = DiagnosticsLog.issueSnapshot().single()

        assertEquals("Preview", issue.scope)
        assertEquals("fetch", issue.what)
        assertTrue(issue.why.contains("host=https://site.example"))
        assertFalse(issue.why.contains("private"))
        assertFalse(issue.why.contains("token=abc"))
        assertTrue(DiagnosticsLog.formatIssues().contains("Preview: fetch"))
        DiagnosticsLog.clear()
    }

    @Test
    fun issueRingBufferIsCapped() {
        DiagnosticsLog.clear()
        repeat(120) { DiagnosticsLog.issue("Drive", "sync", "failure $it mail dev@example.com") }
        assertEquals(80, DiagnosticsLog.issueCount)
        assertFalse(DiagnosticsLog.formatIssues().contains("dev@example.com"))
        DiagnosticsLog.clear()
    }
}
