package com.pinfold.app.diagnostics

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel { INFO, WARN, ERROR }

data class LogEvent(val time: Long, val level: LogLevel, val tag: String, val message: String)

/** A small ring buffer of redacted events. Nothing here keeps user content, URLs or credentials. */
object DiagnosticsLog {
    private const val MAX_EVENTS = 200
    private val events = ArrayDeque<LogEvent>()

    @Synchronized
    fun log(level: LogLevel, tag: String, message: String) {
        events.addLast(LogEvent(System.currentTimeMillis(), level, redact(tag).take(48), redact(message).take(400)))
        while (events.size > MAX_EVENTS) events.removeFirst()
    }

    @Synchronized
    fun snapshot(): List<LogEvent> = events.toList()

    @Synchronized
    fun clear() {
        events.clear()
    }

    val size: Int @Synchronized get() = events.size

    fun format(): String {
        val lines = snapshot()
        if (lines.isEmpty()) return "Savelin diagnostics: no events recorded."
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return buildString {
            appendLine("Savelin diagnostics (redacted)")
            for (event in lines) {
                append('[').append(stamp.format(Date(event.time))).append("] ")
                    .append(event.level.name).append('/').append(event.tag).append(": ").appendLine(event.message)
            }
        }
    }
}
