package com.oshicue.kit.core

/** Price suggestions never change recorded purchases or combine different currencies or products. */
object LedgerPriceSuggestions {
    /** Prefers the latest applicable purchase day for this affiliation. Array order breaks same-day ties. */
    fun price(
        document: LedgerDocument,
        membership: LedgerMembership?,
        productID: String,
        currency: String,
        date: String,
        lastAddedPrices: List<LedgerPrice> = emptyList()
    ): LedgerPrice {
        latestRecord(document, membership, productID, currency, date)?.let {
            return LedgerPrice(productID, currency, it.unitPriceMinor)
        }
        lastAddedPrices.lastOrNull {
            it.productID == productID && it.currency == currency && it.unitPriceMinor >= 0
        }?.let { return it }
        membership?.prices?.firstOrNull { it.productID == productID && it.currency == currency }?.let { return it }
        return LedgerPrice(productID, currency, 0)
    }

    /** A new selection starts with the currency of the applicable history, then the last added purchase. */
    fun currency(
        document: LedgerDocument,
        membership: LedgerMembership?,
        productID: String,
        date: String,
        lastAddedPrices: List<LedgerPrice> = emptyList()
    ): String {
        latestRecord(document, membership, productID, null, date)?.let { return it.currency }
        lastAddedPrices.lastOrNull {
            it.productID == productID && LedgerCurrency.fromCode(it.currency) != null
        }?.let { return it.currency }
        return membership?.prices?.firstOrNull { it.productID == productID }?.currency
            ?: LedgerCurrency.JPY.code
    }

    /** One entry per product/currency pair; the last entry is also the most recently added currency. */
    fun remembering(price: LedgerPrice, prices: List<LedgerPrice>): List<LedgerPrice> {
        if (price.productID.isEmpty() || price.unitPriceMinor < 0 ||
            LedgerCurrency.fromCode(price.currency) == null
        ) return prices
        return prices.filter {
            it.productID != price.productID || it.currency != price.currency
        } + price
    }

    /** Legacy storage has no separate entry-order memory, so existing record order provides a migration seed. */
    fun seededPrices(records: List<LedgerRecord>): List<LedgerPrice> {
        var result = emptyList<LedgerPrice>()
        for (record in records) {
            result = remembering(
                LedgerPrice(record.productID, record.currency, record.unitPriceMinor),
                result
            )
        }
        return result
    }

    private fun latestRecord(
        document: LedgerDocument,
        membership: LedgerMembership?,
        productID: String,
        currency: String?,
        date: String
    ): LedgerRecord? {
        if (membership == null || LedgerDates.parse(date) == null) return null
        var latest: LedgerRecord? = null
        for (record in document.records) {
            if (record.memberID == membership.memberID && record.groupID == membership.groupID &&
                record.productID == productID && (currency == null || record.currency == currency) &&
                record.date <= date
            ) {
                if (latest == null || record.date >= latest.date) latest = record
            }
        }
        return latest
    }
}
