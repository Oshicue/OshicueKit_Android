package com.oshicue.kit.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** JVM port of the iOS Tests/LedgerDeletionTests.swift suite. */
class LedgerDeletionTests {
    @Before
    fun pinLanguage() {
        // Resolving "System" reads android.os.LocaleList, which is unavailable on the JVM.
        L10n.setLanguage(AppLanguage.En)
    }

    private val demo: LedgerDocument
        get() = LedgerDemo.document(KitDate.parseTimestamp("2026-10-14T12:00:00+08:00")!!)

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
        assertEquals(true, deleted.members.firstOrNull { it.id == "mio" }?.isArchived)
        assertTrue(deleted.memberships.filter { it.memberId == "mio" }.all { it.isArchived == true })
        assertEquals(setOf("rina", "sora"), deleted.activeMembers.map { it.id }.toSet())
        assertEquals(setOf("rina-luna", "sora-prism"), deleted.activeMemberships.map { it.id }.toSet())
        assertEquals(original.groups, deleted.activeGroups)
        assertSameSummary(LedgerAnalytics.summarize(original.records), LedgerAnalytics.summarize(deleted.records))
        assertEquals(
            LedgerAnalytics.rankings(original.records, LedgerGrouping.MemberCombined, LedgerMetric.Quantity),
            LedgerAnalytics.rankings(deleted.records, LedgerGrouping.MemberCombined, LedgerMetric.Quantity)
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
        assertEquals(true, deleted.groups.firstOrNull { it.id == "luna" }?.isArchived)
        assertEquals(setOf("prism"), deleted.activeGroups.map { it.id }.toSet())
        assertTrue(deleted.memberships.filter { it.groupId == "luna" }.all { it.isArchived == true })
        assertEquals(setOf("mio-prism", "sora-prism"), deleted.activeMemberships.map { it.id }.toSet())
        // A person whose former group was removed may be linked to a new group later.
        assertEquals(original.members, deleted.activeMembers)
        assertSameSummary(LedgerAnalytics.summarize(original.records), LedgerAnalytics.summarize(deleted.records))
        assertEquals(deleted, deleted.deletingGroup("luna"))
        assertEquals(original, original.deletingGroup("missing"))
    }

    @Test
    fun testActiveMembershipsRequireAnActivePersonAndGroup() {
        var document = demo
        document = document.copy(members = document.members.replacing(0) { it.copy(isArchived = true) })
        document = document.copy(groups = document.groups.replacing(1) { it.copy(isArchived = true) })
        // Imported backups can mark parents without marking every affiliation explicitly.
        assertTrue(document.memberships.all { it.isArchived == null })
        assertEquals(listOf("rina-luna"), document.activeMemberships.map { it.id })
        LedgerImporter.validate(document)
        document = document.copy(members = document.members.replacing(0) { it.copy(isArchived = false) })
        document = document.copy(groups = document.groups.replacing(1) { it.copy(isArchived = false) })
        document = document.copy(memberships = document.memberships.replacing(0) { it.copy(isArchived = true) })
        assertEquals(setOf("mio-prism", "rina-luna", "sora-prism"), document.activeMemberships.map { it.id }.toSet())
    }

    @Test
    fun testArchiveMarkersRoundTripWithPurchaseSnapshots() {
        val deleted = demo.deletingGroup("luna").deletingMember("sora")
        val json = LedgerImporter.encode(deleted)
        assertTrue(
            "Expected JSONEncoder pretty-print separator \"isArchived\" : true in:\n" + json.lineSequence().filter { "isArchived" in it }.joinToString("\n"),
            json.contains("\"isArchived\" : true")
        )
        val decoded = LedgerImporter.parse(json)
        assertEquals(deleted, decoded)
        assertEquals(listOf("mio-prism"), decoded.activeMemberships.map { it.id })
    }

    @Test
    fun testHistoricalPurchaseCanStillBeEditedAfterMemberAndGroupDeletion() {
        var document = demo.deletingMember("mio").deletingGroup("luna")
        val original = document.records[0]
        document = document.copy(records = document.records.replacing(0) {
            it.copy(quantity = 9, note = "Corrected the quantity after the member was removed.")
        })
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

    /**
     * Swift's LedgerSummary is Equatable; the Kotlin LedgerSummary is a plain class with
     * reference equality, so compare every stored field instead.
     */
    private fun assertSameSummary(expected: LedgerSummary, actual: LedgerSummary) {
        assertEquals(expected.quantity, actual.quantity)
        assertEquals(expected.amountsByCurrency, actual.amountsByCurrency)
        assertEquals(expected.productRows, actual.productRows)
        assertEquals(expected.memberCount, actual.memberCount)
    }

    private fun <T> List<T>.replacing(index: Int, transform: (T) -> T): List<T> =
        mapIndexed { position, item -> if (position == index) transform(item) else item }
}
