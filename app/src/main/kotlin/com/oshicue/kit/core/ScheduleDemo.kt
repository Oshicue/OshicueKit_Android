package com.oshicue.kit.core

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

object ScheduleDemo {
    /** Fictional data anchored to the current local hour so the actual system-time line is visible. */
    fun document(now: Instant = Instant.now()): ScheduleDocument {
        val zone = ZoneId.of("Asia/Tokyo")
        val today = now.atZone(zone).toLocalDate()
        val hour = now.atZone(zone).hour
        val anchor = today.atStartOfDay(zone).toInstant().plusSeconds(hour * 3600L)
        val tomorrow = today.plusDays(1)
        val nextAnchor = tomorrow.atStartOfDay(zone).toInstant().plusSeconds(12 * 3600L)
        val venues = listOf(
            Venue("sky-hall", "SKY HALL", "#78C9EB"),
            Venue("orbit-stage", "ORBIT STAGE", "#F4A174"),
            Venue("mint-room", "MINT ROOM", "#94C988"),
            Venue("petal-club", "PETAL CLUB", "#C58EC4"),
            Venue("sunny-studio", "SUNNY STUDIO", "#EECF67")
        )
        val artists = listOf(
            Artist("hoshiiro-note", "星色ノート"),
            Artist("parallel-days", "PARALLEL DAYS"),
            Artist("mint-signal", "ミントシグナル"),
            Artist("luna-palette", "LUNA PALETTE"),
            Artist("sora-melody", "そらメロディ"),
            Artist("neon-ribbon", "NEON RIBBON"),
            Artist("little-compass", "リトルコンパス"),
            Artist("sugar-orbit", "SUGAR ORBIT")
        )
        fun session(id: String, artist: Int, venue: Int, kind: SessionKind, offset: Int, duration: Int, at: Instant, area: String? = null) =
            FestivalSession(
                id = id,
                artistID = artists[artist].id,
                venueID = venues[venue].id,
                kind = kind,
                startsAt = at.plusSeconds(offset * 60L),
                endsAt = at.plusSeconds((offset + duration) * 60L),
                area = area
            )

        val sessions = mutableListOf<FestivalSession>()
        for (index in venues.indices) {
            sessions.add(session("today-live-$index", index, index, SessionKind.live, index * 10, 40, anchor))
            sessions.add(session("today-goods-$index", index, index, SessionKind.goods, index * 10 + 50, 55, anchor, area = "${index + 1}"))
            sessions.add(session("today-live-late-$index", (index + 3) % artists.size, index, SessionKind.live, 85 + index * 8, 35, anchor))
            sessions.add(session("today-goods-late-$index", (index + 3) % artists.size, index, SessionKind.goods, 130 + index * 8, 55, anchor, area = "${index + 1}"))
            sessions.add(session("today-live-evening-$index", (index + 5) % artists.size, index, SessionKind.live, 210 + index * 8, 40, anchor))
            sessions.add(session("tomorrow-live-$index", index, index, SessionKind.live, index * 12, 35, nextAnchor))
            sessions.add(session("tomorrow-goods-$index", index, index, SessionKind.goods, index * 12 + 45, 50, nextAnchor, area = "${index + 1}"))
        }
        val first = FestivalEvent(
            id = "demo-tokyo-idol-circuit", name = "TOKYO IDOL CIRCUIT", city = "Tokyo",
            timeZone = "Asia/Tokyo", venues = venues, artists = artists, sessions = sessions
        )
        val secondVenue = Venue("river-loft", "RIVER LOFT", "#E789A5")
        val secondSessions = listOf(
            FestivalSession("river-live-1", artists[0].id, secondVenue.id, SessionKind.live, anchor.plusSeconds(30 * 60L), anchor.plusSeconds(65 * 60L)),
            FestivalSession("river-goods-1", artists[0].id, secondVenue.id, SessionKind.goods, anchor.plusSeconds(75 * 60L), anchor.plusSeconds(125 * 60L), area = "A"),
            FestivalSession("river-live-2", artists[5].id, secondVenue.id, SessionKind.live, nextAnchor, nextAnchor.plusSeconds(40 * 60L))
        )
        val second = FestivalEvent(
            id = "demo-river-pop", name = "SHIBUYA NIGHT SESSION", city = "Tokyo",
            timeZone = "Asia/Tokyo", venues = listOf(secondVenue), artists = listOf(artists[0], artists[5]), sessions = secondSessions
        )
        return ScheduleDocument(events = listOf(first, second))
    }
}

/** Kept for parity with the iOS side: dates in the current device zone. */
fun kitInstantOfLocalDate(date: java.time.LocalDate): Instant = date.atStartOfDay(ZoneOffset.UTC).toInstant()

fun kitZoneOrGMT(identifier: String): ZoneId = try {
    ZoneId.of(identifier)
} catch (_: Exception) {
    ZoneOffset.UTC
}
