package com.oshicue.kit.core

data class ScheduleDeletionResult(
    val document: ScheduleDocument,
    val favorites: Set<String>
)

/**
 * Deletes one complete timetable and preserves selected sessions in other events.
 * An unknown event returns null so callers do not change or persist any state.
 */
fun ScheduleDocument.deletingEvent(id: String, favorites: Set<String>): ScheduleDeletionResult? {
    if (events.none { it.id == id }) return null
    val remaining = events.filter { it.id != id }
    val document = ScheduleDocument(schemaVersion, remaining)
    return ScheduleDeletionResult(
        document = document,
        favorites = favorites.intersect(document.sessionFavoriteIDs)
    )
}
