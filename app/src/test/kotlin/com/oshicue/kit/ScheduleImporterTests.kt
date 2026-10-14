package com.oshicue.kit

import com.oshicue.kit.core.FestivalSession
import com.oshicue.kit.core.ImportError
import com.oshicue.kit.core.ScheduleDates
import com.oshicue.kit.core.ScheduleDocument
import com.oshicue.kit.core.ScheduleImporter
import com.oshicue.kit.core.SessionKind
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ScheduleImporterTests {
    private fun validObjectJson(): String = """
        {"schemaVersion":1,"events":[{"id":"festival-2026","name":"Example Festival","city":"Tokyo","timeZone":"Asia/Tokyo",
        "venues":[{"id":"hall","name":"Hall","color":"#78C9EB"}],
        "artists":[{"id":"canonical-group","name":"Example Group"}],
        "sessions":[{"id":"live-1","artistID":"canonical-group","venueID":"hall","kind":"live",
        "startsAt":"2026-10-14T23:40:00+09:00","endsAt":"2026-10-15T00:20:00+09:00"}]}]}
    """.trimIndent()

    private fun assertImportError(text: String, key: String, path: String) {
        val error = assertFailsWith<ImportError> { ScheduleImporter.parse(text) }
        assertEquals(key, error.key)
        assertEquals(path, error.path)
        assertTrue(error.detail.isNotEmpty())
    }

    @Test
    fun testValidOffsetAndMidnightOverlap() {
        val document = ScheduleImporter.parse(validObjectJson())
        val event = document.events.first()
        assertEquals(1, event.sessions.size)
        assertEquals(40L * 60, event.sessions[0].endsAt.epochSecond - event.sessions[0].startsAt.epochSecond)
        assertEquals(2, event.days.size)
        assertEquals(1, event.sessions(event.days[0]).size)
        assertEquals(1, event.sessions(event.days[1]).size)
        assertTrue(event.sessions(event.days[1].plusDays(1)).isEmpty())
    }

    @Test
    fun testMidnightEndDoesNotAddAnEmptyDay() {
        val text = validObjectJson().replace("2026-10-15T00:20:00+09:00", "2026-10-15T00:00:00+09:00")
        val document = ScheduleImporter.parse(text)
        assertEquals(1, document.events[0].days.size)
    }

    @Test
    fun testRoundTripAndFractionalSeconds() {
        val text = validObjectJson().replace("2026-10-14T23:40:00+09:00", "2026-10-14T14:40:00.123Z")
        val document = ScheduleImporter.parse(text)
        assertEquals(document, ScheduleImporter.parse(ScheduleImporter.encode(document)))
    }

    @Test
    fun testTimestampMustIncludeAnOffset() {
        val text = validObjectJson().replace("2026-10-14T23:40:00+09:00", "2026-10-14T23:40:00")
        assertImportError(text, "import.error.timestamp", "$.events[0].sessions[0].startsAt")
    }

    @Test
    fun testInvalidCalendarDateAndOffsetAreRejected() {
        for (value in listOf("2026-02-30T12:00:00+09:00", "2026-10-14T12:00:00+09:70", "2026-10-14T25:00:00+09:00")) {
            val text = validObjectJson().replace("2026-10-14T23:40:00+09:00", value)
            assertImportError(text, "import.error.timestamp", "$.events[0].sessions[0].startsAt")
        }
    }

    @Test
    fun testInvalidTimeZoneAndColor() {
        val zoneText = validObjectJson().replace("\"timeZone\":\"Asia/Tokyo\"", "\"timeZone\":\"Mars/Olympus\"")
        assertImportError(zoneText, "import.error.timeZone", "$.events[0].timeZone")
        val colorText = validObjectJson().replace("\"color\":\"#78C9EB\"", "\"color\":\"blue\"")
        assertImportError(colorText, "import.error.color", "$.events[0].venues[0].color")
    }

    @Test
    fun testUnknownReferencesAndNonPositiveDuration() {
        val artistText = validObjectJson().replace("\"artistID\":\"canonical-group\"", "\"artistID\":\"missing\"")
        assertImportError(artistText, "import.error.artistReference", "$.events[0].sessions[0].artistID")
        val venueText = validObjectJson().replace("\"venueID\":\"hall\"", "\"venueID\":\"missing\"")
        assertImportError(venueText, "import.error.venueReference", "$.events[0].sessions[0].venueID")
        val durationText = validObjectJson().replace("2026-10-15T00:20:00+09:00", "2026-10-14T23:40:00+09:00")
        assertImportError(durationText, "import.error.duration", "$.events[0].sessions[0].endsAt")
    }

    @Test
    fun testDuplicateEventAndSessionIDs() {
        val one = """{"id":"festival-2026","name":"Example Festival","city":"Tokyo","timeZone":"Asia/Tokyo","venues":[],"artists":[],"sessions":[]}"""
        assertImportError("""{"schemaVersion":1,"events":[$one,$one]}""", "import.error.duplicateID", "$.events[1].id")
        val session = """{"id":"live-1","artistID":"canonical-group","venueID":"hall","kind":"live","startsAt":"2026-10-14T23:40:00+09:00","endsAt":"2026-10-15T00:20:00+09:00"}"""
        val text = """{"schemaVersion":1,"events":[{"id":"festival-2026","name":"Example Festival","city":"Tokyo","timeZone":"Asia/Tokyo","venues":[{"id":"hall","name":"Hall","color":"#78C9EB"}],"artists":[{"id":"canonical-group","name":"Example Group"}],"sessions":[$session,$session]}]}"""
        assertImportError(text, "import.error.duplicateID", "$.events[0].sessions[1].id")
    }

    @Test
    fun testDuplicateArtistAndVenueIDs() {
        val artist = """{"id":"canonical-group","name":"Example Group"}"""
        val artistText = """{"schemaVersion":1,"events":[{"id":"festival-2026","name":"F","city":"Tokyo","timeZone":"Asia/Tokyo","venues":[{"id":"hall","name":"Hall","color":"#78C9EB"}],"artists":[$artist,$artist],"sessions":[]}]}"""
        assertImportError(artistText, "import.error.duplicateID", "$.events[0].artists[1].id")
        val venue = """{"id":"hall","name":"Hall","color":"#78C9EB"}"""
        val venueText = """{"schemaVersion":1,"events":[{"id":"festival-2026","name":"F","city":"Tokyo","timeZone":"Asia/Tokyo","venues":[$venue,$venue],"artists":[],"sessions":[]}]}"""
        assertImportError(venueText, "import.error.duplicateID", "$.events[0].venues[1].id")
    }

    @Test
    fun testDSTRepeatedLocalHourRepresentsTwoDistinctInstants() {
        val summerOffset = ScheduleDates.parseTimestamp("2026-11-01T01:30:00-04:00")!!
        val winterOffset = ScheduleDates.parseTimestamp("2026-11-01T01:30:00-05:00")!!
        assertEquals(3600L, winterOffset.epochSecond - summerOffset.epochSecond)
        assertEquals("2026-11-01T05:30:00.000Z", ScheduleDates.timestamp(summerOffset))
    }

    @Test
    fun testEmptyStoreAndLongSessionArePermitted() {
        ScheduleImporter.parse("""{"schemaVersion":1,"events":[]}""")
        val text = validObjectJson().replace("2026-10-15T00:20:00+09:00", "2026-10-17T12:00:00+09:00")
        val document = ScheduleImporter.parse(text)
        assertEquals(4, document.events[0].days.size)
    }

    @Test
    fun testPackagedTemplatesAreValid() {
        for (filename in listOf("timetable-template.json", "demo-timetable.json")) {
            val document = ScheduleImporter.parse(File("src/main/assets/$filename").readText())
            assertEquals(2, document.events.size, filename)
            assertTrue(document.events.flatMap { it.sessions }.isNotEmpty(), filename)
        }
    }

    @Test
    fun testSchemaMissingAndWrongTypeFields() {
        val schema = validObjectJson().replace("\"schemaVersion\":1", "\"schemaVersion\":2")
        assertImportError(schema, "import.error.schemaVersion", "$.schemaVersion")
        val missing = validObjectJson().replace("\"name\":\"Example Festival\",", "")
        assertImportError(missing, "import.error.missingField", "$.events[0].name")
        val wrongType = validObjectJson().replace("\"name\":\"Example Festival\"", "\"name\":42")
        assertImportError(wrongType, "import.error.fieldType", "$.events[0].name")
        val empty = validObjectJson().replace("\"name\":\"Example Festival\"", "\"name\":\" \\n \"")
        assertImportError(empty, "import.error.emptyField", "$.events[0].name")
        assertImportError("not json", "import.error.invalidJSON", "$")
    }

    @Test
    fun testUnknownOptionalFieldIsForwardCompatibleButInvalidKindIsNot() {
        val future = validObjectJson().replace("\"city\":\"Tokyo\"", "\"city\":\"Tokyo\",\"futureMetadata\":{\"revision\":3}")
        ScheduleImporter.parse(future)
        val kind = validObjectJson().replace("\"kind\":\"live\"", "\"kind\":\"meeting\"")
        assertImportError(kind, "import.error.invalidJSON", "$.events[0].sessions[0].kind")
    }

    @Test
    fun testMultipleEventsMergeReplacesOnlyMatchingEvent() {
        val document = com.oshicue.kit.core.ScheduleDemo.document(ScheduleDates.parseTimestamp("2026-10-14T04:15:00+09:00")!!)
        val changed = document.events[0].copy(name = "Updated")
        val merged = document.merging(ScheduleDocument(events = listOf(changed)))
        assertEquals(2, merged.events.size)
        assertEquals("Updated", merged.events.first { it.id == changed.id }.name)
        assertEquals(document.events[1], merged.events.first { it.id == document.events[1].id })
    }

    @Test
    fun testDemoTracksNowAtEarlyMorningAndLateNight() {
        for (timestamp in listOf("2026-10-14T04:15:00+09:00", "2026-10-14T23:59:00+09:00")) {
            val now = ScheduleDates.parseTimestamp(timestamp)!!
            val document = com.oshicue.kit.core.ScheduleDemo.document(now)
            ScheduleImporter.validate(document)
            assertEquals(2, document.events.size)
            val event = document.events[0]
            assertTrue(event.venues.size >= 5)
            assertTrue(event.sessions.any { it.startsAt <= now && it.endsAt > now })
            assertTrue(event.days.size >= 2)
            assertTrue(event.sessions.any { it.kind == SessionKind.goods })
            assertEquals(document, ScheduleImporter.parse(ScheduleImporter.encode(document)))
        }
    }
}
