package com.oshicue.kit.core

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant
import java.time.ZoneId

data class ImportError(
    override val key: String,
    override val detail: String,
    override val path: String
) : Exception("$path: $detail"), KitIssue

object ScheduleImporter {
    fun parse(text: String): ScheduleDocument {
        val root = KitJson.parse(text)
            ?: throw ImportError("import.error.invalidJSON", "The text is not valid JSON.", "$")
        val document = decodeDocument(root)
        validate(document)
        return document
    }

    fun encode(document: ScheduleDocument): String {
        validate(document)
        val events = document.events.map { event ->
            val eventObject = JObj()
            eventObject.putArray("artists", event.artists.map { artist ->
                JObj().apply { put("id", artist.id); put("name", artist.name) }.build()
            })
            eventObject.put("city", event.city)
            eventObject.put("id", event.id)
            eventObject.put("name", event.name)
            eventObject.putArray("sessions", event.sessions.map { session ->
                JObj().apply {
                    session.area?.let { put("area", it) }
                    put("artistID", session.artistID)
                    put("endsAt", ScheduleDates.timestamp(session.endsAt, ZoneId.of("UTC")))
                    put("id", session.id)
                    put("kind", session.kind.raw)
                    session.notes?.let { put("notes", it) }
                    put("startsAt", ScheduleDates.timestamp(session.startsAt, ZoneId.of("UTC")))
                    put("venueID", session.venueID)
                }.build()
            })
            eventObject.put("timeZone", event.timeZone)
            eventObject.putArray("venues", event.venues.map { venue ->
                JObj().apply { put("color", venue.color); put("id", venue.id); put("name", venue.name) }.build()
            })
            eventObject.build()
        }
        val root = JObj()
        root.putArray("events", events)
        root.put("schemaVersion", document.schemaVersion)
        return JsonText.encode(root.build())
    }

    fun validate(document: ScheduleDocument) {
        if (document.schemaVersion != 1) {
            throw ImportError(
                "import.error.schemaVersion",
                "Supported schemaVersion is 1; received ${document.schemaVersion}.",
                "$.schemaVersion"
            )
        }
        unique(document.events.map { it.id }, "$.events", "event")
        document.events.forEachIndexed { eventIndex, event ->
            val eventPath = "$.events[$eventIndex]"
            nonempty(event.id, "$eventPath.id")
            nonempty(event.name, "$eventPath.name")
            nonempty(event.city, "$eventPath.city")
            if (!validZone(event.timeZone)) {
                throw ImportError("import.error.timeZone", "Use a valid IANA time zone such as Asia/Tokyo.", "$eventPath.timeZone")
            }
            unique(event.venues.map { it.id }, "$eventPath.venues", "venue")
            unique(event.artists.map { it.id }, "$eventPath.artists", "artist")
            unique(event.sessions.map { it.id }, "$eventPath.sessions", "session")
            event.venues.forEachIndexed { index, venue ->
                val venuePath = "$eventPath.venues[$index]"
                nonempty(venue.id, "$venuePath.id")
                nonempty(venue.name, "$venuePath.name")
                if (!colorPattern.matches(venue.color)) {
                    throw ImportError("import.error.color", "Use a six-digit RGB color, for example #7AC9EC.", "$venuePath.color")
                }
            }
            event.artists.forEachIndexed { index, artist ->
                nonempty(artist.id, "$eventPath.artists[$index].id")
                nonempty(artist.name, "$eventPath.artists[$index].name")
            }
            val artistIDs = event.artists.map { it.id }.toSet()
            val venueIDs = event.venues.map { it.id }.toSet()
            event.sessions.forEachIndexed { index, session ->
                val sessionPath = "$eventPath.sessions[$index]"
                nonempty(session.id, "$sessionPath.id")
                if (session.artistID !in artistIDs) {
                    throw ImportError(
                        "import.error.artistReference",
                        "Artist '${session.artistID}' is not declared in this event's artists.",
                        "$sessionPath.artistID"
                    )
                }
                if (session.venueID !in venueIDs) {
                    throw ImportError(
                        "import.error.venueReference",
                        "Venue '${session.venueID}' is not declared in this event's venues.",
                        "$sessionPath.venueID"
                    )
                }
                if (!(session.endsAt > session.startsAt)) {
                    throw ImportError("import.error.duration", "endsAt must be later than startsAt.", "$sessionPath.endsAt")
                }
            }
        }
    }

    private val colorPattern = Regex("^#[0-9a-fA-F]{6}$")

    private fun validZone(identifier: String): Boolean = try {
        ZoneId.of(identifier)
        true
    } catch (_: Exception) {
        false
    }

    private fun nonempty(value: String, path: String) {
        if (value.trim().isEmpty()) {
            throw ImportError("import.error.emptyField", "This field must not be empty.", path)
        }
    }

    private fun unique(values: List<String>, path: String, label: String) {
        val found = mutableSetOf<String>()
        values.forEachIndexed { index, value ->
            if (!found.add(value)) {
                throw ImportError("import.error.duplicateID", "Duplicate $label ID '$value'.", "$path[$index].id")
            }
        }
    }

    private fun decodeDocument(root: JsonElement): ScheduleDocument {
        val reader = JsonReader()
        val top = reader.obj(root, "$")
        val schemaVersion = reader.int(top, "schemaVersion", "$")
        val eventElements = reader.array(top, "events", "$")
        val events = eventElements.mapIndexed { index, element -> decodeEvent(reader, element, "$.events[$index]") }
        return ScheduleDocument(schemaVersion = schemaVersion, events = events)
    }

