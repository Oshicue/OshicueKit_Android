package com.oshicue.kit

import com.oshicue.kit.core.TimelineItem
import com.oshicue.kit.core.TimelineLayout
import com.oshicue.kit.core.TimelinePlacement
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TimelineLayoutTests {
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
        assertEquals(1, placements.first { it.id == "short-c" }.lane)
        assertNoOverlapsShareLane(sessions, placements)
    }

    @Test
    fun testLayoutIsDeterministicForEqualStarts() {
        val sessions = listOf(item("c", 0, 40), item("b", 0, 20), item("a", 0, 20))
        val placements = TimelineLayout.place(sessions)
        assertEquals(listOf("a", "b", "c"), placements.map { it.id })
        assertEquals(placements, TimelineLayout.place(sessions.reversed()))
        assertEquals(listOf(3, 3, 3), placements.map { it.laneCount })
    }

    @Test
    fun testScheduleWithManyIntervalsHasNoCollisionsAndOptimalWidth() {
        val sessions = (0 until 90).map { index ->
            item("s$index", (index * 37) % 240, (index * 37) % 240 + 5 + (index * 11) % 60)
        }
        val placements = TimelineLayout.place(sessions)
        assertNoOverlapsShareLane(sessions, placements)
        val peak = sessions.maxOf { instant -> sessions.count { it.start <= instant.start && it.end > instant.start } }
        assertEquals(peak, placements.maxOf { it.laneCount })
        assertEquals(sessions.map { it.id }.toSet(), placements.map { it.id }.toSet())
    }

    @Test
    fun testEmptyInput() {
        assertEquals(emptyList(), TimelineLayout.place(emptyList()))
    }

    @Test
    fun testDenseColumnGivesEveryConcurrentSessionReadableWidth() {
        val sessions = (0 until 5).map { item("session-$it", it * 5, 60) }
        val column = TimelineLayout.column(sessions, minimumColumnWidth = 150.0, minimumLaneWidth = 160.0)

        assertEquals(5, column.maximumLaneCount)
        assertTrue(column.placements.all { column.width * it.relativeWidth >= 160 })
        assertEquals(column.placements, TimelineLayout.column(sessions.reversed(), 150.0, 160.0).placements)
        assertNoOverlapsShareLane(sessions, column.placements)
        assertNoHorizontalCollisions(sessions, column)
    }

    @Test
    fun testLaterClustersRegainColumnWidthAccordingToTheirOwnConcurrency() {
        val dense = (0 until 4).map { item("dense-$it", 0, 30) }
        val sessions = dense + listOf(item("isolated", 40, 50), item("pair-a", 60, 90), item("pair-b", 70, 80))
        val column = TimelineLayout.column(sessions, minimumColumnWidth = 150.0, minimumLaneWidth = 160.0)
        val map = column.placements.associateBy { it.id }

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
        assertEquals(300.0, column.width)
        assertTrue(column.placements.all { column.width * it.relativeWidth >= 140 })

        val empty = TimelineLayout.column(emptyList(), minimumColumnWidth = 300.0, minimumLaneWidth = 140.0)
        assertEquals(300.0, empty.width)
        assertTrue(empty.placements.isEmpty())
    }

    private fun assertNoHorizontalCollisions(
        sessions: List<TimelineItem>,
        column: com.oshicue.kit.core.TimelineColumnLayout
    ) {
        val map = column.placements.associateBy { it.id }
        sessions.forEachIndexed { index, first ->
            for (second in sessions.drop(index + 1)) {
                if (first.start < second.end && second.start < first.end) {
                    val a = map.getValue(first.id)
                    val b = map.getValue(second.id)
                    val aLeft = column.width * a.relativeX
                    val aRight = aLeft + column.width * a.relativeWidth
                    val bLeft = column.width * b.relativeX
                    val bRight = bLeft + column.width * b.relativeWidth
                    assertTrue(aRight <= bLeft + 0.000_001 || bRight <= aLeft + 0.000_001)
                }
            }
        }
    }

    private fun assertNoOverlapsShareLane(sessions: List<TimelineItem>, placements: List<TimelinePlacement>) {
        val map = placements.associateBy { it.id }
        sessions.forEachIndexed { index, first ->
            for (second in sessions.drop(index + 1)) {
                if (first.start < second.end && second.start < first.end) {
                    assertTrue(map[first.id]?.lane != map[second.id]?.lane)
                }
            }
        }
    }
}
