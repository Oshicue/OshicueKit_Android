package com.oshicue.kit

import com.oshicue.kit.core.LedgerAnalytics
import com.oshicue.kit.core.LedgerDemo
import com.oshicue.kit.core.LedgerDocument
import com.oshicue.kit.core.LedgerGrouping
import com.oshicue.kit.core.LedgerImporter
import com.oshicue.kit.core.LedgerMetric
import com.oshicue.kit.core.ScheduleDates
import com.oshicue.kit.core.activeGroups
import com.oshicue.kit.core.activeMembers
import com.oshicue.kit.core.activeMemberships
import com.oshicue.kit.core.deletingGroup
import com.oshicue.kit.core.deletingMember
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LedgerDeletionTests {
    private val demo: LedgerDocument
        get() = LedgerDemo.document(ScheduleDates.parseTimestamp("2026-10-14T12:00:00+08:00")!!)

    @Test
    fun testOldBackupWithoutArchiveFieldsRemainsActive() {
        val json = LedgerImporter.encode(demo)
        assertFalse(json.contains("isArchived"))
        val decoded = LedgerImporter.parse(json)
        assertEquals(demo, decoded)
        assertEquals(decoded.groups, decoded.activeGroups)
        assertEquals(decoded.members, decoded.activeMembers)
        assertEquals(decoded.memberships, decoded.activeMemberships)
    }

    @Test
    fun testDeletingMemberAcrossGroupsPreservesHistoryAndTotals() {
        val original = demo
        val deleted = original.deletingMember("mio")
        LedgerImporter.validate(deleted)
        assertEquals(original.records, deleted.records)
        assertEquals(true, deleted.members.first { it.id == "mio" }.isArchived)
        assertTrue(deleted.memberships.filter { it.memberID == "mio" }.all { it.isArchived == true })
        assertEquals(setOf("rina", "sora"), deleted.activeMembers.map { it.id }.toSet())
        assertEquals(setOf("rina-luna", "sora-prism"), deleted.activeMemberships.map { it.id }.toSet())
        assertEquals(original.groups, deleted.activeGroups)
        assertEquals(
            LedgerAnalytics.summarize(original.records),
            LedgerAnalytics.summarize(deleted.records)
        )
        assertEquals(
            LedgerAnalytics.rankings(original.records, LedgerGrouping.memberCombined, LedgerMetric.quantity),
            LedgerAnalytics.rankings(deleted.records, LedgerGrouping.memberCombined, LedgerMetric.quantity)
        )
        assertEquals(deleted, deleted.deletingMember("mio"))
        assertEquals(original, original.deletingMember("missing"))
    }

    @Test
    fun testDeletingGroupKeepsSharedMemberAvailableElsewhere() {
        val original = demo
        val deleted = original.deletingGroup("luna")
        LedgerImporter.validate(deleted)
        assertEquals(original.records, deleted.records)
        assertEquals(true, deleted.groups.first { it.id == "luna" }.isArchived)
        assertEquals(setOf("prism"), deleted.activeGroups.map { it.id }.toSet())
        assertTrue(deleted.memberships.filter { it.groupID == "luna" }.all { it.isArchived == true })
        assertEquals(setOf("mio-prism", "sora-prism"), deleted.activeMemberships.map { it.id }.toSet())
        // A person whose former group was removed may be linked to a new group later.
        assertEquals(original.members, deleted.activeMembers)
        assertEquals(
            LedgerAnalytics.summarize(original.records),
            LedgerAnalytics.summarize(deleted.records)
        )
        assertEquals(deleted, deleted.deletingGroup("luna"))
        assertEquals(original, original.deletingGroup("missing"))
    }

    @Test
    fun testActiveMembershipsRequireAnActivePersonAndGroup() {
        var document = demo.copy(
            members = demo.members.toMutableList().also { it[0] = it[0].copy(isArchived = true) },
            groups = demo.groups.toMutableList().also { it[1] = it[1].copy(isArchived = true) }
        )
        // Imported backups can mark parents without marking every affiliation explicitly.
        assertTrue(document.memberships.all { it.isArchived == null })
        assertEquals(listOf("rina-luna"), document.activeMemberships.map { it.id })
        LedgerImporter.validate(document)
        document = document.copy(
            members = document.members.toMutableList().also { it[0] = it[0].copy(isArchived = false) },
            groups = document.groups.toMutableList().also { it[1] = it[1].copy(isArchived = false) },
            memberships = document.memberships.toMutableList().also { it[0] = it[0].copy(isArchived = true) }
        )
        assertEquals(setOf("mio-prism", "rina-luna", "sora-prism"), document.activeMemberships.map { it.id }.toSet())
    }

    @Test
    fun testArchiveMarkersRoundTripWithPurchaseSnapshots() {
        val deleted = demo.deletingGroup("luna").deletingMember("sora")
        val json = LedgerImporter.encode(deleted)
        assertTrue(json.contains("\"isArchived\" : true"))
        val decoded = LedgerImporter.parse(json)
        assertEquals(deleted, decoded)
        assertEquals(listOf("mio-prism"), decoded.activeMemberships.map { it.id })
    }

    @Test
    fun testHistoricalPurchaseCanStillBeEditedAfterMemberAndGroupDeletion() {
        var document = demo.deletingMember("mio").deletingGroup("luna")
        val original = document.records[0]
        document = document.copy(
            records = document.records.toMutableList().also {
                it[0] = it[0].copy(quantity = 9, note = "Corrected the quantity after the member was removed.")
            }
        )
        LedgerImporter.validate(document)
        assertEquals(original.unitPriceMinor, document.records[0].unitPriceMinor)
        assertEquals(original.memberNameSnapshot, document.records[0].memberNameSnapshot)
        assertEquals(original.groupNameSnapshot, document.records[0].groupNameSnapshot)
        assertEquals(document, LedgerImporter.parse(LedgerImporter.encode(document)))
    }

    @Test
    fun testImportCanRestoreCatalogEntriesWithoutDuplicatingPurchases() {
        val original = demo
        val archived = original.deletingMember("mio").deletingGroup("luna")
        val restored = archived.merging(original)
        assertEquals(original.groups.map { it.id }.toSet(), restored.activeGroups.map { it.id }.toSet())
        assertEquals(original.members.map { it.id }.toSet(), restored.activeMembers.map { it.id }.toSet())
        assertEquals(original.memberships.map { it.id }.toSet(), restored.activeMemberships.map { it.id }.toSet())
        assertEquals(original.records, restored.records)
    }
}
