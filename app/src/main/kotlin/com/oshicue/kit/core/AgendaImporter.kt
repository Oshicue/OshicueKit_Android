package com.oshicue.kit.core

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

data class AgendaError(
    override val key: String,
    override val detail: String,
    override val path: String
) : Exception("$path: $detail"), KitIssue

object AgendaImporter {
    const val maximumDocumentBytes = 10 * 1024 * 1024

    fun parse(data: ByteArray): AgendaDocument {
        if (data.size > maximumDocumentBytes) {
            throw error("size", "Plans must not exceed 10 MB.", "$")
        }
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val text = try {
            decoder.decode(ByteBuffer.wrap(data)).toString()
        } catch (_: Exception) {
            throw error("json", "The plans must contain valid UTF-8 text.", "$")
        }
        return parse(text)
    }

    fun parse(text: String): AgendaDocument {
        if (text.toByteArray(Charsets.UTF_8).size > maximumDocumentBytes) {
            throw error("size", "Plans must not exceed 10 MB.", "$")
        }
        val root = KitJson.parse(text)
            ?: throw error("json", "The text is not valid JSON.", "$")
        val document = decode(root)
        validate(document)
        return document
    }

    /** Reject duplicate IDs within one batch instead of silently discarding a file's plans. */
    fun combining(documents: List<AgendaDocument>): AgendaDocument {
        for (document in documents) validate(document)
        val combined = AgendaDocument(entries = documents.flatMap { it.entries })
        validate(combined)
        return combined
    }

    fun encode(document: AgendaDocument): String {
        validate(document)
        val text = JsonText.encode(encodeElement(document))
        if (text.toByteArray(Charsets.UTF_8).size > maximumDocumentBytes) {
            throw error("size", "Plans must not exceed 10 MB.", "$")
        }
        return text
    }

    fun validate(document: AgendaDocument) {
        if (document.schemaVersion != 1) {
            throw error("version", "Supported schemaVersion is 1.", "$.schemaVersion")
        }
        val ids = mutableSetOf<String>()
        document.entries.forEachIndexed { index, entry ->
            val location = "$.entries[$index]"
            if (entry.id.trim().isEmpty() || !ids.add(entry.id)) {
                throw error("id", "Plan IDs must be nonempty and unique.", "$location.id")
            }
            if (entry.title.trim().isEmpty()) throw error("title", "Enter an activity name.", "$location.title")
            if (AgendaCore.zoneOrNull(entry.timeZone) == null) {
                throw error("zone", "Use a valid IANA time zone.", "$location.timeZone")
            }
            timestamp(entry.startsAt, "$location.startsAt")
            timestamp(entry.endsAt, "$location.endsAt")
            if (!entry.endsAt.isAfter(entry.startsAt)) {
                throw error("range", "The end time must be later than the start time.", "$location.endsAt")
            }
            if (entry.isAllDay) {
                val zone = entry.timeZoneValue
                if (!AgendaCore.isMidnight(entry.startsAt, zone) || !AgendaCore.isMidnight(entry.endsAt, zone)) {
                    throw error("allDay", "All-day plans must use local midnight; the end is exclusive.", location)
                }
            }
            entry.city?.let { city ->
                if (city.name.trim().isEmpty()) throw error("city", "Enter a city name.", "$location.city.name")
                if (city.latitude != null || city.longitude != null) {
                    coordinatePair(city.latitude, city.longitude)
                        ?: throw error("city", "Coordinates must be a valid latitude and longitude pair.", "$location.city")
                }
            }
            entry.location?.let { place(it, "$location.location") }
            if (entry.kind == AgendaKind.flight) {
                val flight = entry.flights.singleOrNull()
                if (entry.isAllDay || flight == null || flight.departureAt == null || flight.arrivalAt == null ||
                    flight.departureTimeZone == null || flight.arrivalTimeZone == null ||
                    entry.startsAt != flight.departureAt || entry.endsAt != flight.arrivalAt ||
                    entry.timeZone != flight.departureTimeZone
                ) {
                    throw error(
                        "flightActivity",
                        "A Flight activity requires exactly one complete timed flight matching the activity.",
                        location
                    )
                }
            }
            entry.flights.forEachIndexed { flightIndex, flight ->
                val path = "$location.flights[$flightIndex]"
                if (flight.number.trim().isEmpty()) throw error("flight", "Enter a flight number.", "$path.number")
                if (flight.departureAt != null) {
                    timestamp(flight.departureAt, "$path.departureAt")
                    val identifier = flight.departureTimeZone
                        ?: throw error("zone", "A flight time requires its time zone.", "$path.departureTimeZone")
                    zone(identifier, "$path.departureTimeZone")
                } else {
                    flight.departureTimeZone?.let { zone(it, "$path.departureTimeZone") }
                }
                if (flight.arrivalAt != null) {
                    timestamp(flight.arrivalAt, "$path.arrivalAt")
                    val identifier = flight.arrivalTimeZone
                        ?: throw error("zone", "A flight time requires its time zone.", "$path.arrivalTimeZone")
                    zone(identifier, "$path.arrivalTimeZone")
                } else {
                    flight.arrivalTimeZone?.let { zone(it, "$path.arrivalTimeZone") }
                }
                if (flight.departureAt != null && flight.arrivalAt != null &&
                    !flight.arrivalAt.isAfter(flight.departureAt)
                ) {
                    throw error("range", "The arrival time must be later than departure.", "$path.arrivalAt")
                }
                flight.departureLocation?.let { place(it, "$path.departureLocation") }
                flight.arrivalLocation?.let { place(it, "$path.arrivalLocation") }
            }
        }
        // Validation also runs for manually edited plans, outside decode/encode.
        val encoded = JsonText.encode(encodeElement(document))
        if (encoded.toByteArray(Charsets.UTF_8).size > maximumDocumentBytes) {
            throw error("size", "Plans must not exceed 10 MB.", "$")
        }
    }

