package com.textvision.alistclient.common.crash

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class SafeCrashHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        runCatching {
            val crashDir = File(context.filesDir, "crash_logs").also { it.mkdirs() }
            val file = File(crashDir, "crash-${System.currentTimeMillis()}-${thread.name}.log")
            file.writeText(sanitizeStackTrace(throwable))
            enforceRetention(crashDir, MAX_CRASH_LOGS)
        }
        defaultHandler?.uncaughtException(thread, throwable)
    }

    fun sanitizeStackTrace(e: Throwable): String = StringWriter().also { e.printStackTrace(PrintWriter(it)) }.toString()
        // Bearer: word/dot/dash chars until next whitespace, quote, or sentence terminator.
        .replace(Regex("Bearer [\\w\\-\\.]+"), "Bearer ***")
        // password: capture value as a run of non-whitespace, non-quote, non-comma, non-} chars so
        // complex passwords (containing @, !, etc.) get fully scrubbed rather than leaking the
        // trailing non-word tail.
        .replace(Regex("password[\"']?\\s*[:=]\\s*[\"']?[^\\s\"',}]+"), "password=***")
        // token: same shape as Bearer.
        .replace(Regex("token[\"']?\\s*[:=]\\s*[\"']?[\\w\\-\\.]+"), "token=***")

    companion object {
        const val MAX_CRASH_LOGS = 10
        internal fun enforceRetention(dir: File, keep: Int) {
            dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(keep)?.forEach { it.delete() }
        }
    }
}
