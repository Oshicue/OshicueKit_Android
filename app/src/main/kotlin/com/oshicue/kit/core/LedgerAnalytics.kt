package com.oshicue.kit.core

import java.util.TreeMap

enum class LedgerPeriod { day, month, year, lifetime }

enum class LedgerGrouping { memberCombined, memberByGroup, group }

enum class LedgerMetric { quantity, spending }

data class LedgerProductSummary(
    val id: String,
    val name: String,
    val quantity: Int,
    val amountsByCurrency: Map<String, Long>
)

data class LedgerSummary(
    val quantity: Int = 0,
    val amountsByCurrency: Map<String, Long> = emptyMap(),
    val productRows: List<LedgerProductSummary> = emptyList(),
    val memberCount: Int = 0
)

data class LedgerRankRow(
    val id: String,
    val name: String,
    val subtitle: String,
    val color: String,
    val memberID: String?,
    val groupID: String?,
    val quantity: Int,
    val amountsByCurrency: Map<String, Long>,
    val rank: Int,
    /** Fraction of the selected quantity or single-currency spending total, in 0...1. */
    val share: Double,
    val productRows: List<LedgerProductSummary>
)

object LedgerAnalytics {
    fun filtered(records: List<LedgerRecord>, period: LedgerPeriod, date: String, currency: String? = null): List<LedgerRecord> {
        if (period != LedgerPeriod.lifetime && LedgerDates.parse(date) == null) return emptyList()
        if (currency != null && LedgerCurrency.fromCode(currency) == null) return emptyList()
        return records.filter { record ->
            if (currency != null && record.currency != currency) return@filter false
            when (period) {
                LedgerPeriod.day -> record.date == date
                LedgerPeriod.month -> record.date.take(7) == date.take(7)
                LedgerPeriod.year -> record.date.take(4) == date.take(4)
                LedgerPeriod.lifetime -> true
            }
        }
    }

    /** Preserves each currency as a separate balance and prices as captured in each purchase. */
    fun summarize(records: List<LedgerRecord>): LedgerSummary {
        var quantity = 0
        val amounts = mutableMapOf<String, Long>()
        val products = mutableMapOf<String, LedgerProductSummary>()
        val ordered = records.sortedWith(compareByDescending<LedgerRecord> { it.date }.thenBy { it.id })
        for (record in ordered) {
            val total = checkedTotal(record)
            quantity = add(quantity, record.quantity)
            amounts[record.currency] = add(amounts[record.currency] ?: 0L, total)
            val product = products[record.productID]
            val nextQuantity = add(product?.quantity ?: 0, record.quantity)
            val nextAmounts = (product?.amountsByCurrency ?: emptyMap()).toMutableMap()
            nextAmounts[record.currency] = add(nextAmounts[record.currency] ?: 0L, total)
            products[record.productID] = LedgerProductSummary(
                id = record.productID,
                name = product?.name ?: record.productNameSnapshot,
                quantity = nextQuantity,
                amountsByCurrency = nextAmounts
            )
        }
        return LedgerSummary(
            quantity = quantity,
            amountsByCurrency = amounts,
            productRows = products.values.sortedBy { it.id },
            memberCount = records.map { it.memberID }.toSet().size
        )
    }

