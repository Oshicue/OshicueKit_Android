package com.oshicue.kit.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/** Port of Tests/ScheduleFavoritesTests.swift. */
class ScheduleFavoritesTests {
    @Before
    fun pinLanguage() {
        // Resolving "System" reads android.os.LocaleList, which is unavailable on the JVM.
        L10n.setLanguage(AppLanguage.En)
    }

    private fun event(id: String, artistId: String = "shared-artist"): FestivalEvent {
        val start = KitDate.parseTimestamp("2026-10-14T12:00:00+09:00")!!
        return FestivalEvent(
            id = id, name = "Festival $id", city = "Tokyo", timeZone = "Asia/Tokyo",
            venues = listOf(Venue("hall", "Hall", "#78C9EB")),
            artists = listOf(Artist(artistId, "Shared Group"), Artist("no-sessions", "No Sessions")),
            sessions = listOf("live", "goods", "encore").mapIndexed { index, sessionId ->
                FestivalSession(
                    id = sessionId, artistId = artistId, venueId = "hall",
                    kind = if (index == 1) SessionKind.Goods else SessionKind.Live,
                    startsAt = start.plusSeconds(index * 3600L),
                    endsAt = start.plusSeconds(index * 3600L + 1800)
                )
            }
        )
    }

    private fun id(eventId: String, sessionId: String): String =
        ScheduleFavorites.id(eventId = eventId, sessionId = sessionId)

    /** Saved-state bytes as ScheduleStore writes them, optionally without a favoritesVersion (legacy). */
    private fun data(document: ScheduleDocument, favorites: Set<String>, version: Int? = null, isDemo: Boolean = false): String {
        val root = JsonObject().apply {
            put("document", ScheduleDocumentCodec.toJson(document))
            put("favorites", JsonArray(favorites.sorted().map { JsonString(it) as JsonValue }.toMutableList()))
            put("isDemo", isDemo)
            if (version != null) put("favoritesVersion", version)
        }
        return root.serialize()
    }

    /** JSONDecoder().decode(ScheduleSavedState.self, from:), as ScheduleStore loads the file. */
    private fun decode(text: String): ScheduleSavedState =
        ScheduleSavedState.fromJson(JsonParser.parse(text).objectOrThrow("$"))

    /** JSONEncoder().encode(state). */
    private fun encode(state: ScheduleSavedState): String = state.toJson().serialize(pretty = true)

    @Test
    fun testCompositeIDIsUnambiguousForBoundariesAndUnicode() {
        val keys = listOf(
            id("ab", "c"), id("a", "bc"), id("a:b", "c"), id("a", ":bc"),
            id("東京", "live"), id("東", "京live"), id("3:a", "live"), id("3", ":alive")
        )
        assertEquals(keys.size, keys.toSet().size)
        assertEquals("6:東京live", id("東京", "live"))
        assertEquals("8:festivallive", id("festival", "live"))
    }

    @Test
    fun testLiveGoodsAndAdditionalLiveSelectionsAreIndependentAcrossEvents() {
        val document = ScheduleDocument(events = listOf(event("first"), event("second")))
        ScheduleImporter.validate(document)
        val live = id("first", "live")
        val goods = id("first", "goods")
        val encore = id("first", "encore")
        val otherEventLive = id("second", "live")

        val liveOnly = ScheduleSavedState(document, setOf(live), isDemo = false)
        val restoredLive = decode(encode(liveOnly))
        assertEquals(setOf(live), restoredLive.favorites)
        assertFalse(restoredLive.favorites.contains(goods))
        assertFalse(restoredLive.favorites.contains(encore))
        assertFalse(restoredLive.favorites.contains(otherEventLive))

        val goodsOnly = ScheduleSavedState(document, setOf(goods), isDemo = false)
        assertEquals(setOf(goods), goodsOnly.favorites)
        val separatelySelected = ScheduleSavedState(document, setOf(goods, encore, otherEventLive), isDemo = false)
        assertEquals(setOf(goods, encore, otherEventLive), separatelySelected.favorites)
        assertFalse(separatelySelected.favorites.contains(live))
    }

