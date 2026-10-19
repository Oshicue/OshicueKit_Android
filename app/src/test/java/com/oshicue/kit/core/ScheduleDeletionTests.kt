package com.oshicue.kit.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Port of Tests/ScheduleDeletionTests.swift. */
class ScheduleDeletionTests {
    @Before
    fun pinLanguage() {
        // Resolving "System" reads android.os.LocaleList, which is unavailable on the JVM.
        L10n.setLanguage(AppLanguage.En)
    }

    private fun favorite(eventId: String, index: Int = 0): String =
        ScheduleFavorites.id(eventId = eventId, sessionId = "$eventId-session-$index")

    private fun event(id: String, artistIds: List<String>): FestivalEvent {
        val venueId = "$id-venue"
        val start = KitDate.parseTimestamp("2026-10-14T12:00:00+09:00")!!
        return FestivalEvent(
            id = id,
            name = "Festival $id",
            city = "Tokyo",
            timeZone = "Asia/Tokyo",
            venues = listOf(Venue(venueId, "Hall $id", "#F43275")),
            artists = artistIds.map { Artist(it, "Artist $it") },
            sessions = artistIds.mapIndexed { index, artistId ->
                FestivalSession(
                    id = "$id-session-$index",
                    artistId = artistId,
                    venueId = venueId,
                    kind = if (index % 2 == 0) SessionKind.Live else SessionKind.Goods,
                    startsAt = start.plusSeconds(index * 3600L),
                    endsAt = start.plusSeconds(index * 3600L + 1800),
                    area = "Area $index",
                    notes = "Original $id session"
                )
            }
        )
    }

    /** XCTUnwrap equivalent. */
    private fun <T : Any> unwrap(value: T?): T {
        assertNotNull("Expected a deletion result", value)
        return value!!
    }

    @Test
    fun testDeletingOneTimetablePreservesOtherEventsInOriginalOrder() {
        val first = event("first", listOf("first-artist"))
        val middle = event("middle", listOf("middle-live", "middle-goods"))
        val last = event("last", listOf("last-artist"))
        val original = ScheduleDocument(events = listOf(first, middle, last))

        val deleted = unwrap(original.deletingEvent(id = "middle", favorites = setOf(favorite("middle"))))

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
        val deleted = unwrap(original.deletingEvent(id = "first", favorites = emptySet()))
        assertEquals(17, deleted.document.schemaVersion)
    }

    @Test
    fun testDeletionKeepsOnlySelectedSessionsFromRemainingEvents() {
        val removed = event("removed", listOf("shared-artist", "removed-only"))
        val retained = event("retained", listOf("shared-artist", "retained-only"))
        val original = ScheduleDocument(events = listOf(removed, retained))

        val deleted = unwrap(
            original.deletingEvent(
                id = "removed",
                favorites = setOf(favorite("removed"), favorite("removed", 1), favorite("retained"), favorite("retained", 1), "stale-session")
            )
        )

        assertEquals(setOf(favorite("retained"), favorite("retained", 1)), deleted.favorites)
        assertEquals(listOf(retained), deleted.document.events)
    }

    @Test
    fun testSameArtistAndSessionIDsDoNotShareSelectionsAcrossEvents() {
        val removed = event("removed", listOf("shared-artist"))
        val base = event("retained", listOf("shared-artist"))
        val retained = base.copy(sessions = listOf(base.sessions[0].copy(id = removed.sessions[0].id)) + base.sessions.drop(1))
        val original = ScheduleDocument(events = listOf(removed, retained))

        val deleted = unwrap(original.deletingEvent(id = "removed", favorites = setOf(favorite("removed"))))

        assertTrue(deleted.favorites.isEmpty())
        assertEquals(listOf(retained), deleted.document.events)
    }

    @Test
    fun testDeletingLastTimetableProducesValidEmptyDocumentRoundTrip() {
        val original = ScheduleDocument(events = listOf(event("only", listOf("artist"))))

        val deleted = unwrap(original.deletingEvent(id = "only", favorites = setOf(favorite("only"))))

        assertEquals(ScheduleDocument(events = emptyList()), deleted.document)
        assertTrue(deleted.favorites.isEmpty())
        val json = ScheduleImporter.encode(deleted.document)
        assertEquals(deleted.document, ScheduleImporter.parse(json))
        assertNull(deleted.document.deletingEvent(id = "only", favorites = setOf("stale-session")))
    }

    @Test
    fun testUnknownEventDoesNotChangeDocumentOrPruneFavorites() {
        val original = ScheduleDocument(events = listOf(event("retained", listOf("artist"))))
        val favorites = setOf(favorite("retained"), "stale-session")
        var document = original
        var savedFavorites = favorites
        val deleted = document.deletingEvent(id = "missing", favorites = savedFavorites)
        if (deleted != null) {
            document = deleted.document
            savedFavorites = deleted.favorites
        }
        assertEquals(original, document)
        assertEquals(favorites, savedFavorites)
        assertNull(original.deletingEvent(id = "missing", favorites = favorites))
    }
}
