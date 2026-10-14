package com.oshicue.kit

import com.oshicue.kit.core.JObj
import com.oshicue.kit.core.JsonText
import com.oshicue.kit.core.KitJson
import com.oshicue.kit.core.LedgerDemo
import com.oshicue.kit.core.LedgerError
import com.oshicue.kit.core.LedgerImporter
import com.oshicue.kit.core.activeMemberships
import com.oshicue.kit.core.deletingGroup
import com.oshicue.kit.core.deletingMember
import com.oshicue.kit.core.favoriteMembers
import com.oshicue.kit.core.favoriteMemberships
import com.oshicue.kit.core.membersForSelection
import com.oshicue.kit.core.preferredRecordMembership
import com.oshicue.kit.core.recordMembershipChoices
import com.oshicue.kit.core.recordMembershipGroups
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LedgerFavoritesTests {
    private val demo get() = LedgerDemo.document(com.oshicue.kit.core.LedgerDates.parse("2026-06-07")!!.atStartOfDay(java.time.ZoneOffset.UTC).toInstant())

    @Test
    fun testExistingSchemaOneBackupDecodesMissingFavoriteAsFalse() {
        val text = LedgerImporter.encode(demo)
        assertTrue(!text.contains("isFavorite"))
        val restored = LedgerImporter.parse(text)
        assertEquals(1, restored.schemaVersion)
        assertTrue(restored.members.all { it.isFavorite == null && !it.isFavorited })
        assertTrue(restored.favoriteMembers.isEmpty())
        assertEquals(demo.activeMemberships.firstOrNull()?.id, restored.preferredRecordMembership()?.id)
    }

    @Test
    fun testFavoriteTrueFalseAndMissingValuesRoundTripWithoutSchemaChange() {
        var document = demo
        document = document.copy(
            members = document.members.toMutableList().also {
                it[0] = it[0].copy(isFavorite = true)
                it[1] = it[1].copy(isFavorite = false)
            }
        )
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
        val root = KitJson.parse(LedgerImporter.encode(demo))!!.jsonObject
        val map = root.toMutableMap()
        val members = (map["members"] as kotlinx.serialization.json.JsonArray).map { it.jsonObject.toMutableMap() }
        members[0]["isFavorite"] = JsonPrimitive("true")
        map["members"] = kotlinx.serialization.json.JsonArray(members.map { JsonObject(it) })
        val text = JsonText.encode(JsonObject(map))
        val error = assertFailsWith<LedgerError> { LedgerImporter.parse(text) }
        assertEquals("ledger.error.fieldType", error.key)
        assertEquals("$.members[0].isFavorite", error.path)
    }

    @Test
    fun testMergeUpdatesSharedFavoritePreferenceAndPreservesHistory() {
        var incoming = demo
        incoming = incoming.copy(members = incoming.members.toMutableList().also { it[0] = it[0].copy(isFavorite = true) })
        val merged = demo.merging(incoming)
        assertEquals(listOf("mio-luna", "mio-prism"), merged.favoriteMemberships.map { it.id })
        assertEquals(demo.records, merged.records)
        assertEquals(merged, merged.merging(incoming))
        incoming = incoming.copy(members = incoming.members.toMutableList().also { it[0] = it[0].copy(isFavorite = false) })
        val unfavorited = merged.merging(incoming)
        assertTrue(unfavorited.favoriteMemberships.isEmpty())
        assertEquals(demo.records, unfavorited.records)
    }

    @Test
    fun testArchivedPersonGroupAndMembershipAreExcludedFromFavoriteShortcuts() {
        var document = demo.copy(members = demo.members.toMutableList().also { it[0] = it[0].copy(isFavorite = true) })
        val removedMember = document.deletingMember("mio")
        assertTrue(removedMember.favoriteMembers.isEmpty())
        assertTrue(removedMember.favoriteMemberships.isEmpty())
        assertEquals(true, removedMember.members.first { it.id == "mio" }.isFavorite)

        val removedGroup = document.deletingGroup("luna")
        assertEquals(listOf("mio"), removedGroup.favoriteMembers.map { it.id })
        assertEquals(listOf("mio-prism"), removedGroup.favoriteMemberships.map { it.id })
        assertEquals("mio-prism", removedGroup.preferredRecordMembership()?.id)

        document = document.copy(
            memberships = document.memberships.toMutableList().also { it[0] = it[0].copy(isArchived = true) }
        )
        assertEquals(listOf("mio-prism"), document.favoriteMemberships.map { it.id })
        document = document.copy(
            memberships = document.memberships.toMutableList().also { it[1] = it[1].copy(isArchived = true) }
        )
        assertTrue(document.favoriteMemberships.isEmpty())
        assertEquals("rina-luna", document.preferredRecordMembership()?.id)
    }

    @Test
    fun testNewPurchaseUsesFavoriteAffiliationAndKeepsGroupPricesDistinct() {
        var document = demo.copy(members = demo.members.toMutableList().also { it[1] = it[1].copy(isFavorite = true) })
        assertEquals("rina", document.membersForSelection.first().id)
        assertEquals("rina-luna", document.preferredRecordMembership()?.id)
        document = document.copy(members = document.members.toMutableList().also { it[0] = it[0].copy(isFavorite = true) })
        assertEquals(listOf("mio-luna", "mio-prism", "rina-luna"), document.favoriteMemberships.map { it.id })
        assertEquals("mio-luna", document.preferredRecordMembership()?.id)
        assertEquals(2000L, document.favoriteMemberships.first { it.id == "mio-luna" }.prices.first().unitPriceMinor)
        assertEquals(2500L, document.favoriteMemberships.first { it.id == "mio-prism" }.prices.first().unitPriceMinor)
        assertEquals(listOf("luna", "prism"), document.recordMembershipGroups().map { it.id })
        assertEquals(listOf("mio-luna", "rina-luna"), document.recordMembershipChoices(inGroup = "luna").map { it.id })
        assertEquals(listOf("mio-prism", "sora-prism"), document.recordMembershipChoices(inGroup = "prism").map { it.id })
    }

    @Test
    fun testFavoriteOrderingIsStableWhenCatalogArrayOrderChanges() {
        var document = demo.copy(
            members = demo.members.toMutableList().also {
                it[0] = it[0].copy(isFavorite = true)
                it[1] = it[1].copy(isFavorite = true)
            }
        )
        val favorites = document.favoriteMemberships.map { it.id }
        val members = document.membersForSelection.map { it.id }
        val groups = document.recordMembershipGroups().map { it.id }
        document = document.copy(
            members = document.members.reversed(),
            memberships = document.memberships.reversed(),
            groups = document.groups.reversed()
        )
        assertEquals(favorites, document.favoriteMemberships.map { it.id })
        assertEquals(members, document.membersForSelection.map { it.id })
        assertEquals(groups, document.recordMembershipGroups().map { it.id })
        assertEquals("mio-luna", document.preferredRecordMembership()?.id)
    }

    @Test
    fun testEditingPurchaseKeepsArchivedOriginalOutsideFavoritesAndWithinItsGroup() {
        var document = demo.copy(
            members = demo.members.toMutableList().also {
                it[1] = it[1].copy(isFavorite = true)
                it[0] = it[0].copy(isFavorite = true)
            }
        )
        document = document.deletingMember("mio").deletingGroup("luna")
        val record = document.records.first { it.id == "demo-cheki" }
        assertEquals("mio-luna", document.preferredRecordMembership(editing = record)?.id)
        assertTrue(document.favoriteMemberships.isEmpty())
        assertEquals(listOf("sora-prism", "mio-luna"), document.recordMembershipChoices(editing = record).map { it.id })
        assertEquals(listOf("luna", "prism"), document.recordMembershipGroups(editing = record).map { it.id })
        assertEquals(listOf("mio-luna"), document.recordMembershipChoices(inGroup = "luna", editing = record).map { it.id })
        assertEquals(1, document.recordMembershipChoices(editing = record).filter { it.id == "mio-luna" }.size)
        assertEquals(demo.records, document.records)
        LedgerImporter.validate(document)
    }
}
