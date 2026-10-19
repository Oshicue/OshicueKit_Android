package com.oshicue.kit.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.time.Instant

/** Port of Tests/TimelineLayoutTests.swift. */
class TimelineLayoutTests {
    @Before
    fun pinLanguage() {
        // Resolving "System" reads android.os.LocaleList, which is unavailable on the JVM.
        L10n.setLanguage(AppLanguage.En)
    }

    private fun item(id: String, start: Int, end: Int): TimelineItem =
        TimelineItem(id, Instant.ofEpochSecond(start * 60L), Instant.ofEpochSecond(end * 60L))

    @Test
    fun testTouchingSessionsUseFullWidth() {
        val placements = TimelineLayout.place(listOf(item("b", 20, 40), item("a", 0, 20)))
        assertEquals(listOf("a", "b"), placements.map { it.id })
        assertTrue(placements.all { it.lane == 0 && it.laneCount == 1 && it.relativeWidth == 1.0 })
    }

    @Test
    fun testOverlapChainUsesTwoLanesAndNextClusterResetsWidth() {
        val sessions = listOf(item("a", 0, 30), item("b", 20, 50), item("c", 40, 60), item("isolated", 70, 80))
        val placements = TimelineLayout.place(sessions)
        assertEquals(listOf(2, 2, 2, 1), placements.map { it.laneCount })
        assertEquals(listOf(0, 1, 0, 0), placements.map { it.lane })
    }

    @Test
    fun testNestedIntervalsUseMinimumConcurrentLanes() {
        val sessions = listOf(item("long", 0, 100), item("short-a", 10, 30), item("short-b", 20, 40), item("short-c", 40, 60))
        val placements = TimelineLayout.place(sessions)
        assertTrue(placements.all { it.laneCount == 3 })
        assertEquals(1, placements.firstOrNull { it.id == "short-c" }?.lane)
        assertNoOverlapsShareLane(sessions, placements)
    }

    @Test
    fun testLayoutIsDeterministicForEqualStarts() {
        val sessions = listOf(item("c", 0, 40), item("b", 0, 20), item("a", 0, 20))
        val placements = TimelineLayout.place(sessions)
        assertEquals(listOf("a", "b", "c"), placements.map { it.id })
        assertEquals(TimelineLayout.place(sessions.reversed()), placements)
        assertEquals(listOf(3, 3, 3), placements.map { it.laneCount })
    }

    @Test
    fun testScheduleWithManyIntervalsHasNoCollisionsAndOptimalWidth() {
        // Fixed input exercises short, long, nested and touching intervals without depending on layout internals.
        val sessions = (0 until 90).map { index ->
            item("s$index", (index * 37) % 240, (index * 37) % 240 + 5 + (index * 11) % 60)
        }
        val placements = TimelineLayout.place(sessions)
        assertNoOverlapsShareLane(sessions, placements)
        val peak = sessions.map { instant ->
            sessions.count { it.start <= instant.start && it.end > instant.start }
        }.max()
        assertEquals(peak, placements.maxOfOrNull { it.laneCount })
        assertEquals(sessions.map { it.id }.toSet(), placements.map { it.id }.toSet())
    }

    @Test
    fun testEmptyInput() {
        assertEquals(emptyList<TimelinePlacement>(), TimelineLayout.place(emptyList()))
    }

    @Test
    fun testDenseColumnGivesEveryConcurrentSessionReadableWidth() {
        val sessions = (0 until 5).map { item("session-$it", it * 5, 60) }
        val column = TimelineLayout.column(sessions, minimumColumnWidth = 150.0, minimumLaneWidth = 160.0)

        assertEquals(5, column.maximumLaneCount)
        assertTrue(column.placements.all { column.width * it.relativeWidth >= 160 })
        assertEquals(
            TimelineLayout.column(sessions.reversed(), minimumColumnWidth = 150.0, minimumLaneWidth = 160.0).placements,
            column.placements
        )
        assertNoOverlapsShareLane(sessions, column.placements)
        assertNoHorizontalCollisions(sessions, column)
    }

    @Test
    fun testLaterClustersRegainColumnWidthAccordingToTheirOwnConcurrency() {
        val dense = (0 until 4).map { item("dense-$it", 0, 30) }
        val sessions = dense + listOf(item("isolated", 40, 50), item("pair-a", 60, 90), item("pair-b", 70, 80))
        val column = TimelineLayout.column(sessions, minimumColumnWidth = 150.0, minimumLaneWidth = 160.0)
        val map = uniqueMap(column.placements)

        assertEquals(4, column.maximumLaneCount)
        assertEquals(0.0, map["isolated"]?.relativeX)
        assertEquals(1.0, map["isolated"]?.relativeWidth)
        assertEquals(0.5, map["pair-a"]?.relativeWidth)
        assertEquals(0.5, map["pair-b"]?.relativeWidth)
        assertTrue(column.placements.all { column.width * it.relativeWidth >= 160 })
        assertNoHorizontalCollisions(sessions, column)
    }

    @Test
    fun testColumnRetainsBaseWidthWhenSessionsAlreadyFit() {
        val column = TimelineLayout.column(
            listOf(item("a", 0, 30), item("b", 10, 20)),
            minimumColumnWidth = 300.0, minimumLaneWidth = 140.0
        )
        assertEquals(300.0, column.width, 0.0)
        assertTrue(column.placements.all { column.width * it.relativeWidth >= 140 })

        val empty = TimelineLayout.column(emptyList(), minimumColumnWidth = 300.0, minimumLaneWidth = 140.0)
        assertEquals(300.0, empty.width, 0.0)
        assertTrue(empty.placements.isEmpty())
    }

    /** Mirrors Dictionary(uniqueKeysWithValues:), which traps on a repeated placement ID. */
    private fun uniqueMap(placements: List<TimelinePlacement>): Map<String, TimelinePlacement> {
        val map = placements.associateBy { it.id }
        assertEquals("Placement IDs must be unique", placements.size, map.size)
        return map
    }

    private fun assertNoHorizontalCollisions(sessions: List<TimelineItem>, column: TimelineColumnLayout) {
        val map = uniqueMap(column.placements)
        for (index in sessions.indices) {
            val first = sessions[index]
            for (second in sessions.drop(index + 1)) {
                if (!(first.start < second.end && second.start < first.end)) continue
                val a = map[first.id]
                val b = map[second.id]
                if (a == null || b == null) {
                    fail("Missing session placement")
                    continue
                }
                val aLeft = column.width * a.relativeX
                val aRight = aLeft + column.width * a.relativeWidth
                val bLeft = column.width * b.relativeX
                val bRight = bLeft + column.width * b.relativeWidth
                assertTrue(
                    "${first.id} and ${second.id} collide horizontally",
                    aRight <= bLeft + 0.000_001 || bRight <= aLeft + 0.000_001
                )
            }
        }
    }

    private fun assertNoOverlapsShareLane(sessions: List<TimelineItem>, placements: List<TimelinePlacement>) {
        val map = uniqueMap(placements)
        for (index in sessions.indices) {
            val first = sessions[index]
            for (second in sessions.drop(index + 1)) {
                if (!(first.start < second.end && second.start < first.end)) continue
                assertNotEquals("${first.id} and ${second.id} share a lane", map[first.id]?.lane, map[second.id]?.lane)
            }
        }
    }
}
