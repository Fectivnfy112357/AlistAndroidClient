package com.textvision.alistclient.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs

/**
 * Human-friendly date formatter used by file-list subtitles.
 * Mirrors the prototype §3 row caption: "今天 14:21", "昨天 09:12", "上周 · 2024-04", "2024-03".
 */
object DateFormatter {
    private const val DAY_MS = 24L * 60L * 60L * 1000L

    fun relativeDateTime(instant: Instant, now: Instant = Clock.System.now()): String {
        val zone = TimeZone.currentSystemDefault()
        val dt = instant.toLocalDateTime(zone)
        val nowDt = now.toLocalDateTime(zone)

        val diffMs = now.toEpochMilliseconds() - instant.toEpochMilliseconds()
        val sameYear = dt.year == nowDt.year
        val daysApart = kotlin.math.abs(daysBetween(dt.date, nowDt.date))

        return when {
            diffMs < 0L -> formatAbsolute(dt)
            daysApart == 0L -> "今天 ${formatHm(dt)}"
            daysApart == 1L -> "昨天 ${formatHm(dt)}"
            daysApart in 2L..6L -> "上周 · ${formatAbsolute(dt)}"
            sameYear -> formatMonth(dt)
            else -> formatAbsolute(dt)
        }
    }

    private fun daysBetween(a: kotlinx.datetime.LocalDate, b: kotlinx.datetime.LocalDate): Long {
        return (a.toEpochDays() - b.toEpochDays()).toLong()
    }

    private fun formatHm(dt: LocalDateTime): String =
        "%02d:%02d".format(dt.hour, dt.minute)

    private fun formatMonth(dt: LocalDateTime): String =
        "%04d-%02d".format(dt.year, dt.monthNumber)

    private fun formatAbsolute(dt: LocalDateTime): String =
        "%04d-%02d-%02d".format(dt.year, dt.monthNumber, dt.dayOfMonth)
}