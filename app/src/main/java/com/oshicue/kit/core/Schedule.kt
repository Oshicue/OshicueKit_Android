package com.oshicue.kit.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class SessionKind(val rawValue: String) {
    Live("live"),
    Goods("goods");

    companion object {
        fun from(value: String): SessionKind? = entries.firstOrNull { it.rawValue == value }
    }
}

data class Venue(val id: String, val name: String, val color: String)

data class Artist(val id: String, val name: String)

data class FestivalSession(
    val id: String,
    val artistId: String,
    val venueId: String,
    val kind: SessionKind,
    val startsAt: Instant,
    val endsAt: Instant,
    val area: String? = null,
    val notes: String? = null
)

data class FestivalEvent(
    val id: String,
    val name: String,
    val city: String,
    val timeZone: String,
    val venues: List<Venue>,
    val artists: List<Artist>,
    val sessions: List<FestivalSession>
) {
    val zone: ZoneId get() = KitDate.zoneOrUtc(timeZone)

    /** Every local day touched by a session, including sessions that cross midnight. */
    val days: List<LocalDate>
        get() {
            val result = sortedSetOf<LocalDate>()
            for (session in sessions) {
                var day = session.startsAt.atZone(zone).toLocalDate()
                while (day.atStartOfDay(zone).toInstant() < session.endsAt) {
                    result.add(day)
                    day = day.plusDays(1)
                }
            }
            return result.toList()
        }

    fun sessionsOn(day: LocalDate): List<FestivalSession> {
        val start = day.atStartOfDay(zone).toInstant()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant()
        return sessions.filter { it.startsAt < end && it.endsAt > start }
            .sortedWith(compareBy({ it.startsAt }, { it.id }))
    }

    fun artist(id: String): Artist? = artists.firstOrNull { it.id == id }
    fun venue(id: String): Venue? = venues.firstOrNull { it.id == id }
}

data class ScheduleDocument(
    val schemaVersion: Int = 1,
    val events: List<FestivalEvent> = emptyList()
) {
    /** A repeated event ID replaces its complete timetable; other imported events remain. */
    fun merging(incoming: ScheduleDocument): ScheduleDocument {
        val newIds = incoming.events.map { it.id }.toSet()
        return ScheduleDocument(schemaVersion, events.filter { it.id !in newIds } + incoming.events)
    }
}

data class TimelineItem(val id: String, val start: Instant, val end: Instant)

data class TimelinePlacement(
    val id: String,
    val lane: Int,
    val laneCount: Int
) {
    val relativeX: Double get() = lane.toDouble() / laneCount
    val relativeWidth: Double get() = 1.0 / laneCount
}

data class TimelineColumnLayout(
    val placements: List<TimelinePlacement>,
    val width: Double,
    val maximumLaneCount: Int
)

object TimelineLayout {
    /**
     * Keeps simultaneous sessions readable by expanding their venue column rather than
     * squeezing lanes. Sessions that just touch at an endpoint share a lane.
     */
    fun column(sessions: List<TimelineItem>, minimumColumnWidth: Double, minimumLaneWidth: Double): TimelineColumnLayout {
        require(minimumColumnWidth.isFinite() && minimumColumnWidth > 0)
        require(minimumLaneWidth.isFinite() && minimumLaneWidth > 0)
        val placements = place(sessions)
        val maximumLaneCount = maxOf(1, placements.maxOfOrNull { it.laneCount } ?: 1)
        val width = maxOf(minimumColumnWidth, maximumLaneCount * minimumLaneWidth)
        return TimelineColumnLayout(placements, width, maximumLaneCount)
    }

