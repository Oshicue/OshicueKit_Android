package com.oshicue.kit.core

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

enum class AgendaKind(val rawValue: String) {
    Live("live"), Flight("flight"), Hotel("hotel"), Restaurant("restaurant"), Other("other");

    companion object {
        fun from(value: String): AgendaKind? = entries.firstOrNull { it.rawValue == value }
    }
}

/** Personal plans are independent of the performance timetable and its favorites. */
data class AgendaDocument(
    val schemaVersion: Int = 1,
    val entries: List<AgendaEntry> = emptyList()
) {
    /** Imports update matching IDs, while unrelated plans remain intact. */
    fun merging(incoming: AgendaDocument): AgendaDocument {
        AgendaImporter.validate(this)
        AgendaImporter.validate(incoming)
        val result = entries.toMutableList()
        for (entry in incoming.entries) {
            val index = result.indexOfFirst { it.id == entry.id }
            if (index >= 0) result[index] = entry else result.add(entry)
        }
        val merged = AgendaDocument(schemaVersion, result)
        AgendaImporter.validate(merged)
        return merged
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
    val id: String = newIdentifier(),
    val title: String = "",
    val kind: AgendaKind = AgendaKind.Live,
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
    val zone: ZoneId get() = KitDate.zoneOrUtc(timeZone)
}

data class AgendaDateRange(val start: String, val end: String)

object AgendaCore {
    /**
     * Inclusive civil-date bounds shared by the calendar and record management.
     * A flight covers the range between its departure and arrival civil dates, each in
     * its airport's zone; other plans use their own zone and exclude a midnight end.
     */
    fun dateRange(entry: AgendaEntry): AgendaDateRange? {
        if (!entry.endsAt.isAfter(entry.startsAt)) return null
        if (entry.kind == AgendaKind.Flight) {
            val flight = entry.flights.firstOrNull()
            val departureAt = flight?.departureAt
            val arrivalAt = flight?.arrivalAt
            val departureZone = flight?.departureTimeZone?.let { KitDate.zone(it) }
            val arrivalZone = flight?.arrivalTimeZone?.let { KitDate.zone(it) }
            if (departureAt != null && arrivalAt != null && departureZone != null && arrivalZone != null) {
                val departureDay = KitDate.day(departureAt, departureZone)
                val arrivalDay = KitDate.day(arrivalAt, arrivalZone)
                return if (departureDay <= arrivalDay) AgendaDateRange(departureDay, arrivalDay)
                else AgendaDateRange(arrivalDay, departureDay)
            }
        }
        val first = KitDate.day(entry.startsAt, entry.zone)
        val exclusiveEnd = entry.endsAt.minusNanos(1)
        val last = KitDate.day(exclusiveEnd, entry.zone)
        return AgendaDateRange(first, last)
    }

    /** A plan belongs to its local civil dates, regardless of the device's current zone. */
    fun entriesOn(day: String, document: AgendaDocument): List<AgendaEntry> {
        if (LedgerDates.parse(day) == null) return emptyList()
        return document.entries.filter { entry ->
            val range = dateRange(entry) ?: return@filter false
            range.start <= day && day <= range.end
        }.sortedWith { left, right ->
            if (left.isAllDay != right.isAllDay) {
                if (left.isAllDay) -1 else 1
            } else {
                val leftKey = localSortKey(left)
                val rightKey = localSortKey(right)
                if (leftKey == rightKey) left.id.compareTo(right.id) else leftKey.compareTo(rightKey)
            }
        }
    }

    private fun localSortKey(entry: AgendaEntry): String {
        val local = LocalDateTime.ofInstant(entry.startsAt, entry.zone)
        return String.format(
            Locale.ROOT, "%04d-%02d-%02dT%02d:%02d:%02d",
            local.year, local.monthValue, local.dayOfMonth, local.hour, local.minute, local.second
        )
    }

    fun draft(day: String, zoneId: ZoneId = ZoneId.systemDefault()): AgendaEntry {
        val date = LedgerDates.parse(day)
        val start = if (date != null) date.atTime(12, 0).atZone(zoneId).toInstant() else Instant.now()
        return AgendaEntry(
            startsAt = start,
            endsAt = start.plusSeconds(3600),
            timeZone = zoneId.id
        )
    }

    /**
     * Connections follow absolute instants, even when two cities display different clocks.
     * All-day plans have no departure time. A hotel supplies check-in and checkout milestones
     * instead of reserving its entire overnight interval.
     */
    fun transfersOn(day: String, document: AgendaDocument): List<AgendaTransfer> {
        if (LedgerDates.parse(day) == null) return emptyList()
        val stops = document.entries.filter { !it.isAllDay }.flatMap { entry ->
            if (entry.kind == AgendaKind.Hotel) {
                listOf(
                    TransferStop(entry, entry.startsAt, entry.startsAt, 2),
                    TransferStop(entry, entry.endsAt, entry.endsAt, 0)
                )
            } else {
                listOf(TransferStop(entry, entry.startsAt, entry.endsAt, 1))
            }
        }.sortedWith(
            compareBy<TransferStop> { it.start }.thenBy { it.phase }.thenBy { it.entry.id }
        )
        val transfers = mutableListOf<AgendaTransfer>()
        for (index in 0 until stops.size - 1) {
            val source = stops[index]
            val target = stops[index + 1]
            if (source.entry.id == target.entry.id) continue
            val sourceZone = if (source.entry.kind == AgendaKind.Flight) {
                source.entry.flights.firstOrNull()?.arrivalTimeZone?.let { KitDate.zone(it) }
                    ?: source.entry.zone
            } else {
                source.entry.zone
            }
            val sourceDay = KitDate.day(source.end, sourceZone)
            val targetDay = KitDate.day(target.start, target.entry.zone)
            if (sourceDay != day && targetDay != day) continue
            transfers.add(AgendaTransfer(source.entry, target.entry, source.end, target.start))
        }
        return transfers
    }

    private data class TransferStop(val entry: AgendaEntry, val start: Instant, val end: Instant, val phase: Int)
}

/** A journey between consecutive scheduled stops. */
class AgendaTransfer(
    val from: AgendaEntry,
    val to: AgendaEntry,
    val departureAt: Instant,
    val arrivalDeadline: Instant
) {
    val id: String
        get() = "${from.id.toByteArray(Charsets.UTF_8).size}:${from.id}|" +
            "${to.id.toByteArray(Charsets.UTF_8).size}:${to.id}|" +
            "${departureAt.toEpochMilli()}|${arrivalDeadline.toEpochMilli()}"

    val origin: AgendaPlace?
        get() = if (from.kind == AgendaKind.Flight) from.flights.firstOrNull()?.arrivalLocation else from.location

    val destination: AgendaPlace?
        get() = if (to.kind == AgendaKind.Flight) to.flights.firstOrNull()?.departureLocation else to.location

    val availableTime: Long get() = arrivalDeadline.toEpochMilli() - departureAt.toEpochMilli()
    val hasTimeConflict: Boolean get() = availableTime < 0
}

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
        val words = query.split(Regex("[\\s\\p{Z}\\u0085]+")).filter { it.isNotEmpty() }
        return entries.filter { entry ->
            if (kind != null && entry.kind != kind) return@filter false
            if (city != null && entry.city?.name != city) return@filter false
            if (isAllDay != null && entry.isAllDay != isAllDay) return@filter false

            val places = mutableListOf(entry.location?.name, entry.venue)
            for (flight in entry.flights) {
                places.add(flight.departureLocation?.name)
                places.add(flight.arrivalLocation?.name)
                places.add(flight.departureAirport)
                places.add(flight.arrivalAirport)
            }
            val zones = mutableListOf<String?>(entry.timeZone)
            for (flight in entry.flights) {
                zones.add(flight.departureTimeZone)
                zones.add(flight.arrivalTimeZone)
            }
            if (place != null && place !in places) return@filter false
            if (timeZone != null && timeZone !in zones) return@filter false

            if (startDate != null || endDate != null) {
                val range = AgendaCore.dateRange(entry) ?: return@filter false
                if (startDate != null && range.end < startDate) return@filter false
                if (endDate != null && range.start > endDate) return@filter false
            }

            if (words.isEmpty()) return@filter true
            val fields = mutableListOf(
                entry.id, entry.title, entry.kind.rawValue, entry.city?.name ?: "",
                entry.city?.country ?: "", entry.location?.name ?: "", entry.location?.address ?: "",
                entry.venue, entry.notes, entry.timeZone
            )
            for (flight in entry.flights) {
                fields.add(flight.number)
                fields.add(flight.airline ?: "")
                fields.add(flight.departureAirport ?: "")
                fields.add(flight.arrivalAirport ?: "")
                fields.add(flight.departureLocation?.name ?: "")
                fields.add(flight.departureLocation?.address ?: "")
                fields.add(flight.arrivalLocation?.name ?: "")
                fields.add(flight.arrivalLocation?.address ?: "")
                fields.add(flight.departureTimeZone ?: "")
                fields.add(flight.arrivalTimeZone ?: "")
            }
            val searchable = fields.joinToString("\n")
            words.all { searchContains(searchable, it) }
        }
    }
}

