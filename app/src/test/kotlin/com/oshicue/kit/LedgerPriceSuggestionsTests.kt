package com.oshicue.kit

import com.oshicue.kit.core.LedgerCurrency
import com.oshicue.kit.core.LedgerDocument
import com.oshicue.kit.core.LedgerMembership
import com.oshicue.kit.core.LedgerPrice
import com.oshicue.kit.core.LedgerPriceSuggestions
import com.oshicue.kit.core.LedgerRecord
import kotlin.test.Test
import kotlin.test.assertEquals

class LedgerPriceSuggestionsTests {
    private val membership = LedgerMembership(
        id = "mio-luna", memberID = "mio", groupID = "luna", color = "#F28AAF",
        prices = listOf(
            LedgerPrice("cheki", "JPY", 2000),
            LedgerPrice("cheki", "CNY", 8000)
        )
    )

    private fun record(
        id: String, date: String, price: Long,
        member: String = "mio", group: String = "luna",
        product: String = "cheki", currency: String = "JPY"
    ) = LedgerRecord(
        id = id, date = date, memberID = member, groupID = group, productID = product,
        quantity = 1, unitPriceMinor = price, currency = currency,
        memberNameSnapshot = member, groupNameSnapshot = group,
        productNameSnapshot = product, colorSnapshot = "#F28AAF"
    )

    private fun suggested(
        records: List<LedgerRecord> = emptyList(),
        selected: LedgerMembership? = null,
        date: String = "2026-10-17",
        product: String = "cheki",
        currency: String = "JPY",
        remembered: List<LedgerPrice> = emptyList()
    ): Long = LedgerPriceSuggestions.price(
        LedgerDocument(records = records), selected ?: membership, product, currency, date, remembered
    ).unitPriceMinor

    @Test
    fun testMostRecentPurchaseDayWinsOverAppendOrderAndConfiguredPrice() {
        val records = listOf(
            record("latest", "2026-10-16", 2500),
            record("older-appended-last", "2026-09-12", 1800)
        )
        assertEquals(2500, suggested(records))
    }

    @Test
    fun testLastRecordOnSameDayWinsAndSelectedDayIsIncluded() {
        val records = listOf(
            record("first", "2026-10-17", 2100),
            record("second", "2026-10-17", 2200)
        )
        assertEquals(2200, suggested(records))
    }

    @Test
    fun testHistoricalEntryUsesOnlyApplicableHistoryBeforeSelectedDay() {
        val records = listOf(
            record("past", "2026-10-01", 1500),
            record("future", "2026-10-18", 3000)
        )
        assertEquals(1500, suggested(records, date = "2026-10-10"))
        assertEquals(2000, suggested(records, date = "2026-09-01"))
    }

    @Test
    fun testExactAffiliationDoesNotTakeOtherMembersOrOtherGroupsHistory() {
        val records = listOf(
            record("other-group", "2026-10-16", 3500, group = "prism"),
            record("other-member", "2026-10-16", 3000, member = "rina")
        )
        assertEquals(2000, suggested(records))
        val remembered = listOf(LedgerPrice("cheki", "JPY", 2800))
        assertEquals(2800, suggested(records, remembered = remembered))
    }

    @Test
    fun testHistoryHasPriorityOverMostRecentlyAddedPriceForAnotherMember() {
        val records = listOf(record("own", "2026-10-01", 2500))
        val remembered = listOf(LedgerPrice("cheki", "JPY", 3000))
        assertEquals(2500, suggested(records, remembered = remembered))
    }

    @Test
    fun testFallbackMemoryMatchesBothProductAndCurrencyBeforeConfiguredPrice() {
        val remembered = listOf(
            LedgerPrice("wide-cheki", "JPY", 4000),
            LedgerPrice("cheki", "CNY", 9000),
            LedgerPrice("cheki", "JPY", 2600)
        )
        assertEquals(2600, suggested(remembered = remembered))
        assertEquals(9000, suggested(currency = "CNY", remembered = remembered))
        assertEquals(0, suggested(product = "phone-photo", remembered = remembered))
        assertEquals(0, suggested(currency = "USD", remembered = remembered))
    }

