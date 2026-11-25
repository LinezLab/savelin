package com.pinfold.app.diagnostics

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class LogLevel { INFO, WARN, ERROR }

data class LogEvent(val time: Long, val level: LogLevel, val tag: String, val message: String)

/** A structured, redacted record of WHAT failed (scope + action) and WHY (redacted reason). */
data class IssueEvent(val time: Long, val scope: String, val what: String, val why: String)

/** A small ring buffer of redacted events. Nothing here keeps user content, URLs or credentials. */
object DiagnosticsLog {
    private const val MAX_EVENTS = 200
    private const val MAX_ISSUES = 80
    private val events = ArrayDeque<LogEvent>()
    private val issues = ArrayDeque<IssueEvent>()

    @Synchronized
    fun log(level: LogLevel, tag: String, message: String) {
        events.addLast(LogEvent(System.currentTimeMillis(), level, redact(tag).take(48), redact(message).take(400)))
        while (events.size > MAX_EVENTS) events.removeFirst()
    }

    /** Structured UI breadcrumb: screen + action only, never a title, URL or token. */
    fun uiAction(screen: String, action: String) = log(LogLevel.INFO, "UI/$screen", action)

    /**
     * Records a failure with its scope, what was attempted and why it failed. All three fields are
     * redacted (URLs become host-only) and kept short, so the next bug report says what is broken
     * without carrying titles, emails, tokens or passphrases.
     */
    @Synchronized
    fun issue(scope: String, what: String, why: String) {
        val safeScope = redact(scope).take(32)
        val safeWhat = redact(what).take(64)
        val safeWhy = redact(why).take(240)
        issues.addLast(IssueEvent(System.currentTimeMillis(), safeScope, safeWhat, safeWhy))
        while (issues.size > MAX_ISSUES) issues.removeFirst()
        log(LogLevel.ERROR, "Issue/$safeScope", "$safeWhat: $safeWhy")
    }

    @Synchronized
    fun snapshot(): List<LogEvent> = events.toList()

    @Synchronized
    fun issueSnapshot(): List<IssueEvent> = issues.toList()

    @Synchronized
    fun snapshotLast(limit: Int): List<LogEvent> =
        if (limit <= 0 || events.isEmpty()) emptyList() else events.takeLast(minOf(limit, events.size))

    @Synchronized
    fun clear() {
        events.clear()
        issues.clear()
    }

    val size: Int @Synchronized get() = events.size
    val issueCount: Int @Synchronized get() = issues.size

    fun format(limit: Int = Int.MAX_VALUE): String {
        val lines = if (limit >= events.size) snapshot() else snapshotLast(limit)
        if (lines.isEmpty()) return "Sonil diagnostics: no events recorded."
        val stamp = stamp()
        return buildString {
            appendLine("Sonil diagnostics (redacted, times UTC)")
            for (event in lines) {
                append('[').append(stamp.format(Date(event.time))).append("] ")
                    .append(event.level.name).append('/').append(event.tag).append(": ").appendLine(event.message)
            }
        }
    }

    fun formatIssues(): String {
        val lines = issueSnapshot()
        if (lines.isEmpty()) return "No issues recorded."
        val stamp = stamp()
        return buildString {
            appendLine("Sonil issues (redacted, times UTC)")
            for (issue in lines) {
                append('[').append(stamp.format(Date(issue.time))).append("] ")
                    .append(issue.scope).append(": ").append(issue.what)
                    .append(" — ").appendLine(issue.why)
            }
        }
    }

    private fun stamp(): SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
}
