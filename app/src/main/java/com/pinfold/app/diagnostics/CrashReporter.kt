package com.pinfold.app.diagnostics

import android.content.Context
import java.io.File

private val inHandler: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }

/** Writes the last crash to app-private storage and offers it once, on the next launch. */
object CrashReporter {
    private const val CRASH_FILE = "crash-last.txt"

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
                val report = runCatching { buildReport(throwable, thread.name) }
                    .getOrElse { "Uncaught ${throwable.javaClass.name} on main" }
                runCatching { DiagnosticsLog.log(LogLevel.ERROR, "Crash", report) }
                runCatching { File(context.filesDir, CRASH_FILE).writeText(report) }
            } finally {
                inHandler.set(false)
            }
            // If there is no platform handler, rethrowing here would feed the same throwable back into
            // this handler and recurse forever. Let the thread die quietly instead.
            runCatching { previous?.uncaughtException(thread, throwable) }
        }
    }

    fun consumePending(context: Context) {
        val file = File(context.filesDir, CRASH_FILE)
        val text = runCatching { if (file.exists()) file.readText() else null }.getOrNull()
        if (!text.isNullOrBlank()) {
            pendingLog = text
            runCatching { file.delete() }
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
