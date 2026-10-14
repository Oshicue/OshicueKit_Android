package com.oshicue.kit.core

/** Filters apply to the activity's civil date, matching the diary's ordinary grouping. */
data class DiaryEntryFilter(
    val startDate: String? = null,
    val endDate: String? = null,
    val status: DiaryStatus? = null,
    val query: String = ""
) {
    val hasValidDateRange: Boolean
        get() {
            if (startDate != null && LedgerDates.parse(startDate) == null) return false
            if (endDate != null && LedgerDates.parse(endDate) == null) return false
            if (startDate != null && endDate != null && startDate > endDate) return false
            return true
        }

    fun matching(entries: List<DiaryEntry>): List<DiaryEntry> {
        if (!hasValidDateRange) return emptyList()
        return DiaryCore.search(entries, query).filter { entry ->
            (startDate == null || entry.event.date >= startDate) &&
                (endDate == null || entry.event.date <= endDate) &&
                (status == null || entry.status == status)
        }
    }
}

fun DiaryDocument.deletingEntries(ids: Set<String>): DiaryDocument =
    copy(entries = entries.filter { it.id !in ids })
