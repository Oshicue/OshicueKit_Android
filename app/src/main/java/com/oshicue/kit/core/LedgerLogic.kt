package com.oshicue.kit.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Independent conditions are combined. Dates are inclusive local civil days. */
data class LedgerRecordFilter(
    val startDate: String? = null,
    val endDate: String? = null,
    val memberId: String? = null,
    val groupId: String? = null,
    val productId: String? = null,
    val currency: String? = null,
    val eventId: String? = null,
    val requiresNoEvent: Boolean = false,
    val query: String = ""
) {
    val hasValidDateRange: Boolean
        get() {
            if (startDate != null && LedgerDates.parse(startDate) == null) return false
            if (endDate != null && LedgerDates.parse(endDate) == null) return false
            if (startDate != null && endDate != null && startDate > endDate) return false
            return true
        }

    fun matching(records: List<LedgerRecord>): List<LedgerRecord> {
        if (!hasValidDateRange) return emptyList()
        val words = query.split(Regex("[\\s\\p{Z}\\u0085]+")).filter { it.isNotEmpty() }
        return records.filter { record ->
            if (startDate != null && record.date < startDate) return@filter false
            if (endDate != null && record.date > endDate) return@filter false
            if (memberId != null && record.memberId != memberId) return@filter false
            if (groupId != null && record.groupId != groupId) return@filter false
            if (productId != null && record.productId != productId) return@filter false
            if (currency != null && record.currency != currency) return@filter false
            if (eventId != null && record.eventId != eventId) return@filter false
            if (requiresNoEvent && record.eventId != null) return@filter false
            val searchable = listOf(
                record.date, record.memberNameSnapshot, record.groupNameSnapshot,
                record.productNameSnapshot, record.eventNameSnapshot ?: "", record.note ?: "", record.currency
            ).joinToString("\n")
            words.all { searchContains(searchable, it) }
        }
    }
}

/** Case- and diacritic-insensitive substring search, matching range(of:options:). */
fun searchContains(haystack: String, needle: String): Boolean {
    if (needle.isEmpty()) return true
    val normalizedNeedle = foldForSearch(needle)
    return foldForSearch(haystack).contains(normalizedNeedle)
}

fun foldForSearch(value: String): String {
    val decomposed = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
    val withoutMarks = decomposed.replace(Regex("\\p{M}+"), "")
    return withoutMarks.lowercase(java.util.Locale.ROOT)
}

/** Standard (human) ordering: numbers in names compare by value, as localizedStandardCompare does. */
fun standardCompare(left: String, right: String): Int {
    val a = left.lowercase(java.util.Locale.ROOT)
    val b = right.lowercase(java.util.Locale.ROOT)
    var i = 0
    var j = 0
    while (i < a.length && j < b.length) {
        val ca = a[i]
        val cb = b[j]
        if (ca.isDigit() && cb.isDigit()) {
            var endA = i
            while (endA < a.length && a[endA].isDigit()) endA++
            var endB = j
            while (endB < b.length && b[endB].isDigit()) endB++
            val numberA = a.substring(i, endA).trimStart('0')
            val numberB = b.substring(j, endB).trimStart('0')
            val comparison = if (numberA.length != numberB.length) {
                numberA.length - numberB.length
            } else {
                numberA.compareTo(numberB)
            }
            if (comparison != 0) return comparison
            i = endA
            j = endB
            continue
        }
        if (ca != cb) return ca.code - cb.code
        i++
        j++
    }
    return (a.length - i) - (b.length - j)
}

class LedgerSummary(
    val quantity: Int = 0,
    val amountsByCurrency: Map<String, Long> = emptyMap(),
    val productRows: List<LedgerProductSummary> = emptyList(),
    val memberCount: Int = 0
)

data class LedgerProductSummary(
    val id: String,
    val name: String,
    val quantity: Int,
    val amountsByCurrency: Map<String, Long>
)

data class LedgerRankRow(
    val id: String,
    val name: String,
    val subtitle: String,
    val color: String,
    val memberId: String?,
    val groupId: String?,
    val quantity: Int,
    val amountsByCurrency: Map<String, Long>,
    val rank: Int,
    val share: Double,
    val productRows: List<LedgerProductSummary>
)

enum class LedgerPeriod { Day, Month, Year, Lifetime }

enum class LedgerGrouping { MemberCombined, MemberByGroup, Group }

enum class LedgerMetric { Quantity, Spending }

