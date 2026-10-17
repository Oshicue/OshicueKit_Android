package com.oshicue.kit.core

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

enum class AgendaKind(val raw: String) {
    live("live"),
    flight("flight"),
    hotel("hotel"),
    restaurant("restaurant"),
    other("other");

    companion object {
        fun fromRaw(value: String): AgendaKind? = entries.firstOrNull { it.raw == value }
    }
}

/** Personal plans are independent of the performance timetable and its favorites. */
data class AgendaDocument(
    val schemaVersion: Int = 1,
    val entries: List<AgendaEntry> = emptyList()
) {
    constructor(entries: List<AgendaEntry>) : this(1, entries)

    /** Imports update matching IDs, while unrelated plans remain intact. */
    fun merging(incoming: AgendaDocument): AgendaDocument {
        AgendaImporter.validate(this)
        AgendaImporter.validate(incoming)
        val result = entries.toMutableList()
        for (entry in incoming.entries) {
            val index = result.indexOfFirst { it.id == entry.id }
            if (index >= 0) result[index] = entry else result.add(entry)
        }
        val document = AgendaDocument(entries = result)
        AgendaImporter.validate(document)
        return document
    }
}

data class AgendaCity(
    val name: String,
    val country: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

/** A routable venue or airport. City coordinates are deliberately kept separate. */
data class AgendaPlace(
    val name: String,
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class AgendaFlight(
    val number: String = "",
    val airline: String? = null,
    val departureAirport: String? = null,
    val arrivalAirport: String? = null,
    val departureAt: Instant? = null,
    val arrivalAt: Instant? = null,
    val departureTimeZone: String? = null,
    val arrivalTimeZone: String? = null,
    val departureLocation: AgendaPlace? = null,
    val arrivalLocation: AgendaPlace? = null
)

data class AgendaEntry(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val kind: AgendaKind = AgendaKind.live,
    val startsAt: Instant,
    val endsAt: Instant,
    val timeZone: String = ZoneId.systemDefault().id,
    val isAllDay: Boolean = false,
    val city: AgendaCity? = null,
    val location: AgendaPlace? = null,
    val venue: String = "",
    val notes: String = "",
    val flights: List<AgendaFlight> = emptyList()
) {
    val timeZoneValue: ZoneId
        get() = try {
            ZoneId.of(timeZone)
        } catch (_: Exception) {
            ZoneOffset.UTC
        }
}

object AgendaCore {
    /**
     * Inclusive civil-date bounds shared by the calendar and record management.
     * Bounds can be compared directly without enumerating a long stay's individual days.
     */
    fun dateRange(entry: AgendaEntry): Pair<String, String>? {
        if (!entry.endsAt.isAfter(entry.startsAt)) return null
        if (entry.kind == AgendaKind.flight) {
            val flight = entry.flights.firstOrNull()
            if (flight?.departureAt != null && flight.arrivalAt != null &&
                flight.departureTimeZone != null && flight.arrivalTimeZone != null
            ) {
                val departureZone = zoneOrNull(flight.departureTimeZone)
                val arrivalZone = zoneOrNull(flight.arrivalTimeZone)
                if (departureZone != null && arrivalZone != null) {
                    val departureDay = LedgerDates.string(flight.departureAt, departureZone)
                    val arrivalDay = LedgerDates.string(flight.arrivalAt, arrivalZone)
                    return minOf(departureDay, arrivalDay) to maxOf(departureDay, arrivalDay)
                }
            }
        }
        val first = LedgerDates.string(entry.startsAt, entry.timeZoneValue)
        // The immediately preceding representable instant also handles submillisecond plans.
        val last = LedgerDates.string(entry.endsAt.minusNanos(1), entry.timeZoneValue)
        return first to last
    }

    /**
     * A plan belongs to its local civil dates, regardless of the device's current zone.
     * A flight covers the inclusive range between its departure and arrival civil dates,
     * each in its airport's zone, even when crossing the date line reverses their order.
     * Other plans have exclusive end times; midnight does not occupy the following day.
     */
    fun entries(day: String, document: AgendaDocument): List<AgendaEntry> {
        if (LedgerDates.parse(day) == null) return emptyList()
        return document.entries.filter { entry ->
            val range = dateRange(entry) ?: return@filter false
            range.first <= day && day <= range.second
        }.sortedWith(compareBy<AgendaEntry>({ !it.isAllDay }, { localSortKey(it) }, { it.id }))
    }

    fun draft(day: String, timeZone: ZoneId = ZoneId.systemDefault()): AgendaEntry {
        val date = LedgerDates.parse(day)
        val start = if (date != null) {
            date.atTime(LocalTime.NOON).atZone(timeZone).toInstant()
        } else {
            Instant.now()
        }
        return AgendaEntry(startsAt = start, endsAt = start.plusSeconds(3600), timeZone = timeZone.id)
    }

    private fun localSortKey(entry: AgendaEntry): String {
        val value = entry.startsAt.atZone(entry.timeZoneValue)
        return "%04d-%02d-%02dT%02d:%02d:%02d".format(
            value.year, value.monthValue, value.dayOfMonth,
            value.hour, value.minute, value.second
        )
    }

    internal fun zoneOrNull(identifier: String?): ZoneId? {
        if (identifier == null) return null
        return try {
            ZoneId.of(identifier)
        } catch (_: Exception) {
            null
        }
    }

    internal fun startOfDay(instant: Instant, zone: ZoneId): Instant =
        instant.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()

    internal fun isMidnight(instant: Instant, zone: ZoneId): Boolean {
        val local = instant.atZone(zone)
        return local.toLocalTime() == LocalTime.MIDNIGHT && local.toLocalDate().atStartOfDay(zone).toInstant() == instant
    }
}
