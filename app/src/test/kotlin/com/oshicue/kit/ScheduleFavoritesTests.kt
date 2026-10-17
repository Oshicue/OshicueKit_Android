package com.oshicue.kit

import com.oshicue.kit.core.Artist
import com.oshicue.kit.core.FestivalEvent
import com.oshicue.kit.core.FestivalSession
import com.oshicue.kit.core.ImportError
import com.oshicue.kit.core.JObj
import com.oshicue.kit.core.KitJson
import com.oshicue.kit.core.ScheduleDates
import com.oshicue.kit.core.ScheduleDocument
import com.oshicue.kit.core.ScheduleFavorites
import com.oshicue.kit.core.ScheduleImporter
import com.oshicue.kit.core.ScheduleSavedState
import com.oshicue.kit.core.SessionKind
import com.oshicue.kit.core.Venue
import com.oshicue.kit.core.favoritesForArtistIDs
import com.oshicue.kit.core.sessionFavoriteIDs
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScheduleFavoritesTests {
    private fun event(id: String, artistID: String = "shared-artist"): FestivalEvent {
        val start = ScheduleDates.parseTimestamp("2026-10-14T12:00:00+09:00")!!
        return FestivalEvent(
            id = id,
            name = "Festival $id",
            city = "Tokyo",
            timeZone = "Asia/Tokyo",
            venues = listOf(Venue("hall", "Hall", "#78C9EB")),
            artists = listOf(Artist(artistID, "Shared Group"), Artist("no-sessions", "No Sessions")),
            sessions = listOf("live", "goods", "encore").mapIndexed { index, sessionID ->
                FestivalSession(
                    id = sessionID,
                    artistID = artistID,
                    venueID = "hall",
                    kind = if (index == 1) SessionKind.goods else SessionKind.live,
                    startsAt = start.plusSeconds(index * 3600L),
                    endsAt = start.plusSeconds(index * 3600L + 1800L)
                )
            }
        )
    }

    private fun id(eventID: String, sessionID: String): String = ScheduleFavorites.id(eventID, sessionID)

    private fun stateData(
        documentElement: JsonElement,
        favorites: Set<String>,
        version: Int? = null,
        isDemo: Boolean = false
    ): JsonElement {
        val root = JObj()
        root.putElement("document", documentElement)
        root.putStringSet("favorites", favorites)
        root.put("isDemo", isDemo)
        version?.let { root.put("favoritesVersion", it) }
        return root.build()
    }

    private fun data(document: ScheduleDocument, favorites: Set<String>, version: Int? = null, isDemo: Boolean = false): JsonElement =
        stateData(KitJson.parse(ScheduleImporter.encode(document))!!, favorites, version, isDemo)

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

        val liveOnly = ScheduleSavedState(document = document, favorites = setOf(live), isDemo = false)
        val restoredLive = ScheduleSavedState.decode(liveOnly.encode())
        assertEquals(setOf(live), restoredLive.favorites)
        assertFalse(restoredLive.favorites.contains(goods))
        assertFalse(restoredLive.favorites.contains(encore))
        assertFalse(restoredLive.favorites.contains(otherEventLive))

        val goodsOnly = ScheduleSavedState(document = document, favorites = setOf(goods), isDemo = false)
        assertEquals(setOf(goods), goodsOnly.favorites)
        val separatelySelected = ScheduleSavedState(document = document, favorites = setOf(goods, encore, otherEventLive), isDemo = false)
        assertEquals(setOf(goods, encore, otherEventLive), separatelySelected.favorites)
        assertFalse(separatelySelected.favorites.contains(live))
    }

    @Test
    fun testLegacyFavoritesExpandToEveryPreviouslyVisibleSessionAndRoundTripAsVersionTwo() {
        val document = ScheduleDocument(events = listOf(event("first"), event("second"), event("other", artistID = "other-artist")))
        val legacyData = data(document, setOf("shared-artist", "no-sessions", "stale-artist"), isDemo = true)

        val migrated = ScheduleSavedState.decode(legacyData)

        val expected = setOf("first", "second").flatMap { eventID ->
            listOf("live", "goods", "encore").map { id(eventID, it) }
        }.toSet()
        assertEquals(document, migrated.document)
        assertEquals(expected, migrated.favorites)
        assertTrue(migrated.isDemo)
        assertTrue(migrated.needsFavoriteMigration)
        assertEquals(expected, document.favoritesForArtistIDs(setOf("shared-artist")))
        assertTrue(document.favoritesForArtistIDs(setOf("no-sessions", "stale-artist")).isEmpty())

        val upgradedData = migrated.encode()
        assertEquals(2, (upgradedData["favoritesVersion"] as? JsonPrimitive)?.content?.toInt())
        val restored = ScheduleSavedState.decode(upgradedData)
        assertEquals(document, restored.document)
        assertEquals(expected, restored.favorites)
        assertTrue(restored.isDemo)
        assertFalse(restored.needsFavoriteMigration)
    }

    @Test
    fun testExplicitVersionOneMigratesAndVersionTwoDoesNotGuessFromIDContents() {
        val selected = id("first", "live")
        val document = ScheduleDocument(events = listOf(event("first", artistID = selected)))
        val legacy = ScheduleSavedState.decode(data(document, setOf(selected), version = 1))
        assertEquals(document.sessionFavoriteIDs, legacy.favorites)
        assertTrue(legacy.needsFavoriteMigration)

        val current = ScheduleSavedState.decode(data(document, setOf(selected), version = 2))
        assertEquals(setOf(selected), current.favorites)
        assertFalse(current.needsFavoriteMigration)
    }

    @Test
    fun testVersionTwoEmptySelectionRemainsEmpty() {
        val document = ScheduleDocument(events = listOf(event("first")))
        val saved = ScheduleSavedState.decode(data(document, emptySet(), version = 2))
        assertTrue(saved.favorites.isEmpty())
        assertFalse(saved.needsFavoriteMigration)
    }

    @Test
    fun testVersionTwoPrunesMissingSessionsAndRequestsOneRepairWrite() {
        val document = ScheduleDocument(events = listOf(event("first")))
        val selected = id("first", "goods")
        val saved = ScheduleSavedState.decode(
            data(document, setOf(selected, id("first", "removed"), id("missing", "live")), version = 2)
        )
        assertEquals(setOf(selected), saved.favorites)
        assertTrue(saved.needsFavoriteMigration)

        val restored = ScheduleSavedState.decode(saved.encode())
        assertEquals(setOf(selected), restored.favorites)
        assertFalse(restored.needsFavoriteMigration)
    }

    @Test
    fun testUnknownFavoritesVersionIsRejected() {
        val document = ScheduleDocument(events = listOf(event("first")))
        val error = assertFailsWith<IllegalStateException> {
            ScheduleSavedState.decode(data(document, emptySet(), version = 3))
        }
        assertTrue(error.message?.contains("favorites version") == true)
    }

    @Test
    fun testSavedDocumentIsValidatedBeforeFavoritesMigration() {
        val invalidElement = KitJson.parse(
            """
            {"schemaVersion":1,"events":[{"id":"first","name":"Festival first","city":"Tokyo","timeZone":"Asia/Tokyo",
            "venues":[{"id":"hall","name":"Hall","color":"#78C9EB"}],
            "artists":[{"id":"shared-artist","name":"Shared Group"}],
            "sessions":[{"id":"live","artistID":"shared-artist","venueID":"hall","kind":"live",
            "startsAt":"2026-10-14T12:00:00+09:00","endsAt":"2026-10-14T12:30:00+09:00"},
            {"id":"live","artistID":"shared-artist","venueID":"hall","kind":"goods",
            "startsAt":"2026-10-14T13:00:00+09:00","endsAt":"2026-10-14T13:30:00+09:00"}]}]}
            """.trimIndent()
        )!!
        val error = assertFailsWith<ImportError> {
            ScheduleSavedState.decode(stateData(invalidElement, setOf("shared-artist")))
        }
        assertEquals("import.error.duplicateID", error.key)
        assertEquals("$.events[0].sessions[1].id", error.path)
    }

    @Test
    fun testTimetableUpdateKeepsExistingSessionSelectionsAndPrunesRemovedOnes() {
        val original = ScheduleDocument(events = listOf(event("updated"), event("retained")))
        val source = event("updated")
        val remainingSessions = source.sessions.filterNot { it.id == "goods" }.toMutableList()
        remainingSessions.add(remainingSessions[0].copy(id = "new-live"))
        val updated = source.copy(
            artists = source.artists.mapIndexed { index, artist ->
                if (index == 0) artist.copy(name = "Renamed Group") else artist
            },
            sessions = remainingSessions
        )
        val merged = original.merging(ScheduleDocument(events = listOf(updated)))
        ScheduleImporter.validate(merged)
        val selection = setOf(id("updated", "live"), id("updated", "goods"), id("retained", "goods"))

        val saved = ScheduleSavedState(document = merged, favorites = selection, isDemo = false)

        assertEquals(setOf(id("updated", "live"), id("retained", "goods")), saved.favorites)
        assertFalse(saved.favorites.contains(id("updated", "new-live")))
        assertFalse(saved.needsFavoriteMigration)
    }
}