object LedgerAnalytics {
    fun filtered(records: List<LedgerRecord>, period: LedgerPeriod, date: String, currency: String? = null): List<LedgerRecord> {
        if (period != LedgerPeriod.Lifetime && LedgerDates.parse(date) == null) return emptyList()
        if (currency != null && LedgerCurrency.from(currency) == null) return emptyList()
        return records.filter { record ->
            if (currency != null && record.currency != currency) return@filter false
            when (period) {
                LedgerPeriod.Day -> record.date == date
                LedgerPeriod.Month -> record.date.take(7) == date.take(7)
                LedgerPeriod.Year -> record.date.take(4) == date.take(4)
                LedgerPeriod.Lifetime -> true
            }
        }
    }

    /** Preserves each currency as a separate balance and prices as captured in each purchase. */
    fun summarize(records: List<LedgerRecord>): LedgerSummary {
        var quantity = 0
        val amounts = mutableMapOf<String, Long>()
        val products = LinkedHashMap<String, MutableProduct>()
        val ordered = records.sortedWith(compareByDescending<LedgerRecord> { it.date }.thenBy { it.id })
        for (record in ordered) {
            val total = checkedTotal(record)
            quantity = try {
                Math.addExact(quantity, record.quantity)
            } catch (_: ArithmeticException) {
                throw overflow()
            }
            amounts[record.currency] = try {
                Math.addExact(amounts[record.currency] ?: 0L, total)
            } catch (_: ArithmeticException) {
                throw overflow()
            }
            val product = products.getOrPut(record.productId) {
                MutableProduct(record.productId, record.productNameSnapshot)
            }
            product.quantity = try {
                Math.addExact(product.quantity, record.quantity)
            } catch (_: ArithmeticException) {
                throw overflow()
            }
            product.amounts[record.currency] = try {
                Math.addExact(product.amounts[record.currency] ?: 0L, total)
            } catch (_: ArithmeticException) {
                throw overflow()
            }
        }
        return LedgerSummary(
            quantity = quantity,
            amountsByCurrency = amounts,
            productRows = products.values.sortedBy { it.id }.map {
                LedgerProductSummary(it.id, it.name, it.quantity, it.amounts)
            },
            memberCount = records.map { it.memberId }.toSet().size
        )
    }

    /** Spending requires a single currency. Quantity combines currencies without converting. */
    fun rankings(
        records: List<LedgerRecord>,
        grouping: LedgerGrouping,
        metric: LedgerMetric,
        currency: String? = null
    ): List<LedgerRankRow> {
        if (currency != null && LedgerCurrency.from(currency) == null) {
            throw LedgerError("ledger.error.currency", "Unsupported currency '$currency'.", "$.currency")
        }
        if (metric == LedgerMetric.Spending && currency == null) {
            throw LedgerError(
                "ledger.error.currencyRequired",
                "Select one currency for spending rankings. Different currencies are never added together.",
                "$.currency"
            )
        }
        val selected = records.filter { currency == null || it.currency == currency }
        val summary = summarize(selected)
        val buckets = selected.groupBy { record ->
            when (grouping) {
                LedgerGrouping.MemberCombined -> "member:${record.memberId}"
                LedgerGrouping.MemberByGroup -> "membership:${LedgerImporter.pair(record.memberId, record.groupId)}"
                LedgerGrouping.Group -> "group:${record.groupId}"
            }
        }
        val rows = mutableListOf<LedgerRankRow>()
        for ((id, bucket) in buckets) {
            val ordered = bucket.sortedWith(compareByDescending<LedgerRecord> { it.date }.thenBy { it.id })
            val latest = ordered.firstOrNull() ?: continue
            val bucketSummary = summarize(bucket)
            val seenGroups = mutableSetOf<String>()
            val groupNames = ordered.filter { seenGroups.add(it.groupId) }.map { it.groupNameSnapshot }
            rows.add(
                LedgerRankRow(
                    id = id,
                    name = if (grouping == LedgerGrouping.Group) latest.groupNameSnapshot else latest.memberNameSnapshot,
                    subtitle = if (grouping == LedgerGrouping.Group) "" else groupNames.joinToString(" / "),
                    color = latest.colorSnapshot,
                    memberId = if (grouping == LedgerGrouping.Group) null else latest.memberId,
                    groupId = if (grouping == LedgerGrouping.MemberCombined) null else latest.groupId,
                    quantity = bucketSummary.quantity,
                    amountsByCurrency = bucketSummary.amountsByCurrency,
                    rank = 0,
                    share = 0.0,
                    productRows = bucketSummary.productRows
                )
            )
        }
        fun value(row: LedgerRankRow): Long =
            if (metric == LedgerMetric.Quantity) row.quantity.toLong()
            else row.amountsByCurrency[currency] ?: 0L
        val sorted = rows.sortedWith { left, right ->
            val a = value(left)
            val b = value(right)
            if (a == b) left.id.compareTo(right.id) else b.compareTo(a)
        }
        val overall = if (metric == LedgerMetric.Quantity) summary.quantity.toLong()
        else summary.amountsByCurrency[currency] ?: 0L
        var previous: Long? = null
        var rank = 0
        return sorted.mapIndexed { index, row ->
            val current = value(row)
            if (previous != current) rank = index + 1
            previous = current
            row.copy(
                rank = rank,
                share = if (overall > 0) current.toDouble() / overall.toDouble() else 0.0
            )
        }
    }

