package com.pinfold.app.diagnostics

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val inHandler: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }

/**
 * Writes a complete, redacted crash report to app-private storage and offers it once, on the next
 * launch. The handler never throws and never re-enters itself, even if the platform handler is
 * missing or any of the reporting steps fail.
 */
object CrashReporter {
    private const val TAG = "SavelinDiagnostics"
    private const val CRASH_FILE = "crash-last.txt"
    internal const val FULL_REPORT_PATH = "crash/last-crash.txt"
    internal const val RECENT_EVENT_COUNT = 50
    private const val MAX_CAUSES = 16
    private const val LOGCAT_CHUNK = 3200

    var pendingLog: String? = null
        private set

    fun install(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        if (previous is Handler) return
        Thread.setDefaultUncaughtExceptionHandler(Handler(context.applicationContext, previous))
        consumePending(context)
    }

    private class Handler(
        private val context: Context,
        private val previous: Thread.UncaughtExceptionHandler?,
    ) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(thread: Thread, throwable: Throwable) {
            if (inHandler.get()) return
            inHandler.set(true)
            try {
                val events = runCatching { DiagnosticsLog.snapshotLast(RECENT_EVENT_COUNT) }.getOrElse { emptyList() }
                val metadata = runCatching { deviceInfo(context) }.getOrElse { "App/device metadata unavailable" }
                val report = runCatching { buildReport(throwable, thread.name, metadata, events = events) }
                    .getOrElse { "Uncaught ${throwable.javaClass.name} on ${thread.name} (full report unavailable: ${it.javaClass.name})" }
                runCatching { DiagnosticsLog.issue("UI", "crash", throwable.javaClass.name + " on " + thread.name) }
                runCatching { DiagnosticsLog.log(LogLevel.ERROR, "Crash", "Full crash report written (${report.length} chars)") }
                logcat(report)
                runCatching { writeReports(context, report) }
            } finally {
                inHandler.set(false)
            }
            // With no platform handler, rethrowing here would feed the same throwable back into this
            // handler and recurse forever. Let the thread die quietly instead.
            runCatching { previous?.uncaughtException(thread, throwable) }
        }
    }

    /** Reads the pending one-shot file, deleting it after a successful read. */
    fun consumePending(context: Context) {
        val text = runCatching {
            val file = File(context.filesDir, CRASH_FILE)
            if (!file.exists()) null else file.readText().also { runCatching { file.delete() } }
        }.getOrNull()
        if (!text.isNullOrBlank()) {
            pendingLog = text
            logcat("Full diagnostics from the previous launch:\n$text")
        }
    }

    fun clearPending() {
        pendingLog = null
    }

    /** The persistent, human-readable report, readable from Settings at any time. */
    fun lastReport(context: Context): String? = runCatching {
        val file = File(context.filesDir, FULL_REPORT_PATH)
        if (file.exists()) file.readText().takeIf { it.isNotBlank() } else null
    }.getOrNull()

    /** The Settings "Copy full diagnostics" payload: last crash report + the current event log. */
    fun fullDiagnostics(context: Context): String = buildString {
        val stored = lastReport(context)
        if (stored.isNullOrBlank()) appendLine("No crash report stored yet.")
        else {
            appendLine(stored.trimEnd())
            appendLine()
        }
        appendLine("-- CURRENT REDACTED EVENT LOG (last $RECENT_EVENT_COUNT) --")
        append(DiagnosticsLog.format(RECENT_EVENT_COUNT))
        appendLine()
        appendLine("-- RECORDED ISSUES (what failed and why) --")
        append(DiagnosticsLog.formatIssues())
    }

    @Suppress("Deprecation")
    fun appVersionLabel(context: Context): String = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        "${info.versionName} (code ${info.longVersionCode})"
    }.getOrElse { "unknown" }

    /**
     * Debug hook: deliberately throws a sample exception, builds the exact report the real handler
     * would build and writes it, without taking down the app. Long-press the Version row.
     */
    fun writeTestReport(context: Context): String {
        val sample = runCatching {
            throw IllegalArgumentException(
                "Sample: visit https://example.com/private/path?token=abc " +
                    "mail dev@example.com passphrase=hunter2 token=abcdef0123456789abcdef0123456789"
            )
        }.exceptionOrNull()!!
        val report = runCatching { build(sample, "main", context) }
            .getOrElse { buildReport(sample, "main") }
        runCatching { writeReports(context, report) }
        logcat(report)
        return report
    }

    private fun build(throwable: Throwable, threadName: String, context: Context): String {
        val events = runCatching { DiagnosticsLog.snapshotLast(RECENT_EVENT_COUNT) }.getOrElse { emptyList() }
        return buildReport(throwable, threadName, deviceInfo(context), events = events)
    }

    @Suppress("Deprecation")
    private fun deviceInfo(context: Context): String = buildString {
        append("App version: ").appendLine(redact(appVersionLabel(context)))
        append("ABI: ").appendLine(redact(runCatching { Build.SUPPORTED_ABIS.joinToString(", ") }.getOrElse { Build.CPU_ABI }))
        append("Android SDK: ").appendLine(Build.VERSION.SDK_INT.toString())
        append("Device: ").appendLine(redact("${Build.MANUFACTURER} ${Build.MODEL}"))
    }

    private fun writeReports(context: Context, report: String) {
        runCatching {
            val dir = File(context.filesDir, "crash")
            if (!dir.exists()) dir.mkdirs()
            File(dir, "last-crash.txt").writeText(report)
        }
        runCatching { File(context.filesDir, CRASH_FILE).writeText(report) }
    }

    /** Logcat is capped per line, so chunk the report under the fixed tag. */
    internal fun logcat(text: String) {
        runCatching {
            if (text.length <= LOGCAT_CHUNK) {
                Log.e(TAG, text)
            } else {
                var offset = 0
                var part = 1
                while (offset < text.length) {
                    val end = minOf(offset + LOGCAT_CHUNK, text.length)
                    Log.e(TAG, "[part ${part++}] " + text.substring(offset, end))
                    offset = end
                }
            }
        }
    }

    /** Pure JVM function so the format and redaction can be unit-tested without a device. */
    internal fun buildReport(
        throwable: Throwable,
        threadName: String,
        metadata: String = "",
        timestampMillis: Long = System.currentTimeMillis(),
        events: List<LogEvent> = emptyList(),
    ): String {
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US)
        stamp.timeZone = TimeZone.getTimeZone("UTC")
        val eventStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        eventStamp.timeZone = TimeZone.getTimeZone("UTC")
        val text = buildString {
            appendLine("========== SAVELIN FULL DIAGNOSTICS ==========")
            append("Timestamp: ").append(stamp.format(Date(timestampMillis)))
                .append(" (epoch ").append(timestampMillis).appendLine(")")
            append("Thread: ").appendLine(redact(threadName))
            if (metadata.isNotBlank()) {
                appendLine("-- APP / DEVICE --")
                appendLine(metadata.trimEnd())
            }
            appendLine("-- EXCEPTION --")
            append("Type: ").appendLine(throwable.javaClass.name)
            append("Message: ").appendLine(redact(throwable.message ?: "<no message>"))
            appendLine("-- CAUSE CHAIN AND ALL STACK FRAMES --")
            var cause: Throwable? = throwable
            var depth = 0
            val seen = HashSet<Int>()
            while (cause != null && depth < MAX_CAUSES && seen.add(System.identityHashCode(cause))) {
                append('[').append(depth).append("] ").append(cause.javaClass.name)
                append(": ").appendLine(redact(cause.message ?: "<no message>"))
                for (frame in cause.stackTrace) {
                    append("    at ").append(frame.className).append('.').append(frame.methodName)
                        .append('(').append(redact(frame.fileName ?: "Unknown Source")).append(':')
                        .append(frame.lineNumber).appendLine(')')
                }
                appendLine()
                val next = cause.cause
                if (next != null && next !== cause) appendLine("Caused by:")
                cause = next
                depth++
            }
            appendLine("-- RECENT REDACTED UI EVENTS (last $RECENT_EVENT_COUNT) --")
            val recent = events.takeLast(RECENT_EVENT_COUNT)
            if (recent.isEmpty()) {
                appendLine("(none)")
            } else {
                for (event in recent) {
                    append('[').append(eventStamp.format(Date(event.time))).append("] ")
                        .append(event.level.name).append('/').append(redact(event.tag)).append(": ")
                        .appendLine(redact(event.message))
                }
            }
            appendLine("-- RECORDED ISSUES (what failed and why) --")
            val recordedIssues = DiagnosticsLog.issueSnapshot()
            if (recordedIssues.isEmpty()) {
                appendLine("(none)")
            } else {
                for (issue in recordedIssues.takeLast(RECENT_EVENT_COUNT)) {
                    append('[').append(eventStamp.format(Date(issue.time))).append("] ")
                        .append(redact(issue.scope)).append(": ").append(redact(issue.what))
                        .append(" — ").appendLine(redact(issue.why))
                }
            }
            appendLine("========== END SAVELIN DIAGNOSTICS ==========")
        }
        return redact(text)
    }
}
