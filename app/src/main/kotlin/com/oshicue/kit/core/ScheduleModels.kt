package com.oshicue.kit.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

data class ScheduleDocument(
    val schemaVersion: Int = 1,
    val events: List<FestivalEvent>
) {
    /** A repeated event ID replaces its complete timetable. Other imported events remain available. */
    fun merging(incoming: ScheduleDocument): ScheduleDocument {
        val newIDs = incoming.events.map { it.id }.toSet()
        return ScheduleDocument(events = events.filter { it.id !in newIDs } + incoming.events)
    }
}

data class FestivalEvent(
    val id: String,
    val name: String,
    val city: String,
    val timeZone: String,
    val venues: List<Venue>,
    val artists: List<Artist>,
    val sessions: List<FestivalSession>
) {
    val timeZoneValue: ZoneId get() = try {
        ZoneId.of(timeZone)
    } catch (_: Exception) {
        ZoneOffset.UTC
    }

    /** Includes every local day touched by a session, including sessions that cross midnight. */
    val days: List<LocalDate>
        get() {
            val zone = timeZoneValue
            val result = sortedSetOf<LocalDate>()
            for (session in sessions) {
                var day = session.startsAt.atZone(zone).toLocalDate()
                while (day.atStartOfDay(zone).toInstant() < session.endsAt) {
                    result.add(day)
                    day = day.plusDays(1)
                }
            }
            return result.toList()
        }

    fun sessions(on: LocalDate): List<FestivalSession> {
        val zone = timeZoneValue
        val start = on.atStartOfDay(zone).toInstant()
        val end = on.plusDays(1).atStartOfDay(zone).toInstant()
        return sessions.filter { it.startsAt < end && it.endsAt > start }
            .sortedWith(compareBy({ it.startsAt }, { it.id }))
    }

    fun artist(id: String): Artist? = artists.firstOrNull { it.id == id }
    fun venue(id: String): Venue? = venues.firstOrNull { it.id == id }
}

data class Venue(val id: String, val name: String, val color: String)

data class Artist(val id: String, val name: String)

enum class SessionKind(val raw: String) {
    live("live"),
    goods("goods");

    companion object {
        fun fromRaw(value: String): SessionKind? = entries.firstOrNull { it.raw == value }
    }
}

data class FestivalSession(
    val id: String,
    val artistID: String,
    val venueID: String,
    val kind: SessionKind,
    val startsAt: Instant,
    val endsAt: Instant,
    val area: String? = null,
    val notes: String? = null
) {
    val timelineItem: TimelineItem get() = TimelineItem(id, startsAt, endsAt)
}
