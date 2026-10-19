package com.oshicue.kit.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/** Port of Tests/DiaryManagementTests.swift. */
class DiaryManagementTests {
    @Before
    fun pinLanguage() {
        // Resolving "System" reads android.os.LocaleList, which is unavailable on the JVM.
        L10n.setLanguage(AppLanguage.En)
    }

    private val document: DiaryDocument
        get() {
            val timestamp = Instant.ofEpochSecond(1_792_009_200)
            return DiaryDocument(entries = listOf(
                DiaryEntry(id = "first", title = "Tokyo live", body = "Birthday encore", tags = listOf("travel"), status = DiaryStatus.Ready,
                    event = DiaryEventContext(ledgerEventId = "shared-event", name = "Autumn Fes", date = "2026-10-12"), createdAt = timestamp, updatedAt = timestamp),
                DiaryEntry(id = "second", title = "Tokyo live", body = "Talk after the show", status = DiaryStatus.Draft,
                    event = DiaryEventContext(ledgerEventId = "shared-event", name = "Autumn Fes", date = "2026-10-13"), createdAt = timestamp, updatedAt = timestamp),
                DiaryEntry(id = "third", title = "Osaka live", status = DiaryStatus.Ready,
                    event = DiaryEventContext(date = "2026-10-14"), createdAt = timestamp, updatedAt = timestamp)
            ))
        }

    @Test
    fun testDateStatusAndSearchCombineWithInclusiveCivilDays() {
        val entries = document.entries
        assertEquals(listOf("first"), DiaryEntryFilter(startDate = "2026-10-12", endDate = "2026-10-13", status = DiaryStatus.Ready, query = "TOKYO travel").matching(entries).map { it.id })
        assertEquals(listOf("second"), DiaryEntryFilter(startDate = "2026-10-13", endDate = "2026-10-13").matching(entries).map { it.id })
        assertEquals(listOf("second"), DiaryEntryFilter(status = DiaryStatus.Draft, query = "Autumn Talk").matching(entries).map { it.id })
        assertEquals(listOf("first"), DiaryEntryFilter(endDate = "2026-10-12").matching(entries).map { it.id })
    }

    @Test
    fun testInvalidDatesReversedRangeAndMissingSearchCannotSelectAll() {
        val entries = document.entries
        assertTrue(DiaryEntryFilter(startDate = "2026-10-14", endDate = "2026-10-12").matching(entries).isEmpty())
        assertTrue(DiaryEntryFilter(endDate = "2026-02-30").matching(entries).isEmpty())
        assertTrue(DiaryEntryFilter(status = DiaryStatus.Draft, query = "Osaka").matching(entries).isEmpty())
        assertEquals(entries, DiaryEntryFilter(query = " \n ").matching(entries))
    }

    @Test
    fun testDeletingSelectedEntriesKeepsUnselectedBodyAndSharedEventContext() {
        val original = document
        val result = original.deletingEntries(setOf("first", "third", "missing"))
        assertEquals(listOf(original.entries[1]), result.entries)
        assertEquals("Talk after the show", result.entries[0].body)
        assertEquals("shared-event", result.entries[0].event.ledgerEventId)
        DiaryImporter.validate(result)
        assertEquals(result, DiaryImporter.parse(DiaryImporter.encode(result)))
        assertEquals(result, result.deletingEntries(setOf("first", "third")))
        assertEquals(original, original.deletingEntries(emptySet()))
        assertEquals(3, original.entries.size)
    }

    @Test
    fun testEmptyResultRemainsAValidBackup() {
        val original = document
        val result = original.deletingEntries(original.entries.map { it.id }.toSet())
        assertTrue(result.entries.isEmpty())
        assertEquals(result, DiaryImporter.parse(DiaryImporter.encode(result)))
    }
}