    private fun decodeEvent(reader: JsonReader, element: JsonElement, path: String): FestivalEvent {
        val obj = reader.obj(element, path)
        val venues = reader.array(obj, "venues", path).mapIndexed { index, venueElement ->
            val venuePath = "$path.venues[$index]"
            val venue = reader.obj(venueElement, venuePath)
            Venue(
                id = reader.string(venue, "id", venuePath),
                name = reader.string(venue, "name", venuePath),
                color = reader.string(venue, "color", venuePath)
            )
        }
        val artists = reader.array(obj, "artists", path).mapIndexed { index, artistElement ->
            val artistPath = "$path.artists[$index]"
            val artist = reader.obj(artistElement, artistPath)
            Artist(
                id = reader.string(artist, "id", artistPath),
                name = reader.string(artist, "name", artistPath)
            )
        }
        val sessions = reader.array(obj, "sessions", path).mapIndexed { index, sessionElement ->
            val sessionPath = "$path.sessions[$index]"
            val session = reader.obj(sessionElement, sessionPath)
            val kindText = reader.string(session, "kind", sessionPath)
            val kind = SessionKind.fromRaw(kindText)
                ?: throw ImportError("import.error.invalidJSON", "Use 'live' or 'goods' for kind.", "$sessionPath.kind")
            FestivalSession(
                id = reader.string(session, "id", sessionPath),
                artistID = reader.string(session, "artistID", sessionPath),
                venueID = reader.string(session, "venueID", sessionPath),
                kind = kind,
                startsAt = reader.timestamp(session, "startsAt", sessionPath),
                endsAt = reader.timestamp(session, "endsAt", sessionPath),
                area = reader.optionalString(session, "area", sessionPath),
                notes = reader.optionalString(session, "notes", sessionPath)
            )
        }
        return FestivalEvent(
            id = reader.string(obj, "id", path),
            name = reader.string(obj, "name", path),
            city = reader.string(obj, "city", path),
            timeZone = reader.string(obj, "timeZone", path),
            venues = venues,
            artists = artists,
            sessions = sessions
        )
    }
}

/** Shared structural JSON decoding with CodingPath-shaped error reports. */
class JsonReader(
    private val errorPrefix: String = "import.error",
    private val make: (String, String, String) -> Exception = { key, detail, path -> ImportError(key, detail, path) }
) {
    fun obj(element: JsonElement?, path: String): JsonObject =
        element as? JsonObject ?: fail("fieldType", "Expected a dictionary but found ${typeName(element)}.", path)

    fun array(obj: JsonObject, key: String, path: String): JsonArray {
        val value = obj[key] ?: fail("missingField", "Required field '$key' is missing.", "$path.$key")
        return value as? JsonArray ?: fail("fieldType", "Expected an array for '$key' but found ${typeName(value)}.", "$path.$key")
    }

    fun string(obj: JsonObject, key: String, path: String): String {
        val value = obj[key] ?: fail("missingField", "Required field '$key' is missing.", "$path.$key")
        return requireString(value, key, path)
    }

    fun optionalString(obj: JsonObject, key: String, path: String): String? {
        val value = obj[key] ?: return null
        if (value is JsonNull) return null
        return requireString(value, key, path)
    }

    fun int(obj: JsonObject, key: String, path: String): Int {
        val value = obj[key] ?: fail("missingField", "Required field '$key' is missing.", "$path.$key")
        val primitive = value as? JsonPrimitive
        if (primitive == null || primitive.isString) {
            fail("fieldType", "Expected an integer for '$key' but found ${typeName(value)}.", "$path.$key")
        }
        return primitive.content.toIntOrNull()
            ?: fail("fieldType", "Expected an integer for '$key' but found ${typeName(value)}.", "$path.$key")
    }

    fun long(obj: JsonObject, key: String, path: String): Long {
        val value = obj[key] ?: fail("missingField", "Required field '$key' is missing.", "$path.$key")
        val primitive = value as? JsonPrimitive
        if (primitive == null || primitive.isString) {
            fail("fieldType", "Expected an integer for '$key' but found ${typeName(value)}.", "$path.$key")
        }
        return primitive.content.toLongOrNull()
            ?: fail("fieldType", "Expected an integer for '$key' but found ${typeName(value)}.", "$path.$key")
    }

    fun optionalBool(obj: JsonObject, key: String, path: String): Boolean? {
        val value = obj[key] ?: return null
        if (value is JsonNull) return null
        val primitive = value as? JsonPrimitive
        if (primitive == null || primitive.isString) {
            fail("fieldType", "Expected a boolean for '$key' but found ${typeName(value)}.", "$path.$key")
        }
        return when (primitive.content) {
            "true" -> true
            "false" -> false
            else -> fail("fieldType", "Expected a boolean for '$key' but found ${typeName(value)}.", "$path.$key")
        }
    }

    /** Reads an ISO 8601 timestamp; non-strings are type errors and malformed strings are timestamp errors. */
    fun timestamp(obj: JsonObject, key: String, path: String): Instant {
        val value = obj[key] ?: fail("missingField", "Required field '$key' is missing.", "$path.$key")
        val text = requireString(value, key, path)
        return ScheduleDates.parseTimestamp(text)
            ?: fail("timestamp", "Use an ISO 8601 timestamp with seconds and Z or an explicit offset.", "$path.$key")
    }

    private fun requireString(value: JsonElement, key: String, path: String): String {
        val primitive = value as? JsonPrimitive
        if (primitive == null || !primitive.isString) {
            fail("fieldType", "Expected a string for '$key' but found ${typeName(value)}.", "$path.$key")
        }
        return primitive.content
    }

    private fun fail(category: String, detail: String, path: String): Nothing = throw make("$errorPrefix.$category", detail, path)
}