/** A fixed confirmation snapshot counts only IDs which still exist in the document. */
class AgendaEntryDeletion(entries: List<AgendaEntry>, selectedIds: Set<String>) {
    val entryIds: Set<String>
    val entryCount: Int
    val countsByKind: Map<AgendaKind, Int>

    init {
        val selected = entries.filter { it.id in selectedIds }
        entryIds = selected.map { it.id }.toSet()
        entryCount = selected.size
        countsByKind = selected.groupingBy { it.kind }.eachCount()
    }
}

fun AgendaDocument.deletingEntries(ids: Set<String>): AgendaDocument =
    copy(entries = entries.filter { it.id !in ids })

class AgendaError(val key: String, val path: String) : Exception("$key · $path")

object AgendaImporter {
    const val maximumDocumentBytes = 10 * 1024 * 1024

    fun parse(text: String): AgendaDocument {
        val bytes = text.toByteArray(Charsets.UTF_8)
        if (bytes.size > maximumDocumentBytes) throw issue("size", "$")
        val root = try {
            JsonParser.parse(text).objectOrThrow("$")
        } catch (failure: JsonParseException) {
            throw issue("json", "$")
        } catch (failure: JsonIssue) {
            throw issueFor(failure)
        }
        val document = try {
            AgendaCodec.fromJson(root)
        } catch (failure: JsonIssue) {
            throw issueFor(failure)
        } catch (failure: AgendaError) {
            throw failure
        }
        validate(document)
        return document
    }