    private class MutableProduct(val id: String, val name: String) {
        var quantity: Int = 0
        val amounts = mutableMapOf<String, Long>()
    }

    private fun checkedTotal(record: LedgerRecord): Long {
        if (LedgerCurrency.from(record.currency) == null) {
            throw LedgerError("ledger.error.currency", "Unsupported currency.", "$.records.currency")
        }
        if (record.quantity <= 0) throw LedgerError("ledger.error.quantity", "quantity must be positive.", "$.records.quantity")
        if (record.unitPriceMinor < 0) throw LedgerError("ledger.error.price", "unitPriceMinor must not be negative.", "$.records.unitPriceMinor")
        return record.totalMinor ?: throw overflow()
    }

    private fun overflow(): LedgerError =
        LedgerError("ledger.error.overflow", "The ledger total is too large.", "$.records")
}

/** A confirmation is a fixed selection with quantities and separate currency totals. */
class LedgerRecordDeletion(records: List<LedgerRecord>, selectedIds: Set<String>) {
    val recordIds: Set<String>
    val recordCount: Int
    val quantity: Int
    val amountsByCurrency: Map<String, Long>

    init {
        val selected = records.filter { it.id in selectedIds }
        val summary = LedgerAnalytics.summarize(selected)
        recordIds = selected.map { it.id }.toSet()
        recordCount = selected.size
        quantity = summary.quantity
        amountsByCurrency = summary.amountsByCurrency
    }
}

/** Catalog pickers exclude deleted entries. Archived identities remain for history. */
val LedgerDocument.activeGroups: List<LedgerGroup> get() = groups.filter { it.isActive }
val LedgerDocument.activeMembers: List<LedgerMember> get() = members.filter { it.isActive }

val LedgerDocument.activeMemberships: List<LedgerMembership>
    get() {
        val groupIds = activeGroups.map { it.id }.toSet()
        val memberIds = activeMembers.map { it.id }.toSet()
        return memberships.filter { it.isActive && it.groupId in groupIds && it.memberId in memberIds }
    }

/** Deleting a person applies to every group, leaving past purchases and prices intact. */
fun LedgerDocument.deletingMember(id: String): LedgerDocument {
    if (members.none { it.id == id }) return this
    return copy(
        members = members.map { if (it.id == id) it.copy(isArchived = true) else it },
        memberships = memberships.map { if (it.memberId == id) it.copy(isArchived = true) else it }
    )
}

/** Group deletion affects its affiliations, without deleting a shared member elsewhere. */
fun LedgerDocument.deletingGroup(id: String): LedgerDocument {
    if (groups.none { it.id == id }) return this
    return copy(
        groups = groups.map { if (it.id == id) it.copy(isArchived = true) else it },
        memberships = memberships.map { if (it.groupId == id) it.copy(isArchived = true) else it }
    )
}

fun LedgerDocument.deletingMembers(ids: Set<String>): LedgerDocument =
    ids.sorted().fold(this) { document, id -> document.deletingMember(id) }

fun LedgerDocument.deletingGroups(ids: Set<String>): LedgerDocument =
    ids.sorted().fold(this) { document, id -> document.deletingGroup(id) }

fun LedgerDocument.deletingRecords(ids: Set<String>): LedgerDocument =
    copy(records = records.filter { it.id !in ids })

/** An archived person never appears among favorites, even if retained in a backup. */
val LedgerDocument.favoriteMembers: List<LedgerMember>
    get() = activeMembers.filter { it.isFavorited }.sortedWith(::memberPrecedes)

/** Used when linking an existing person to another group. Each person appears once. */
val LedgerDocument.membersForSelection: List<LedgerMember>
    get() = activeMembers.sortedWith { left, right ->
        if (left.isFavorited != right.isFavorited) {
            if (left.isFavorited) -1 else 1
        } else memberPrecedes(left, right)
    }

