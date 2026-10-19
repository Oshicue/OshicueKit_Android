package com.oshicue.kit.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneOffset

/** JVM port of the iOS Tests/LedgerFavoritesTests.swift suite. */
class LedgerFavoritesTests {
    @Before
    fun pinLanguage() {
        // Resolving "System" reads android.os.LocaleList, which is unavailable on the JVM.
        L10n.setLanguage(AppLanguage.En)
    }

    // Swift's LedgerDates.parse returns noon UTC on the civil day; the Kotlin port returns a LocalDate.
    private val demo: LedgerDocument
        get() = LedgerDemo.document(LedgerDates.parse("2026-06-07")!!.atTime(12, 0).toInstant(ZoneOffset.UTC))

    @Test
    fun testExistingSchemaOneBackupDecodesMissingFavoriteAsFalse() {
        val text = LedgerImporter.encode(demo)
        assertFalse(text.contains("isFavorite"))
        val restored = LedgerImporter.parse(text)
        assertEquals(1, restored.schemaVersion)
        assertTrue(restored.members.all { it.isFavorite == null && !it.isFavorited })
        assertTrue(restored.favoriteMembers.isEmpty())
        assertEquals(demo.activeMemberships.firstOrNull()?.id, restored.preferredRecordMembership()?.id)
    }

    @Test
    fun testFavoriteTrueFalseAndMissingValuesRoundTripWithoutSchemaChange() {
        var document = demo
        document = document.withMember(0) { it.copy(isFavorite = true) }
        document = document.withMember(1) { it.copy(isFavorite = false) }
        val text = LedgerImporter.encode(document)
        val restored = LedgerImporter.parse(text)
        assertEquals(document, restored)
        assertEquals(1, restored.schemaVersion)
        assertEquals(listOf("mio"), restored.favoriteMembers.map { it.id })
        assertEquals(listOf("mio", "rina", "sora"), restored.membersForSelection.map { it.id })
        assertEquals(false, restored.members[1].isFavorite)
        assertNull(restored.members[2].isFavorite)
    }

    @Test
    fun testInvalidFavoriteTypeIdentifiesMemberField() {
        val root = JsonParser.parse(LedgerImporter.encode(demo)) as JsonObject
        val members = root["members"] as JsonArray
        (members.items[0] as JsonObject).put("isFavorite", "true")
        root["members"] = members
        val text = root.serialize()
        val error = assertThrows(Throwable::class.java) { LedgerImporter.parse(text) }
        assertEquals("ledger.error.fieldType", (error as? LedgerError)?.key)
        assertEquals("$.members[0].isFavorite", (error as? LedgerError)?.path)
    }

    @Test
    fun testMergeUpdatesSharedFavoritePreferenceAndPreservesHistory() {
        var incoming = demo.withMember(0) { it.copy(isFavorite = true) }
        val merged = demo.merging(incoming)
        assertEquals(listOf("mio-luna", "mio-prism"), merged.favoriteMemberships.map { it.id })
        assertEquals(demo.records, merged.records)
        assertEquals(merged, merged.merging(incoming))
        incoming = incoming.withMember(0) { it.copy(isFavorite = false) }
        val unfavorited = merged.merging(incoming)
        assertTrue(unfavorited.favoriteMemberships.isEmpty())
        assertEquals(demo.records, unfavorited.records)
    }

    @Test
    fun testArchivedPersonGroupAndMembershipAreExcludedFromFavoriteShortcuts() {
        var document = demo.withMember(0) { it.copy(isFavorite = true) }
        val removedMember = document.deletingMember("mio")
        assertTrue(removedMember.favoriteMembers.isEmpty())
        assertTrue(removedMember.favoriteMemberships.isEmpty())
        assertEquals(true, removedMember.members.firstOrNull { it.id == "mio" }?.isFavorite)

        val removedGroup = document.deletingGroup("luna")
        assertEquals(listOf("mio"), removedGroup.favoriteMembers.map { it.id })
        assertEquals(listOf("mio-prism"), removedGroup.favoriteMemberships.map { it.id })
        assertEquals("mio-prism", removedGroup.preferredRecordMembership()?.id)

        document = document.withMembership(0) { it.copy(isArchived = true) }
        assertEquals(listOf("mio-prism"), document.favoriteMemberships.map { it.id })
        document = document.withMembership(1) { it.copy(isArchived = true) }
        assertTrue(document.favoriteMemberships.isEmpty())
        assertEquals("rina-luna", document.preferredRecordMembership()?.id)
    }

