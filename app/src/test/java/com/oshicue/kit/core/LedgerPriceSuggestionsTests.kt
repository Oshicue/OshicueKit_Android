package com.oshicue.kit.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** JVM port of the iOS Tests/LedgerPriceSuggestionsTests.swift suite. */
class LedgerPriceSuggestionsTests {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Before
    fun pinLanguage() {
        // Resolving "System" reads android.os.LocaleList, which is unavailable on the JVM.
        L10n.setLanguage(AppLanguage.En)
    }

    private val membership: LedgerMembership
        get() = LedgerMembership(
            id = "mio-luna", memberId = "mio", groupId = "luna", color = "#F28AAF",
            prices = listOf(
                LedgerPrice(productId = "cheki", currency = "JPY", unitPriceMinor = 2000),
                LedgerPrice(productId = "cheki", currency = "CNY", unitPriceMinor = 8000)
            )
        )

    private fun record(
        id: String, date: String, price: Long, member: String = "mio", group: String = "luna",
        product: String = "cheki", currency: String = "JPY"
    ): LedgerRecord = LedgerRecord(
        id = id, date = date, memberId = member, groupId = group, productId = product, quantity = 1,
        unitPriceMinor = price, currency = currency, memberNameSnapshot = member, groupNameSnapshot = group,
        productNameSnapshot = product, colorSnapshot = "#F28AAF"
    )

    private fun suggested(
        records: List<LedgerRecord> = emptyList(), membership: LedgerMembership? = null,
        date: String = "2026-10-17", product: String = "cheki", currency: String = "JPY",
        remembered: List<LedgerPrice> = emptyList()
    ): Long = LedgerPriceSuggestions.price(
        LedgerDocument(records = records), membership ?: this.membership,
        productId = product, currency = currency, on = date, lastAddedPrices = remembered
    ).unitPriceMinor

    @Test
    fun testMostRecentPurchaseDayWinsOverAppendOrderAndConfiguredPrice() {
        val records = listOf(
            record("latest", date = "2026-10-16", price = 2500),
            record("older-appended-last", date = "2026-09-12", price = 1800)
        )
        assertEquals(2500L, suggested(records))
    }

    @Test
    fun testLastRecordOnSameDayWinsAndSelectedDayIsIncluded() {
        val records = listOf(
            record("first", date = "2026-10-17", price = 2100),
            record("second", date = "2026-10-17", price = 2200)
        )
        assertEquals(2200L, suggested(records))
    }

    @Test
    fun testHistoricalEntryUsesOnlyApplicableHistoryBeforeSelectedDay() {
        val records = listOf(
            record("past", date = "2026-10-01", price = 1500),
            record("future", date = "2026-10-18", price = 3000)
        )
        assertEquals(1500L, suggested(records, date = "2026-10-10"))
        assertEquals(2000L, suggested(records, date = "2026-09-01"))
    }

    @Test
    fun testExactAffiliationDoesNotTakeOtherMembersOrOtherGroupsHistory() {
        val records = listOf(
            record("other-group", date = "2026-10-16", price = 3500, group = "prism"),
            record("other-member", date = "2026-10-16", price = 3000, member = "rina")
        )
        assertEquals(2000L, suggested(records))
        val remembered = listOf(LedgerPrice(productId = "cheki", currency = "JPY", unitPriceMinor = 2800))
        assertEquals(2800L, suggested(records, remembered = remembered))
    }

    @Test
    fun testHistoryHasPriorityOverMostRecentlyAddedPriceForAnotherMember() {
        val records = listOf(record("own", date = "2026-10-01", price = 2500))
        val remembered = listOf(LedgerPrice(productId = "cheki", currency = "JPY", unitPriceMinor = 3000))
        assertEquals(2500L, suggested(records, remembered = remembered))
    }

    @Test
    fun testFallbackMemoryMatchesBothProductAndCurrencyBeforeConfiguredPrice() {
        val remembered = listOf(
            LedgerPrice(productId = "wide-cheki", currency = "JPY", unitPriceMinor = 4000),
            LedgerPrice(productId = "cheki", currency = "CNY", unitPriceMinor = 9000),
            LedgerPrice(productId = "cheki", currency = "JPY", unitPriceMinor = 2600)
        )
        assertEquals(2600L, suggested(remembered = remembered))
        assertEquals(9000L, suggested(currency = "CNY", remembered = remembered))
        assertEquals(0L, suggested(product = "phone-photo", remembered = remembered))
        assertEquals(0L, suggested(currency = "USD", remembered = remembered))
    }

    @Test
    fun testHistoryNeverMixesProductOrCurrency() {
        val records = listOf(
            record("cheki-jpy", date = "2026-10-01", price = 2300),
            record("other-product", date = "2026-10-16", price = 5000, product = "wide-cheki"),
            record("other-currency", date = "2026-10-17", price = 10000, currency = "CNY")
        )
        assertEquals(2300L, suggested(records))
        assertEquals(10000L, suggested(records, currency = "CNY"))
    }