    /** Rejects duplicate IDs within one batch instead of silently discarding plans. */
    fun combining(documents: List<AgendaDocument>): AgendaDocument {
        val result = AgendaDocument(entries = documents.flatMap { it.entries })
        documents.forEach { validate(it) }
        validate(result)
        return result
    }

    fun encode(document: AgendaDocument): String {
        validate(document)
        return AgendaCodec.toJson(document).serialize(pretty = true)
    }

    fun validate(document: AgendaDocument) {
        if (document.schemaVersion != 1) throw issue("version", "$.schemaVersion")
        val ids = mutableSetOf<String>()
        document.entries.forEachIndexed { index, entry ->
            val path = "$.entries[$index]"
            if (entry.id.isBlank() || !ids.add(entry.id)) throw issue("id", "$path.id")
            if (entry.title.isBlank()) throw issue("title", "$path.title")
            zone(entry.timeZone, "$path.timeZone")
            timestamp(entry.startsAt, "$path.startsAt")
            timestamp(entry.endsAt, "$path.endsAt")
            if (!entry.endsAt.isAfter(entry.startsAt)) throw issue("range", "$path.endsAt")
            if (entry.isAllDay) {
                val zoneId = entry.zone
                val startDay = entry.startsAt.atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant()
                val endDay = entry.endsAt.atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant()
                if (startDay != entry.startsAt || endDay != entry.endsAt) throw issue("allDay", path)
            }
            val city = entry.city
            if (city != null) {
                if (city.name.isBlank()) throw issue("city", "$path.city.name")
                if (city.latitude != null || city.longitude != null) {
                    val latitude = city.latitude
                    val longitude = city.longitude
                    val valid = latitude != null && longitude != null &&
                        latitude.isFinite() && longitude.isFinite() &&
                        latitude in -90.0..90.0 && longitude in -180.0..180.0
                    if (!valid) throw issue("city", "$path.city")
                }
            }
            entry.location?.let { place(it, "$path.location") }
            if (entry.kind == AgendaKind.Flight) {
                val flight = entry.flights.singleOrNull()
                val departureAt = flight?.departureAt
                val arrivalAt = flight?.arrivalAt
                val departureZone = flight?.departureTimeZone
                val valid = flight != null && !entry.isAllDay &&
                    departureAt != null && arrivalAt != null &&
                    departureZone != null && flight.arrivalTimeZone != null &&
                    entry.startsAt == departureAt && entry.endsAt == arrivalAt &&
                    entry.timeZone == departureZone
                if (!valid) throw issue("flightActivity", path)
            }
            entry.flights.forEachIndexed { flightIndex, flight ->
                val flightPath = "$path.flights[$flightIndex]"
                if (flight.number.isBlank()) throw issue("flight", "$flightPath.number")
                val departureAt = flight.departureAt
                if (departureAt != null) {
                    timestamp(departureAt, "$flightPath.departureAt")
                    val identifier = flight.departureTimeZone ?: throw issue("zone", "$flightPath.departureTimeZone")
                    zone(identifier, "$flightPath.departureTimeZone")
                } else {
                    flight.departureTimeZone?.let { zone(it, "$flightPath.departureTimeZone") }
                }
                val arrivalAt = flight.arrivalAt
                if (arrivalAt != null) {
                    timestamp(arrivalAt, "$flightPath.arrivalAt")
                    val identifier = flight.arrivalTimeZone ?: throw issue("zone", "$flightPath.arrivalTimeZone")
                    zone(identifier, "$flightPath.arrivalTimeZone")
                } else {
                    flight.arrivalTimeZone?.let { zone(it, "$flightPath.arrivalTimeZone") }
                }
                if (departureAt != null && arrivalAt != null && !arrivalAt.isAfter(departureAt)) {
                    throw issue("range", "$flightPath.arrivalAt")
                }
                flight.departureLocation?.let { place(it, "$flightPath.departureLocation") }
                flight.arrivalLocation?.let { place(it, "$flightPath.arrivalLocation") }
            }
        }
        val encoded = AgendaCodec.toJson(document).serialize(pretty = true)
        if (encoded.toByteArray(Charsets.UTF_8).size > maximumDocumentBytes) throw issue("size", "$")
    }