    @Test
    fun testLegacyFavoritesExpandToEveryPreviouslyVisibleSessionAndRoundTripAsVersionTwo() {
        val document = ScheduleDocument(events = listOf(event("first"), event("second"), event("other", artistId = "other-artist")))
        val legacyData = data(document, setOf("shared-artist", "no-sessions", "stale-artist"), isDemo = true)

        val migrated = decode(legacyData)

        val expected = listOf("first", "second").flatMap { eventId ->
            listOf("live", "goods", "encore").map { id(eventId, it) }
        }.toSet()
        assertEquals(document, migrated.document)
        assertEquals(expected, migrated.favorites)
        assertTrue(migrated.isDemo)
        assertTrue(migrated.needsFavoriteMigration)
        assertEquals(expected, document.favoritesForArtists(setOf("shared-artist")))
        assertTrue(document.favoritesForArtists(setOf("no-sessions", "stale-artist")).isEmpty())

        val upgradedData = encode(migrated)
        val obj = JsonParser.parse(upgradedData) as? JsonObject ?: return fail("Saved state must be a JSON object")
        assertEquals(2L, (obj["favoritesVersion"] as? JsonNumber)?.longValue())
        val restored = decode(upgradedData)
        assertEquals(document, restored.document)
        assertEquals(expected, restored.favorites)
        assertTrue(restored.isDemo)
        assertFalse(restored.needsFavoriteMigration)
    }

    @Test
    fun testExplicitVersionOneMigratesAndVersionTwoDoesNotGuessFromIDContents() {
        val selected = id("first", "live")
        val document = ScheduleDocument(events = listOf(event("first", artistId = selected)))
        val legacy = decode(data(document, setOf(selected), version = 1))
        assertEquals(document.sessionFavoriteIds, legacy.favorites)
        assertTrue(legacy.needsFavoriteMigration)

        val current = decode(data(document, setOf(selected), version = 2))
        assertEquals(setOf(selected), current.favorites)
        assertFalse(current.needsFavoriteMigration)
    }

    @Test
    fun testVersionTwoEmptySelectionRemainsEmpty() {
        val document = ScheduleDocument(events = listOf(event("first")))
        val saved = decode(data(document, emptySet(), version = 2))
        assertTrue(saved.favorites.isEmpty())
        assertFalse(saved.needsFavoriteMigration)
    }

    @Test
    fun testVersionTwoPrunesMissingSessionsAndRequestsOneRepairWrite() {
        val document = ScheduleDocument(events = listOf(event("first")))
        val selected = id("first", "goods")
        val saved = decode(
            data(document, setOf(selected, id("first", "removed"), id("missing", "live")), version = 2)
        )
        assertEquals(setOf(selected), saved.favorites)
        assertTrue(saved.needsFavoriteMigration)

        val restored = decode(encode(saved))
        assertEquals(setOf(selected), restored.favorites)
        assertFalse(restored.needsFavoriteMigration)
    }

    @Test
    fun testUnknownFavoritesVersionIsRejected() {
        val document = ScheduleDocument(events = listOf(event("first")))
        val failure = runCatching { decode(data(document, emptySet(), version = 3)) }.exceptionOrNull()
            ?: return fail("Expected an unsupported-version decoding error, but decoding succeeded")
        // Swift throws DecodingError.dataCorrupted at codingPath [favoritesVersion]; the port reports
        // that category as ImportError invalidJSON with the field's JSON path.
        if (failure !is ImportError) return fail("Expected an unsupported-version decoding error, received $failure")
        assertEquals("import.error.invalidJSON", failure.key)
        assertEquals("favoritesVersion", failure.path.substringAfterLast('.'))
    }

    @Test
    fun testSavedDocumentIsValidatedBeforeFavoritesMigration() {
        val base = event("first")
        val invalid = base.copy(
            sessions = base.sessions.mapIndexed { index, session ->
                if (index == 1) session.copy(id = base.sessions[0].id) else session
            }
        )
        val document = ScheduleDocument(events = listOf(invalid))
        val failure = runCatching { decode(data(document, setOf("shared-artist"))) }.exceptionOrNull()
            ?: return fail("Expected the saved document to be rejected")
        assertEquals("import.error.duplicateID", (failure as? ImportError)?.key)
        assertEquals("$.events[0].sessions[1].id", (failure as? ImportError)?.path)
    }

    @Test
    fun testTimetableUpdateKeepsExistingSessionSelectionsAndPrunesRemovedOnes() {
        val original = ScheduleDocument(events = listOf(event("updated"), event("retained")))
        val base = event("updated")
        val withoutGoods = base.sessions.filter { it.id != "goods" }
        val newSession = withoutGoods[0].copy(id = "new-live")
        val updated = base.copy(
            artists = base.artists.mapIndexed { index, artist -> if (index == 0) artist.copy(name = "Renamed Group") else artist },
            sessions = withoutGoods + newSession
        )
        val merged = original.merging(ScheduleDocument(events = listOf(updated)))
        ScheduleImporter.validate(merged)
        val selection = setOf(id("updated", "live"), id("updated", "goods"), id("retained", "goods"))

        val saved = ScheduleSavedState(merged, selection, isDemo = false)

        assertEquals(setOf(id("updated", "live"), id("retained", "goods")), saved.favorites)
        assertFalse(saved.favorites.contains(id("updated", "new-live")))
        assertFalse(saved.needsFavoriteMigration)
    }
}
