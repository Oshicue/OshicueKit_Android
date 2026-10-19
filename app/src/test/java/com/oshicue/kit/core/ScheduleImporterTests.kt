package com.oshicue.kit.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.Duration

/** Port of Tests/ScheduleImporterTests.swift. */
class ScheduleImporterTests {
    @Before
    fun pinLanguage() {
        // Resolving "System" reads android.os.LocaleList, which is unavailable on the JVM.
        L10n.setLanguage(AppLanguage.En)
    }

    /** Built as plain maps, like the Swift [String: Any] fixture, so the oracle is independent of the core JSON code. */
    private fun validObject(): MutableMap<String, Any?> = mutableMapOf(
        "schemaVersion" to 1,
        "events" to mutableListOf<Any?>(
            mutableMapOf<String, Any?>(
                "id" to "festival-2026", "name" to "Example Festival", "city" to "Tokyo", "timeZone" to "Asia/Tokyo",
                "venues" to mutableListOf<Any?>(mutableMapOf<String, Any?>("id" to "hall", "name" to "Hall", "color" to "#78C9EB")),
                "artists" to mutableListOf<Any?>(mutableMapOf<String, Any?>("id" to "canonical-group", "name" to "Example Group")),
                "sessions" to mutableListOf<Any?>(
                    mutableMapOf<String, Any?>(
                        "id" to "live-1", "artistID" to "canonical-group", "venueID" to "hall", "kind" to "live",
                        "startsAt" to "2026-10-14T23:40:00+09:00", "endsAt" to "2026-10-15T00:20:00+09:00"
                    )
                )
            )
        )
    )

    /** Equivalent of JSONSerialization.data(withJSONObject:options: [.sortedKeys]). */
    private fun text(value: Any?): String = StringBuilder().also { writeFixture(it, value) }.toString()

