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
            crashDir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(10)?.forEach { it.delete() }
        }
        defaultHandler?.uncaughtException(thread, throwable)
    }

    fun sanitizeStackTrace(e: Throwable): String = StringWriter().also { e.printStackTrace(PrintWriter(it)) }.toString()
        .replace(Regex("Bearer [\\w\\-\\.]+"), "Bearer ***")
        .replace(Regex("password[\"']?\\s*[:=]\\s*[\"']?[\\w]+"), "password=***")
        .replace(Regex("token[\"']?\\s*[:=]\\s*[\"']?[\\w\\-\\.]+"), "token=***")
}
