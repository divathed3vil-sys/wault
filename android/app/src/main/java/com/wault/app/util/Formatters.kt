package com.wault.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Display formatting helpers. The label conventions ("Yesterday", initials,
 * short account names) intentionally mirror the web demo.
 */
object Formatters {

    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
    private val dayFormat = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

    /** "16:05" today, "Yesterday", else "12 Feb" — chat-row timestamps. */
    fun chatTimestamp(epochMs: Long, nowMs: Long = System.currentTimeMillis()): String {
        val zone = ZoneId.systemDefault()
        val dateTime = Instant.ofEpochMilli(epochMs).atZone(zone)
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        val date = dateTime.toLocalDate()
        return when (date) {
            today -> timeFormat.format(dateTime)
            today.minusDays(1) -> "Yesterday"
            else -> dayFormat.format(date)
        }
    }

    /** "09:23" — bubble timestamps. */
    fun messageTime(epochMs: Long): String =
        timeFormat.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))

    /** "Malith Fernando" -> "MF" (mirrors the web's initialsOf). */
    fun initialsOf(name: String): String {
        val parts = name.trim().split(WHITESPACE).filter { it != MIDDLE_DOT }
        val first = parts.firstOrNull()?.take(1).orEmpty().ifEmpty { "W" }
        val last = if (parts.size > 1) parts.last().take(1) else ""
        return (first + last).uppercase(Locale.getDefault())
    }

    /** "Sales · Aadhil" -> "Sales" (mirrors the web's accountShortName). */
    fun shortAccountName(name: String): String =
        name.split(" · ").firstOrNull().orEmpty().ifEmpty { name }

    private val WHITESPACE = Regex("\\s+")
    private const val MIDDLE_DOT = "·"
}
