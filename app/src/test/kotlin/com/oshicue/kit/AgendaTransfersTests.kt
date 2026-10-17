package com.oshicue.kit

import com.oshicue.kit.core.AgendaCity
import com.oshicue.kit.core.AgendaDocument
import com.oshicue.kit.core.AgendaEntry
import com.oshicue.kit.core.AgendaFlight
import com.oshicue.kit.core.AgendaImporter
import com.oshicue.kit.core.AgendaKind
import com.oshicue.kit.core.AgendaTransfer
import com.oshicue.kit.core.ScheduleDates
import com.oshicue.kit.core.agendaTransfers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AgendaTransfersTests {
    private val tokyo = com.oshicue.kit.core.AgendaPlace("会场", "Tokyo", 35.65, 139.75)
    private val narita = com.oshicue.kit.core.AgendaPlace("成田机场", "NRT", 35.77, 140.39)
    private val haneda = com.oshicue.kit.core.AgendaPlace("羽田机场", "HND", 35.54, 139.78)
    private fun date(text: String) = ScheduleDates.parseTimestamp(text)!!

    private fun entry(
        id: String,
        kind: AgendaKind = AgendaKind.live,
        start: String = "2026-11-21T09:00:00+09:00",
        end: String = "2026-11-21T10:00:00+09:00",
        zone: String = "Asia/Tokyo"
    ): AgendaEntry {
        val flight = AgendaFlight(
            number = "NH 1", departureAt = date(start), arrivalAt = date(end),
            departureTimeZone = zone, arrivalTimeZone = zone,
            departureLocation = narita, arrivalLocation = haneda
        )
        return AgendaEntry(
            id = id, title = id, kind = kind, startsAt = date(start), endsAt = date(end),
            timeZone = zone, location = tokyo,
            flights = if (kind == AgendaKind.flight) listOf(flight) else emptyList()
        )
    }

    @Test
    fun testEveryKindRoundTripsInOneCalendar() {
        val entries = AgendaKind.entries.map { entry(it.raw, it) }
        val document = AgendaDocument(entries)
        assertEquals(document, AgendaImporter.parse(AgendaImporter.encode(document)))
        assertEquals(AgendaKind.entries.toSet(), com.oshicue.kit.core.AgendaCore.entries("2026-11-21", document).map { it.kind }.toSet())
    }

    @Test
    fun testAllTwentyFiveDirectedKindsGenerateTransfers() {
        for (sourceKind in AgendaKind.entries) {
            for (targetKind in AgendaKind.entries) {
                val source = entry("source", sourceKind)
                val target = entry("target", targetKind, start = "2026-11-21T11:00:00+09:00", end = "2026-11-21T12:00:00+09:00")
                val document = AgendaDocument(listOf(target, source))
                AgendaImporter.validate(document)
                val connection = agendaTransfers("2026-11-21", document).first {
                    it.from.id == source.id && it.to.id == target.id
                }
                assertEquals(source.endsAt, connection.departureAt)
                assertEquals(target.startsAt, connection.arrivalDeadline)
                assertEquals(if (sourceKind == AgendaKind.flight) haneda else tokyo, connection.origin)
                assertEquals(if (targetKind == AgendaKind.flight) narita else tokyo, connection.destination)
            }
        }
    }

    @Test
    fun testFlightUsesArrivalAndDepartureAirportsInsteadOfEntryLocation() {
        val one = entry("one", AgendaKind.flight)
        val two = entry("two", AgendaKind.flight)
        val connection = AgendaTransfer(one, two)
        assertEquals(haneda, connection.origin)
        assertEquals(narita, connection.destination)
        val missing = one.copy(flights = one.flights.map { it.copy(arrivalLocation = null) })
        assertNull(AgendaTransfer(missing, two).origin)
    }

    @Test
    fun testCityOrLegacyFlightDoesNotImplyAPreciseVenue() {
        val one = entry("legacy").copy(
            location = null,
            city = AgendaCity("Tokyo", latitude = 35.68, longitude = 139.69),
            venue = "Arena",
            flights = entry("flight", AgendaKind.flight).flights
        )
        val connection = AgendaTransfer(one, one)
        assertNull(connection.origin)
        assertNull(connection.destination)
    }

    @Test
    fun testAbsoluteOrderOverridesWallClockOrderAcrossTimeZones() {
        val tokyoEntry = entry("tokyo", start = "2026-11-21T22:00:00+09:00", end = "2026-11-21T23:00:00+09:00")
        val laEntry = entry(
            "la", start = "2026-11-21T09:00:00-08:00", end = "2026-11-21T10:00:00-08:00",
            zone = "America/Los_Angeles"
        )
        val connections = agendaTransfers("2026-11-21", AgendaDocument(listOf(laEntry, tokyoEntry)))
        assertEquals(listOf("tokyo"), connections.map { it.from.id })
        assertEquals(listOf("la"), connections.map { it.to.id })
        assertEquals(3.0 * 3600, connections.first().availableSeconds)
    }

    @Test
    fun testAfterMidnightIncludesPreviousDayPredecessorAndIgnoresAllDay() {
        val evening = entry("evening", start = "2026-11-21T22:00:00+09:00", end = "2026-11-21T23:50:00+09:00")
        val next = entry("next", start = "2026-11-22T00:30:00+09:00", end = "2026-11-22T01:30:00+09:00")
        val allDay = entry("all", start = "2026-11-22T00:00:00+09:00", end = "2026-11-23T00:00:00+09:00")
            .copy(isAllDay = true)
        val document = AgendaDocument(listOf(allDay, next, evening))
        val connection = agendaTransfers("2026-11-22", document).first()
        assertEquals("evening", connection.from.id)
        assertEquals("next", connection.to.id)
        assertEquals(40.0 * 60, connection.availableSeconds)
        assertEquals(listOf(connection.id), agendaTransfers("2026-11-21", document).map { it.id })
        assertTrue(agendaTransfers("2026-11-23", document).isEmpty())
    }

    @Test
    fun testFlightArrivalCivilDayControlsOutboundTransfer() {
        var flight = entry(
            "flight", AgendaKind.flight,
            start = "2026-11-21T19:00:00-08:00", end = "2026-11-22T08:00:00-08:00",
            zone = "America/Los_Angeles"
        )
        flight = flight.copy(flights = flight.flights.map { it.copy(arrivalTimeZone = "Asia/Tokyo") })
        val next = entry("show", start = "2026-11-23T03:00:00+09:00", end = "2026-11-23T04:00:00+09:00")
        val document = AgendaDocument(listOf(flight, next))
        assertEquals(1, agendaTransfers("2026-11-23", document).size)
        assertTrue(agendaTransfers("2026-11-22", document).isEmpty())
    }

    @Test
    fun testWestboundFlightUsesEachAirportCivilDate() {
        var flight = entry(
            "flight", AgendaKind.flight,
            start = "2026-11-21T22:00:00+09:00", end = "2026-11-21T15:00:00-08:00"
        )
        flight = flight.copy(flights = flight.flights.map { it.copy(arrivalTimeZone = "America/Los_Angeles") })
        val document = AgendaDocument(listOf(flight))
        AgendaImporter.validate(document)
        assertEquals(listOf(flight.id), com.oshicue.kit.core.AgendaCore.entries("2026-11-21", document).map { it.id })
        assertTrue(com.oshicue.kit.core.AgendaCore.entries("2026-11-20", document).isEmpty())
        assertTrue(com.oshicue.kit.core.AgendaCore.entries("2026-11-22", document).isEmpty())
    }

    @Test
    fun testDateLineFlightWithEarlierArrivalCivilDateShowsBothDates() {
        var flight = entry(
            "flight", AgendaKind.flight,
            start = "2026-11-22T01:00:00+14:00", end = "2026-11-21T03:00:00-10:00",
            zone = "Pacific/Kiritimati"
        )
        flight = flight.copy(flights = flight.flights.map { it.copy(arrivalTimeZone = "Pacific/Honolulu") })
        val document = AgendaDocument(listOf(flight))
        AgendaImporter.validate(document)
        for (day in listOf("2026-11-21", "2026-11-22")) {
            assertEquals(listOf(flight.id), com.oshicue.kit.core.AgendaCore.entries(day, document).map { it.id })
        }
        for (day in listOf("2026-11-20", "2026-11-23")) {
            assertTrue(com.oshicue.kit.core.AgendaCore.entries(day, document).isEmpty())
        }
    }

    @Test
    fun testEastboundFlightIncludesIntermediateCivilDates() {
        var flight = entry(
            "flight", AgendaKind.flight,
            start = "2026-11-21T19:00:00-08:00", end = "2026-11-23T01:00:00+09:00",
            zone = "America/Los_Angeles"
        )
        flight = flight.copy(flights = flight.flights.map { it.copy(arrivalTimeZone = "Asia/Tokyo") })
        val document = AgendaDocument(listOf(flight))
        AgendaImporter.validate(document)
        for (day in listOf("2026-11-21", "2026-11-22", "2026-11-23")) {
            assertEquals(listOf(flight.id), com.oshicue.kit.core.AgendaCore.entries(day, document).map { it.id })
        }
        assertTrue(com.oshicue.kit.core.AgendaCore.entries("2026-11-24", document).isEmpty())
    }

    @Test
    fun testMidnightFlightArrivalRemainsVisibleOnArrivalDay() {
        val flight = entry(
            "flight", AgendaKind.flight,
            start = "2026-11-21T23:00:00+09:00", end = "2026-11-22T00:00:00+09:00"
        )
        val live = entry("live", start = "2026-11-21T23:00:00+09:00", end = "2026-11-22T00:00:00+09:00")
        val document = AgendaDocument(listOf(live, flight))
        AgendaImporter.validate(document)
        assertEquals(
            setOf(live.id, flight.id),
            com.oshicue.kit.core.AgendaCore.entries("2026-11-21", document).map { it.id }.toSet()
        )
        assertEquals(listOf(flight.id), com.oshicue.kit.core.AgendaCore.entries("2026-11-22", document).map { it.id })
    }

    @Test
    fun testOverlapsRemainVisibleAsConflicts() {
        val long = entry("long", end = "2026-11-21T12:00:00+09:00")
        val overlapping = entry("next", start = "2026-11-21T11:00:00+09:00", end = "2026-11-21T13:00:00+09:00")
        val connection = agendaTransfers("2026-11-21", AgendaDocument(listOf(overlapping, long))).first()
        assertTrue(connection.hasTimeConflict)
        assertEquals(-3600.0, connection.availableSeconds)
    }

    @Test
    fun testHotelStayUsesCheckInAndCheckoutWithoutBlockingShows() {
        val hotel = entry(
            "hotel", AgendaKind.hotel,
            start = "2026-11-21T15:00:00+09:00", end = "2026-11-23T11:00:00+09:00"
        )
        val show = entry("show", start = "2026-11-21T18:00:00+09:00", end = "2026-11-21T20:00:00+09:00")
        val dinner = entry("dinner", AgendaKind.restaurant, start = "2026-11-21T21:00:00+09:00", end = "2026-11-21T22:00:00+09:00")
        val flight = entry("flight", AgendaKind.flight, start = "2026-11-23T14:00:00+09:00", end = "2026-11-23T16:00:00+09:00")
        val document = AgendaDocument(listOf(hotel, show, dinner, flight))
        val checkIn = agendaTransfers("2026-11-21", document).first()
        assertEquals(hotel, checkIn.from)
        assertEquals(show, checkIn.to)
        assertEquals(hotel.startsAt, checkIn.departureAt)
        assertFalse(checkIn.hasTimeConflict)
        val checkout = agendaTransfers("2026-11-23", document).last()
        assertEquals(hotel, checkout.from)
        assertEquals(flight, checkout.to)
        assertEquals(hotel.endsAt, checkout.departureAt)
        assertEquals(3.0 * 3600, checkout.availableSeconds)
        assertTrue(agendaTransfers("2026-11-21", AgendaDocument(listOf(hotel))).isEmpty())
    }

    @Test
    fun testMilestoneTransferIDsAreStableAndUnique() {
        val one = entry("one", AgendaKind.hotel, start = "2026-11-21T09:00:00+09:00", end = "2026-11-21T12:00:00+09:00")
        val two = entry("two", AgendaKind.hotel, start = "2026-11-21T10:00:00+09:00", end = "2026-11-21T13:00:00+09:00")
        val ids = agendaTransfers("2026-11-21", AgendaDocument(listOf(one, two))).map { it.id }
        assertEquals(3, ids.size)
        assertEquals(3, ids.toSet().size)
        assertEquals(ids, agendaTransfers("2026-11-21", AgendaDocument(listOf(two, one))).map { it.id })
    }

    @Test
    fun testEmptyAndInvalidSelectedDatesHaveNoConnections() {
        assertTrue(agendaTransfers("2026-02-30", AgendaDocument(listOf(entry("one"), entry("two")))).isEmpty())
        assertTrue(agendaTransfers("2026-11-21", AgendaDocument()).isEmpty())
    }

    @Test
    fun testPlaceAddressDefaultsAndMissingKindPreservesLegacyFlights() {
        val text = """{"schemaVersion":1,"entries":[{"id":"legacy","title":"Legacy","startsAt":"2026-11-21T09:00:00+09:00","endsAt":"2026-11-21T10:00:00+09:00","timeZone":"Asia/Tokyo","location":{"name":"Arena"},"flights":[{"number":"NH 920"}]}]}"""
        val restored = AgendaImporter.parse(text).entries.first()
        assertEquals(AgendaKind.live, restored.kind)
        assertEquals(com.oshicue.kit.core.AgendaPlace("Arena"), restored.location)
        assertEquals(listOf("NH 920"), restored.flights.map { it.number })
    }

    @Test
    fun testRejectsMalformedPlacesAndCoordinates() {
        for (place in listOf(
            com.oshicue.kit.core.AgendaPlace(" "),
            com.oshicue.kit.core.AgendaPlace("Arena", latitude = 35.0),
            com.oshicue.kit.core.AgendaPlace("Arena", latitude = 91.0, longitude = 139.0),
            com.oshicue.kit.core.AgendaPlace("Arena", latitude = 35.0, longitude = -181.0),
            com.oshicue.kit.core.AgendaPlace("Arena", latitude = Double.POSITIVE_INFINITY, longitude = 139.0)
        )) {
            val one = entry("one").copy(location = place)
            val error = kotlin.runCatching { AgendaImporter.validate(AgendaDocument(listOf(one))) }.exceptionOrNull()
            assertEquals("agenda.error.location", (error as? com.oshicue.kit.core.AgendaError)?.key)
        }
    }
}
