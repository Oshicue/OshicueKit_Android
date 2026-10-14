package com.oshicue.kit.core

import java.time.ZoneId

/** A horizontally scrollable venue column's overlap cluster layout. */
data class TimelineItem(val id: String, val start: java.time.Instant, val end: java.time.Instant)

data class TimelinePlacement(val id: String, val lane: Int, val laneCount: Int) {
    val relativeX: Double get() = lane.toDouble() / laneCount
    val relativeWidth: Double get() = 1.0 / laneCount
}

data class TimelineColumnLayout(
    val placements: List<TimelinePlacement>,
    val width: Double,
    val maximumLaneCount: Int
)

object TimelineLayout {
    fun column(sessions: List<TimelineItem>, minimumColumnWidth: Double, minimumLaneWidth: Double): TimelineColumnLayout {
        require(minimumColumnWidth.isFinite() && minimumColumnWidth > 0)
        require(minimumLaneWidth.isFinite() && minimumLaneWidth > 0)
        val placements = place(sessions)
        val maximumLaneCount = maxOf(1, placements.maxOfOrNull { it.laneCount } ?: 1)
        return TimelineColumnLayout(
            placements = placements,
            width = maxOf(minimumColumnWidth, maximumLaneCount * minimumLaneWidth),
            maximumLaneCount = maximumLaneCount
        )
    }

    /** Sessions that just touch at an endpoint share a lane. Width resets for each connected overlap cluster. */
    fun place(sessions: List<TimelineItem>): List<TimelinePlacement> {
        val sorted = sessions.sortedWith(compareBy({ it.start }, { it.end }, { it.id }))
        val placements = mutableListOf<TimelinePlacement>()
        var cluster = mutableListOf<TimelineItem>()
        var clusterEnd: java.time.Instant? = null

        fun placeCluster() {
            val laneEnds = mutableListOf<java.time.Instant>()
            val assigned = mutableListOf<Pair<String, Int>>()
            for (item in cluster) {
                val freeLane = laneEnds.indexOfFirst { it <= item.start }
                val lane = if (freeLane >= 0) freeLane else laneEnds.size
                if (freeLane >= 0) laneEnds[freeLane] = item.end else laneEnds.add(item.end)
                assigned.add(item.id to lane)
            }
            placements.addAll(assigned.map { TimelinePlacement(it.first, it.second, laneEnds.size) })
        }

        for (item in sorted) {
            val end = clusterEnd
            if (end != null && item.start >= end) {
                placeCluster()
                cluster = mutableListOf()
                clusterEnd = null
            }
            cluster.add(item)
            clusterEnd = maxOf(clusterEnd ?: item.end, item.end)
        }
        if (cluster.isNotEmpty()) placeCluster()
        return placements
    }
}

/** Utility for schedule grouping used by screens. */
fun ZoneId.kitStartOfDay(date: java.time.Instant): java.time.Instant =
    date.atZone(this).toLocalDate().atStartOfDay(this).toInstant()