    private fun writeFixture(builder: StringBuilder, value: Any?) {
        when (value) {
            null -> builder.append("null")
            is String -> {
                builder.append('"')
                for (c in value) {
                    when {
                        c == '"' -> builder.append("\\\"")
                        c == '\\' -> builder.append("\\\\")
                        c < ' ' -> builder.append(String.format(java.util.Locale.ROOT, "\\u%04x", c.code))
                        else -> builder.append(c)
                    }
                }
                builder.append('"')
            }
            is Boolean, is Int, is Long -> builder.append(value.toString())
            is Map<*, *> -> {
                builder.append('{')
                value.keys.map { it as String }.sorted().forEachIndexed { index, key ->
                    if (index > 0) builder.append(',')
                    writeFixture(builder, key)
                    builder.append(':')
                    writeFixture(builder, value[key])
                }
                builder.append('}')
            }
            is List<*> -> {
                builder.append('[')
                value.forEachIndexed { index, item ->
                    if (index > 0) builder.append(',')
                    writeFixture(builder, item)
                }
                builder.append(']')
            }
            else -> throw IllegalArgumentException("Unsupported fixture value $value")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun editedEvent(transform: (MutableMap<String, Any?>) -> Unit): String {
        val obj = validObject()
        val events = obj["events"] as MutableList<Any?>
        transform(events[0] as MutableMap<String, Any?>)
        return text(obj)
    }

    @Suppress("UNCHECKED_CAST")
    private fun editedSession(transform: (MutableMap<String, Any?>) -> Unit): String = editedEvent { event ->
        val sessions = event["sessions"] as MutableList<Any?>
        transform(sessions[0] as MutableMap<String, Any?>)
    }

    private fun assertImportError(text: String, key: String, path: String) {
        val failure = runCatching { ScheduleImporter.parse(text) }.exceptionOrNull()
            ?: return fail("Expected ImportError for $key at $path, but parsing succeeded")
        if (failure !is ImportError) return fail("Expected ImportError, received $failure")
        assertEquals(key, failure.key)
        assertEquals(path, failure.path)
        assertFalse(failure.detail.isEmpty())
    }

    private fun <T : Any> unwrap(value: T?, message: String = "Expected a non-null value"): T {
        assertNotNull(message, value)
        return value!!
    }

    @Test
    fun testValidOffsetAndMidnightOverlap() {
        val document = ScheduleImporter.parse(text(validObject()))
        val event = unwrap(document.events.firstOrNull())
        assertEquals(1, event.sessions.size)
        assertEquals(Duration.ofMinutes(40), Duration.between(event.sessions[0].startsAt, event.sessions[0].endsAt))
        assertEquals(2, event.days.size)
        assertEquals(1, event.sessionsOn(event.days[0]).size)
        assertEquals(1, event.sessionsOn(event.days[1]).size)
        val thirdDay = event.days[1].plusDays(1)
        assertTrue(event.sessionsOn(thirdDay).isEmpty())
    }

    @Test
    fun testMidnightEndDoesNotAddAnEmptyDay() {
        val document = ScheduleImporter.parse(editedSession { it["endsAt"] = "2026-10-15T00:00:00+09:00" })
        assertEquals(1, document.events[0].days.size)
    }

    @Test
    fun testRoundTripAndFractionalSeconds() {
        val document = ScheduleImporter.parse(editedSession { it["startsAt"] = "2026-10-14T14:40:00.123Z" })
        assertEquals(document, ScheduleImporter.parse(ScheduleImporter.encode(document)))
    }

    @Test
    fun testTimestampMustIncludeAnOffset() {
        assertImportError(
            editedSession { it["startsAt"] = "2026-10-14T23:40:00" },
            key = "import.error.timestamp", path = "$.events[0].sessions[0].startsAt"
        )
    }

    @Test
    fun testInvalidCalendarDateAndOffsetAreRejected() {
        for (value in listOf("2026-02-30T12:00:00+09:00", "2026-10-14T12:00:00+09:70", "2026-10-14T25:00:00+09:00")) {
            assertImportError(
                editedSession { it["startsAt"] = value },
                key = "import.error.timestamp", path = "$.events[0].sessions[0].startsAt"
            )
        }
    }

    @Test
    fun testInvalidTimeZoneAndColor() {
        assertImportError(
            editedEvent { it["timeZone"] = "Mars/Olympus" },
            key = "import.error.timeZone", path = "$.events[0].timeZone"
        )
        assertImportError(
            editedEvent { it["venues"] = mutableListOf<Any?>(mutableMapOf<String, Any?>("id" to "hall", "name" to "Hall", "color" to "blue")) },
            key = "import.error.color", path = "$.events[0].venues[0].color"
        )
    }

    @Test
    fun testUnknownReferencesAndNonPositiveDuration() {
        assertImportError(
            editedSession { it["artistID"] = "missing" },
            key = "import.error.artistReference", path = "$.events[0].sessions[0].artistID"
        )
        assertImportError(
            editedSession { it["venueID"] = "missing" },
            key = "import.error.venueReference", path = "$.events[0].sessions[0].venueID"
        )
        assertImportError(
            editedSession { it["endsAt"] = it["startsAt"] },
            key = "import.error.duration", path = "$.events[0].sessions[0].endsAt"
        )
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun testDuplicateEventAndSessionIDs() {
        val obj = validObject()
        val event = (obj["events"] as List<Any?>)[0]
        obj["events"] = mutableListOf(event, event)
        assertImportError(text(obj), key = "import.error.duplicateID", path = "$.events[1].id")
        assertImportError(editedEvent { ev ->
            val session = (ev["sessions"] as List<Any?>)[0]
            ev["sessions"] = mutableListOf(session, session)
        }, key = "import.error.duplicateID", path = "$.events[0].sessions[1].id")
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun testDuplicateArtistAndVenueIDs() {
        assertImportError(editedEvent { ev ->
            val artist = (ev["artists"] as List<Any?>)[0]
            ev["artists"] = mutableListOf(artist, artist)
        }, key = "import.error.duplicateID", path = "$.events[0].artists[1].id")
        assertImportError(editedEvent { ev ->
            val venue = (ev["venues"] as List<Any?>)[0]
            ev["venues"] = mutableListOf(venue, venue)
        }, key = "import.error.duplicateID", path = "$.events[0].venues[1].id")
    }

    @Test
    fun testDSTRepeatedLocalHourRepresentsTwoDistinctInstants() {
        val summerOffset = unwrap(KitDate.parseTimestamp("2026-11-01T01:30:00-04:00"))
        val winterOffset = unwrap(KitDate.parseTimestamp("2026-11-01T01:30:00-05:00"))
        assertEquals(Duration.ofSeconds(3600), Duration.between(summerOffset, winterOffset))
        assertEquals("2026-11-01T05:30:00.000Z", KitDate.timestamp(summerOffset))
    }

    @Test
    fun testEmptyStoreAndLongSessionArePermitted() {
        ScheduleImporter.parse("""{"schemaVersion":1,"events":[]}""")
        val document = ScheduleImporter.parse(editedSession { it["endsAt"] = "2026-10-17T12:00:00+09:00" })
        assertEquals(4, document.events[0].days.size)
    }

    @Test
    fun testPackagedTemplatesAreValid() {
        // iOS reads the repository's Templates folder; the Android port packages its templates as app assets.
        val root = File(System.getProperty("user.dir") ?: ".")
        val templates = listOf(File(root, "src/main/assets/templates"), File(root, "app/src/main/assets/templates"))
            .firstOrNull { it.isDirectory } ?: return fail("Packaged templates directory not found from $root")
        for (filename in listOf("timetable-template.json", "demo-timetable.json")) {
            val file = File(templates, filename)
            assertTrue("Missing packaged template ${file.path}", file.isFile)
            val document = ScheduleImporter.parse(file.readText(Charsets.UTF_8))
            assertEquals(2, document.events.size)
            assertFalse(document.events.flatMap { it.sessions }.isEmpty())
        }
    }

    @Test
    fun testSchemaMissingAndWrongTypeFields() {
        val obj = validObject()
        obj["schemaVersion"] = 2
        assertImportError(text(obj), key = "import.error.schemaVersion", path = "$.schemaVersion")
        assertImportError(editedEvent { it.remove("name") }, key = "import.error.missingField", path = "$.events[0].name")
        assertImportError(editedEvent { it["name"] = 42 }, key = "import.error.fieldType", path = "$.events[0].name")
        assertImportError(editedEvent { it["name"] = " \n " }, key = "import.error.emptyField", path = "$.events[0].name")
        assertImportError("not json", key = "import.error.invalidJSON", path = "$")
    }

    @Test
    fun testUnknownOptionalFieldIsForwardCompatibleButInvalidKindIsNot() {
        ScheduleImporter.parse(editedEvent { it["futureMetadata"] = mutableMapOf<String, Any?>("revision" to 3) })
        // Swift decodes an unknown enum raw value as DecodingError.dataCorrupted, mapped to invalidJSON.
        assertImportError(
            editedSession { it["kind"] = "meeting" },
            key = "import.error.invalidJSON", path = "$.events[0].sessions[0].kind"
        )
    }

    @Test
    fun testMultipleEventsMergeReplacesOnlyMatchingEvent() {
        val document = ScheduleDemo.document(now = unwrap(KitDate.parseTimestamp("2026-10-14T04:15:00+09:00")))
        val changedEvent = document.events[0].copy(name = "Updated")
        val merged = document.merging(ScheduleDocument(events = listOf(changedEvent)))
        assertEquals(2, merged.events.size)
        assertEquals("Updated", merged.events.firstOrNull { it.id == changedEvent.id }?.name)
        assertEquals(document.events[1], merged.events.firstOrNull { it.id == document.events[1].id })
    }

    @Test
    fun testDemoTracksNowAtEarlyMorningAndLateNight() {
        for (timestamp in listOf("2026-10-14T04:15:00+09:00", "2026-10-14T23:59:00+09:00")) {
            val now = unwrap(KitDate.parseTimestamp(timestamp))
            val document = ScheduleDemo.document(now = now)
            ScheduleImporter.validate(document)
            assertEquals(2, document.events.size)
            val event = document.events[0]
            assertTrue(event.venues.size >= 5)
            assertTrue("A session is live at $timestamp", event.sessions.any { it.startsAt <= now && it.endsAt > now })
            assertTrue(event.days.size >= 2)
            assertTrue(event.sessions.any { it.kind == SessionKind.Goods })
            assertEquals(document, ScheduleImporter.parse(ScheduleImporter.encode(document)))
        }
    }
}