    /** Spending requires a single currency. Quantity can combine currencies without converting money. */
    fun rankings(
        records: List<LedgerRecord>,
        grouping: LedgerGrouping,
        metric: LedgerMetric,
        currency: String? = null
    ): List<LedgerRankRow> {
        if (currency != null && LedgerCurrency.fromCode(currency) == null) {
            throw LedgerError("ledger.error.currency", "Unsupported currency '$currency'.", "$.currency")
        }
        if (metric == LedgerMetric.spending && currency == null) {
            throw LedgerError(
                "ledger.error.currencyRequired",
                "Select one currency for spending rankings. Different currencies are never added together.",
                "$.currency"
            )
        }
        val selected = records.filter { currency == null || it.currency == currency }
        val summary = summarize(selected)
        val buckets = linkedMapOf<String, MutableList<LedgerRecord>>()
        for (record in selected) {
            val key = when (grouping) {
                LedgerGrouping.memberCombined -> "member:${record.memberID}"
                LedgerGrouping.memberByGroup -> "membership:${LedgerImporter.pair(record.memberID, record.groupID)}"
                LedgerGrouping.group -> "group:${record.groupID}"
            }
            buckets.getOrPut(key) { mutableListOf() }.add(record)
        }
        val rows = mutableListOf<LedgerRankRow>()
        for ((id, bucket) in buckets) {
            val ordered = bucket.sortedWith(compareByDescending<LedgerRecord> { it.date }.thenBy { it.id })
            val latest = ordered.firstOrNull() ?: continue
            val bucketSummary = summarize(bucket)
            val seenGroups = mutableSetOf<String>()
            val groupNames = ordered.filter { seenGroups.add(it.groupID) }.map { it.groupNameSnapshot }
            rows.add(
                LedgerRankRow(
                    id = id,
                    name = if (grouping == LedgerGrouping.group) latest.groupNameSnapshot else latest.memberNameSnapshot,
                    subtitle = if (grouping == LedgerGrouping.group) "" else groupNames.joinToString(" / "),
                    color = latest.colorSnapshot,
                    memberID = if (grouping == LedgerGrouping.group) null else latest.memberID,
                    groupID = if (grouping == LedgerGrouping.memberCombined) null else latest.groupID,
                    quantity = bucketSummary.quantity,
                    amountsByCurrency = bucketSummary.amountsByCurrency,
                    rank = 0,
                    share = 0.0,
                    productRows = bucketSummary.productRows
                )
            )
        }
        fun value(row: LedgerRankRow): Long =
            if (metric == LedgerMetric.quantity) row.quantity.toLong() else row.amountsByCurrency[currency] ?: 0L

        val sorted = rows.sortedWith(compareByDescending<LedgerRankRow> { value(it) }.thenBy { it.id })
        val overall: Long = if (metric == LedgerMetric.quantity) summary.quantity.toLong() else summary.amountsByCurrency[currency] ?: 0L
        var previousValue: Long? = null
        var rank = 0
        return sorted.mapIndexed { index, row ->
            val currentValue = value(row)
            if (previousValue != currentValue) rank = index + 1
            previousValue = currentValue
            row.copy(
                rank = rank,
                share = if (overall > 0) currentValue.toDouble() / overall.toDouble() else 0.0
            )
        }
    }

    private fun checkedTotal(record: LedgerRecord): Long {
        if (LedgerCurrency.fromCode(record.currency) == null) {
            throw LedgerError("ledger.error.currency", "Unsupported currency.", "$.records.currency")
        }
        if (record.quantity <= 0) throw LedgerError("ledger.error.quantity", "quantity must be positive.", "$.records.quantity")
        if (record.unitPriceMinor < 0) throw LedgerError("ledger.error.price", "unitPriceMinor must not be negative.", "$.records.unitPriceMinor")
        return record.totalMinor ?: throw overflow()
    }

    private fun add(first: Int, second: Int): Int = try {
        Math.addExact(first, second)
    } catch (_: ArithmeticException) {
        throw overflow()
    }

    private fun add(first: Long, second: Long): Long = try {
        Math.addExact(first, second)
    } catch (_: ArithmeticException) {
        throw overflow()
    }

    private fun overflow() = LedgerError("ledger.error.overflow", "The ledger total is too large.", "$.records")
}

/** Deterministic ordering helper shared by the catalog screens. */
internal fun <T> List<T>.kitSortedBy(selector: (T) -> String): List<T> =
    sortedWith { left, right -> KitText.standardCompare(selector(left), selector(right)) }

internal fun sortedAmountEntries(amounts: Map<String, Long>): List<Map.Entry<String, Long>> =
    TreeMap(amounts).entries.toList()