    @Test
    fun testNewPurchaseUsesFavoriteAffiliationAndKeepsGroupPricesDistinct() {
        var document = demo.withMember(1) { it.copy(isFavorite = true) }
        assertEquals("rina", document.membersForSelection.firstOrNull()?.id)
        assertEquals("rina-luna", document.preferredRecordMembership()?.id)
        document = document.withMember(0) { it.copy(isFavorite = true) }
        assertEquals(listOf("mio-luna", "mio-prism", "rina-luna"), document.favoriteMemberships.map { it.id })
        assertEquals("mio-luna", document.preferredRecordMembership()?.id)
        assertEquals(2000L, document.favoriteMemberships.firstOrNull { it.id == "mio-luna" }?.prices?.firstOrNull()?.unitPriceMinor)
        assertEquals(2500L, document.favoriteMemberships.firstOrNull { it.id == "mio-prism" }?.prices?.firstOrNull()?.unitPriceMinor)
        assertEquals(listOf("luna", "prism"), document.recordMembershipGroups().map { it.id })
        assertEquals(listOf("mio-luna", "rina-luna"), document.recordMembershipChoicesIn("luna").map { it.id })
        assertEquals(listOf("mio-prism", "sora-prism"), document.recordMembershipChoicesIn("prism").map { it.id })
    }

    @Test
    fun testFavoriteOrderingIsStableWhenCatalogArrayOrderChanges() {
        var document = demo
        document = document.withMember(0) { it.copy(isFavorite = true) }
        document = document.withMember(1) { it.copy(isFavorite = true) }
        val favorites = document.favoriteMemberships.map { it.id }
        val members = document.membersForSelection.map { it.id }
        val groups = document.recordMembershipGroups().map { it.id }
        document = document.copy(members = document.members.reversed())
        document = document.copy(memberships = document.memberships.reversed())
        document = document.copy(groups = document.groups.reversed())
        assertEquals(favorites, document.favoriteMemberships.map { it.id })
        assertEquals(members, document.membersForSelection.map { it.id })
        assertEquals(groups, document.recordMembershipGroups().map { it.id })
        assertEquals("mio-luna", document.preferredRecordMembership()?.id)
    }

    @Test
    fun testEditingPurchaseKeepsArchivedOriginalOutsideFavoritesAndWithinItsGroup() {
        var document = demo
        document = document.withMember(1) { it.copy(isFavorite = true) }
        document = document.withMember(0) { it.copy(isFavorite = true) }
        document = document.deletingMember("mio").deletingGroup("luna")
        val record = requireNotNull(document.records.firstOrNull { it.id == "demo-cheki" }) { "demo-cheki is missing" }
        assertEquals("mio-luna", document.preferredRecordMembership(editing = record)?.id)
        assertTrue(document.favoriteMemberships.isEmpty())
        assertEquals(listOf("sora-prism", "mio-luna"), document.recordMembershipChoices(editing = record).map { it.id })
        assertEquals(listOf("luna", "prism"), document.recordMembershipGroups(editing = record).map { it.id })
        assertEquals(listOf("mio-luna"), document.recordMembershipChoicesIn("luna", editing = record).map { it.id })
        assertEquals(1, document.recordMembershipChoices(editing = record).filter { it.id == "mio-luna" }.size)
        assertEquals(demo.records, document.records)
        LedgerImporter.validate(document)
    }

    private fun <T> List<T>.replacing(index: Int, transform: (T) -> T): List<T> =
        mapIndexed { position, item -> if (position == index) transform(item) else item }

    private fun LedgerDocument.withMember(index: Int, transform: (LedgerMember) -> LedgerMember): LedgerDocument =
        copy(members = members.replacing(index, transform))

    private fun LedgerDocument.withMembership(index: Int, transform: (LedgerMembership) -> LedgerMembership): LedgerDocument =
        copy(memberships = memberships.replacing(index, transform))
}
