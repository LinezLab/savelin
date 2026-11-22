package com.pinfold.app.diagnostics

import android.content.Context
import java.io.File

/** Writes the last crash to app-private storage and offers it once, on the next launch. */
object CrashReporter {
    private const val CRASH_FILE = "crash-last.txt"

    var pendingLog: String? = null
        private set

    fun install(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val report = buildReport(throwable, thread.name)
            DiagnosticsLog.log(LogLevel.ERROR, "Crash", report)
            runCatching { File(context.filesDir, CRASH_FILE).writeText(report) }
            previous?.uncaughtException(thread, throwable)
                ?: throw throwable
        }
        consumePending(context)
    }

    fun consumePending(context: Context) {
        val file = File(context.filesDir, CRASH_FILE)
        val text = runCatching { if (file.exists()) file.readText() else null }.getOrNull()
        if (!text.isNullOrBlank()) {
            pendingLog = text
            file.delete()
        }
    }

    fun clearPending() {
        pendingLog = null
    }

    /** Class names and frame names only: no messages, paths, URLs or user text. */
    internal fun buildReport(throwable: Throwable, threadName: String): String = buildString {
        append("Uncaught ").append(throwable.javaClass.name).append(" on ").append(redact(threadName))
        var cause: Throwable? = throwable
        var depth = 0
        while (cause != null && depth < 4) {
            append(" | ").append(cause.javaClass.name)
            cause.stackTrace.take(8).forEach { append(" at ").append(it.className).append('.').append(it.methodName) }
            cause = cause.cause
            depth++
        }
    }
}