/** Each active affiliation remains a distinct choice with its group color and prices. */
val LedgerDocument.favoriteMemberships: List<LedgerMembership>
    get() {
        val favoriteIds = favoriteMembers.map { it.id }.toSet()
        return activeMemberships.filter { it.memberId in favoriteIds }.sortedWith(membershipComparator())
    }

/**
 * Existing purchases keep their original identity, including archived members or groups.
 * New purchases use the first active favorite, or the existing default when there are none.
 */
fun LedgerDocument.preferredRecordMembership(editing: LedgerRecord? = null): LedgerMembership? {
    if (editing != null) {
        memberships.firstOrNull { it.memberId == editing.memberId && it.groupId == editing.groupId }?.let { return it }
    }
    return favoriteMemberships.firstOrNull() ?: activeMemberships.firstOrNull()
}

fun LedgerDocument.recordMembershipChoices(editing: LedgerRecord? = null): List<LedgerMembership> {
    val choices = activeMemberships.toMutableList()
    if (editing != null) {
        val original = memberships.firstOrNull { it.memberId == editing.memberId && it.groupId == editing.groupId }
        if (original != null && choices.none { it.id == original.id }) choices.add(original)
    }
    return choices
}

/** Favorite shortcuts do not replace the complete, grouped catalog below them. */
fun LedgerDocument.recordMembershipGroups(editing: LedgerRecord? = null): List<LedgerGroup> {
    val groupIds = recordMembershipChoices(editing).map { it.groupId }.toSet()
    return groups.filter { it.id in groupIds }.sortedWith { left, right ->
        val comparison = standardCompare(left.name, right.name)
        if (comparison == 0) left.id.compareTo(right.id) else comparison
    }
}

fun LedgerDocument.recordMembershipChoicesIn(groupId: String, editing: LedgerRecord? = null): List<LedgerMembership> =
    recordMembershipChoices(editing).filter { it.groupId == groupId }.sortedWith(membershipComparator())

private fun memberPrecedes(left: LedgerMember, right: LedgerMember): Int {
    val comparison = standardCompare(left.name, right.name)
    return if (comparison == 0) left.id.compareTo(right.id) else comparison
}

/** Orders affiliations by member name, then group name, then membership ID. */
private fun LedgerDocument.membershipComparator(): Comparator<LedgerMembership> = Comparator { left, right ->
    val leftName = members.firstOrNull { it.id == left.memberId }?.name ?: ""
    val rightName = members.firstOrNull { it.id == right.memberId }?.name ?: ""
    val memberComparison = standardCompare(leftName, rightName)
    if (memberComparison != 0) return@Comparator memberComparison
    val leftGroup = groups.firstOrNull { it.id == left.groupId }?.name ?: ""
    val rightGroup = groups.firstOrNull { it.id == right.groupId }?.name ?: ""
    val groupComparison = standardCompare(leftGroup, rightGroup)
    if (groupComparison != 0) groupComparison else left.id.compareTo(right.id)
}

/** Price suggestions never change recorded purchases or mix currencies or products. */
object LedgerPriceSuggestions {
    /** Prefers the latest applicable purchase day for this affiliation. */
    fun price(
        document: LedgerDocument,
        membership: LedgerMembership?,
        productId: String,
        currency: String,
        on: String,
        lastAddedPrices: List<LedgerPrice> = emptyList()
    ): LedgerPrice {
        latestRecord(document, membership, productId, currency, on)?.let {
            return LedgerPrice(productId, currency, it.unitPriceMinor)
        }
        lastAddedPrices.lastOrNull {
            it.productId == productId && it.currency == currency && it.unitPriceMinor >= 0
        }?.let { return it }
        membership?.prices?.firstOrNull { it.productId == productId && it.currency == currency }?.let { return it }
        return LedgerPrice(productId, currency, 0)
    }

    /** A new selection starts with the currency of applicable history, then the last purchase. */
    fun currency(
        document: LedgerDocument,
        membership: LedgerMembership?,
        productId: String,
        on: String,
        lastAddedPrices: List<LedgerPrice> = emptyList()
    ): String {
        latestRecord(document, membership, productId, null, on)?.let { return it.currency }
        lastAddedPrices.lastOrNull {
            it.productId == productId && LedgerCurrency.from(it.currency) != null
        }?.let { return it.currency }
        return membership?.prices?.firstOrNull { it.productId == productId }?.currency
            ?: LedgerCurrency.JPY.code
    }