    private fun error(category: String, detail: String, path: String) =
        AgendaError("agenda.error.$category", detail, path)

    private fun zone(identifier: String, path: String) {
        if (AgendaCore.zoneOrNull(identifier) == null) {
            throw error("zone", "Use a valid IANA time zone.", path)
        }
    }

    private fun timestamp(value: Instant, path: String) {
        val valid = try {
            ScheduleDates.parseTimestamp(ScheduleDates.timestamp(value)) != null
        } catch (_: Exception) {
            false
        }
        if (!valid) throw error("timestamp", "Use an ISO 8601 timestamp with Z or an explicit offset.", path)
    }

    private fun place(value: AgendaPlace, path: String) {
        if (value.name.trim().isEmpty()) throw error("location", "Enter a place name.", "$path.name")
        if (value.latitude != null || value.longitude != null) {
            coordinatePair(value.latitude, value.longitude)
                ?: throw error("location", "Coordinates must be a valid latitude and longitude pair.", path)
        }
    }

    private fun coordinatePair(latitude: Double?, longitude: Double?): Pair<Double, Double>? {
        if (latitude == null || longitude == null) return null
        if (!latitude.isFinite() || !longitude.isFinite()) return null
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
        return latitude to longitude
    }

    internal fun encodeElement(document: AgendaDocument): JsonObject {
        val root = JObj()
        root.putArray("entries", document.entries.map { entry ->
            JObj().apply {
                entry.city?.let { city ->
                    putElement("city", JObj().apply {
                        putString("country", city.country)
                        city.latitude?.let { put("latitude", it) }
                        city.longitude?.let { put("longitude", it) }
                        put("name", city.name)
                    }.build())
                }
                put("endsAt", ScheduleDates.timestamp(entry.endsAt, ZoneOffset.UTC))
                putArray("flights", entry.flights.map { flight ->
                    JObj().apply {
                        putString("airline", flight.airline)
                        putString("arrivalAirport", flight.arrivalAirport)
                        flight.arrivalAt?.let { put("arrivalAt", ScheduleDates.timestamp(it, ZoneOffset.UTC)) }
                        putString("arrivalTimeZone", flight.arrivalTimeZone)
                        flight.arrivalLocation?.let { arrival -> putElement("arrivalLocation", encodePlace(arrival)) }
                        putString("departureAirport", flight.departureAirport)
                        flight.departureAt?.let { put("departureAt", ScheduleDates.timestamp(it, ZoneOffset.UTC)) }
                        putString("departureTimeZone", flight.departureTimeZone)
                        flight.departureLocation?.let { departure -> putElement("departureLocation", encodePlace(departure)) }
                        put("number", flight.number)
                    }.build()
                })
                put("id", entry.id)
                put("isAllDay", entry.isAllDay)
                put("kind", entry.kind.raw)
                entry.location?.let { location -> putElement("location", encodePlace(location)) }
                put("notes", entry.notes)
                put("startsAt", ScheduleDates.timestamp(entry.startsAt, ZoneOffset.UTC))
                put("timeZone", entry.timeZone)
                put("title", entry.title)
                put("venue", entry.venue)
            }.build()
        })
        root.put("schemaVersion", document.schemaVersion)
        return root.build()
    }