    @Test
    fun testCurrencyUsesLatestApplicableHistoryThenLastAddedProductThenConfiguration() {
        val document = LedgerDocument(records = listOf(
            record("recent", date = "2026-10-16", price = 9000, currency = "CNY"),
            record("appended-past", date = "2026-10-01", price = 2500)
        ))
        val remembered = listOf(
            LedgerPrice(productId = "cheki", currency = "JPY", unitPriceMinor = 3000),
            LedgerPrice(productId = "wide-cheki", currency = "USD", unitPriceMinor = 1000),
            LedgerPrice(productId = "cheki", currency = "HKD", unitPriceMinor = 10000)
        )
        assertEquals("CNY", LedgerPriceSuggestions.currency(document, membership, productId = "cheki", on = "2026-10-17", lastAddedPrices = remembered))
        assertEquals("JPY", LedgerPriceSuggestions.currency(document, membership, productId = "cheki", on = "2026-10-10", lastAddedPrices = remembered))
        assertEquals("HKD", LedgerPriceSuggestions.currency(LedgerDocument(), membership, productId = "cheki", on = "2026-10-17", lastAddedPrices = remembered))
        assertEquals("JPY", LedgerPriceSuggestions.currency(LedgerDocument(), membership, productId = "cheki", on = "2026-10-17"))
        assertEquals("JPY", LedgerPriceSuggestions.currency(LedgerDocument(), null, productId = "new-product", on = "2026-10-17"))
    }

    @Test
    fun testRememberingReplacesOnlyMatchingPairAndPreservesLatestCurrencyOrder() {
        val initial = listOf(
            LedgerPrice(productId = "cheki", currency = "JPY", unitPriceMinor = 2000),
            LedgerPrice(productId = "cheki", currency = "CNY", unitPriceMinor = 8000),
            LedgerPrice(productId = "wide-cheki", currency = "JPY", unitPriceMinor = 3500)
        )
        val newest = LedgerPrice(productId = "cheki", currency = "JPY", unitPriceMinor = 2400)
        val remembered = LedgerPriceSuggestions.remembering(newest, initial)
        assertEquals(listOf(initial[1], initial[2], newest), remembered)
        assertEquals(remembered, LedgerPriceSuggestions.remembering(newest, remembered))
        val zero = LedgerPrice(productId = "cheki", currency = "JPY", unitPriceMinor = 0)
        assertEquals(zero, LedgerPriceSuggestions.remembering(zero, remembered).last())
    }

    @Test
    fun testLegacySeedUsesLastAppendRatherThanDateAndSurvivesSeparateMemoryRoundTrip() {
        val records = listOf(
            record("latest-day", date = "2026-10-17", price = 3000),
            record("last-added", date = "2026-09-10", price = 2200),
            record("yuan", date = "2026-10-01", price = 9000, currency = "CNY")
        )
        val memory = LedgerPriceSuggestions.seededPrices(records)
        assertEquals(
            listOf(
                LedgerPrice(productId = "cheki", currency = "JPY", unitPriceMinor = 2200),
                LedgerPrice(productId = "cheki", currency = "CNY", unitPriceMinor = 9000)
            ),
            memory
        )
        // iOS round-trips [LedgerPrice] through JSONEncoder/JSONDecoder, which is how LedgerStore keeps this
        // memory apart from the exported document. Android has no public LedgerPrice codec, so the round trip
        // goes through LedgerStore's own persistence: a legacy file without lastAddedPrices is seeded, saved,
        // and decoded again by a fresh store.
        val decoded = roundTripThroughStore(records)
        assertEquals(memory, decoded)
        assertEquals(2200L, suggested(emptyList(), remembered = decoded))
        assertEquals(3000L, suggested(records, remembered = decoded))
    }

    @Test
    fun testUnknownPriceAndMissingAffiliationAlwaysProduceNumericZero() {
        val suggestion = LedgerPriceSuggestions.price(LedgerDocument(), null, productId = "new-product", currency = "CNY", on = "2026-10-17")
        assertEquals(0L, suggestion.unitPriceMinor)
        assertEquals("0", LedgerCurrency.CNY.priceText(suggestion.unitPriceMinor))
        assertEquals(0L, suggested(membership = LedgerMembership(id = "new", memberId = "new", groupId = "luna", color = "#F28AAF")))
    }

    private fun roundTripThroughStore(records: List<LedgerRecord>): List<LedgerPrice> {
        val directory = temporaryFolder.newFolder("ledger-store")
        val document = LedgerDocument(
            groups = listOf(LedgerGroup(id = "luna", name = "luna")),
            members = listOf(LedgerMember(id = "mio", name = "mio")),
            memberships = listOf(membership),
            products = LedgerDocument.defaultProducts,
            records = records
        )
        val legacyState = JsonObject().put("document", LedgerCodec.toJson(document)).put("isDemo", false)
        File(directory, "ledger.json").writeText(legacyState.serialize(pretty = true), Charsets.UTF_8)
        val legacy = LedgerStore(directory)
        assertNull(legacy.storageError)
        assertEquals(document, legacy.document)
        assertEquals(true, legacy.commit { it })
        val restored = LedgerStore(directory)
        assertNull(restored.storageError)
        return restored.lastAddedPrices
    }
}
