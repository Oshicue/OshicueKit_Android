package com.oshicue.kit

import com.oshicue.kit.core.AgendaCity
import com.oshicue.kit.core.AgendaCore
import com.oshicue.kit.core.AgendaDocument
import com.oshicue.kit.core.AgendaEntry
import com.oshicue.kit.core.AgendaEntryDeletion
import com.oshicue.kit.core.AgendaEntryFilter
import com.oshicue.kit.core.AgendaFlight
import com.oshicue.kit.core.AgendaImporter
import com.oshicue.kit.core.AgendaKind
import com.oshicue.kit.core.AgendaPlace
import com.oshicue.kit.core.ScheduleDates
import com.oshicue.kit.core.deletingEntries
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AgendaManagementTests {
    private fun date(text: String) = ScheduleDates.parseTimestamp(text)!!
    private fun plan(
        id: String,
        start: String = "2026-11-21T17:00:00+09:00",
        end: String = "2026-11-21T20:00:00+09:00",
        kind: AgendaKind = AgendaKind.live,
        zone: String = "Asia/Tokyo",
        allDay: Boolean = false
    ) = AgendaEntry(
        id = id, title = "Tokyo show", kind = kind,
        startsAt = date(start), endsAt = date(end), timeZone = zone, isAllDay = allDay
    )

    private fun flight(
        id: String, start: String, end: String,
        departureZone: String, arrivalZone: String
    ): AgendaEntry {
        val entry = plan(id, start, end, AgendaKind.flight, departureZone)
        return entry.copy(
            flights = listOf(
                AgendaFlight(
                    number = "NH 106", departureAt = entry.startsAt, arrivalAt = entry.endsAt,
                    departureTimeZone = departureZone, arrivalTimeZone = arrivalZone
                )
            )
        )
    }

    @Test
    fun testInclusiveBoundsMatchOverlappingCivilDatesWithoutEnumeratingLongStays() {
        val entries = listOf(
            plan("before", "2026-11-19T17:00:00+09:00", "2026-11-19T20:00:00+09:00"),
            plan("long", "2020-01-01T00:00:00+09:00", "2040-01-01T00:00:00+09:00", AgendaKind.hotel),
            plan("inside"),
            plan("after", "2026-11-23T17:00:00+09:00", "2026-11-23T20:00:00+09:00")
        )
        assertEquals(
            listOf("long", "inside"),
            AgendaEntryFilter(startDate = "2026-11-21", endDate = "2026-11-21").matching(entries).map { it.id }
        )
        assertEquals(
            listOf("long", "after"),
            AgendaEntryFilter(startDate = "2026-11-23").matching(entries).map { it.id }
        )
        assertEquals(
            listOf("before", "long"),
            AgendaEntryFilter(endDate = "2026-11-19").matching(entries).map { it.id }
        )
        assertTrue(AgendaEntryFilter(startDate = "2040-01-01").matching(entries).isEmpty())
    }

    @Test
    fun testInvalidDateBoundsNeverBroadenToAllEntries() {
        val entries = listOf(plan("one"))
        for (filter in listOf(
            AgendaEntryFilter(startDate = "2026-02-30"),
            AgendaEntryFilter(endDate = "21/11/2026"),
            AgendaEntryFilter(startDate = "2026-11-22", endDate = "2026-11-21")
        )) {
            assertFalse(filter.hasValidDateRange)
            assertTrue(filter.matching(entries).isEmpty())
        }
        assertEquals(entries, AgendaEntryFilter(query = " \n\t ").matching(entries))
    }

    @Test
    fun testMidnightIsExclusiveForTimedAndAllDayPlansButOvernightStillMatches() {
        val entries = listOf(
            plan("timed", "2026-11-21T23:00:00+09:00", "2026-11-22T00:00:00+09:00"),
            plan("allDay", "2026-11-21T00:00:00+09:00", "2026-11-22T00:00:00+09:00", allDay = true),
            plan("overnight", "2026-11-21T23:00:00+09:00", "2026-11-22T00:01:00+09:00")
        )
        val filter = AgendaEntryFilter(startDate = "2026-11-22", endDate = "2026-11-22")
        assertEquals(listOf("overnight"), filter.matching(entries).map { it.id })
        assertEquals(
            filter.matching(entries).map { it.id },
            AgendaCore.entries("2026-11-22", AgendaDocument(entries)).map { it.id }
        )
        assertEquals(3, AgendaEntryFilter(startDate = "2026-11-21", endDate = "2026-11-21").matching(entries).size)
    }

    @Test
    fun testDateBoundsFollowCivilDaysAcrossBothDaylightSavingTransitions() {
        val entries = listOf(
            plan("spring", "2026-03-07T00:00:00-08:00", "2026-03-10T00:00:00-07:00", zone = "America/Los_Angeles", allDay = true),
            plan("fall", "2026-10-31T00:00:00-07:00", "2026-11-03T00:00:00-08:00", zone = "America/Los_Angeles", allDay = true)
        )
        AgendaImporter.validate(AgendaDocument(entries))
        for ((day, expected) in listOf(
            "2026-03-08" to listOf("spring"),
            "2026-03-09" to listOf("spring"),
            "2026-03-10" to emptyList(),
            "2026-11-01" to listOf("fall"),
            "2026-11-02" to listOf("fall"),
            "2026-11-03" to emptyList()
        )) {
            assertEquals(expected, AgendaEntryFilter(startDate = day, endDate = day).matching(entries).map { it.id })
        }
    }

    @Test
    fun testFlightIncludesDepartureAndArrivalCivilDatesEvenAcrossReversedDateLine() {
        val entry = flight(
            "date-line", "2026-11-22T00:30:00+14:00", "2026-11-21T12:30:00-10:00",
            "Pacific/Kiritimati", "Pacific/Honolulu"
        )
        val document = AgendaDocument(listOf(entry))
        AgendaImporter.validate(document)
        assertEquals("2026-11-21", AgendaCore.dateRange(entry)?.first)
        assertEquals("2026-11-22", AgendaCore.dateRange(entry)?.second)
        for (day in listOf("2026-11-21", "2026-11-22")) {
            assertEquals(listOf(entry), AgendaEntryFilter(startDate = day, endDate = day).matching(listOf(entry)))
            assertEquals(listOf(entry), AgendaCore.entries(day, document))
        }
        assertTrue(AgendaEntryFilter(endDate = "2026-11-20").matching(listOf(entry)).isEmpty())
        assertTrue(AgendaEntryFilter(startDate = "2026-11-23").matching(listOf(entry)).isEmpty())
    }

    @Test
    fun testMidnightArrivalBelongsToFlightArrivalDay() {
        val entry = flight(
            "midnight-flight", "2026-11-21T20:00:00+09:00", "2026-11-22T00:00:00+09:00",
            "Asia/Tokyo", "Asia/Tokyo"
        )
        AgendaImporter.validate(AgendaDocument(listOf(entry)))
        assertEquals(
            listOf(entry),
            AgendaEntryFilter(startDate = "2026-11-22", endDate = "2026-11-22").matching(listOf(entry))
        )
    }

    @Test
    fun testIndependentConditionsIntersectAndIdentityOptionsUseExactValues() {
        val entry = plan("target").copy(
            city = AgendaCity("Tokyo", "Japan"),
            location = AgendaPlace("Main Hall", "Shibuya 1"),
            venue = "Legacy Hall",
            flights = listOf(
                AgendaFlight(
                    number = "NH 920", departureAirport = "PVG", arrivalAirport = "NRT",
                    departureTimeZone = "Asia/Shanghai", arrivalTimeZone = "Asia/Tokyo",
                    departureLocation = AgendaPlace("Pudong"), arrivalLocation = AgendaPlace("Narita")
                )
            )
        )
        val base = AgendaEntryFilter(
            startDate = "2026-11-21", endDate = "2026-11-21", kind = AgendaKind.live,
            city = "Tokyo", place = "Main Hall", timeZone = "Asia/Tokyo", isAllDay = false,
            query = "target Japan"
        )
        assertEquals(listOf(entry), base.matching(listOf(entry, plan("other"))))
        for (place in listOf("Main Hall", "Legacy Hall", "PVG", "NRT", "Pudong", "Narita")) {
            assertEquals(listOf(entry), base.copy(place = place).matching(listOf(entry)))
        }
        assertEquals(listOf(entry), base.copy(timeZone = "Asia/Shanghai").matching(listOf(entry)))
        assertTrue(base.copy(city = "tokyo").matching(listOf(entry)).isEmpty())
        assertTrue(base.copy(place = "Hall").matching(listOf(entry)).isEmpty())
        assertTrue(base.copy(kind = AgendaKind.hotel).matching(listOf(entry)).isEmpty())
        assertTrue(base.copy(timeZone = "UTC").matching(listOf(entry)).isEmpty())
        assertTrue(base.copy(isAllDay = true).matching(listOf(entry)).isEmpty())
    }

    @Test
    fun testSearchIncludesEveryTextFieldAndAllLegacyFlightsWithAndTokens() {
        val entry = plan("unique-id", kind = AgendaKind.restaurant).copy(
            title = "Café rendezvous", city = AgendaCity("Tokyo", "Japan"),
            location = AgendaPlace("Supper Room", "Shibuya Avenue"),
            venue = "Old Bistro", notes = "Reservation confirmed",
            flights = listOf(
                AgendaFlight(
                    number = "NH 920", airline = "ANA", departureAirport = "PVG", arrivalAirport = "NRT",
                    departureTimeZone = "Asia/Shanghai", arrivalTimeZone = "Asia/Tokyo",
                    departureLocation = AgendaPlace("Pudong", "Qihang Road"),
                    arrivalLocation = AgendaPlace("Narita", "Chiba Airport Street")
                ),
                AgendaFlight(number = "JL 123", airline = "JAL", departureAirport = "HND", arrivalAirport = "ITM")
            )
        )
        for (query in listOf(
            "unique-id", "CAFE", "restaurant", "Tokyo Japan", "Supper Avenue", "Old Bistro", "confirmed",
            "Asia/Tokyo", "NH 920 ANA PVG NRT", "Pudong Qihang", "Narita Chiba", "Asia/Shanghai",
            "JL 123 JAL HND ITM", " cafe\nconfirmed\tJL "
        )) {
            assertEquals(listOf(entry), AgendaEntryFilter(query = query).matching(listOf(entry)), query)
        }
        assertTrue(AgendaEntryFilter(query = "cafe missing").matching(listOf(entry)).isEmpty())
    }

    @Test
    fun testDeletionConfirmationCountsOnlyExistingSelectionByKind() {
        val entries = listOf(
            plan("one"), plan("two"), plan("hotel", kind = AgendaKind.hotel),
            plan("dinner", kind = AgendaKind.restaurant)
        )
        val deletion = AgendaEntryDeletion(entries, setOf("one", "two", "hotel", "missing"))
        assertEquals(setOf("one", "two", "hotel"), deletion.entryIDs)
        assertEquals(3, deletion.entryCount)
        assertEquals(mapOf(AgendaKind.live to 2, AgendaKind.hotel to 1), deletion.countsByKind)
        val empty = AgendaEntryDeletion(entries, setOf("missing"))
        assertEquals(0, empty.entryCount)
        assertEquals(emptySet(), empty.entryIDs)
        assertEquals(emptyMap(), empty.countsByKind)
    }

    @Test
    fun testDeletionPreservesOrderedRetainedPlansAndTheirCompleteFlightData() {
        val retained = flight(
            "keep-flight", "2026-11-21T09:00:00+08:00", "2026-11-21T13:00:00+09:00",
            "Asia/Shanghai", "Asia/Tokyo"
        ).copy(
            city = AgendaCity("Tokyo", "Japan", 35.6762, 139.6503),
            notes = "Keep\nUnicode ✈️", venue = "Airport",
            flights = listOf(
                AgendaFlight(
                    number = "NH 106", airline = "ANA",
                    departureAt = date("2026-11-21T09:00:00+08:00"),
                    arrivalAt = date("2026-11-21T13:00:00+09:00"),
                    departureTimeZone = "Asia/Shanghai", arrivalTimeZone = "Asia/Tokyo",
                    departureLocation = AgendaPlace("Pudong", "Airport Road", 31.1443, 121.8083)
                )
            )
        )
        val original = AgendaDocument(listOf(plan("delete"), retained, plan("keep-hotel", kind = AgendaKind.hotel)))
        val result = original.deletingEntries(setOf("delete", "unknown"))
        assertEquals(original.entries.drop(1), result.entries)
        assertEquals(original.schemaVersion, result.schemaVersion)
        assertEquals(3, original.entries.size)
        assertEquals(original, original.deletingEntries(emptySet()))
        assertEquals(result, result.deletingEntries(setOf("delete", "unknown")))
        AgendaImporter.validate(result)
        assertEquals(result, AgendaImporter.parse(AgendaImporter.encode(result)))
        assertEquals(
            result.entries + plan("new"),
            result.merging(AgendaDocument(listOf(plan("new")))).entries
        )
    }

    @Test
    fun testDeletingEveryEntryProducesValidEmptyJsonAndDoesNotMutateSource() {
        val original = AgendaDocument(listOf(plan("one"), plan("two", kind = AgendaKind.other)))
        val result = original.deletingEntries(original.entries.map { it.id }.toSet())
        assertEquals(AgendaDocument(), result)
        assertEquals(result, AgendaImporter.parse(AgendaImporter.encode(result)))
        val invalid = plan("invalid").copy(title = " ")
        assertTrue(runCatching { result.merging(AgendaDocument(listOf(plan("new"), invalid))) }.isFailure)
        assertEquals(AgendaDocument(), result)
        assertEquals(2, original.entries.size)
    }
}