    fun place(sessions: List<TimelineItem>): List<TimelinePlacement> {
        val sorted = sessions.sortedWith(
            compareBy<TimelineItem> { it.start }.thenBy { it.end }.thenBy { it.id }
        )
        val placements = mutableListOf<TimelinePlacement>()
        var cluster = mutableListOf<TimelineItem>()
        var clusterEnd: Instant? = null

        fun placeCluster() {
            val laneEnds = mutableListOf<Instant>()
            val assigned = mutableListOf<Pair<String, Int>>()
            for (item in cluster) {
                val freeLane = laneEnds.indexOfFirst { !it.isAfter(item.start) }
                val lane = if (freeLane >= 0) {
                    laneEnds[freeLane] = item.end
                    freeLane
                } else {
                    laneEnds.add(item.end)
                    laneEnds.size - 1
                }
                assigned.add(item.id to lane)
            }
            assigned.forEach { (id, lane) ->
                placements.add(TimelinePlacement(id, lane, laneEnds.size))
            }
        }

        for (item in sorted) {
            val end = clusterEnd
            if (end != null && !item.start.isBefore(end)) {
                placeCluster()
                cluster = mutableListOf()
                clusterEnd = null
            }
            cluster.add(item)
            clusterEnd = if (clusterEnd == null || item.end.isAfter(clusterEnd)) item.end else clusterEnd
        }
        if (cluster.isNotEmpty()) placeCluster()
        return placements
    }
}

object ScheduleFavorites {
    /** Session IDs are unique within one event, so both IDs identify a selection. */
    fun id(eventId: String, sessionId: String): String =
        "${eventId.toByteArray(Charsets.UTF_8).size}:$eventId$sessionId"
}

val ScheduleDocument.sessionFavoriteIds: Set<String>
    get() = events.flatMap { event ->
        event.sessions.map { ScheduleFavorites.id(event.id, it.id) }
    }.toSet()

/** Expands legacy group selections to the sessions they previously displayed. */
fun ScheduleDocument.favoritesForArtists(artistIds: Set<String>): Set<String> =
    events.flatMap { event ->
        event.sessions.filter { it.artistId in artistIds }
            .map { ScheduleFavorites.id(event.id, it.id) }
    }.toSet()

class ScheduleDeletionResult(val document: ScheduleDocument, val favorites: Set<String>)

/**
 * Deletes one complete timetable and preserves selected sessions in other events.
 * An unknown event returns null so callers do not change or persist any state.
 */
fun ScheduleDocument.deletingEvent(id: String, favorites: Set<String>): ScheduleDeletionResult? {
    if (events.none { it.id == id }) return null
    val document = ScheduleDocument(schemaVersion, events.filter { it.id != id })
    return ScheduleDeletionResult(document, favorites.intersect(document.sessionFavoriteIds))
}

/** Local persistence has its own version; the imported timetable schema stays unchanged. */
class ScheduleSavedState(
    val document: ScheduleDocument,
    favorites: Set<String>,
    val isDemo: Boolean,
    val needsFavoriteMigration: Boolean = false
) {
    val favorites: Set<String> = favorites.intersect(document.sessionFavoriteIds)

    fun toJson(): JsonObject = JsonObject().apply {
        put("document", ScheduleDocumentCodec.toJson(document))
        put("favorites", JsonArray(favorites.sorted().map { JsonString(it) as JsonValue }.toMutableList()))
        put("isDemo", isDemo)
        put("favoritesVersion", 2)
    }

    companion object {
        fun fromJson(root: JsonObject): ScheduleSavedState {
            val document = ScheduleDocumentCodec.fromJson(root.requireObject("document", "$"))
            ScheduleImporter.validate(document)
            val version = root.optionalLong("favoritesVersion", "$")?.toInt() ?: 1
            val saved = root.requireArray("favorites", "$").items.map { it.stringOrThrow("$.favorites") }.toSet()
            val isDemo = root.requireBool("isDemo", "$")
            return when (version) {
                1 -> ScheduleSavedState(document, document.favoritesForArtists(saved), isDemo, true)
                2 -> {
                    val intersection = saved.intersect(document.sessionFavoriteIds)
                    ScheduleSavedState(document, intersection, isDemo, intersection != saved)
                }
                else -> throw ImportError(
                    "import.error.invalidJSON",
                    "Unsupported schedule favorites version $version.",
                    "$.favoritesVersion"
                )
            }
        }
    }
}

object ScheduleDocumentCodec {
    fun toJson(document: ScheduleDocument): JsonObject = JsonObject().apply {
        put("schemaVersion", document.schemaVersion)
        put("events", JsonArray(document.events.map { eventToJson(it) }.toMutableList()))
    }