    @Test
    fun testHistoryNeverMixesProductOrCurrency() {
        val records = listOf(
            record("cheki-jpy", "2026-10-01", 2300),
            record("other-product", "2026-10-16", 5000, product = "wide-cheki"),
            record("other-currency", "2026-10-17", 10000, currency = "CNY")
        )
        assertEquals(2300, suggested(records))
        assertEquals(10000, suggested(records, currency = "CNY"))
    }

    @Test
    fun testCurrencyUsesLatestApplicableHistoryThenLastAddedProductThenConfiguration() {
        val document = LedgerDocument(
            records = listOf(
                record("recent", "2026-10-16", 9000, currency = "CNY"),
                record("appended-past", "2026-10-01", 2500)
            )
        )
        val remembered = listOf(
            LedgerPrice("cheki", "JPY", 3000),
            LedgerPrice("wide-cheki", "USD", 1000),
            LedgerPrice("cheki", "HKD", 10000)
        )
        assertEquals(
            "CNY",
            LedgerPriceSuggestions.currency(document, membership, "cheki", "2026-10-17", remembered)
        )
        assertEquals(
            "JPY",
            LedgerPriceSuggestions.currency(document, membership, "cheki", "2026-10-10", remembered)
        )
        assertEquals(
            "HKD",
            LedgerPriceSuggestions.currency(LedgerDocument(), membership, "cheki", "2026-10-17", remembered)
        )
        assertEquals(
            "JPY",
            LedgerPriceSuggestions.currency(LedgerDocument(), membership, "cheki", "2026-10-17")
        )
        assertEquals(
            "JPY",
            LedgerPriceSuggestions.currency(LedgerDocument(), null, "new-product", "2026-10-17")
        )
    }

    @Test
    fun testRememberingReplacesOnlyMatchingPairAndPreservesLatestCurrencyOrder() {
        val initial = listOf(
            LedgerPrice("cheki", "JPY", 2000),
            LedgerPrice("cheki", "CNY", 8000),
            LedgerPrice("wide-cheki", "JPY", 3500)
        )
        val newest = LedgerPrice("cheki", "JPY", 2400)
        val remembered = LedgerPriceSuggestions.remembering(newest, initial)
        assertEquals(listOf(initial[1], initial[2], newest), remembered)
        assertEquals(remembered, LedgerPriceSuggestions.remembering(newest, remembered))
        val zero = LedgerPrice("cheki", "JPY", 0)
        assertEquals(zero, LedgerPriceSuggestions.remembering(zero, remembered).last())
    }

    @Test
    fun testLegacySeedUsesLastAppendRatherThanDateAndSurvivesSeparateMemoryRoundTrip() {
        val records = listOf(
            record("latest-day", "2026-10-17", 3000),
            record("last-added", "2026-09-10", 2200),
            record("yuan", "2026-10-01", 9000, currency = "CNY")
        )
        val memory = LedgerPriceSuggestions.seededPrices(records)
        assertEquals(
            listOf(LedgerPrice("cheki", "JPY", 2200), LedgerPrice("cheki", "CNY", 9000)),
            memory
        )
        assertEquals(2200, suggested(remembered = memory))
        assertEquals(3000, suggested(records, remembered = memory))
    }

    @Test
    fun testUnknownPriceAndMissingAffiliationAlwaysProduceNumericZero() {
        val suggestion = LedgerPriceSuggestions.price(
            LedgerDocument(), null, "new-product", "CNY", "2026-10-17"
        )
        assertEquals(0, suggestion.unitPriceMinor)
        assertEquals("0", LedgerCurrency.CNY.priceText(suggestion.unitPriceMinor))
        assertEquals(
            0,
            suggested(selected = LedgerMembership(id = "new", memberID = "new", groupID = "luna", color = "#F28AAF"))
        )
    }
}