    private fun timestamp(value: Instant, path: String) {
        if (KitDate.parseTimestamp(KitDate.timestamp(value)) == null) throw issue("timestamp", path)
    }

    private fun place(location: AgendaPlace, path: String) {
        if (location.name.isBlank()) throw issue("location", "$path.name")
        if (location.latitude != null || location.longitude != null) {
            val latitude = location.latitude
            val longitude = location.longitude
            val valid = latitude != null && longitude != null &&
                latitude.isFinite() && longitude.isFinite() &&
                latitude in -90.0..90.0 && longitude in -180.0..180.0
            if (!valid) throw issue("location", path)
        }
    }

    private fun zone(identifier: String, path: String) {
        if (KitDate.zone(identifier) == null) throw issue("zone", path)
    }

    private fun issueFor(issue: JsonIssue): AgendaError = when (issue.category) {
        "missingField" -> AgendaError("agenda.error.field", issue.path)
        else -> AgendaError("agenda.error.json", issue.path)
    }

    private fun issue(category: String, path: String): AgendaError = AgendaError("agenda.error.$category", path)
}

object AgendaCodec {
    fun toJson(document: AgendaDocument): JsonObject = JsonObject().apply {
        put("schemaVersion", document.schemaVersion)
        put("entries", JsonArray(document.entries.map { entry ->
            JsonObject().apply {
                put("id", entry.id)
                put("title", entry.title)
                put("kind", entry.kind.rawValue)
                put("startsAt", KitDate.timestamp(entry.startsAt))
                put("endsAt", KitDate.timestamp(entry.endsAt))
                put("timeZone", entry.timeZone)
                put("isAllDay", entry.isAllDay)
                entry.city?.let { city ->
                    put("city", JsonObject().apply {
                        put("name", city.name)
                        city.country?.let { put("country", it) }
                        city.latitude?.let { put("latitude", it) }
                        city.longitude?.let { put("longitude", it) }
                    })
                }
                entry.location?.let { put("location", placeToJson(it)) }
                put("venue", entry.venue)
                put("notes", entry.notes)
                put("flights", JsonArray(entry.flights.map { flight ->
                    JsonObject().apply {
                        put("number", flight.number)
                        flight.airline?.let { put("airline", it) }
                        flight.departureAirport?.let { put("departureAirport", it) }
                        flight.arrivalAirport?.let { put("arrivalAirport", it) }
                        flight.departureAt?.let { put("departureAt", KitDate.timestamp(it)) }
                        flight.arrivalAt?.let { put("arrivalAt", KitDate.timestamp(it)) }
                        flight.departureTimeZone?.let { put("departureTimeZone", it) }
                        flight.arrivalTimeZone?.let { put("arrivalTimeZone", it) }
                        flight.departureLocation?.let { put("departureLocation", placeToJson(it)) }
                        flight.arrivalLocation?.let { put("arrivalLocation", placeToJson(it)) }
                    }
                }.toMutableList()))
            }
        }.toMutableList()))
    }

    private fun placeToJson(place: AgendaPlace): JsonObject = JsonObject().apply {
        put("name", place.name)
        put("address", place.address)
        place.latitude?.let { put("latitude", it) }
        place.longitude?.let { put("longitude", it) }
    }

