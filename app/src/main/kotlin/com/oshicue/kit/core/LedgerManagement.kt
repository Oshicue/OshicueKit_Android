package com.oshicue.kit.core

/** Independent conditions are combined. Dates are inclusive local civil days. */
data class LedgerRecordFilter(
    val startDate: String? = null,
    val endDate: String? = null,
    val memberID: String? = null,
    val groupID: String? = null,
    val productID: String? = null,
    val currency: String? = null,
    val eventID: String? = null,
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
        val words = KitText.words(query)
        return records.filter { record ->
            if (startDate != null && record.date < startDate) return@filter false
            if (endDate != null && record.date > endDate) return@filter false
            if (memberID != null && record.memberID != memberID) return@filter false
            if (groupID != null && record.groupID != groupID) return@filter false
            if (productID != null && record.productID != productID) return@filter false
            if (currency != null && record.currency != currency) return@filter false
            if (eventID != null && record.eventID != eventID) return@filter false
            if (requiresNoEvent && record.eventID != null) return@filter false
            val searchable = listOf(
                record.date, record.memberNameSnapshot, record.groupNameSnapshot,
                record.productNameSnapshot, record.eventNameSnapshot ?: "", record.note ?: "", record.currency
            ).joinToString("\n")
            words.all { KitText.contains(searchable, it) }
        }
    }
}

/** A confirmation is a fixed selection with quantities and separate currency totals. */
class LedgerRecordDeletion(records: List<LedgerRecord>, selectedIDs: Set<String>) {
    val recordIDs: Set<String>
    val recordCount: Int
    val quantity: Int
    val amountsByCurrency: Map<String, Long>

    init {
        val selected = records.filter { it.id in selectedIDs }
        val summary = LedgerAnalytics.summarize(selected)
        recordIDs = selected.map { it.id }.toSet()
        recordCount = selected.size
        quantity = summary.quantity
        amountsByCurrency = summary.amountsByCurrency
    }
}
