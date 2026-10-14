package com.oshicue.kit

import com.oshicue.kit.core.LedgerAnalytics
import com.oshicue.kit.core.LedgerDates
import com.oshicue.kit.core.LedgerDemo
import com.oshicue.kit.core.LedgerImporter
import com.oshicue.kit.core.LedgerRecordDeletion
import com.oshicue.kit.core.LedgerRecordFilter
import com.oshicue.kit.core.activeMemberships
import com.oshicue.kit.core.deletingGroup
import com.oshicue.kit.core.deletingMember
import com.oshicue.kit.core.deletingRecords
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LedgerManagementTests {
    private val demo get() = LedgerDemo.document(LedgerDates.parse("2026-10-14")!!.atStartOfDay(java.time.ZoneOffset.UTC).toInstant())

    @Test
    fun testDateBoundsAreInclusiveAndCanBeUsedSeparately() {
        val records = demo.records
        assertEquals(
            setOf("demo-cheki", "demo-wide", "demo-rina", "demo-shared"),
            LedgerRecordFilter(startDate = "2026-10-13", endDate = "2026-10-14").matching(records).map { it.id }.toSet()
        )
        assertEquals(
            listOf("demo-phone"),
            LedgerRecordFilter(endDate = "2026-10-12").matching(records).map { it.id }
        )
        assertEquals(3, LedgerRecordFilter(startDate = "2026-10-14", endDate = "2026-10-14").matching(records).size)
    }

    @Test
    fun testAllConditionsCombineAndHistoricalCatalogStillMatches() {
        val archived = demo.deletingMember("mio").deletingGroup("luna")
        val filter = LedgerRecordFilter(
            startDate = "2026-10-14", endDate = "2026-10-14", memberID = "mio",
            groupID = "luna", productID = "cheki", currency = "JPY", eventID = "demo-fes", query = "mIO mini"
        )
        assertEquals(listOf("demo-cheki"), filter.matching(archived.records).map { it.id })
        assertTrue(filter.copy(groupID = "prism").matching(archived.records).isEmpty())
        assertTrue(filter.copy(currency = "CNY").matching(archived.records).isEmpty())
        assertTrue(filter.copy(requiresNoEvent = true).matching(archived.records).isEmpty())
    }

    @Test
    fun testNoEventSearchAndInvalidRangeNeverBroadenToAllRecords() {
        val records = demo.records
        assertEquals(
            setOf("demo-rina", "demo-shared", "demo-phone"),
            LedgerRecordFilter(requiresNoEvent = true).matching(records).map { it.id }.toSet()
        )
        assertTrue(LedgerRecordFilter(startDate = "2026-10-15", endDate = "2026-10-14").matching(records).isEmpty())
        assertTrue(LedgerRecordFilter(startDate = "2026-02-30").matching(records).isEmpty())
        assertTrue(LedgerRecordFilter(query = "Mio missing").matching(records).isEmpty())
        var noted = records
        noted = noted.toMutableList().also { it[0] = it[0].copy(note = "Birthday gift") }
        assertEquals(listOf("demo-cheki"), LedgerRecordFilter(query = "gift mio").matching(noted).map { it.id })
        assertEquals(records, LedgerRecordFilter(query = "  \n ").matching(records))
    }

    @Test
    fun testConfirmationCountsOnlySelectedExistingIDsAndSeparatesCurrencies() {
        val deletion = LedgerRecordDeletion(demo.records, setOf("demo-cheki", "demo-phone", "missing"))
        assertEquals(setOf("demo-cheki", "demo-phone"), deletion.recordIDs)
        assertEquals(2, deletion.recordCount)
        assertEquals(7, deletion.quantity)
        assertEquals(mapOf("JPY" to 10000L, "CNY" to 16000L), deletion.amountsByCurrency)
        val empty = LedgerRecordDeletion(demo.records, emptySet())
        assertEquals(0, empty.recordCount)
        assertTrue(empty.amountsByCurrency.isEmpty())
    }

    @Test
    fun testBatchDeletionPreservesAllCatalogAndUnselectedSnapshots() {
        val original = demo
        val result = original.deletingRecords(setOf("demo-cheki", "demo-phone", "missing"))
        assertEquals(original.records.filter { it.id !in setOf("demo-cheki", "demo-phone") }, result.records)
        assertEquals(original.groups, result.groups)
        assertEquals(original.members, result.members)
        assertEquals(original.memberships, result.memberships)
        assertEquals(original.products, result.products)
        assertEquals(original.events, result.events)
        LedgerImporter.validate(result)
        assertEquals(result, LedgerImporter.parse(LedgerImporter.encode(result)))
        assertEquals(9, LedgerAnalytics.summarize(result.records).quantity)
        assertEquals(result, result.deletingRecords(setOf("demo-cheki", "demo-phone")))
        assertEquals(original, original.deletingRecords(emptySet()))
        assertEquals(5, original.records.size)
    }

    @Test
    fun testDeletingAllPurchasesLeavesReusableCatalogAndEvents() {
        val original = demo
        val result = original.deletingRecords(original.records.map { it.id }.toSet())
        assertTrue(result.records.isEmpty())
        assertEquals(original.activeMemberships, result.activeMemberships)
        assertEquals(original.events, result.events)
        assertEquals(original.products, result.products)
        LedgerImporter.validate(result)
    }
}
