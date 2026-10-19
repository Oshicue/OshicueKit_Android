package com.oshicue.kit.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/** Port of Tests/AgendaTransfersTests.swift. */
class AgendaTransfersTests {
    @Before
    fun pinLanguage() {
        // Resolving "System" reads android.os.LocaleList, which is unavailable on the JVM.
        L10n.setLanguage(AppLanguage.En)
    }

    private val tokyo = AgendaPlace(name = "会场", address = "Tokyo", latitude = 35.65, longitude = 139.75)
    private val narita = AgendaPlace(name = "成田机场", address = "NRT", latitude = 35.77, longitude = 140.39)
    private val haneda = AgendaPlace(name = "羽田机场", address = "HND", latitude = 35.54, longitude = 139.78)

    private fun date(text: String): Instant = KitDate.parseTimestamp(text)!!

    private fun entry(
        id: String,
        kind: AgendaKind = AgendaKind.Live,
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
            timeZone = zone, location = tokyo, flights = if (kind == AgendaKind.Flight) listOf(flight) else emptyList()
        )
    }

    /** Mirrors issue(_:path:_:): an AgendaError with this key and, when given, this exact path. */
    private fun issue(category: String, path: String? = null, message: String = "", operation: () -> Unit) {
        val error = assertThrows(message, Throwable::class.java) { operation() }
        assertEquals("$message thrown: $error", "agenda.error.$category", (error as? AgendaError)?.key)
        if (path != null) assertEquals(message, path, (error as? AgendaError)?.path)
    }

    /** The encoded document as a mutable JSON tree (JsonObject is a reference type, unlike [String: Any]). */
    private fun encodedObject(entry: AgendaEntry): JsonObject {
        val text = AgendaImporter.encode(AgendaDocument(entries = listOf(entry)))
        return unwrap(JsonParser.parse(text) as? JsonObject)
    }

    private fun firstEntry(root: JsonObject): JsonObject = (root["entries"] as JsonArray).items[0] as JsonObject

    private fun json(root: JsonObject): String = root.serialize()

    private fun <T> unwrap(value: T?, message: String = ""): T {
        assertNotNull(message, value)
        return value!!
    }

    /** Kotlin has no defaulted AgendaTransfer(from:to:); pass the same defaults explicitly. */
    private fun transfer(from: AgendaEntry, to: AgendaEntry) = AgendaTransfer(from, to, from.endsAt, to.startsAt)

    private fun AgendaEntry.withFirstFlight(update: (AgendaFlight) -> AgendaFlight): AgendaEntry =
        copy(flights = listOf(update(flights[0])) + flights.drop(1))

    @Test
    fun testEveryKindRoundTripsInOneCalendar() {
        val entries = AgendaKind.entries.map { entry(it.rawValue, kind = it) }
        val document = AgendaDocument(entries = entries)
        val restored = AgendaImporter.parse(AgendaImporter.encode(document))
        assertEquals(document, restored)
        assertEquals(AgendaKind.entries.toSet(), AgendaCore.entriesOn("2026-11-21", restored).map { it.kind }.toSet())
    }

    @Test
    fun testPlaceAddressDefaultsAndMissingKindPreservesLegacyFlights() {
        val root = encodedObject(entry("legacy"))
        val first = firstEntry(root)
        first.fields.remove("kind")
        first["location"] = JsonObject().put("name", "Arena")
        first["flights"] = JsonArray(mutableListOf<JsonValue>(JsonObject().put("number", "NH 920")))
        val restored = unwrap(AgendaImporter.parse(json(root)).entries.firstOrNull())
        assertEquals(AgendaKind.Live, restored.kind)
        assertEquals(AgendaPlace(name = "Arena"), restored.location)
        assertEquals(listOf("NH 920"), restored.flights.map { it.number })
    }

    @Test
    fun testRejectsUnknownNullAndNonStringKindsAtTheirField() {
        for (value in listOf<JsonValue>(JsonString("concert"), JsonNull, JsonNumber("7"))) {
            val root = encodedObject(entry("one"))
            firstEntry(root)["kind"] = value
            val data = json(root)
            issue("json", path = "$.entries[0].kind", message = "kind ${value.serialize()}") { AgendaImporter.parse(data) }
        }
    }

    @Test
    fun testRejectsMalformedPlacesAndCoordinates() {
        for (place in listOf(
            AgendaPlace(name = " "), AgendaPlace(name = "Arena", latitude = 35.0),
            AgendaPlace(name = "Arena", latitude = 91.0, longitude = 139.0),
            AgendaPlace(name = "Arena", latitude = 35.0, longitude = -181.0),
            AgendaPlace(name = "Arena", latitude = Double.POSITIVE_INFINITY, longitude = 139.0)
        )) {
            val one = entry("one").copy(location = place)
            issue("location", message = "$place") { AgendaImporter.validate(AgendaDocument(entries = listOf(one))) }
        }
        val root = encodedObject(entry("one"))
        firstEntry(root)["location"] = JsonObject().put("name", "Arena").put("latitude", "north").put("longitude", 139)
        val data = json(root)
        issue("json", path = "$.entries[0].location.latitude") { AgendaImporter.parse(data) }
        val flight = entry("flight", kind = AgendaKind.Flight)
            .withFirstFlight { it.copy(arrivalLocation = AgendaPlace(name = "Airport", latitude = 35.0)) }
        issue("location", path = "$.entries[0].flights[0].arrivalLocation") {
            AgendaImporter.validate(AgendaDocument(entries = listOf(flight)))
        }
    }

    @Test
    fun testFlightActivityRequiresOneCompleteAuthoritativeFlight() {
        val valid = entry("flight", kind = AgendaKind.Flight)
        AgendaImporter.validate(AgendaDocument(entries = listOf(valid)))
        val invalid = mutableListOf<AgendaEntry>()
        invalid += valid.copy(flights = emptyList())
        invalid += valid.copy(flights = valid.flights + valid.flights[0])
        invalid += valid.withFirstFlight { it.copy(departureAt = null) }
        invalid += valid.withFirstFlight { it.copy(arrivalAt = null) }
        invalid += valid.withFirstFlight { it.copy(departureTimeZone = null) }
        invalid += valid.withFirstFlight { it.copy(arrivalTimeZone = null) }
        invalid += valid.copy(startsAt = valid.startsAt.minusSeconds(60))
        invalid += valid.copy(endsAt = valid.endsAt.plusSeconds(60))
        invalid += valid.copy(timeZone = "Asia/Shanghai")
        invalid += valid.copy(
            isAllDay = true,
            startsAt = date("2026-11-21T00:00:00+09:00"), endsAt = date("2026-11-22T00:00:00+09:00")
        )
        invalid.forEachIndexed { index, value ->
            issue("flightActivity", message = "invalid[$index]") { AgendaImporter.validate(AgendaDocument(entries = listOf(value))) }
        }
    }

    @Test
    fun testAllTwentyFiveDirectedKindsGenerateTransfers() {
        for (sourceKind in AgendaKind.entries) {
            for (targetKind in AgendaKind.entries) {
                val source = entry("source", kind = sourceKind)
                val target = entry("target", kind = targetKind, start = "2026-11-21T11:00:00+09:00", end = "2026-11-21T12:00:00+09:00")
                val document = AgendaDocument(entries = listOf(target, source))
                AgendaImporter.validate(document)
                val label = "$sourceKind → $targetKind"
                val connection = unwrap(AgendaCore.transfersOn("2026-11-21", document).firstOrNull {
                    it.from.id == source.id && it.to.id == target.id
                }, label)
                assertEquals(label, source.endsAt, connection.departureAt)
                assertEquals(label, target.startsAt, connection.arrivalDeadline)
                assertEquals(label, if (sourceKind == AgendaKind.Flight) haneda else tokyo, connection.origin)
                assertEquals(label, if (targetKind == AgendaKind.Flight) narita else tokyo, connection.destination)
            }
        }
    }

    @Test
    fun testFlightUsesArrivalAndDepartureAirportsInsteadOfEntryLocation() {
        val one = entry("one", kind = AgendaKind.Flight); val two = entry("two", kind = AgendaKind.Flight)
        val connection = transfer(one, two)
        assertEquals(haneda, connection.origin)
        assertEquals(narita, connection.destination)
        val missing = one.withFirstFlight { it.copy(arrivalLocation = null) }
        assertNull(transfer(missing, two).origin)
    }

    @Test
    fun testCityOrLegacyFlightDoesNotImplyAPreciseVenue() {
        val one = entry("legacy").copy(
            location = null,
            city = AgendaCity(name = "Tokyo", latitude = 35.68, longitude = 139.69),
            venue = "Arena",
            flights = entry("flight", kind = AgendaKind.Flight).flights
        )
        val connection = transfer(one, one)
        assertNull(connection.origin); assertNull(connection.destination)
    }

    @Test
    fun testAbsoluteOrderOverridesWallClockOrderAcrossTimeZones() {
        val tokyoEntry = entry("tokyo", start = "2026-11-21T22:00:00+09:00", end = "2026-11-21T23:00:00+09:00")
        val laEntry = entry("la", start = "2026-11-21T09:00:00-08:00", end = "2026-11-21T10:00:00-08:00", zone = "America/Los_Angeles")
        val connections = AgendaCore.transfersOn("2026-11-21", AgendaDocument(entries = listOf(laEntry, tokyoEntry)))
        assertEquals(listOf("tokyo"), connections.map { it.from.id })
        assertEquals(listOf("la"), connections.map { it.to.id })
        // availableTime is a TimeInterval (seconds) on iOS and epoch milliseconds (Long) on Android.
        assertEquals(3 * 3600 * 1000L, unwrap(connections.firstOrNull()).availableTime)
    }

    @Test
    fun testAfterMidnightIncludesPreviousDayPredecessorAndIgnoresAllDay() {
        val evening = entry("evening", start = "2026-11-21T22:00:00+09:00", end = "2026-11-21T23:50:00+09:00")
        val next = entry("next", start = "2026-11-22T00:30:00+09:00", end = "2026-11-22T01:30:00+09:00")
        val allDay = entry("all", start = "2026-11-22T00:00:00+09:00", end = "2026-11-23T00:00:00+09:00").copy(isAllDay = true)
        val document = AgendaDocument(entries = listOf(allDay, next, evening))
        val connection = unwrap(AgendaCore.transfersOn("2026-11-22", document).firstOrNull())
        assertEquals(evening.id, connection.from.id); assertEquals(next.id, connection.to.id)
        assertEquals(40 * 60 * 1000L, connection.availableTime) // milliseconds on Android
        assertEquals(listOf(connection.id), AgendaCore.transfersOn("2026-11-21", document).map { it.id })
        assertTrue(AgendaCore.transfersOn("2026-11-23", document).isEmpty())
    }

    @Test
    fun testFlightArrivalCivilDayControlsOutboundTransfer() {
        val flight = entry("flight", kind = AgendaKind.Flight, start = "2026-11-21T19:00:00-08:00", end = "2026-11-22T08:00:00-08:00", zone = "America/Los_Angeles")
            .withFirstFlight { it.copy(arrivalTimeZone = "Asia/Tokyo") }
        val next = entry("show", start = "2026-11-23T03:00:00+09:00", end = "2026-11-23T04:00:00+09:00")
        val document = AgendaDocument(entries = listOf(flight, next))
        assertEquals(1, AgendaCore.transfersOn("2026-11-23", document).size)
        assertTrue(AgendaCore.transfersOn("2026-11-22", document).isEmpty())
    }

    @Test
    fun testWestboundFlightUsesEachAirportCivilDate() {
        val flight = entry("flight", kind = AgendaKind.Flight, start = "2026-11-21T22:00:00+09:00", end = "2026-11-21T15:00:00-08:00")
            .withFirstFlight { it.copy(arrivalTimeZone = "America/Los_Angeles") }
        val document = AgendaDocument(entries = listOf(flight))
        AgendaImporter.validate(document)
        assertEquals(listOf(flight.id), AgendaCore.entriesOn("2026-11-21", document).map { it.id })
        assertTrue(AgendaCore.entriesOn("2026-11-20", document).isEmpty())
        assertTrue(AgendaCore.entriesOn("2026-11-22", document).isEmpty())
    }

    @Test
    fun testDateLineFlightWithEarlierArrivalCivilDateShowsBothDates() {
        val flight = entry("flight", kind = AgendaKind.Flight, start = "2026-11-22T01:00:00+14:00", end = "2026-11-21T03:00:00-10:00", zone = "Pacific/Kiritimati")
            .withFirstFlight { it.copy(arrivalTimeZone = "Pacific/Honolulu") }
        val document = AgendaDocument(entries = listOf(flight))
        AgendaImporter.validate(document)
        for (day in listOf("2026-11-21", "2026-11-22")) {
            assertEquals(day, listOf(flight.id), AgendaCore.entriesOn(day, document).map { it.id })
        }
        for (day in listOf("2026-11-20", "2026-11-23")) {
            assertTrue(day, AgendaCore.entriesOn(day, document).isEmpty())
        }
    }

    @Test
    fun testEastboundFlightIncludesIntermediateCivilDates() {
        val flight = entry("flight", kind = AgendaKind.Flight, start = "2026-11-21T19:00:00-08:00", end = "2026-11-23T01:00:00+09:00", zone = "America/Los_Angeles")
            .withFirstFlight { it.copy(arrivalTimeZone = "Asia/Tokyo") }
        val document = AgendaDocument(entries = listOf(flight))
        AgendaImporter.validate(document)
        for (day in listOf("2026-11-21", "2026-11-22", "2026-11-23")) {
            assertEquals(day, listOf(flight.id), AgendaCore.entriesOn(day, document).map { it.id })
        }
        assertTrue(AgendaCore.entriesOn("2026-11-24", document).isEmpty())
    }

    @Test
    fun testMidnightFlightArrivalRemainsVisibleOnArrivalDay() {
        val flight = entry("flight", kind = AgendaKind.Flight, start = "2026-11-21T23:00:00+09:00", end = "2026-11-22T00:00:00+09:00")
        val live = flight.copy(kind = AgendaKind.Live, id = "live", flights = emptyList())
        val document = AgendaDocument(entries = listOf(live, flight))
        AgendaImporter.validate(document)
        assertEquals(setOf(live.id, flight.id), AgendaCore.entriesOn("2026-11-21", document).map { it.id }.toSet())
        assertEquals(listOf(flight.id), AgendaCore.entriesOn("2026-11-22", document).map { it.id })
    }

    @Test
    fun testOverlapsRemainVisibleAsConflicts() {
        val long = entry("long", end = "2026-11-21T12:00:00+09:00")
        val overlapping = entry("next", start = "2026-11-21T11:00:00+09:00", end = "2026-11-21T13:00:00+09:00")
        val connection = unwrap(AgendaCore.transfersOn("2026-11-21", AgendaDocument(entries = listOf(overlapping, long))).firstOrNull())
        assertTrue(connection.hasTimeConflict); assertEquals(-3600 * 1000L, connection.availableTime) // milliseconds
    }

    @Test
    fun testHotelStayUsesCheckInAndCheckoutWithoutBlockingShows() {
        val hotel = entry("hotel", kind = AgendaKind.Hotel, start = "2026-11-21T15:00:00+09:00", end = "2026-11-23T11:00:00+09:00")
        val show = entry("show", start = "2026-11-21T18:00:00+09:00", end = "2026-11-21T20:00:00+09:00")
        val dinner = entry("dinner", kind = AgendaKind.Restaurant, start = "2026-11-21T21:00:00+09:00", end = "2026-11-21T22:00:00+09:00")
        val flight = entry("flight", kind = AgendaKind.Flight, start = "2026-11-23T14:00:00+09:00", end = "2026-11-23T16:00:00+09:00")
        val document = AgendaDocument(entries = listOf(hotel, show, dinner, flight))
        val checkIn = unwrap(AgendaCore.transfersOn("2026-11-21", document).firstOrNull())
        assertEquals(hotel, checkIn.from)
        assertEquals(show, checkIn.to)
        assertEquals(hotel.startsAt, checkIn.departureAt)
        assertFalse(checkIn.hasTimeConflict)
        val checkout = unwrap(AgendaCore.transfersOn("2026-11-23", document).lastOrNull())
        assertEquals(hotel, checkout.from); assertEquals(flight, checkout.to)
        assertEquals(hotel.endsAt, checkout.departureAt)
        assertEquals(3 * 3600 * 1000L, checkout.availableTime) // milliseconds
        assertTrue(AgendaCore.transfersOn("2026-11-21", AgendaDocument(entries = listOf(hotel))).isEmpty())
    }

    @Test
    fun testMilestoneTransferIDsAreStableAndUnique() {
        val one = entry("one", kind = AgendaKind.Hotel, start = "2026-11-21T09:00:00+09:00", end = "2026-11-21T12:00:00+09:00")
        val two = entry("two", kind = AgendaKind.Hotel, start = "2026-11-21T10:00:00+09:00", end = "2026-11-21T13:00:00+09:00")
        val document = AgendaDocument(entries = listOf(one, two))
        val ids = AgendaCore.transfersOn("2026-11-21", document).map { it.id }
        assertEquals(3, ids.size); assertEquals(3, ids.toSet().size)
        assertEquals(ids, AgendaCore.transfersOn("2026-11-21", AgendaDocument(entries = listOf(two, one))).map { it.id })
    }

    @Test
    fun testEmptyAndInvalidSelectedDatesHaveNoConnections() {
        assertTrue(AgendaCore.transfersOn("2026-02-30", AgendaDocument(entries = listOf(entry("one"), entry("two")))).isEmpty())
        assertTrue(AgendaCore.transfersOn("2026-11-21", AgendaDocument()).isEmpty())
    }
}
