package com.oshicue.kit.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/** Port of Tests/AgendaManagementTests.swift. */
class AgendaManagementTests {
    @Before
    fun pinLanguage() {
        // Resolving "System" reads android.os.LocaleList, which is unavailable on the JVM.
        L10n.setLanguage(AppLanguage.En)
    }

    private fun date(text: String): Instant = KitDate.parseTimestamp(text)!!

    private fun plan(
        id: String,
        start: String = "2026-11-21T17:00:00+09:00",
        end: String = "2026-11-21T20:00:00+09:00",
        kind: AgendaKind = AgendaKind.Live,
        zone: String = "Asia/Tokyo",
        allDay: Boolean = false
    ): AgendaEntry = AgendaEntry(
        id = id, title = "Tokyo show", kind = kind, startsAt = date(start), endsAt = date(end),
        timeZone = zone, isAllDay = allDay
    )

    private fun flight(id: String, start: String, end: String, departureZone: String, arrivalZone: String): AgendaEntry {
        val entry = plan(id, start, end, kind = AgendaKind.Flight, zone = departureZone)
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
            plan("long", "2020-01-01T00:00:00+09:00", "2040-01-01T00:00:00+09:00", kind = AgendaKind.Hotel),
            plan("inside"), plan("after", "2026-11-23T17:00:00+09:00", "2026-11-23T20:00:00+09:00")
        )
        assertEquals(listOf("long", "inside"), AgendaEntryFilter(startDate = "2026-11-21", endDate = "2026-11-21").matching(entries).map { it.id })
        assertEquals(listOf("long", "after"), AgendaEntryFilter(startDate = "2026-11-23").matching(entries).map { it.id })
        assertEquals(listOf("before", "long"), AgendaEntryFilter(endDate = "2026-11-19").matching(entries).map { it.id })
        assertEquals(emptyList<AgendaEntry>(), AgendaEntryFilter(startDate = "2040-01-01").matching(entries))
    }

    @Test
    fun testInvalidDateBoundsNeverBroadenToAllEntries() {
        val entries = listOf(plan("one"))
        for (filter in listOf(
            AgendaEntryFilter(startDate = "2026-02-30"), AgendaEntryFilter(endDate = "21/11/2026"),
            AgendaEntryFilter(startDate = "2026-11-22", endDate = "2026-11-21")
        )) {
            assertFalse("$filter", filter.hasValidDateRange)
            assertTrue("$filter", filter.matching(entries).isEmpty())
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
            AgendaCore.entriesOn("2026-11-22", AgendaDocument(entries = entries)).map { it.id },
            filter.matching(entries).map { it.id }
        )
        assertEquals(3, AgendaEntryFilter(startDate = "2026-11-21", endDate = "2026-11-21").matching(entries).size)
    }

    @Test
    fun testDateBoundsFollowCivilDaysAcrossBothDaylightSavingTransitions() {
        val entries = listOf(
            plan("spring", "2026-03-07T00:00:00-08:00", "2026-03-10T00:00:00-07:00", zone = "America/Los_Angeles", allDay = true),
            plan("fall", "2026-10-31T00:00:00-07:00", "2026-11-03T00:00:00-08:00", zone = "America/Los_Angeles", allDay = true)
        )
        AgendaImporter.validate(AgendaDocument(entries = entries))
        for ((day, expected) in listOf(
            "2026-03-08" to listOf("spring"), "2026-03-09" to listOf("spring"), "2026-03-10" to emptyList(),
            "2026-11-01" to listOf("fall"), "2026-11-02" to listOf("fall"), "2026-11-03" to emptyList()
        )) {
            assertEquals(day, expected, AgendaEntryFilter(startDate = day, endDate = day).matching(entries).map { it.id })
        }
    }

    @Test
    fun testFlightIncludesDepartureAndArrivalCivilDatesEvenAcrossReversedDateLine() {
        val entry = flight(
            "date-line", "2026-11-22T00:30:00+14:00", "2026-11-21T12:30:00-10:00",
            departureZone = "Pacific/Kiritimati", arrivalZone = "Pacific/Honolulu"
        )
        val document = AgendaDocument(entries = listOf(entry)); AgendaImporter.validate(document)
        assertEquals("2026-11-21", AgendaCore.dateRange(entry)?.start)
        assertEquals("2026-11-22", AgendaCore.dateRange(entry)?.end)
        for (day in listOf("2026-11-21", "2026-11-22")) {
            assertEquals(day, listOf(entry), AgendaEntryFilter(startDate = day, endDate = day).matching(listOf(entry)))
            assertEquals(day, listOf(entry), AgendaCore.entriesOn(day, document))
        }
        assertTrue(AgendaEntryFilter(endDate = "2026-11-20").matching(listOf(entry)).isEmpty())
        assertTrue(AgendaEntryFilter(startDate = "2026-11-23").matching(listOf(entry)).isEmpty())
    }

    @Test
    fun testMidnightArrivalBelongsToFlightArrivalDay() {
        val entry = flight(
            "midnight-flight", "2026-11-21T20:00:00+09:00", "2026-11-22T00:00:00+09:00",
            departureZone = "Asia/Tokyo", arrivalZone = "Asia/Tokyo"
        )
        AgendaImporter.validate(AgendaDocument(entries = listOf(entry)))
        assertEquals(listOf(entry), AgendaEntryFilter(startDate = "2026-11-22", endDate = "2026-11-22").matching(listOf(entry)))
    }

    @Test
    fun testIndependentConditionsIntersectAndIdentityOptionsUseExactValues() {
        val entry = plan("target").copy(
            city = AgendaCity(name = "Tokyo", country = "Japan"),
            location = AgendaPlace(name = "Main Hall", address = "Shibuya 1"),
            venue = "Legacy Hall",
            flights = listOf(
                AgendaFlight(
                    number = "NH 920", departureAirport = "PVG", arrivalAirport = "NRT",
                    departureTimeZone = "Asia/Shanghai", arrivalTimeZone = "Asia/Tokyo",
                    departureLocation = AgendaPlace(name = "Pudong"), arrivalLocation = AgendaPlace(name = "Narita")
                )
            )
        )
        val base = AgendaEntryFilter(
            startDate = "2026-11-21", endDate = "2026-11-21", kind = AgendaKind.Live, city = "Tokyo",
            place = "Main Hall", timeZone = "Asia/Tokyo", isAllDay = false, query = "target Japan"
        )
        assertEquals(listOf(entry), base.matching(listOf(entry, plan("other"))))
        for (place in listOf("Main Hall", "Legacy Hall", "PVG", "NRT", "Pudong", "Narita")) {
            val filter = base.copy(place = place)
            assertEquals(place, listOf(entry), filter.matching(listOf(entry)))
        }
        var changed = base.copy(timeZone = "Asia/Shanghai")
        assertEquals(listOf(entry), changed.matching(listOf(entry)))
        changed = base.copy(city = "tokyo"); assertTrue(changed.matching(listOf(entry)).isEmpty())
        changed = base.copy(place = "Hall"); assertTrue(changed.matching(listOf(entry)).isEmpty())
        changed = base.copy(kind = AgendaKind.Hotel); assertTrue(changed.matching(listOf(entry)).isEmpty())
        changed = base.copy(timeZone = "UTC"); assertTrue(changed.matching(listOf(entry)).isEmpty())
        changed = base.copy(isAllDay = true); assertTrue(changed.matching(listOf(entry)).isEmpty())
    }

    @Test
    fun testSearchIncludesEveryTextFieldAndAllLegacyFlightsWithAndTokens() {
        val entry = plan("unique-id", kind = AgendaKind.Restaurant).copy(
            title = "Café rendezvous", city = AgendaCity(name = "Tokyo", country = "Japan"),
            location = AgendaPlace(name = "Supper Room", address = "Shibuya Avenue"),
            venue = "Old Bistro", notes = "Reservation confirmed",
            flights = listOf(
                AgendaFlight(
                    number = "NH 920", airline = "ANA", departureAirport = "PVG", arrivalAirport = "NRT",
                    departureTimeZone = "Asia/Shanghai", arrivalTimeZone = "Asia/Tokyo",
                    departureLocation = AgendaPlace(name = "Pudong", address = "Qihang Road"),
                    arrivalLocation = AgendaPlace(name = "Narita", address = "Chiba Airport Street")
                ),
                AgendaFlight(number = "JL 123", airline = "JAL", departureAirport = "HND", arrivalAirport = "ITM")
            )
        )
        for (query in listOf(
            "unique-id", "CAFE", "restaurant", "Tokyo Japan", "Supper Avenue", "Old Bistro", "confirmed",
            "Asia/Tokyo", "NH 920 ANA PVG NRT", "Pudong Qihang", "Narita Chiba", "Asia/Shanghai", "JL 123 JAL HND ITM",
            " cafe\nconfirmed\tJL "
        )) {
            assertEquals(query, listOf(entry), AgendaEntryFilter(query = query).matching(listOf(entry)))
        }
        assertTrue(AgendaEntryFilter(query = "cafe missing").matching(listOf(entry)).isEmpty())
    }

    @Test
    fun testDeletionConfirmationCountsOnlyExistingSelectionByKind() {
        val entries = listOf(plan("one"), plan("two"), plan("hotel", kind = AgendaKind.Hotel), plan("dinner", kind = AgendaKind.Restaurant))
        val deletion = AgendaEntryDeletion(entries, selectedIds = setOf("one", "two", "hotel", "missing"))
        assertEquals(setOf("one", "two", "hotel"), deletion.entryIds)
        assertEquals(3, deletion.entryCount)
        assertEquals(mapOf(AgendaKind.Live to 2, AgendaKind.Hotel to 1), deletion.countsByKind)
        val empty = AgendaEntryDeletion(entries, selectedIds = setOf("missing"))
        assertEquals(0, empty.entryCount); assertEquals(emptySet<String>(), empty.entryIds)
        assertEquals(emptyMap<AgendaKind, Int>(), empty.countsByKind)
    }

    @Test
    fun testDeletionPreservesOrderedRetainedPlansAndTheirCompleteFlightData() {
        var retained = flight(
            "keep-flight", "2026-11-21T09:00:00+08:00", "2026-11-21T13:00:00+09:00",
            departureZone = "Asia/Shanghai", arrivalZone = "Asia/Tokyo"
        )
        retained = retained.copy(
            city = AgendaCity(name = "Tokyo", country = "Japan", latitude = 35.6762, longitude = 139.6503),
            notes = "Keep\nUnicode ✈️", venue = "Airport",
            flights = listOf(
                retained.flights[0].copy(
                    airline = "ANA",
                    departureLocation = AgendaPlace(name = "Pudong", address = "Airport Road", latitude = 31.1443, longitude = 121.8083)
                )
            )
        )
        val original = AgendaDocument(entries = listOf(plan("delete"), retained, plan("keep-hotel", kind = AgendaKind.Hotel)))
        val result = original.deletingEntries(setOf("delete", "unknown"))
        assertEquals(original.entries.drop(1), result.entries)
        assertEquals(original.schemaVersion, result.schemaVersion)
        assertEquals(3, original.entries.size)
        assertEquals(original, original.deletingEntries(emptySet()))
        assertEquals(result, result.deletingEntries(setOf("delete", "unknown")))
        AgendaImporter.validate(result)
        assertEquals(result, AgendaImporter.parse(AgendaImporter.encode(result)))
        assertEquals(result.entries + plan("new"), result.merging(AgendaDocument(entries = listOf(plan("new")))).entries)
    }

    @Test
    fun testDeletingEveryEntryProducesValidEmptyJsonAndDoesNotMutateSource() {
        val original = AgendaDocument(entries = listOf(plan("one"), plan("two", kind = AgendaKind.Other)))
        val result = original.deletingEntries(original.entries.map { it.id }.toSet())
        assertEquals(AgendaDocument(), result)
        assertEquals(result, AgendaImporter.parse(AgendaImporter.encode(result)))
        val invalid = plan("invalid").copy(title = " ")
        // merging only ever throws AgendaError on iOS.
        assertThrows(AgendaError::class.java) { result.merging(AgendaDocument(entries = listOf(plan("new"), invalid))) }
        assertEquals(AgendaDocument(), result); assertEquals(2, original.entries.size)
    }
}