    private fun eventToJson(event: FestivalEvent): JsonObject = JsonObject().apply {
        put("id", event.id)
        put("name", event.name)
        put("city", event.city)
        put("timeZone", event.timeZone)
        put("venues", JsonArray(event.venues.map {
            JsonObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("color", it.color)
            }
        }.toMutableList()))
        put("artists", JsonArray(event.artists.map {
            JsonObject().apply {
                put("id", it.id)
                put("name", it.name)
            }
        }.toMutableList()))
        put("sessions", JsonArray(event.sessions.map { sessionToJson(it) }.toMutableList()))
    }

    private fun sessionToJson(session: FestivalSession): JsonObject = JsonObject().apply {
        put("id", session.id)
        put("artistID", session.artistId)
        put("venueID", session.venueId)
        put("kind", session.kind.rawValue)
        put("startsAt", JsonString(KitDate.timestamp(session.startsAt)))
        put("endsAt", JsonString(KitDate.timestamp(session.endsAt)))
        session.area?.let { put("area", it) }
        session.notes?.let { put("notes", it) }
    }

    fun fromJson(root: JsonObject): ScheduleDocument {
        val schemaVersion = root.requireInt("schemaVersion", "$")
        val events = root.requireArray("events", "$").items.mapIndexed { index, value ->
            eventFromJson(value.objectOrThrow("$.events[$index]"), "$.events[$index]")
        }
        return ScheduleDocument(schemaVersion, events)
    }

    private fun eventFromJson(json: JsonObject, path: String): FestivalEvent {
        val venues = json.requireArray("venues", path).items.mapIndexed { index, value ->
            val venue = value.objectOrThrow("$path.venues[$index]")
            val venuePath = "$path.venues[$index]"
            Venue(
                id = venue.requireString("id", venuePath),
                name = venue.requireString("name", venuePath),
                color = venue.requireString("color", venuePath)
            )
        }
        val artists = json.requireArray("artists", path).items.mapIndexed { index, value ->
            val artist = value.objectOrThrow("$path.artists[$index]")
            val artistPath = "$path.artists[$index]"
            Artist(artist.requireString("id", artistPath), artist.requireString("name", artistPath))
        }
        val sessions = json.requireArray("sessions", path).items.mapIndexed { index, value ->
            val session = value.objectOrThrow("$path.sessions[$index]")
            val sessionPath = "$path.sessions[$index]"
            val kindText = session.requireString("kind", sessionPath)
            val kind = SessionKind.from(kindText)
                ?: importIssue(ImportErrorKey.InvalidJson, "Unknown session kind '$kindText'.", "$sessionPath.kind")
            val startText = session.requireString("startsAt", sessionPath)
            val startsAt = KitDate.parseTimestamp(startText)
                ?: importIssue(
                    ImportErrorKey.Timestamp,
                    "Use an ISO 8601 timestamp with seconds and Z or an explicit offset, for example 2026-10-14T14:30:00+09:00.",
                    "$sessionPath.startsAt"
                )
            val endText = session.requireString("endsAt", sessionPath)
            val endsAt = KitDate.parseTimestamp(endText)
                ?: importIssue(
                    ImportErrorKey.Timestamp,
                    "Use an ISO 8601 timestamp with seconds and Z or an explicit offset, for example 2026-10-14T14:30:00+09:00.",
                    "$sessionPath.endsAt"
                )
            FestivalSession(
                id = session.requireString("id", sessionPath),
                artistId = session.requireString("artistID", sessionPath),
                venueId = session.requireString("venueID", sessionPath),
                kind = kind,
                startsAt = startsAt,
                endsAt = endsAt,
                area = session.optionalString("area", sessionPath),
                notes = session.optionalString("notes", sessionPath)
            )
        }
        return FestivalEvent(
            id = json.requireString("id", path),
            name = json.requireString("name", path),
            city = json.requireString("city", path),
            timeZone = json.requireString("timeZone", path),
            venues = venues,
            artists = artists,
            sessions = sessions
        )
    }
}

