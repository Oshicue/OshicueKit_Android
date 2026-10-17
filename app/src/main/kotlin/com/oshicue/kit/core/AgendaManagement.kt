package com.oshicue.kit.core

/** Independent conditions are combined. Date bounds include every overlapping civil day. */
data class AgendaEntryFilter(
    val startDate: String? = null,
    val endDate: String? = null,
    val kind: AgendaKind? = null,
    val city: String? = null,
    val place: String? = null,
    val timeZone: String? = null,
    val isAllDay: Boolean? = null,
    val query: String = ""
) {
    val hasValidDateRange: Boolean
        get() {
            if (startDate != null && LedgerDates.parse(startDate) == null) return false
            if (endDate != null && LedgerDates.parse(endDate) == null) return false
            if (startDate != null && endDate != null && startDate > endDate) return false
            return true
        }

    fun matching(entries: List<AgendaEntry>): List<AgendaEntry> {
        if (!hasValidDateRange) return emptyList()
        val words = KitText.words(query)
        return entries.filter { entry ->
            if (kind != null && entry.kind != kind) return@filter false
            if (city != null && entry.city?.name != city) return@filter false
            if (isAllDay != null && entry.isAllDay != isAllDay) return@filter false

            val places = buildList {
                entry.location?.name?.let(::add)
                add(entry.venue)
                for (flight in entry.flights) {
                    flight.departureLocation?.name?.let(::add)
                    flight.arrivalLocation?.name?.let(::add)
                    flight.departureAirport?.let(::add)
                    flight.arrivalAirport?.let(::add)
                }
            }
            val zones = buildList {
                add(entry.timeZone)
                for (flight in entry.flights) {
                    flight.departureTimeZone?.let(::add)
                    flight.arrivalTimeZone?.let(::add)
                }
            }
            if (place != null && place !in places) return@filter false
            if (timeZone != null && timeZone !in zones) return@filter false

            if (startDate != null || endDate != null) {
                val range = AgendaCore.dateRange(entry) ?: return@filter false
                if (startDate != null && range.second < startDate) return@filter false
                if (endDate != null && range.first > endDate) return@filter false
            }

            if (words.isEmpty()) return@filter true
            val fields = buildList {
                add(entry.id)
                add(entry.title)
                add(entry.kind.raw)
                add(entry.city?.name ?: "")
                add(entry.city?.country ?: "")
                add(entry.location?.name ?: "")
                add(entry.location?.address ?: "")
                add(entry.venue)
                add(entry.notes)
                add(entry.timeZone)
                for (flight in entry.flights) {
                    add(flight.number)
                    add(flight.airline ?: "")
                    add(flight.departureAirport ?: "")
                    add(flight.arrivalAirport ?: "")
                    add(flight.departureLocation?.name ?: "")
                    add(flight.departureLocation?.address ?: "")
                    add(flight.arrivalLocation?.name ?: "")
                    add(flight.arrivalLocation?.address ?: "")
                    add(flight.departureTimeZone ?: "")
                    add(flight.arrivalTimeZone ?: "")
                }
            }
            val searchable = fields.joinToString("\n")
            words.all { KitText.contains(searchable, it) }
        }
    }
}

/** A fixed confirmation snapshot counts only IDs which still exist in the document. */
data class AgendaEntryDeletion(
    val entryIDs: Set<String>,
    val entryCount: Int,
    val countsByKind: Map<AgendaKind, Int>
) {
    constructor(entries: List<AgendaEntry>, selectedIDs: Set<String>) : this(
        entryIDs = entries.filter { it.id in selectedIDs }.map { it.id }.toSet(),
        entryCount = entries.count { it.id in selectedIDs },
        countsByKind = entries.filter { it.id in selectedIDs }.groupingBy { it.kind }.eachCount()
    )
}

/** Deleting plans preserves the complete, ordered values of all remaining plans. */
fun AgendaDocument.deletingEntries(ids: Set<String>): AgendaDocument =
    copy(entries = entries.filter { it.id !in ids })
