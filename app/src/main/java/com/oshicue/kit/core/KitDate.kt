package com.oshicue.kit.core

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Date handling shared by every module. Timestamps always identify an absolute instant with
 * seconds and either Z or an explicit numeric offset, matching the iOS importer contract.
 */
object KitDate {
    private val timestampPattern = Regex(
        """^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})(?:\.(\d+))?(Z|[+-]\d{2}:\d{2})$"""
    )
    private val civilPattern = Regex("""^(\d{4})-(\d{2})-(\d{2})$""")

    val utc: ZoneId = ZoneOffset.UTC

    fun zone(identifier: String): ZoneId? = try {
        // iOS accepts named zones only; reject "Z" and "+09:00" style offsets.
        if (identifier.isBlank() || identifier == "Z" || identifier.startsWith("+") || identifier.startsWith("-")) {
            null
        } else {
            ZoneId.of(identifier).takeUnless { it is ZoneOffset }
        }
    } catch (_: Exception) {
        null
    }

    fun zoneOrUtc(identifier: String): ZoneId = zone(identifier) ?: utc

    /** Parses a strict ISO 8601 timestamp; null when the value is not an absolute instant. */
    fun parseTimestamp(text: String): Instant? {
        val match = timestampPattern.matchEntire(text) ?: return null
        val groups = match.groupValues
        val year = groups[1].toInt()
        val month = groups[2].toInt()
        val day = groups[3].toInt()
        val hour = groups[4].toInt()
        val minute = groups[5].toInt()
        val second = groups[6].toInt()
        if (year <= 0 || hour >= 24 || minute >= 60 || second >= 60) return null
        val local = try {
            LocalDateTime.of(year, month, day, hour, minute, second)
        } catch (_: Exception) {
            return null
        }
        val fraction = groups[7]
        val nanos = if (fraction.isEmpty()) 0 else {
            val padded = (fraction + "000000000").substring(0, 9)
            padded.toInt()
        }
        val offsetText = groups[8]
        val offset = if (offsetText == "Z") {
            ZoneOffset.UTC
        } else {
            val sign = if (offsetText.startsWith("-")) -1 else 1
            val hours = offsetText.substring(1, 3).toInt()
            val minutes = offsetText.substring(4, 6).toInt()
            if (hours >= 24 || minutes >= 60) return null
            try {
                ZoneOffset.ofHoursMinutes(sign * hours, sign * minutes)
            } catch (_: Exception) {
                return null
            }
        }
        return try {
            local.atOffset(offset).withNano(nanos).toInstant()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Formats an instant for document storage. Milliseconds match the iOS fractional-seconds
     * formatter, which never emits more than three fractional digits.
     */
    fun timestamp(instant: Instant): String {
        val millis = instant.toEpochMilli()
        val value = Instant.ofEpochMilli(millis)
        val utcTime = LocalDateTime.ofInstant(value, utc)
        val fraction = value.nano / 1_000_000
        return String.format(
            Locale.ROOT,
            "%04d-%02d-%02dT%02d:%02d:%02d.%03dZ",
            utcTime.year, utcTime.monthValue, utcTime.dayOfMonth,
            utcTime.hour, utcTime.minute, utcTime.second, fraction
        )
    }

    /** Parses a local civil day, validating Gregorian month and day lengths. */
    fun parseDay(text: String): LocalDate? {
        val match = civilPattern.matchEntire(text) ?: return null
        val year = match.groupValues[1].toInt()
        val month = match.groupValues[2].toInt()
        val day = match.groupValues[3].toInt()
        if (year <= 0) return null
        return try {
            LocalDate.of(year, month, day)
        } catch (_: Exception) {
            null
        }
    }

    fun day(instant: Instant, zoneId: ZoneId): String {
        val date = instant.atZone(zoneId).toLocalDate()
        return formatDay(date)
    }

    fun formatDay(date: LocalDate): String = String.format(
        Locale.ROOT, "%04d-%02d-%02d", date.year, date.monthValue, date.dayOfMonth
    )

    /** The civil day displayed for a local civil date, at noon to avoid daylight-saving edges. */
    fun noonInstant(day: String, zoneId: ZoneId): Instant {
        val date = parseDay(day) ?: LocalDate.now(zoneId)
        return date.atTime(12, 0).atZone(zoneId).toInstant()
    }

    fun startOfDay(instant: Instant, zoneId: ZoneId): Instant =
        instant.atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant()

    fun startOfNextDay(instant: Instant, zoneId: ZoneId): Instant =
        instant.atZone(zoneId).toLocalDate().plusDays(1).atStartOfDay(zoneId).toInstant()

    fun startOfHour(instant: Instant, zoneId: ZoneId): Instant {
        val local = LocalDateTime.ofInstant(instant, zoneId).truncatedTo(ChronoUnit.HOURS)
        return local.atZone(zoneId).toInstant()
    }

    fun startOfNextHour(instant: Instant, zoneId: ZoneId): Instant {
        val local = LocalDateTime.ofInstant(instant, zoneId).truncatedTo(ChronoUnit.HOURS).plusHours(1)
        return local.atZone(zoneId).toInstant()
    }

    fun addDays(instant: Instant, days: Long, zoneId: ZoneId): Instant =
        LocalDateTime.ofInstant(instant, zoneId).plusDays(days).atZone(zoneId).toInstant()

    fun addHours(instant: Instant, hours: Long, zoneId: ZoneId): Instant =
        LocalDateTime.ofInstant(instant, zoneId).plusHours(hours).atZone(zoneId).toInstant()

    fun minutesBetween(start: Instant, end: Instant): Double =
        (end.toEpochMilli() - start.toEpochMilli()) / 60_000.0

    /** Formats an instant in a time zone using the current app locale. */
    fun string(instant: Instant, pattern: String, zoneId: ZoneId, locale: Locale = L10n.locale): String {
        val formatter = DateTimeFormatter.ofPattern(pattern, locale)
        return formatter.format(LocalDateTime.ofInstant(instant, zoneId))
    }

    fun time(instant: Instant, zoneId: ZoneId): String =
        string(instant, "HH:mm", zoneId, Locale.ROOT)

    /** Sunday-first narrow weekday symbols, matching DateFormatter.veryShortWeekdaySymbols. */
    fun veryShortWeekdays(locale: Locale = L10n.locale): List<String> =
        (0 until 7).map { offset ->
            java.time.DayOfWeek.SUNDAY.plus(offset.toLong()).getDisplayName(TextStyle.NARROW, locale)
        }

    fun isSameDay(first: Instant, second: Instant, zoneId: ZoneId): Boolean =
        first.atZone(zoneId).toLocalDate() == second.atZone(zoneId).toLocalDate()

    fun isToday(instant: Instant, zoneId: ZoneId): Boolean =
        instant.atZone(zoneId).toLocalDate() == LocalDate.now(zoneId)

    fun today(zoneId: ZoneId): String = formatDay(LocalDate.now(zoneId))
}