object ScheduleImporter {
    fun parse(text: String): ScheduleDocument {
        val root = try {
            JsonParser.parse(text).objectOrThrow("$")
        } catch (issue: JsonIssue) {
            throw importErrorFromIssue(issue, invalidJsonKey = ImportErrorKey.InvalidJson)
        } catch (failure: JsonParseException) {
            throw ImportError(ImportErrorKey.InvalidJson.key, failure.message ?: "Invalid JSON.", "$")
        }
        val document = try {
            ScheduleDocumentCodec.fromJson(root)
        } catch (issue: JsonIssue) {
            throw importErrorFromIssue(issue, invalidJsonKey = ImportErrorKey.InvalidJson)
        }
        try {
            validate(document)
        } catch (issue: JsonIssue) {
            throw importErrorFromIssue(issue, invalidJsonKey = ImportErrorKey.InvalidJson)
        }
        return document
    }

    private fun importErrorFromIssue(issue: JsonIssue, invalidJsonKey: ImportErrorKey): ImportError {
        val key = when (issue.category) {
            "missingField" -> ImportErrorKey.MissingField
            "fieldType", "valueNotFound" -> ImportErrorKey.FieldType
            else -> invalidJsonKey
        }
        return ImportError(key.key, issue.detail, issue.path)
    }

    fun encode(document: ScheduleDocument): String {
        validate(document)
        return ScheduleDocumentCodec.toJson(document).serialize(pretty = true)
    }