    private fun encodePlace(place: AgendaPlace): JsonObject = JObj().apply {
        put("address", place.address)
        place.latitude?.let { put("latitude", it) }
        place.longitude?.let { put("longitude", it) }
        put("name", place.name)
    }.build()

    private fun decode(root: JsonElement): AgendaDocument {
        val top = root as? JsonObject ?: throw error("json", "Expected a JSON object.", "$")
        val schemaVersion = int(top, "schemaVersion", "$")
        val entriesElement = required(top, "entries", "$")
        val entriesArray = entriesElement as? JsonArray
            ?: throw error("json", "Expected an array for entries.", "$.entries")
        val entries = entriesArray.mapIndexed { index, element -> decodeEntry(element, "$.entries[$index]") }
        return AgendaDocument(schemaVersion, entries)
    }

    private fun decodeEntry(element: JsonElement, path: String): AgendaEntry {
        val obj = element as? JsonObject ?: throw error("json", "Expected a plan object.", path)
        val id = string(obj, "id", path)
        val title = string(obj, "title", path)
        val kind = if (obj.containsKey("kind")) {
            val value = obj["kind"]
            if (value is JsonNull) throw error("json", "Unsupported activity type.", "$path.kind")
            val text = stringValue(value!!, "$path.kind")
            AgendaKind.fromRaw(text) ?: throw error("json", "Unsupported activity type '$text'.", "$path.kind")
        } else {
            AgendaKind.live
        }
        val startsAt = timestampValue(obj, "startsAt", path)
        val endsAt = timestampValue(obj, "endsAt", path)
        val timeZone = string(obj, "timeZone", path)
        val isAllDay = optionalBool(obj, "isAllDay", path) ?: false
        val city = optionalObject(obj, "city", path)?.let { decodeCity(it, "$path.city") }
        val location = optionalObject(obj, "location", path)?.let { decodePlace(it, "$path.location") }
        val venue = optionalString(obj, "venue", path) ?: ""
        val notes = optionalString(obj, "notes", path) ?: ""
        val flights = optionalElement(obj, "flights")?.let { value ->
            val array = value as? JsonArray ?: throw error("json", "Expected an array for flights.", "$path.flights")
            array.mapIndexed { index, flight -> decodeFlight(flight, "$path.flights[$index]") }
        } ?: emptyList()
        return AgendaEntry(
            id = id,
            title = title,
            kind = kind,
            startsAt = startsAt,
            endsAt = endsAt,
            timeZone = timeZone,
            isAllDay = isAllDay,
            city = city,
            location = location,
            venue = venue,
            notes = notes,
            flights = flights
        )
    }

    private fun decodeCity(element: JsonElement, path: String): AgendaCity {
        val obj = element as? JsonObject ?: throw error("json", "Expected a city object.", path)
        return AgendaCity(
            name = string(obj, "name", path),
            country = optionalString(obj, "country", path),
            latitude = optionalDouble(obj, "latitude", path),
            longitude = optionalDouble(obj, "longitude", path)
        )
    }

    private fun decodePlace(element: JsonElement, path: String): AgendaPlace {
        val obj = element as? JsonObject ?: throw error("json", "Expected a place object.", path)
        return AgendaPlace(
            name = string(obj, "name", path),
            address = optionalString(obj, "address", path) ?: "",
            latitude = optionalDouble(obj, "latitude", path),
            longitude = optionalDouble(obj, "longitude", path)
        )
    }

