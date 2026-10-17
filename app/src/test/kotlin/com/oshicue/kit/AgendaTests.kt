package com.oshicue.kit

import com.oshicue.kit.core.AgendaCity
import com.oshicue.kit.core.AgendaCore
import com.oshicue.kit.core.AgendaDocument
import com.oshicue.kit.core.AgendaEntry
import com.oshicue.kit.core.AgendaError
import com.oshicue.kit.core.AgendaFlight
import com.oshicue.kit.core.AgendaImporter
import com.oshicue.kit.core.AgendaKind
import com.oshicue.kit.core.AgendaPlace
import com.oshicue.kit.core.LedgerDates
import com.oshicue.kit.core.ScheduleDates
import com.oshicue.kit.core.ScheduleImporter
import java.io.File
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AgendaTests {
    private fun date(text: String) = ScheduleDates.parseTimestamp(text)!!
    private fun plan(
        id: String = "one",
        start: String = "2026-11-21T17:00:00+09:00",
        end: String = "2026-11-21T20:00:00+09:00",
        zone: String = "Asia/Tokyo",
        allDay: Boolean = false
    ) = AgendaEntry(
        id = id,
        title = "东京演出",
        startsAt = date(start),
        endsAt = date(end),
        timeZone = zone,
        isAllDay = allDay
    )

    private fun assertIssue(category: String, operation: () -> Unit) {
        val error = assertFailsWith<AgendaError> { operation() }
        assertEquals("agenda.error.$category", error.key)
        assertTrue(error.path.startsWith("$"))
    }

    @Test
    fun testMinimalImportDefaultsOptionalFields() {
        val json = """{"schemaVersion":1,"entries":[{"id":"one","title":"Show","startsAt":"2026-11-21T17:00:00+09:00","endsAt":"2026-11-21T20:00:00+09:00","timeZone":"Asia/Tokyo"}]}"""
        val entry = AgendaImporter.parse(json).entries.first()
        assertFalse(entry.isAllDay)
        assertNull(entry.city)
        assertEquals(AgendaKind.live, entry.kind)
        assertNull(entry.location)
        assertEquals(emptyList(), entry.flights)
        assertEquals("", entry.venue)
        assertEquals("", entry.notes)
    }

    @Test
    fun testRoundTripKeepsCityFlightsAndUnicodeNotes() {
        var entry = plan()
        entry = entry.copy(
            city = AgendaCity("东京", "日本", 35.6762, 139.6503),
            notes = "往返 ✈️\n会场见。",
            flights = listOf(
                AgendaFlight(
                    number = "NH 920", airline = "ANA", departureAirport = "PVG", arrivalAirport = "NRT",
                    departureAt = date("2026-11-21T09:00:00+08:00"),
                    arrivalAt = date("2026-11-21T13:00:00+09:00"),
                    departureTimeZone = "Asia/Shanghai", arrivalTimeZone = "Asia/Tokyo"
                ),
                AgendaFlight(number = "NH 919")
            )
        )
        val document = AgendaDocument(entries = listOf(entry))
        assertEquals(document, AgendaImporter.parse(AgendaImporter.encode(document)))
    }

    @Test
    fun testDestinationDateDoesNotShiftWithDeviceTimeZone() {
        val document = AgendaDocument(listOf(plan("one", "2026-11-21T00:10:00+09:00", "2026-11-21T00:30:00+09:00")))
        assertEquals(1, AgendaCore.entries("2026-11-21", document).size)
        assertTrue(AgendaCore.entries("2026-11-20", document).isEmpty())
    }

    @Test
    fun testOvernightAndExclusiveMidnightEnd() {
        var entry = plan("one", "2026-11-21T23:00:00+09:00", "2026-11-22T01:00:00+09:00")
        for (day in listOf("2026-11-21", "2026-11-22")) {
            assertEquals(1, AgendaCore.entries(day, AgendaDocument(listOf(entry))).size)
        }
        entry = entry.copy(endsAt = date("2026-11-22T00:00:00+09:00"))
        assertTrue(AgendaCore.entries("2026-11-22", AgendaDocument(listOf(entry))).isEmpty())
    }

    @Test
    fun testAllDayAcrossDaylightSavingTimeUsesCivilDates() {
        val entry = plan(
            "one", "2026-03-07T00:00:00-08:00", "2026-03-10T00:00:00-07:00",
            zone = "America/Los_Angeles", allDay = true
        )
        val document = AgendaDocument(listOf(entry))
        AgendaImporter.validate(document)
        for (day in listOf("2026-03-07", "2026-03-08", "2026-03-09")) {
            assertEquals(1, AgendaCore.entries(day, document).size)
        }
        assertTrue(AgendaCore.entries("2026-03-10", document).isEmpty())
        assertEquals(71L * 3600, entry.endsAt.epochSecond - entry.startsAt.epochSecond)
    }

    @Test
    fun testSortsByDestinationWallClockWithAllDayFirst() {
        val allDay = plan("all", "2026-11-21T00:00:00+09:00", "2026-11-22T00:00:00+09:00", allDay = true)
        val morning = plan("morning", "2026-11-21T09:00:00-08:00", "2026-11-21T10:00:00-08:00", zone = "America/Los_Angeles")
        val evening = plan("evening")
        val result = AgendaCore.entries("2026-11-21", AgendaDocument(listOf(evening, morning, allDay)))
        assertEquals(listOf("all", "morning", "evening"), result.map { it.id })
    }

    @Test
    fun testMergeUpdatesMatchingIDsAndPreservesUnrelatedPlans() {
        val original = AgendaDocument(listOf(plan(), plan("keep")))
        val update = plan().copy(title = "更新活动")
        val merged = original.merging(AgendaDocument(listOf(update, plan("new"))))
        assertEquals(3, merged.entries.size)
        assertEquals(update, merged.entries[0])
        assertEquals(original.entries[1], merged.entries[1])
        assertEquals("东京演出", original.entries[0].title)
        assertEquals(merged, AgendaImporter.parse(AgendaImporter.encode(merged)))
    }

    @Test
    fun testDuplicateIDsAcrossFilesAreRejected() {
        val document = AgendaDocument(listOf(plan()))
        assertIssue("id") { AgendaImporter.combining(listOf(document, document)) }
    }

    @Test
    fun testInvalidImportCannotPartiallyMerge() {
        val original = AgendaDocument(listOf(plan()))
        val invalid = plan("bad").copy(title = " ")
        assertIssue("title") { original.merging(AgendaDocument(listOf(plan("good"), invalid))) }
        assertEquals(1, original.entries.size)
    }

    @Test
    fun testRejectsInvalidRangeZoneTitleAndIDs() {
        assertIssue("range") {
            AgendaImporter.validate(AgendaDocument(listOf(plan().copy(endsAt = plan().startsAt))))
        }
        assertIssue("zone") {
            AgendaImporter.validate(AgendaDocument(listOf(plan().copy(timeZone = "Wrong/Zone"))))
        }
        assertIssue("title") {
            AgendaImporter.validate(AgendaDocument(listOf(plan().copy(title = " \n"))))
        }
        assertIssue("id") {
            AgendaImporter.validate(AgendaDocument(listOf(plan(), plan())))
        }
        assertIssue("id") {
            AgendaImporter.validate(AgendaDocument(listOf(plan().copy(id = " "))))
        }
    }

    @Test
    fun testRejectsNonMidnightAllDayAndInvalidCoordinates() {
        assertIssue("allDay") { AgendaImporter.validate(AgendaDocument(listOf(plan(allDay = true)))) }
        assertIssue("city") {
            AgendaImporter.validate(
                AgendaDocument(listOf(plan().copy(city = AgendaCity("Tokyo", latitude = 91.0, longitude = 139.0))))
            )
        }
        assertIssue("city") {
            AgendaImporter.validate(
                AgendaDocument(listOf(plan().copy(city = AgendaCity("Tokyo", latitude = 35.0))))
            )
        }
        assertIssue("city") {
            AgendaImporter.validate(AgendaDocument(listOf(plan().copy(city = AgendaCity(" ")))))
        }
    }

    @Test
    fun testFlightsCompareAbsoluteTimesAcrossInternationalDateLine() {
        var entry = plan().copy(
            flights = listOf(
                AgendaFlight(
                    number = "NH 106",
                    departureAt = date("2026-11-21T22:00:00+09:00"),
                    arrivalAt = date("2026-11-21T15:00:00-08:00"),
                    departureTimeZone = "Asia/Tokyo", arrivalTimeZone = "America/Los_Angeles"
                )
            )
        )
        AgendaImporter.validate(AgendaDocument(listOf(entry)))
        entry = entry.copy(
            flights = entry.flights.map { it.copy(arrivalAt = date("2026-11-21T01:00:00Z")) }
        )
        assertIssue("range") { AgendaImporter.validate(AgendaDocument(listOf(entry))) }
    }

    @Test
    fun testFlightTimeNeedsZoneButNumberAloneIsValid() {
        var entry = plan().copy(flights = listOf(AgendaFlight(number = "NH 920")))
        AgendaImporter.validate(AgendaDocument(listOf(entry)))
        entry = entry.copy(flights = entry.flights.map { it.copy(departureAt = entry.startsAt) })
        assertIssue("zone") { AgendaImporter.validate(AgendaDocument(listOf(entry))) }
        entry = entry.copy(flights = listOf(AgendaFlight(number = " ")))
        assertIssue("flight") { AgendaImporter.validate(AgendaDocument(listOf(entry))) }
    }

    @Test
    fun testRejectsMalformedTimestampsWithPrecisePaths() {
        for (value in listOf(
            "2026-02-30T10:00:00Z",
            "2026-11-21T17:00:00",
            "2026-11-21T25:00:00+09:00"
        )) {
            val text = """{"schemaVersion":1,"entries":[{"id":"one","title":"Show","startsAt":"$value","endsAt":"2026-11-21T20:00:00+09:00","timeZone":"Asia/Tokyo"}]}"""
            val error = assertFailsWith<AgendaError> { AgendaImporter.parse(text) }
            assertEquals("agenda.error.timestamp", error.key)
            assertEquals("$.entries[0].startsAt", error.path)
        }
    }

    @Test
    fun testRejectsOversizedNonUTF8AndUnsupportedVersion() {
        assertIssue("size") {
            AgendaImporter.parse(ByteArray(AgendaImporter.maximumDocumentBytes + 1) { ' '.code.toByte() })
        }
        assertIssue("json") { AgendaImporter.parse(byteArrayOf(0xFF.toByte(), 0xFE.toByte())) }
        assertIssue("version") { AgendaImporter.validate(AgendaDocument(schemaVersion = 2)) }
    }

    @Test
    fun testEmptyDocumentAndInvalidSelectionAreSafe() {
        assertEquals(AgendaDocument(), AgendaImporter.parse(AgendaImporter.encode(AgendaDocument())))
        assertTrue(AgendaCore.entries("2026-02-30", AgendaDocument(listOf(plan()))).isEmpty())
    }

    @Test
    fun testDraftUsesSelectedCivilDayInEveryZone() {
        for (name in listOf("Asia/Tokyo", "America/Los_Angeles", "Pacific/Kiritimati")) {
            val entry = AgendaCore.draft("2026-11-21", ZoneId.of(name))
            assertEquals("2026-11-21", LedgerDates.string(entry.startsAt, entry.timeZoneValue))
            assertEquals(3600L, entry.endsAt.epochSecond - entry.startsAt.epochSecond)
        }
    }

    @Test
    fun testBundledTemplateIsValidAndSeparateFromTimetable() {
        val json = File("src/main/assets/agenda-template.json").readText()
        val document = AgendaImporter.parse(json)
        assertEquals(AgendaKind.entries.toSet(), document.entries.map { it.kind }.toSet())
        assertEquals(1, document.entries.first { it.kind == AgendaKind.flight }.flights.size)
        assertFailsWith<Exception> { ScheduleImporter.parse(json) }
    }

    @Test
    fun testMissingKindDefaultsLiveAndPlaceAddressDefaults() {
        val text = """{"schemaVersion":1,"entries":[{"id":"one","title":"Show","startsAt":"2026-11-21T17:00:00+09:00","endsAt":"2026-11-21T20:00:00+09:00","timeZone":"Asia/Tokyo","location":{"name":"Arena"},"flights":[{"number":"NH 920"}]}]}"""
        val restored = AgendaImporter.parse(text).entries.first()
        assertEquals(AgendaKind.live, restored.kind)
        assertEquals(AgendaPlace("Arena"), restored.location)
        assertEquals(listOf("NH 920"), restored.flights.map { it.number })
    }

    @Test
    fun testRejectsUnknownNullAndNonStringKindsAtTheirField() {
        for (value in listOf("\"concert\"", "null", "7")) {
            val text = """{"schemaVersion":1,"entries":[{"id":"one","title":"Show","kind":$value,"startsAt":"2026-11-21T17:00:00+09:00","endsAt":"2026-11-21T20:00:00+09:00","timeZone":"Asia/Tokyo"}]}"""
            val error = assertFailsWith<AgendaError> { AgendaImporter.parse(text) }
            assertEquals("agenda.error.json", error.key)
            assertEquals("$.entries[0].kind", error.path)
        }
    }

    @Test
    fun testRejectsMalformedPlaceCoordinateTypes() {
        val text = """{"schemaVersion":1,"entries":[{"id":"one","title":"Show","startsAt":"2026-11-21T17:00:00+09:00","endsAt":"2026-11-21T20:00:00+09:00","timeZone":"Asia/Tokyo","location":{"name":"Arena","latitude":"north","longitude":139}}]}"""
        val error = assertFailsWith<AgendaError> { AgendaImporter.parse(text) }
        assertEquals("agenda.error.json", error.key)
        assertEquals("$.entries[0].location.latitude", error.path)
    }

    @Test
    fun testFlightActivityRequiresOneCompleteAuthoritativeFlight() {
        val valid = plan().copy(
            kind = AgendaKind.flight,
            flights = listOf(
                AgendaFlight(
                    number = "NH 1",
                    departureAt = date("2026-11-21T17:00:00+09:00"),
                    arrivalAt = date("2026-11-21T20:00:00+09:00"),
                    departureTimeZone = "Asia/Tokyo", arrivalTimeZone = "Asia/Tokyo"
                )
            )
        )
        AgendaImporter.validate(AgendaDocument(listOf(valid)))
        val invalid = mutableListOf<AgendaEntry>()
        invalid.add(valid.copy(flights = emptyList()))
        invalid.add(valid.copy(flights = valid.flights + valid.flights))
        invalid.add(valid.copy(flights = valid.flights.map { it.copy(departureAt = null) }))
        invalid.add(valid.copy(flights = valid.flights.map { it.copy(arrivalAt = null) }))
        invalid.add(valid.copy(flights = valid.flights.map { it.copy(departureTimeZone = null) }))
        invalid.add(valid.copy(flights = valid.flights.map { it.copy(arrivalTimeZone = null) }))
        invalid.add(valid.copy(startsAt = valid.startsAt.minusSeconds(60)))
        invalid.add(valid.copy(endsAt = valid.endsAt.plusSeconds(60)))
        invalid.add(valid.copy(timeZone = "Asia/Shanghai"))
        invalid.add(
            valid.copy(
                isAllDay = true,
                startsAt = date("2026-11-21T00:00:00+09:00"),
                endsAt = date("2026-11-22T00:00:00+09:00")
            )
        )
        for (entry in invalid) {
            assertIssue("flightActivity") { AgendaImporter.validate(AgendaDocument(listOf(entry))) }
        }
    }
}