    fun validate(document: ScheduleDocument) {
        if (document.schemaVersion != 1) {
            throw ImportError(
                ImportErrorKey.SchemaVersion.key,
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
            if (KitDate.zone(event.timeZone) == null) {
                throw ImportError(
                    ImportErrorKey.TimeZone.key,
                    "Use a valid IANA time zone such as Asia/Tokyo.",
                    "$eventPath.timeZone"
                )
            }
            unique(event.venues.map { it.id }, "$eventPath.venues", "venue")
            unique(event.artists.map { it.id }, "$eventPath.artists", "artist")
            unique(event.sessions.map { it.id }, "$eventPath.sessions", "session")
            event.venues.forEachIndexed { index, venue ->
                val venuePath = "$eventPath.venues[$index]"
                nonempty(venue.id, "$venuePath.id")
                nonempty(venue.name, "$venuePath.name")
                if (!Regex("^#[0-9a-fA-F]{6}$").matches(venue.color)) {
                    throw ImportError(
                        ImportErrorKey.Color.key,
                        "Use a six-digit RGB color, for example #7AC9EC.",
                        "$venuePath.color"
                    )
                }
            }
            event.artists.forEachIndexed { index, artist ->
                nonempty(artist.id, "$eventPath.artists[$index].id")
                nonempty(artist.name, "$eventPath.artists[$index].name")
            }
            val artistIds = event.artists.map { it.id }.toSet()
            val venueIds = event.venues.map { it.id }.toSet()
            event.sessions.forEachIndexed { index, session ->
                val sessionPath = "$eventPath.sessions[$index]"
                nonempty(session.id, "$sessionPath.id")
                if (session.artistId !in artistIds) {
                    throw ImportError(
                        ImportErrorKey.ArtistReference.key,
                        "Artist '${session.artistId}' is not declared in this event's artists.",
                        "$sessionPath.artistID"
                    )
                }
                if (session.venueId !in venueIds) {
                    throw ImportError(
                        ImportErrorKey.VenueReference.key,
                        "Venue '${session.venueId}' is not declared in this event's venues.",
                        "$sessionPath.venueID"
                    )
                }
                if (!session.endsAt.isAfter(session.startsAt)) {
                    throw ImportError(
                        ImportErrorKey.Duration.key,
                        "endsAt must be later than startsAt.",
                        "$sessionPath.endsAt"
                    )
                }
            }
        }
    }

    private fun nonempty(value: String, path: String) {
        if (value.isBlank()) {
            throw ImportError(ImportErrorKey.EmptyField.key, "This field must not be empty.", path)
        }
    }

    private fun unique(values: List<String>, path: String, label: String) {
        val found = mutableSetOf<String>()
        values.forEachIndexed { index, value ->
            if (!found.add(value)) {
                throw ImportError(
                    ImportErrorKey.DuplicateId.key,
                    "Duplicate $label ID '$value'.",
                    "$path[$index].id"
                )
            }
        }
    }
}

object ScheduleDemo {
    /** Fictional data anchored to the current local hour so the system-time line is visible. */
    fun document(now: Instant = Instant.now()): ScheduleDocument {
        val zone = KitDate.zoneOrUtc("Asia/Tokyo")
        val today = now.atZone(zone).toLocalDate()
        val hour = hoursOf(now, zone)
        val anchorInstant = today.atStartOfDay(zone).toInstant().plusSeconds(hour * 3600L)
        val tomorrow = today.plusDays(1)
        val nextAnchor = tomorrow.atStartOfDay(zone).toInstant().plusSeconds(12 * 3600L)
        val venues = listOf(
            Venue("sky-hall", "SKY HALL", "#78C9EB"),
            Venue("orbit-stage", "ORBIT STAGE", "#F4A174"),
            Venue("mint-room", "MINT ROOM", "#94C988"),
            Venue("petal-club", "PETAL CLUB", "#C58EC4"),
            Venue("sunny-studio", "SUNNY STUDIO", "#EECF67")
        )
        val artists = listOf(
            Artist("hoshiiro-note", "星色ノート"),
            Artist("parallel-days", "PARALLEL DAYS"),
            Artist("mint-signal", "ミントシグナル"),
            Artist("luna-palette", "LUNA PALETTE"),
            Artist("sora-melody", "そらメロディ"),
            Artist("neon-ribbon", "NEON RIBBON"),
            Artist("little-compass", "リトルコンパス"),
            Artist("sugar-orbit", "SUGAR ORBIT")
        )

        fun session(
            id: String, artist: Int, venue: Int, kind: SessionKind,
            offset: Int, duration: Int, base: Instant, area: String? = null
        ): FestivalSession = FestivalSession(
            id, artists[artist].id, venues[venue].id, kind,
            base.plusSeconds(offset * 60L), base.plusSeconds((offset + duration) * 60L), area
        )

        val sessions = mutableListOf<FestivalSession>()
        venues.indices.forEach { index ->
            sessions.add(session("today-live-$index", index, index, SessionKind.Live, index * 10, 40, anchorInstant))
            sessions.add(session("today-goods-$index", index, index, SessionKind.Goods, index * 10 + 50, 55, anchorInstant, "${index + 1}"))
            sessions.add(session("today-live-late-$index", (index + 3) % artists.size, index, SessionKind.Live, 85 + index * 8, 35, anchorInstant))
            sessions.add(session("today-goods-late-$index", (index + 3) % artists.size, index, SessionKind.Goods, 130 + index * 8, 55, anchorInstant, "${index + 1}"))
            sessions.add(session("today-live-evening-$index", (index + 5) % artists.size, index, SessionKind.Live, 210 + index * 8, 40, anchorInstant))
            sessions.add(session("tomorrow-live-$index", index, index, SessionKind.Live, index * 12, 35, nextAnchor))
            sessions.add(session("tomorrow-goods-$index", index, index, SessionKind.Goods, index * 12 + 45, 50, nextAnchor, "${index + 1}"))
        }
        val first = FestivalEvent(
            "demo-tokyo-idol-circuit", "TOKYO IDOL CIRCUIT", "Tokyo", "Asia/Tokyo",
            venues, artists, sessions
        )
        val secondVenue = Venue("river-loft", "RIVER LOFT", "#E789A5")
        val secondSessions = listOf(
            FestivalSession(
                "river-live-1", artists[0].id, secondVenue.id, SessionKind.Live,
                anchorInstant.plusSeconds(30 * 60L), anchorInstant.plusSeconds(65 * 60L)
            ),
            FestivalSession(
                "river-goods-1", artists[0].id, secondVenue.id, SessionKind.Goods,
                anchorInstant.plusSeconds(75 * 60L), anchorInstant.plusSeconds(125 * 60L), "A"
            ),
            FestivalSession(
                "river-live-2", artists[5].id, secondVenue.id, SessionKind.Live,
                nextAnchor, nextAnchor.plusSeconds(40 * 60L)
            )
        )
        val second = FestivalEvent(
            "demo-river-pop", "SHIBUYA NIGHT SESSION", "Tokyo", "Asia/Tokyo",
            listOf(secondVenue), listOf(artists[0], artists[5]), secondSessions
        )
        return ScheduleDocument(events = listOf(first, second))
    }

    private fun hoursOf(now: Instant, zone: ZoneId): Long =
        now.atZone(zone).toLocalTime().hour.toLong()
}