    fun fromJson(root: JsonObject): AgendaDocument = AgendaDocument(
        schemaVersion = root.requireInt("schemaVersion", "$"),
        entries = root.requireArray("entries", "$").items.mapIndexed { index, value ->
            val json = value.objectOrThrow("$.entries[$index]")
            val path = "$.entries[$index]"
            val kind = if (json.fieldOrNull("kind") == null) AgendaKind.Live
            else AgendaKind.from(json.requireString("kind", path))
                ?: throw AgendaError("agenda.error.json", "$path.kind")
            val cityJson = json.optionalObject("city", path)
            val city = cityJson?.let {
                AgendaCity(
                    name = it.requireString("name", "$path.city"),
                    country = it.optionalString("country", "$path.city"),
                    latitude = it.optionalDouble("latitude", "$path.city"),
                    longitude = it.optionalDouble("longitude", "$path.city")
                )
            }
            val flights = (json.optionalArray("flights", path) ?: JsonArray()).items.mapIndexed { flightIndex, flightValue ->
                val flight = flightValue.objectOrThrow("$path.flights[$flightIndex]")
                val flightPath = "$path.flights[$flightIndex]"
                AgendaFlight(
                    number = flight.requireString("number", flightPath),
                    airline = flight.optionalString("airline", flightPath),
                    departureAirport = flight.optionalString("departureAirport", flightPath),
                    arrivalAirport = flight.optionalString("arrivalAirport", flightPath),
                    departureAt = optionalTimestamp(flight, "departureAt", flightPath),
                    arrivalAt = optionalTimestamp(flight, "arrivalAt", flightPath),
                    departureTimeZone = flight.optionalString("departureTimeZone", flightPath),
                    arrivalTimeZone = flight.optionalString("arrivalTimeZone", flightPath),
                    departureLocation = optionalPlace(flight, "departureLocation", flightPath),
                    arrivalLocation = optionalPlace(flight, "arrivalLocation", flightPath)
                )
            }
            AgendaEntry(
                id = json.requireString("id", path),
                title = json.requireString("title", path),
                kind = kind,
                startsAt = requireTimestamp(json, "startsAt", path),
                endsAt = requireTimestamp(json, "endsAt", path),
                timeZone = json.requireString("timeZone", path),
                isAllDay = json.optionalBool("isAllDay", path) ?: false,
                city = city,
                location = optionalPlace(json, "location", path),
                venue = json.optionalString("venue", path) ?: "",
                notes = json.optionalString("notes", path) ?: "",
                flights = flights
            )
        }
    )

    private fun optionalPlace(json: JsonObject, key: String, path: String): AgendaPlace? {
        val placeJson = json.optionalObject(key, path) ?: return null
        return AgendaPlace(
            name = placeJson.requireString("name", "$path.$key"),
            address = placeJson.optionalString("address", "$path.$key") ?: "",
            latitude = placeJson.optionalDouble("latitude", "$path.$key"),
            longitude = placeJson.optionalDouble("longitude", "$path.$key")
        )
    }

    private fun optionalTimestamp(json: JsonObject, key: String, path: String): Instant? {
        val text = json.optionalString(key, path) ?: return null
        return KitDate.parseTimestamp(text)
            ?: throw AgendaError("agenda.error.timestamp", "$path.$key")
    }

    private fun requireTimestamp(json: JsonObject, key: String, path: String): Instant {
        val text = json.requireString(key, path)
        return KitDate.parseTimestamp(text)
            ?: throw AgendaError("agenda.error.timestamp", "$path.$key")
    }
}

/** Human-readable summaries shared by the agenda cards and detail screens. */
object AgendaPresentation {
    fun timeRange(entry: AgendaEntry): String {
        if (entry.isAllDay) return L10n.text("agenda.allDay")
        if (entry.kind == AgendaKind.Flight) {
            val flight = entry.flights.firstOrNull()
            val departureAt = flight?.departureAt
            val arrivalAt = flight?.arrivalAt
            val departureZone = flight?.departureTimeZone?.let { KitDate.zone(it) }
            val arrivalZone = flight?.arrivalTimeZone?.let { KitDate.zone(it) }
            if (departureAt != null && arrivalAt != null && departureZone != null && arrivalZone != null) {
                return KitDate.string(departureAt, "MM.dd HH:mm", departureZone) + " · " + flight.departureTimeZone +
                    " → " + KitDate.string(arrivalAt, "MM.dd HH:mm", arrivalZone) + " · " + flight.arrivalTimeZone
            }
        }
        return KitDate.string(entry.startsAt, "MM.dd HH:mm", entry.zone) +
            " – " + KitDate.string(entry.endsAt, "MM.dd HH:mm", entry.zone)
    }

    fun error(value: Throwable): String = when (value) {
        is AgendaError -> L10n.text(value.key) + "\n" + value.path
        else -> value.localizedMessage ?: ""
    }
}