    /** One entry per product/currency pair; the last entry is the most recently added. */
    fun remembering(price: LedgerPrice, prices: List<LedgerPrice>): List<LedgerPrice> {
        if (price.productId.isEmpty() || price.unitPriceMinor < 0 || LedgerCurrency.from(price.currency) == null) {
            return prices
        }
        return prices.filter { it.productId != price.productId || it.currency != price.currency } + price
    }

    /** Legacy storage has no entry-order memory, so record order provides a migration seed. */
    fun seededPrices(records: List<LedgerRecord>): List<LedgerPrice> {
        var result = emptyList<LedgerPrice>()
        for (record in records) {
            result = remembering(LedgerPrice(record.productId, record.currency, record.unitPriceMinor), result)
        }
        return result
    }

    private fun latestRecord(
        document: LedgerDocument,
        membership: LedgerMembership?,
        productId: String,
        currency: String?,
        on: String
    ): LedgerRecord? {
        if (membership == null || LedgerDates.parse(on) == null) return null
        var latest: LedgerRecord? = null
        for (record in document.records) {
            if (record.memberId != membership.memberId || record.groupId != membership.groupId) continue
            if (record.productId != productId) continue
            if (currency != null && record.currency != currency) continue
            if (record.date > on) continue
            if (latest == null || record.date >= latest.date) latest = record
        }
        return latest
    }
}

object LedgerDemo {
    /** Default fictional entries for first launch. */
    fun document(now: Instant = Instant.now()): LedgerDocument {
        val zone = KitDate.zoneOrUtc("Asia/Shanghai")
        val today = LedgerDates.string(now, zone)
        val earlier = LedgerDates.string(now.minusSeconds(86_400), zone)
        val thirdDay = LedgerDates.string(now.minusSeconds(172_800), zone)
        val groups = listOf(LedgerGroup("luna", "LUNA NOTE"), LedgerGroup("prism", "PRISM DAYS"))
        val members = listOf(
            LedgerMember("mio", "Mio"),
            LedgerMember("rina", "Rina"),
            LedgerMember("sora", "Sora")
        )
        val memberships = listOf(
            LedgerMembership(
                "mio-luna", "mio", "luna", "#F28AAF",
                listOf(
                    LedgerPrice("cheki", "JPY", 2000),
                    LedgerPrice("wide-cheki", "JPY", 3500),
                    LedgerPrice("phone-photo", "JPY", 1500)
                )
            ),
            LedgerMembership("mio-prism", "mio", "prism", "#9B8CF5", listOf(LedgerPrice("cheki", "JPY", 2500))),
            LedgerMembership("rina-luna", "rina", "luna", "#66C7A5", listOf(LedgerPrice("cheki", "JPY", 2000))),
            LedgerMembership("sora-prism", "sora", "prism", "#67B9EF", listOf(LedgerPrice("phone-photo", "CNY", 8000)))
        )
        val events = listOf(
            LedgerEvent("demo-fes", "Oshicue Mini Fes", today, "Tokyo", "Luna Hall", "Shibuya, Tokyo")
        )
        val records = listOf(
            LedgerRecord(
                "demo-cheki", today, "mio", "luna", "cheki", 5, 2000, "JPY", "demo-fes",
                "Mio", "LUNA NOTE", "Cheki", "#F28AAF",
                "Oshicue Mini Fes", "Tokyo", "Luna Hall", "Shibuya, Tokyo"
            ),
            LedgerRecord(
                "demo-wide", today, "mio", "luna", "wide-cheki", 2, 3500, "JPY", "demo-fes",
                "Mio", "LUNA NOTE", "WIDE Cheki", "#F28AAF",
                "Oshicue Mini Fes", "Tokyo", "Luna Hall"
            ),
            LedgerRecord(
                "demo-rina", today, "rina", "luna", "cheki", 3, 2000, "JPY", null,
                "Rina", "LUNA NOTE", "Cheki", "#66C7A5"
            ),
            LedgerRecord(
                "demo-shared", earlier, "mio", "prism", "cheki", 4, 2500, "JPY", null,
                "Mio", "PRISM DAYS", "Cheki", "#9B8CF5"
            ),
            LedgerRecord(
                "demo-phone", thirdDay, "sora", "prism", "phone-photo", 2, 8000, "CNY", null,
                "Sora", "PRISM DAYS", "Phone photo", "#67B9EF"
            )
        )
        return LedgerDocument(
            groups = groups, members = members, memberships = memberships,
            products = LedgerDocument.defaultProducts, events = events, records = records
        )
    }
}
