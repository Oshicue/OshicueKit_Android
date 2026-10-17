package com.oshicue.kit.core

import java.time.Instant

/**
 * A journey between consecutive scheduled stops. Routing estimates are kept out of JSON:
 * the platform maps app calculates them for these explicit places at the requested departure time.
 */
data class AgendaTransfer(
    val from: AgendaEntry,
    val to: AgendaEntry,
    val departureAt: Instant = from.endsAt,
    val arrivalDeadline: Instant = to.startsAt
) {
    /** Length-prefixed entry IDs and milestone instants keep IDs stable and unambiguous. */
    val id: String
        get() = "${from.id.toByteArray(Charsets.UTF_8).size}:${from.id}|" +
            "${to.id.toByteArray(Charsets.UTF_8).size}:${to.id}|" +
            "${departureAt.toEpochMilli()}|${arrivalDeadline.toEpochMilli()}"

    val origin: AgendaPlace?
        get() = if (from.kind == AgendaKind.flight) {
            from.flights.firstOrNull()?.arrivalLocation
        } else {
            from.location
        }

    val destination: AgendaPlace?
        get() = if (to.kind == AgendaKind.flight) {
            to.flights.firstOrNull()?.departureLocation
        } else {
            to.location
        }

    val availableSeconds: Double
        get() = (arrivalDeadline.toEpochMilli() - departureAt.toEpochMilli()) / 1000.0

    val hasTimeConflict: Boolean get() = availableSeconds < 0
}

/**
 * Connections follow absolute instants, even when two cities display different clocks.
 * All-day plans have no departure time. A hotel stay supplies check-in and checkout
 * milestones instead of reserving its entire overnight interval. Activities during a
 * stay remain adjacent, and a hotel by itself never creates a journey to itself.
 * A journey appears on either endpoint's civil day, including a predecessor before midnight.
 */
fun agendaTransfers(day: String, document: AgendaDocument): List<AgendaTransfer> {
    if (LedgerDates.parse(day) == null) return emptyList()
    val stops = document.entries.filter { !it.isAllDay }.flatMap { entry ->
        if (entry.kind == AgendaKind.hotel) {
            listOf(
                TransferStop(entry, entry.startsAt, entry.startsAt, 2),
                TransferStop(entry, entry.endsAt, entry.endsAt, 0)
            )
        } else {
            listOf(TransferStop(entry, entry.startsAt, entry.endsAt, 1))
        }
    }.sortedWith(compareBy({ it.start }, { it.phase }, { it.entry.id }))

    return stops.zipWithNext().mapNotNull { (source, target) ->
        if (source.entry.id == target.entry.id) return@mapNotNull null
        val sourceZone = if (source.entry.kind == AgendaKind.flight) {
            AgendaCore.zoneOrNull(source.entry.flights.firstOrNull()?.arrivalTimeZone)
                ?: source.entry.timeZoneValue
        } else {
            source.entry.timeZoneValue
        }
        val sourceDay = LedgerDates.string(source.end, sourceZone)
        val targetDay = LedgerDates.string(target.start, target.entry.timeZoneValue)
        if (sourceDay != day && targetDay != day) return@mapNotNull null
        AgendaTransfer(source.entry, target.entry, source.end, target.start)
    }
}

private data class TransferStop(
    val entry: AgendaEntry,
    val start: Instant,
    val end: Instant,
    val phase: Int
)
