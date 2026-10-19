package com.oshicue.kit.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.TimeZone

/** Port of Tests/AgendaTests.swift. */
class AgendaTests {
    @Before
    fun pinLanguage() {
        // Resolving "System" reads android.os.LocaleList, which is unavailable on the JVM.
        L10n.setLanguage(AppLanguage.En)
    }

    private fun date(text: String): Instant = KitDate.parseTimestamp(text)!!

    private fun plan(
        id: String = "one",
        start: String = "2026-11-21T17:00:00+09:00",
        end: String = "2026-11-21T20:00:00+09:00",
        zone: String = "Asia/Tokyo",
        allDay: Boolean = false
    ): AgendaEntry = AgendaEntry(
        id = id, title = "东京演出", startsAt = date(start), endsAt = date(end),
        timeZone = zone, isAllDay = allDay
    )

    /** Mirrors assertIssue: the thrown error is an AgendaError with this key and a "$"-rooted path. */
    private fun assertIssue(category: String, operation: () -> Unit) {
        val error = assertThrows(Throwable::class.java) { operation() }
        val issue = error as? AgendaError
        assertEquals("thrown: $error", "agenda.error.$category", issue?.key)
        assertTrue("path: ${issue?.path}", issue?.path?.startsWith("$") == true)
    }

    private fun <T> unwrap(value: T?): T {
        assertNotNull(value)
        return value!!
    }

    @Test
    fun testMinimalImportDefaultsOptionalFields() {
        val json = """{"schemaVersion":1,"entries":[{"id":"one","title":"Show","startsAt":"2026-11-21T17:00:00+09:00","endsAt":"2026-11-21T20:00:00+09:00","timeZone":"Asia/Tokyo"}]}"""
        val entry = unwrap(AgendaImporter.parse(json).entries.firstOrNull())
        assertFalse(entry.isAllDay); assertNull(entry.city)
        assertEquals(AgendaKind.Live, entry.kind); assertNull(entry.location)
        assertEquals(emptyList<AgendaFlight>(), entry.flights); assertEquals("", entry.venue); assertEquals("", entry.notes)
    }

