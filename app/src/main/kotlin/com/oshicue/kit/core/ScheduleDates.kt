package com.oshicue.kit.core

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object ScheduleDates {
    private val timestampPattern = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(?:\\.[0-9]+)?(?:Z|[+-][0-9]{2}:[0-9]{2})$")
    private val writer = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSXXX")

    /** ISO 8601 timestamps must identify an absolute instant with Z or an explicit numeric offset. */
    fun parseTimestamp(text: String): Instant? {
        if (!timestampPattern.matches(text)) return null
        val parts = text.take(19).split(Regex("[^0-9]+")).filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }
        if (parts.size != 6 || parts[0] <= 0 || parts[3] >= 24 || parts[4] >= 60 || parts[5] >= 60) return null
        return try {
            Instant.parse(text)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    fun timestamp(date: Instant, zone: ZoneId = ZoneOffset.UTC): String =
        writer.withZone(zone).format(Instant.ofEpochMilli(date.millisRounded()))

    fun minutes(date: Instant, after: Instant): Double = (date.toEpochMilli() - after.toEpochMilli()) / 60_000.0
}

/** Milliseconds rounded, matching the diary's timestamp comparison. */
internal fun Instant.millisRounded(): Long {
    val seconds = epochSecond
    val nanos = nano
    return seconds * 1000L + ((nanos + 500_000) / 1_000_000L)
}
