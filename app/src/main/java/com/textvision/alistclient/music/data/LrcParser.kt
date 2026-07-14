package com.textvision.alistclient.music.data

import com.textvision.alistclient.music.data.model.LrcLine

/**
 * Parses LRC lyrics text into a time-ordered list of [LrcLine].
 * - Lines like `[mm:ss.xx]text` or `[mm:ss]text` produce entries.
 * - Lines with multiple time tags (e.g. `[00:01.00][00:30.00]chorus`) produce multiple entries.
 * - Metadata tags (`[ti:]`, `[ar:]`, `[al:]`, `[by:]`, `[offset:]`) are ignored.
 * - Empty lines and lines with no valid tags are skipped.
 * - The list is sorted by [LrcLine.timeMs] ascending.
 */
object LrcParser {

    private val tagRegex = Regex("""\[(\d+):(\d{1,2})(?:[.:](\d{1,3}))?]""")
    private val metaRegex = Regex("""\[(ti|ar|al|by|offset|length):.*]""", RegexOption.IGNORE_CASE)

    fun parse(text: String): List<LrcLine> {
        if (text.isBlank()) return emptyList()
        val out = mutableListOf<LrcLine>()
        text.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEach
            // Strip metadata tags lines entirely.
            val remaining = line.replace(metaRegex, "").trim()
            if (remaining.isEmpty()) return@forEach
            val matches = tagRegex.findAll(remaining).toList()
            if (matches.isEmpty()) return@forEach
            val text = remaining.substring(matches.last().range.last + 1).trim()
            if (text.isEmpty()) return@forEach
            matches.forEach { m ->
                val (minStr, secStr, msStr) = m.destructured
                val minutes = minStr.toLong()
                val seconds = secStr.toLong()
                val millis = when {
                    msStr.isEmpty() -> 0L
                    msStr.length == 1 -> msStr.toLong() * 100
                    msStr.length == 2 -> msStr.toLong() * 10
                    else -> msStr.substring(0, 3).toLong()
                }
                out += LrcLine(
                    timeMs = minutes * 60_000 + seconds * 1_000 + millis,
                    text = text,
                )
            }
        }
        return out.sortedBy { it.timeMs }
    }
}