    @Test
    fun testRoundTripKeepsCityFlightsAndUnicodeNotes() {
        val entry = plan().copy(
            city = AgendaCity(name = "东京", country = "日本", latitude = 35.6762, longitude = 139.6503),
            notes = "往返 ✈️\n会场见。",
            flights = listOf(
                AgendaFlight(
                    number = "NH 920", airline = "ANA", departureAirport = "PVG", arrivalAirport = "NRT",
                    departureAt = date("2026-11-21T09:00:00+08:00"), arrivalAt = date("2026-11-21T13:00:00+09:00"),
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
        val document = AgendaDocument(entries = listOf(plan("one", "2026-11-21T00:10:00+09:00", "2026-11-21T00:30:00+09:00")))
        // The XCTest ran under the simulator's zone. Run the same assertions under the JVM's own
        // zone first, then under extreme device zones, restoring the default afterwards.
        val original = TimeZone.getDefault()
        val deviceZones = listOf(original) + listOf(
            "Pacific/Kiritimati", "Pacific/Pago_Pago", "America/Los_Angeles", "UTC", "Asia/Tokyo"
        ).map { TimeZone.getTimeZone(it) }
        try {
            for (device in deviceZones) {
                TimeZone.setDefault(device)
                assertEquals("device ${ZoneId.systemDefault()}", 1, AgendaCore.entriesOn("2026-11-21", document).size)
                assertTrue("device ${ZoneId.systemDefault()}", AgendaCore.entriesOn("2026-11-20", document).isEmpty())
            }
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test
    fun testOvernightAndExclusiveMidnightEnd() {
        var entry = plan("one", "2026-11-21T23:00:00+09:00", "2026-11-22T01:00:00+09:00")
        for (day in listOf("2026-11-21", "2026-11-22")) {
            assertEquals(day, 1, AgendaCore.entriesOn(day, AgendaDocument(entries = listOf(entry))).size)
        }
        entry = entry.copy(endsAt = date("2026-11-22T00:00:00+09:00"))
        assertTrue(AgendaCore.entriesOn("2026-11-22", AgendaDocument(entries = listOf(entry))).isEmpty())
    }

    @Test
    fun testAllDayAcrossDaylightSavingTimeUsesCivilDates() {
        val entry = plan("one", "2026-03-07T00:00:00-08:00", "2026-03-10T00:00:00-07:00", zone = "America/Los_Angeles", allDay = true)
        val document = AgendaDocument(entries = listOf(entry)); AgendaImporter.validate(document)
        for (day in listOf("2026-03-07", "2026-03-08", "2026-03-09")) {
            assertEquals(day, 1, AgendaCore.entriesOn(day, document).size)
        }
        assertTrue(AgendaCore.entriesOn("2026-03-10", document).isEmpty())
        assertEquals(Duration.ofHours(71), Duration.between(entry.startsAt, entry.endsAt))
    }

    @Test
    fun testSortsByDestinationWallClockWithAllDayFirst() {
        val allDay = plan("all", "2026-11-21T00:00:00+09:00", "2026-11-22T00:00:00+09:00", allDay = true)
        val morning = plan("morning", "2026-11-21T09:00:00-08:00", "2026-11-21T10:00:00-08:00", zone = "America/Los_Angeles")
        assertEquals(
            listOf("all", "morning", "evening"),
            AgendaCore.entriesOn("2026-11-21", AgendaDocument(entries = listOf(plan("evening"), morning, allDay))).map { it.id }
        )
    }

    @Test
    fun testMergeUpdatesMatchingIDsAndPreservesUnrelatedPlans() {
        val original = AgendaDocument(entries = listOf(plan(), plan("keep")))
        val update = plan().copy(title = "更新活动")
        val merged = original.merging(AgendaDocument(entries = listOf(update, plan("new"))))
        assertEquals(3, merged.entries.size); assertEquals(update, merged.entries[0])
        assertEquals(original.entries[1], merged.entries[1]); assertEquals("东京演出", original.entries[0].title)
        assertEquals(merged, merged.merging(AgendaImporter.parse(AgendaImporter.encode(merged))))
    }

    @Test
    fun testDuplicateIDsAcrossFilesAreRejected() {
        val document = AgendaDocument(entries = listOf(plan()))
        assertIssue("id") { AgendaImporter.combining(listOf(document, document)) }
    }

    @Test
    fun testInvalidImportCannotPartiallyMerge() {
        val original = AgendaDocument(entries = listOf(plan())); val invalid = plan("bad").copy(title = " ")
        assertIssue("title") { original.merging(AgendaDocument(entries = listOf(plan("good"), invalid))) }
        assertEquals(1, original.entries.size)
    }

    @Test
    fun testRejectsInvalidRangeZoneTitleAndIDs() {
        var entry = plan(); entry = entry.copy(endsAt = entry.startsAt)
        assertIssue("range") { AgendaImporter.validate(AgendaDocument(entries = listOf(entry))) }
        entry = plan().copy(timeZone = "Wrong/Zone")
        assertIssue("zone") { AgendaImporter.validate(AgendaDocument(entries = listOf(entry))) }
        entry = plan().copy(title = " \n")
        assertIssue("title") { AgendaImporter.validate(AgendaDocument(entries = listOf(entry))) }
        assertIssue("id") { AgendaImporter.validate(AgendaDocument(entries = listOf(plan(), plan()))) }
        entry = plan().copy(id = " ")
        assertIssue("id") { AgendaImporter.validate(AgendaDocument(entries = listOf(entry))) }
    }

    @Test
    fun testRejectsNonMidnightAllDayAndInvalidCoordinates() {
        assertIssue("allDay") { AgendaImporter.validate(AgendaDocument(entries = listOf(plan(allDay = true)))) }
        var entry = plan().copy(city = AgendaCity(name = "Tokyo", latitude = 91.0, longitude = 139.0))
        assertIssue("city") { AgendaImporter.validate(AgendaDocument(entries = listOf(entry))) }
        entry = entry.copy(city = AgendaCity(name = "Tokyo", latitude = 35.0))
        assertIssue("city") { AgendaImporter.validate(AgendaDocument(entries = listOf(entry))) }
        entry = entry.copy(city = AgendaCity(name = " "))
        assertIssue("city") { AgendaImporter.validate(AgendaDocument(entries = listOf(entry))) }
    }

    @Test
    fun testFlightsCompareAbsoluteTimesAcrossInternationalDateLine() {
        var entry = plan().copy(
            flights = listOf(
                AgendaFlight(
                    number = "NH 106", departureAt = date("2026-11-21T22:00:00+09:00"), arrivalAt = date("2026-11-21T15:00:00-08:00"),
                    departureTimeZone = "Asia/Tokyo", arrivalTimeZone = "America/Los_Angeles"
                )
            )
        )
        AgendaImporter.validate(AgendaDocument(entries = listOf(entry)))
        entry = entry.copy(flights = listOf(entry.flights[0].copy(arrivalAt = date("2026-11-21T01:00:00Z"))))
        assertIssue("range") { AgendaImporter.validate(AgendaDocument(entries = listOf(entry))) }
    }

    @Test
    fun testFlightTimeNeedsZoneButNumberAloneIsValid() {
        var entry = plan().copy(flights = listOf(AgendaFlight(number = "NH 920")))
        AgendaImporter.validate(AgendaDocument(entries = listOf(entry)))
        entry = entry.copy(flights = listOf(entry.flights[0].copy(departureAt = entry.startsAt)))
        assertIssue("zone") { AgendaImporter.validate(AgendaDocument(entries = listOf(entry))) }
        entry = entry.copy(flights = listOf(AgendaFlight(number = " ")))
        assertIssue("flight") { AgendaImporter.validate(AgendaDocument(entries = listOf(entry))) }
    }

    @Test
    fun testRejectsMalformedTimestampsWithPrecisePaths() {
        val data = AgendaImporter.encode(AgendaDocument(entries = listOf(plan())))
        val root = unwrap(JsonParser.parse(data) as? JsonObject)
        val entry = (root["entries"] as JsonArray).items[0] as JsonObject
        for (value in listOf("2026-02-30T10:00:00Z", "2026-11-21T17:00:00", "2026-11-21T25:00:00+09:00")) {
            entry.put("startsAt", value)
            val json = root.serialize()
            assertIssue("timestamp") { AgendaImporter.parse(json) }
        }
        entry.fields.remove("startsAt")
        val json = root.serialize()
        val error = assertThrows(Throwable::class.java) { AgendaImporter.parse(json) }
        assertEquals("$.entries[0].startsAt", (error as? AgendaError)?.path)
    }

    @Test
    fun testRejectsOversizedNonUTF8AndUnsupportedVersion() {
        // The Swift importer accepts Data; the Kotlin importer accepts text. Build the same bytes and decode
        // them the way the Android import path does (FileSupport.readDocumentText: bytes.toString(UTF_8)).
        val oversized = ByteArray(AgendaImporter.maximumDocumentBytes + 1) { 32 }.toString(Charsets.UTF_8)
        assertIssue("size") { AgendaImporter.parse(oversized) }
        val nonUtf8 = byteArrayOf(0xFF.toByte(), 0xFE.toByte()).toString(Charsets.UTF_8)
        assertIssue("json") { AgendaImporter.parse(nonUtf8) }
        assertIssue("version") { AgendaImporter.validate(AgendaDocument(schemaVersion = 2)) }
    }

    @Test
    fun testEmptyDocumentAndInvalidSelectionAreSafe() {
        assertEquals(AgendaDocument(), AgendaImporter.parse(AgendaImporter.encode(AgendaDocument())))
        assertTrue(AgendaCore.entriesOn("2026-02-30", AgendaDocument(entries = listOf(plan()))).isEmpty())
    }

    @Test
    fun testDraftUsesSelectedCivilDayInEveryZone() {
        for (name in listOf("Asia/Tokyo", "America/Los_Angeles", "Pacific/Kiritimati")) {
            val entry = AgendaCore.draft("2026-11-21", ZoneId.of(name))
            assertEquals(name, "2026-11-21", LedgerDates.string(entry.startsAt, entry.zone))
            assertEquals(name, Duration.ofHours(1), Duration.between(entry.startsAt, entry.endsAt))
        }
    }

    @Test
    fun testBundledTemplateIsValidAndSeparateFromTimetable() {
        // iOS reads Templates/agenda-template.json from the repository; Android bundles the same file as an asset.
        val json = bundledTemplate("agenda-template.json").readText(Charsets.UTF_8)
        val document = AgendaImporter.parse(json)
        assertEquals(AgendaKind.entries.toSet(), document.entries.map { it.kind }.toSet())
        assertEquals(1, document.entries.firstOrNull { it.kind == AgendaKind.Flight }?.flights?.size)
        // ScheduleImporter.parse only ever throws ImportError on iOS.
        assertThrows(ImportError::class.java) { ScheduleImporter.parse(json) }
    }

    /** Gradle runs unit tests from the module directory; also tolerate the project root. */
    private fun bundledTemplate(name: String): File {
        val candidates = listOf(File("src/main/assets/templates/$name"), File("app/src/main/assets/templates/$name"))
        return candidates.firstOrNull { it.isFile } ?: error("Template $name not found from ${File("").absolutePath}")
    }
}
