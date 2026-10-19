package com.oshicue.kit.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneOffset

/** JVM port of the iOS Tests/LedgerManagementTests.swift suite. */
class LedgerManagementTests {
    @Before
    fun pinLanguage() {
        // Resolving "System" reads android.os.LocaleList, which is unavailable on the JVM.
        L10n.setLanguage(AppLanguage.En)
    }

    // Swift's LedgerDates.parse returns noon UTC on the civil day; the Kotlin port returns a LocalDate.
    private val demo: LedgerDocument
        get() = LedgerDemo.document(LedgerDates.parse("2026-10-14")!!.atTime(12, 0).toInstant(ZoneOffset.UTC))

    @Test
    fun testDateBoundsAreInclusiveAndCanBeUsedSeparately() {
        val records = demo.records
        assertEquals(
            setOf("demo-cheki", "demo-wide", "demo-rina", "demo-shared"),
            LedgerRecordFilter(startDate = "2026-10-13", endDate = "2026-10-14").matching(records).map { it.id }.toSet()
        )
        assertEquals(listOf("demo-phone"), LedgerRecordFilter(endDate = "2026-10-12").matching(records).map { it.id })
        assertEquals(3, LedgerRecordFilter(startDate = "2026-10-14", endDate = "2026-10-14").matching(records).size)
    }

    @Test
    fun testAllConditionsCombineAndHistoricalCatalogStillMatches() {
        val archived = demo.deletingMember("mio").deletingGroup("luna")
        val filter = LedgerRecordFilter(
            startDate = "2026-10-14", endDate = "2026-10-14", memberId = "mio",
            groupId = "luna", productId = "cheki", currency = "JPY", eventId = "demo-fes", query = "mIO mini"
        )
        assertEquals(listOf("demo-cheki"), filter.matching(archived.records).map { it.id })
        var different = filter.copy(groupId = "prism")
        assertTrue(different.matching(archived.records).isEmpty())
        different = filter.copy(currency = "CNY")
        assertTrue(different.matching(archived.records).isEmpty())
        different = filter.copy(requiresNoEvent = true)
        assertTrue(different.matching(archived.records).isEmpty())
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
        val noted = records.mapIndexed { index, record -> if (index == 0) record.copy(note = "Birthday gift") else record }
        assertEquals(listOf("demo-cheki"), LedgerRecordFilter(query = "gift mio").matching(noted).map { it.id })
        assertEquals(records, LedgerRecordFilter(query = "  \n ").matching(records))
    }

    @Test
    fun testConfirmationCountsOnlySelectedExistingIDsAndSeparatesCurrencies() {
        val deletion = LedgerRecordDeletion(demo.records, selectedIds = setOf("demo-cheki", "demo-phone", "missing"))
        assertEquals(setOf("demo-cheki", "demo-phone"), deletion.recordIds)
        assertEquals(2, deletion.recordCount)
        assertEquals(7, deletion.quantity)
        assertEquals(mapOf("JPY" to 10000L, "CNY" to 16000L), deletion.amountsByCurrency)
        val empty = LedgerRecordDeletion(demo.records, selectedIds = emptySet())
        assertEquals(0, empty.recordCount)
        assertEquals(emptyMap<String, Long>(), empty.amountsByCurrency)
    }

    @Test
    fun testBatchDeletionPreservesAllCatalogAndUnselectedSnapshots() {
        val original = demo
        val result = original.deletingRecords(setOf("demo-cheki", "demo-phone", "missing"))
        assertEquals(original.records.filter { it.id !in listOf("demo-cheki", "demo-phone") }, result.records)
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
