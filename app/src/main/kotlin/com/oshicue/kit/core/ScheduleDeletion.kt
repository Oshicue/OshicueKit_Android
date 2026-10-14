package com.oshicue.kit.core

data class ScheduleDeletionResult(
    val document: ScheduleDocument,
    val favorites: Set<String>
)

/**
 * Deletes one complete timetable. Favorites shared with another event remain available.
 * An unknown event returns null so callers do not change or persist any state.
 */
fun ScheduleDocument.deletingEvent(id: String, favorites: Set<String>): ScheduleDeletionResult? {
    if (events.none { it.id == id }) return null
    val remaining = events.filter { it.id != id }
    val remainingArtistIDs = remaining.flatMap { event -> event.artists.map { it.id } }.toSet()
    return ScheduleDeletionResult(
        document = ScheduleDocument(schemaVersion, remaining),
        favorites = favorites.intersect(remainingArtistIDs)
    )
}
