package com.oshicue.kit

import com.oshicue.kit.core.Artist
import com.oshicue.kit.core.FestivalEvent
import com.oshicue.kit.core.FestivalSession
import com.oshicue.kit.core.ScheduleDates
import com.oshicue.kit.core.ScheduleDocument
import com.oshicue.kit.core.ScheduleImporter
import com.oshicue.kit.core.SessionKind
import com.oshicue.kit.core.Venue
import com.oshicue.kit.core.deletingEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScheduleDeletionTests {
    private fun event(id: String, artistIDs: List<String>): FestivalEvent {
        val venueID = "$id-venue"
        val start = ScheduleDates.parseTimestamp("2026-10-14T12:00:00+09:00")!!
        return FestivalEvent(
            id = id,
            name = "Festival $id",
            city = "Tokyo",
            timeZone = "Asia/Tokyo",
            venues = listOf(Venue(venueID, "Hall $id", "#F43275")),
            artists = artistIDs.map { Artist(it, "Artist $it") },
            sessions = artistIDs.mapIndexed { index, artistID ->
                FestivalSession(
                    id = "$id-session-$index",
                    artistID = artistID,
                    venueID = venueID,
                    kind = if (index % 2 == 0) SessionKind.live else SessionKind.goods,
                    startsAt = start.plusSeconds(index * 3600L),
                    endsAt = start.plusSeconds(index * 3600L + 1800L),
                    area = "Area $index",
                    notes = "Original $id session"
                )
            }
        )
    }

    @Test
    fun testDeletingOneTimetablePreservesOtherEventsInOriginalOrder() {
        val first = event("first", listOf("first-artist"))
        val middle = event("middle", listOf("middle-live", "middle-goods"))
        val last = event("last", listOf("last-artist"))
        val original = ScheduleDocument(events = listOf(first, middle, last))

        val deleted = original.deletingEvent("middle", setOf("middle-live"))!!

        assertEquals(listOf(first, last), deleted.document.events)
        assertEquals(first.venues + last.venues, deleted.document.events.flatMap { it.venues })
        assertEquals(first.artists + last.artists, deleted.document.events.flatMap { it.artists })
        assertEquals(first.sessions + last.sessions, deleted.document.events.flatMap { it.sessions })
        assertTrue(deleted.favorites.isEmpty())
        assertEquals(listOf(first, middle, last), original.events)
        ScheduleImporter.validate(deleted.document)
    }

    @Test
    fun testDeletionPreservesSchemaVersion() {
        val original = ScheduleDocument(schemaVersion = 17, events = listOf(event("first", listOf("artist"))))
        val deleted = original.deletingEvent("first", emptySet())!!
        assertEquals(17, deleted.document.schemaVersion)
    }

    @Test
    fun testSharedArtistFavoriteRemainsAndRemovedArtistFavoriteIsPruned() {
        val removed = event("removed", listOf("shared-artist", "removed-only"))
        val retained = event("retained", listOf("shared-artist", "retained-only"))
        val original = ScheduleDocument(events = listOf(removed, retained))

        val deleted = original.deletingEvent("removed", setOf("shared-artist", "removed-only", "retained-only", "stale-artist"))!!

        assertEquals(setOf("shared-artist", "retained-only"), deleted.favorites)
        assertEquals(listOf(retained), deleted.document.events)
    }

    @Test
    fun testFavoriteUsesCanonicalArtistIDRatherThanMatchingName() {
        var removed = event("removed", listOf("first-person"))
        var retained = event("retained", listOf("second-person"))
        removed = removed.copy(artists = listOf(Artist("first-person", "Same Stage Name")))
        retained = retained.copy(artists = listOf(Artist("second-person", "Same Stage Name")))
        val original = ScheduleDocument(events = listOf(removed, retained))

        val deleted = original.deletingEvent("removed", setOf("first-person"))!!

        assertTrue(deleted.favorites.isEmpty())
        assertEquals(listOf(retained), deleted.document.events)
    }

    @Test
    fun testDeletingLastTimetableProducesValidEmptyDocumentRoundTrip() {
        val original = ScheduleDocument(events = listOf(event("only", listOf("artist"))))

        val deleted = original.deletingEvent("only", setOf("artist"))!!

        assertEquals(ScheduleDocument(events = emptyList()), deleted.document)
        assertTrue(deleted.favorites.isEmpty())
        val json = ScheduleImporter.encode(deleted.document)
        assertEquals(deleted.document, ScheduleImporter.parse(json))
        assertNull(deleted.document.deletingEvent("only", setOf("stale-artist")))
    }

    @Test
    fun testUnknownEventDoesNotChangeDocumentOrPruneFavorites() {
        val original = ScheduleDocument(events = listOf(event("retained", listOf("artist"))))
        val favorites = setOf("artist", "stale-artist")
        val document = original
        val savedFavorites = favorites
        assertNull(original.deletingEvent("missing", favorites))
        assertEquals(original, document)
        assertEquals(favorites, savedFavorites)
    }
}