    private fun decodeFlight(element: JsonElement, path: String): AgendaFlight {
        val obj = element as? JsonObject ?: throw error("json", "Expected a flight object.", path)
        return AgendaFlight(
            number = string(obj, "number", path),
            airline = optionalString(obj, "airline", path),
            departureAirport = optionalString(obj, "departureAirport", path),
            arrivalAirport = optionalString(obj, "arrivalAirport", path),
            departureAt = optionalTimestamp(obj, "departureAt", path),
            arrivalAt = optionalTimestamp(obj, "arrivalAt", path),
            departureTimeZone = optionalString(obj, "departureTimeZone", path),
            arrivalTimeZone = optionalString(obj, "arrivalTimeZone", path),
            departureLocation = optionalObject(obj, "departureLocation", path)
                ?.let { decodePlace(it, "$path.departureLocation") },
            arrivalLocation = optionalObject(obj, "arrivalLocation", path)
                ?.let { decodePlace(it, "$path.arrivalLocation") }
        )
    }

    private fun required(obj: JsonObject, key: String, path: String): JsonElement =
        obj[key] ?: throw error("field", "Required field '$key' is missing.", "$path.$key")

    private fun optionalElement(obj: JsonObject, key: String): JsonElement? {
        val value = obj[key]
        return if (value == null || value is JsonNull) null else value
    }

    private fun string(obj: JsonObject, key: String, parentPath: String): String =
        stringValue(required(obj, key, parentPath), "$parentPath.$key")

    private fun optionalString(obj: JsonObject, key: String, parentPath: String): String? {
        val value = optionalElement(obj, key) ?: return null
        val primitive = value as? JsonPrimitive
        if (primitive == null || !primitive.isString) {
            throw error("json", "Expected a string for '$key'.", "$parentPath.$key")
        }
        return primitive.content
    }

    private fun stringValue(value: JsonElement, path: String): String {
        val primitive = value as? JsonPrimitive
        if (primitive == null || !primitive.isString) {
            throw error("json", "Expected a string.", path)
        }
        return primitive.content
    }

    private fun int(obj: JsonObject, key: String, path: String): Int {
        val value = required(obj, key, path)
        val primitive = value as? JsonPrimitive
        if (primitive == null || primitive.isString) {
            throw error("json", "Expected an integer for '$key'.", "$path.$key")
        }
        return primitive.content.toIntOrNull()
            ?: throw error("json", "Expected an integer for '$key'.", "$path.$key")
    }

    private fun optionalBool(obj: JsonObject, key: String, parentPath: String): Boolean? {
        val value = optionalElement(obj, key) ?: return null
        val primitive = value as? JsonPrimitive
        if (primitive == null || primitive.isString) {
            throw error("json", "Expected a boolean for '$key'.", "$parentPath.$key")
        }
        return when (primitive.content) {
            "true" -> true
            "false" -> false
            else -> throw error("json", "Expected a boolean for '$key'.", "$parentPath.$key")
        }
    }

    private fun optionalDouble(obj: JsonObject, key: String, path: String): Double? {
        val value = optionalElement(obj, key) ?: return null
        val primitive = value as? JsonPrimitive
        if (primitive == null || primitive.isString) {
            throw error("json", "Expected a number for '$key'.", "$path.$key")
        }
        val number = primitive.content.toDoubleOrNull()
        if (number == null || !number.isFinite()) {
            throw error("json", "Expected a number for '$key'.", "$path.$key")
        }
        return number
    }

    private fun timestampValue(obj: JsonObject, key: String, path: String): Instant {
        val text = string(obj, key, path)
        return ScheduleDates.parseTimestamp(text)
            ?: throw error("timestamp", "Use an ISO 8601 timestamp with Z or an explicit offset.", "$path.$key")
    }

    private fun optionalTimestamp(obj: JsonObject, key: String, path: String): Instant? {
        val text = optionalString(obj, key, path) ?: return null
        return ScheduleDates.parseTimestamp(text)
            ?: throw error("timestamp", "Use an ISO 8601 timestamp with Z or an explicit offset.", "$path.$key")
    }

    private fun optionalObject(obj: JsonObject, key: String, path: String): JsonObject? {
        val value = optionalElement(obj, key) ?: return null
        return value as? JsonObject ?: throw error("json", "Expected an object for '$key'.", "$path.$key")
    }
}
